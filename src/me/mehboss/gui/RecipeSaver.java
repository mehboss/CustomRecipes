package me.mehboss.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe;

public class RecipeSaver {

	// GUI slot constants
	private static final int SLOT_IDENTIFIER = 7;
	private static final int SLOT_PLACEABLE_TOGGLE = 8;
	private static final int SLOT_RESULT = 23;
	private static final int SLOT_PERMISSION = 25;
	private static final int SLOT_NAME = 23;
	private static final int SLOT_ENABLED_TOGGLE = 40;
	private static final int SLOT_SHAPELESS_TOGGLE = 53;

	// Crafting grid slots (keep the same as before)
	private static final int[] CRAFT_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };

	// Save the recipe map to the configuration file
	public void saveRecipe(Inventory inv, Player p, String recipeName) {

		File recipesFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipesFolder.exists()) {
			recipesFolder.mkdirs();
		}

		if (recipeName == null || recipeName.isEmpty() || recipeName.equals("null") || recipeName.equals(" "))
			return;

		boolean matrixEmpty = true;
		boolean slotEmpty = inv.getItem(SLOT_RESULT) == null || inv.getItem(SLOT_RESULT).getType() == Material.AIR;
		for (int slot : CRAFT_SLOTS) {
			if (inv.getItem(slot) != null && inv.getItem(slot).getType() != Material.AIR) {
				matrixEmpty = false;
				break;
			}
		}

		if (matrixEmpty || slotEmpty) {
			return;
		}

		File recipeFile = new File(recipesFolder, recipeName + ".yml");
		FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

		Map<String, Object> recipeData = convertToRecipeConfig(inv, recipeName);
		recipeConfig.set(recipeName, recipeData);

		try {
			recipeConfig.save(recipeFile);
			p.sendMessage(ChatColor.GREEN + "Recipe saved successfully to " + recipeFile.getName() + "!");
		} catch (IOException e) {
			e.printStackTrace();
			p.sendMessage(ChatColor.RED + "Failed to save the recipe to " + recipeFile.getName() + ".");
		}

		Main.getInstance().reload();
		p.closeInventory();
	}

	public Map<String, Object> convertToRecipeConfig(Inventory inventory, String recipeName) {
		// Use LinkedHashMap so YAML preserves our field order (matching your example)
		LinkedHashMap<String, Object> cfg = new LinkedHashMap<>();

		// ====== Result item ======
		ItemStack resultItem = safeGet(inventory, SLOT_RESULT);
		Material resultMat = (resultItem != null && resultItem.getType() != Material.AIR) ? resultItem.getType()
				: Material.AIR;
		int resultAmount = (resultItem != null && resultItem.getAmount() > 0) ? resultItem.getAmount() : 1;

		// ====== Read GUI toggles and text ======
		boolean enabled = readEnabledToggle(inventory);
		boolean shapeless = readBooleanToggle(inventory, SLOT_SHAPELESS_TOGGLE, "Shapeless");
		boolean placeable = readBooleanToggle(inventory, SLOT_PLACEABLE_TOGGLE, "Placeable");
		String identifier = readIdentifier(inventory, resultItem, recipeName);
		String permission = readPermission(inventory, identifier);
		String displayNameColored = readNameColored(inventory); // KEEP colors

		// ====== Name & Lore (keep colors only for these) ======
		List<String> lore = getItemLoreList(resultItem);
		if (lore == null)
			lore = new ArrayList<>();

		// ====== Enchantments on result -> ["NAME:LEVEL", ...] ======
		List<String> enchantList = readEnchantList(resultItem);

		// ====== Ingredient lettering (KEEP your previous approach) ======
		// 1) Map ItemStack -> Letter with dedupe via isSimilar (same as before)
		Map<ItemStack, String> letters = setItemLetters(inventory);
		// 2) Build ItemCrafting rows from those letters (same as before)
		List<String> itemCrafting = generateItemCrafting(letters, inventory);
		// 3) Build Ingredients map (letter -> primitives) — NOT ItemStacks
		LinkedHashMap<String, Object> ingredients = buildIngredientsFromLetters(inventory, letters);

		// =======================
		// Write fields IN ORDER
		// =======================
		cfg.put("Enabled", enabled);
		cfg.put("Shapeless", shapeless);
		cfg.put("Cooldown", 60);

		cfg.put("Item", resultMat.name());
		cfg.put("Item-Damage", "none");
		cfg.put("Amount", resultAmount);

		cfg.put("Placeable", placeable);
		cfg.put("Ignore-Data", false);
		cfg.put("Ignore-Model-Data", false);

		// Defaults matching your schema
		cfg.put("Multi-Resulted", false);
		cfg.put("Exact-Choice", false);
		cfg.put("Custom-Tagged", false);
		cfg.put("Durability", "100");

		cfg.put("Identifier", identifier);
		cfg.put("Converter", "none");
		cfg.put("Permission", permission);

		cfg.put("Auto-Discover-Recipe", true);
		cfg.put("Book-Category", "MISC");

		// KEEP colors
		if (displayNameColored != null && !displayNameColored.isEmpty()) {
			cfg.put("Name", displayNameColored);
		} else {
			cfg.put("Name", "none");
		}
		cfg.put("Lore", lore); // KEEP colored lore from result (or [])

		cfg.put("Effects", new ArrayList<>()); // hook your effects GUI if applicable
		cfg.put("Hide-Enchants", true);
		cfg.put("Enchantments", enchantList);

		cfg.put("ItemCrafting", itemCrafting);
		cfg.put("Ingredients", ingredients);

		cfg.put("Custom-Tags", new ArrayList<>());
		cfg.put("Item-Flags", new ArrayList<>());
		cfg.put("Attribute", new ArrayList<>());
		cfg.put("Custom-Model-Data", "none");
		cfg.put("Disabled-Worlds", new ArrayList<>());

		return cfg;
	}

	// ====== Build Ingredients using your existing lettering map (A..Z, dedupe by
	// isSimilar) ======
	private LinkedHashMap<String, Object> buildIngredientsFromLetters(Inventory inv, Map<ItemStack, String> letters) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		// We want to maintain the same order as letters were assigned. Since the
		// original
		// map came from iterating slots in order, we’ll re-scan those slots and add new
		// letters only once.
		Set<String> seen = new HashSet<>();

		for (int i = 0; i < CRAFT_SLOTS.length; i++) {
			ItemStack stack = inv.getItem(CRAFT_SLOTS[i]);
			if (stack == null || stack.getType() == Material.AIR)
				continue;

			String letter = letters.get(stack);
			if (letter == null || seen.contains(letter))
				continue;
			seen.add(letter);

			LinkedHashMap<String, Object> ing = new LinkedHashMap<>();
			ing.put("Material", stack.getType().name());
			ing.put("Identifier", findIngredientIdentifier(stack)); // "none" if not a custom result
			ing.put("Amount", Math.max(1, stack.getAmount()));
			ing.put("Name", getIngredientNamePlain(stack)); // plain (no colors), like your example

			out.put(letter, ing);
		}
		return out;
	}

	// ====== Your original lettering logic (kept) ======
	private Map<ItemStack, String> setItemLetters(Inventory inventory) {
		int[] ingredientSlots = CRAFT_SLOTS;
		Map<ItemStack, String> itemToLetterMap = new HashMap<>();
		Set<String> usedLetters = new HashSet<>();

		for (int i = 0; i < ingredientSlots.length; i++) {
			ItemStack current = inventory.getItem(ingredientSlots[i]);

			if (current != null && current.getType() != Material.AIR) {
				// Check if we've already seen an item similar to this one
				ItemStack matchingItem = null;
				for (ItemStack key : itemToLetterMap.keySet()) {
					if (key.isSimilar(current)) {
						matchingItem = key;
						break;
					}
				}

				if (matchingItem != null) {
					// Use the same letter
					itemToLetterMap.put(current, itemToLetterMap.get(matchingItem));
				} else {
					// New letter
					String letter = getUniqueLetter(usedLetters);
					usedLetters.add(letter);
					itemToLetterMap.put(current, letter);
				}
			}
		}
		return itemToLetterMap;
	}

	private String getUniqueLetter(Set<String> usedLetters) {
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			String letter = String.valueOf(ch);
			if (!usedLetters.contains(letter)) {
				return letter;
			}
		}
		return "X"; // fallback
	}

	public List<String> generateItemCrafting(Map<ItemStack, String> ingredients, Inventory inv) {
		List<String> itemCrafting = new ArrayList<>(Arrays.asList("XXX", "XXX", "XXX"));
		int[] ingredientSlots = CRAFT_SLOTS; // mapping from inventory slots to crafting grid positions

		// Update the crafting pattern with the ingredient letters
		for (int i = 0; i < ingredientSlots.length; i++) {
			int slot = ingredientSlots[i];
			ItemStack stack = inv.getItem(slot);
			if (stack == null || stack.getType() == Material.AIR)
				continue;

			String letter = ingredients.get(stack);
			if (letter == null)
				continue;

			int row = i / 3;
			int col = i % 3;

			String currentRow = itemCrafting.get(row);
			currentRow = currentRow.substring(0, col) + letter + currentRow.substring(col + 1);
			itemCrafting.set(row, currentRow);
		}

		return itemCrafting;
	}

	// ====== Readers / helpers ======

	private boolean readEnabledToggle(Inventory inv) {
		ItemStack toggle = safeGet(inv, SLOT_ENABLED_TOGGLE);
		if (toggle == null || toggle.getType() == Material.AIR || toggle.getItemMeta() == null) {
			return true; // default to enabled
		}
		String name = ChatColor.stripColor(toggle.getItemMeta().getDisplayName());
		return name != null && name.toLowerCase().contains("enabled") && !name.toLowerCase().contains("disabled");
	}

	private boolean readBooleanToggle(Inventory inv, int slot, String labelPrefix) {
		ItemStack toggle = safeGet(inv, slot);
		if (toggle == null || toggle.getType() == Material.AIR || toggle.getItemMeta() == null) {
			return false;
		}
		String name = ChatColor.stripColor(toggle.getItemMeta().getDisplayName());
		if (name != null) {
			String lc = name.toLowerCase();
			if (lc.startsWith(labelPrefix.toLowerCase())) {
				return lc.contains("true");
			}
			if (lc.contains("true"))
				return true;
			if (lc.contains("false"))
				return false;
		}
		return false;
	}

	private String readIdentifier(Inventory inv, ItemStack result, String fallbackName) {
		// Prefer slot 7 lore first line or display name, STRIPPED
		String id = readFirstLoreOrNameStripped(inv, SLOT_IDENTIFIER);
		if (id != null && !id.isEmpty() && !"none".equalsIgnoreCase(id))
			return id;

		// Fallbacks: result NBT identifier, then result display name stripped, then
		// recipe name
		String nbt = getCustomIdentifier(result);
		if (nbt != null && !"none".equalsIgnoreCase(nbt))
			return nbt;

		String dn = getItemName(result);
		if (dn != null && !"none".equalsIgnoreCase(dn))
			return ChatColor.stripColor(dn);

		return ChatColor.stripColor(fallbackName);
	}

	private String readPermission(Inventory inv, String identifier) {
		String perm = readFirstLoreOrNameStripped(inv, SLOT_PERMISSION);
		if (perm == null || perm.isEmpty() || "none".equalsIgnoreCase(perm)) {
			perm = "crecipe.recipe." + identifier;
		}
		return perm;
	}

	// KEEP colors for Name
	private String readNameColored(Inventory inv) {
		ItemStack item = safeGet(inv, SLOT_NAME);
		if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null)
			return null;
		ItemMeta im = item.getItemMeta();
		return im.hasDisplayName() ? im.getDisplayName() : null;
	}

	// STRIP colors helper for IDs / permissions
	private String readFirstLoreOrNameStripped(Inventory inv, int slot) {
		ItemStack item = safeGet(inv, slot);
		if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null)
			return null;
		ItemMeta im = item.getItemMeta();
		if (im.hasLore() && im.getLore() != null && !im.getLore().isEmpty()) {
			return ChatColor.stripColor(im.getLore().get(0));
		}
		if (im.hasDisplayName()) {
			return ChatColor.stripColor(im.getDisplayName());
		}
		return null;
	}

	private ItemStack safeGet(Inventory inv, int slot) {
		try {
			return inv.getItem(slot);
		} catch (Exception ex) {
			return null;
		}
	}

	// KEEP colors: result item lore list (or null)
	public List<String> getItemLoreList(ItemStack itemStack) {
		if (itemStack != null && itemStack.hasItemMeta()) {
			ItemMeta meta = itemStack.getItemMeta();
			if (meta != null && meta.hasLore()) {
				return new ArrayList<>(meta.getLore());
			}
		}
		return null;
	}

	// Result display name (may include colors)
	public String getItemName(ItemStack itemStack) {
		return (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
				? itemStack.getItemMeta().getDisplayName()
				: "none";
	}

	// Ingredient identifier (recipe ref or NBT), default "none"
	private String findIngredientIdentifier(ItemStack item) {
		if (item == null)
			return "none";
		Recipe r = Main.getInstance().getRecipeUtil().getRecipeFromResult(item);
		if (r != null && r.getKey() != null && !r.getKey().isEmpty())
			return r.getKey();

		String nbt = getCustomIdentifier(item);
		return (nbt != null && !"none".equalsIgnoreCase(nbt)) ? nbt : "none";
	}

	// Ingredient plain display name (no colors) or "none"
	private String getIngredientNamePlain(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return "none";
		ItemMeta im = item.getItemMeta();
		if (im.hasDisplayName()) {
			return ChatColor.stripColor(im.getDisplayName());
		}
		return "none";
	}

	public String getCustomIdentifier(ItemStack itemStack) {
		if (itemStack != null && NBTEditor.contains(itemStack, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			return NBTEditor.getString(itemStack, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");
		}
		return "none";
	}

	private List<String> readEnchantList(ItemStack item) {
		List<String> list = new ArrayList<>();
		if (item == null || !item.hasItemMeta())
			return list;
		ItemMeta meta = item.getItemMeta();
		if (meta == null || meta.getEnchants() == null || meta.getEnchants().isEmpty())
			return list;
		for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
			String name = e.getKey().getName();
			if (name == null || name.isEmpty()) {
				name = e.getKey().getKey().getKey().toUpperCase();
			}
			list.add(name + ":" + e.getValue());
		}
		return list;
	}
}