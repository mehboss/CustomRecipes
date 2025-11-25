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
import java.util.concurrent.atomic.AtomicBoolean;

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
import me.mehboss.commands.CommandRemove;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class RecipeSaver {

	// GUI slot constants
	private static final int SLOT_IDENTIFIER = 9;
	private static final int SLOT_PLACEABLE_TOGGLE = 8;
	private static final int SLOT_RESULT = 24;
	private static final int SLOT_PERMISSION = 26;
	private static final int SLOT_ENABLED_TOGGLE = 53;
	private static final int SLOT_GROUP = 51;
	private static final int SLOT_SHAPELESS_TOGGLE = 52;
	private static final int SLOT_EXACTCHOICE_TOGGLE = 18;
	private static final int SLOT_CONVERTER = 17;

	// Crafting grid slots (keep the same as before)
	private static final int[] CRAFT_SLOTS = { 11, 12, 13, 20, 21, 22, 29, 30, 31 };
	private static final int[] COOKING_SLOTS = { 11, 29 };
	private static final int[] OTHER_SLOTS = { 20, 22 };
	private static int[] CONTROL_SLOTS = CRAFT_SLOTS;

	// Save the recipe map to the configuration file
	public void saveRecipe(Inventory inv, Player p, String coloredName) {

		String recipeName = ChatColor.stripColor(coloredName).replace("EDITING:", "").trim().replace(" ", "_");
		File recipesFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipesFolder.exists()) {
			recipesFolder.mkdirs();
		}

		if (recipeName == null || recipeName.isEmpty() || recipeName.equals("null") || recipeName.equals(" "))
			return;

		String rawConverter = readLoreTag(inv);
		RecipeType type = rawConverter.equals("none") ? RecipeType.SHAPED : RecipeType.valueOf(rawConverter);
		boolean showShapeless = (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS);
		boolean isCooking = (type == RecipeType.FURNACE || type == RecipeType.BLASTFURNACE || type == RecipeType.SMOKER
				|| type == RecipeType.CAMPFIRE);

		if (!showShapeless) {
			CONTROL_SLOTS = OTHER_SLOTS;
			if (isCooking)
				CONTROL_SLOTS = COOKING_SLOTS;
		}

		boolean matrixEmpty = true;
		boolean slotEmpty = inv.getItem(SLOT_RESULT) == null || inv.getItem(SLOT_RESULT).getType() == Material.AIR;
		for (int slot : CONTROL_SLOTS) {
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

		Main.getInstance().recipeManager.addRecipes(recipeName);
		p.closeInventory();
	}

	public Map<String, Object> convertToRecipeConfig(Inventory inventory, String recipeName) {
		// Use LinkedHashMap so YAML preserves our field order (matching your example)
		LinkedHashMap<String, Object> cfg = new LinkedHashMap<>();

		// ====== Result item ======
		ItemStack resultItem = safeGet(inventory, SLOT_RESULT);
		int resultAmount = (resultItem != null && resultItem.getAmount() > 0) ? resultItem.getAmount() : 1;

		// ====== Read GUI toggles and text ======
		AtomicBoolean resultHasID = new AtomicBoolean(false);
		boolean enabled = readEnabledToggle(inventory);
		boolean shapeless = readBooleanToggle(inventory, SLOT_SHAPELESS_TOGGLE);
		boolean exactchoice = readBooleanToggle(inventory, SLOT_EXACTCHOICE_TOGGLE);
		boolean placeable = readBooleanToggle(inventory, SLOT_PLACEABLE_TOGGLE);
		String identifier = readIdentifier(inventory, resultItem, recipeName);
		String permission = readPermission(inventory, identifier);
		String displayNameColored = readNameColored(inventory);
		String group = readFirstLoreOrNameStripped(inventory, SLOT_GROUP);
		String rawConverterType = readLoreTag(inventory);
		String converterType = rawConverterType.equals("SHAPED") || rawConverterType.equals("SHAPELESS") ? "none"
				: rawConverterType;
		List<String> enchantList = readEnchantList(resultItem);
		Map<ItemStack, String> letters = setItemLetters(inventory);
		List<String> itemCrafting = generateItemCrafting(letters, inventory);
		List<String> lore = getItemLoreList(resultItem);
		if (lore == null)
			lore = new ArrayList<>();

		LinkedHashMap<String, Object> ingredients = buildIngredientsFromLetters(inventory, letters);

		cfg.put("Enabled", enabled);
		cfg.put("Shapeless", shapeless);
		cfg.put("Group", group);
		cfg.put("Cooldown", -1);

		cfg.put("Item", getItemValue(resultItem, identifier, resultHasID));
		cfg.put("Item-Damage", "none");
		cfg.put("Amount", resultAmount);

		cfg.put("Placeable", placeable);
		cfg.put("Exact-Choice", exactchoice);
		cfg.put("Custom-Tagged", false);
		cfg.put("Durability", resultItem.getDurability());

		cfg.put("Identifier", identifier);
		cfg.put("Converter", converterType);
		cfg.put("Permission", permission);

		cfg.put("Auto-Discover-Recipe", true);
		cfg.put("Book-Category", "MISC");

		if (resultHasID.get()) {
			cfg.put("Name", "none");
			cfg.put("Lore", new ArrayList<>());

		} else {
			if (displayNameColored != null && !displayNameColored.isEmpty()) {
				cfg.put("Name", displayNameColored);
			} else {
				cfg.put("Name", "none");
			}

			cfg.put("Lore", lore);
		}

		cfg.put("Effects", new ArrayList<>());
		cfg.put("Hide-Enchants", true);
		cfg.put("Enchantments", enchantList);

		cfg.put("Commands.Give-Item", true);
		cfg.put("Commands.Run-Commands", new ArrayList<>());

		cfg.put("ItemCrafting", itemCrafting);
		cfg.put("Ingredients", ingredients);

		cfg.put("ItemsLeftover", new ArrayList<>());

		cfg.put("Flags.Ignore-Data", false);
		cfg.put("Flags.Ignore-Model-Data", false);
		cfg.put("Flags.Ignore-Name", false);

		cfg.put("Use-Conditions", false);
		cfg.put("Conditions_All", new ArrayList<>());

		cfg.put("Custom-Tags", new ArrayList<>());
		cfg.put("Item-Flags", new ArrayList<>());
		cfg.put("Attribute", new ArrayList<>());
		cfg.put("Custom-Model-Data", "none");
		cfg.put("Disabled-Worlds", new ArrayList<>());

		return cfg;
	}

	private String getItemValue(ItemStack item, String id, AtomicBoolean hasID) {
		if (item == null || item.getType() == Material.AIR)
			return "AIR"; // fallback for empty slots

		// Try custom key from RecipeUtil (for plugin-based custom items)
		String key = Main.getInstance().getRecipeUtil().getKeyFromResult(item);
		if (key != null && !key.isEmpty() && !key.equals(id)) {
			hasID.set(true);
			return key;
		}

		// Fallback to Material name
		return item.getType().toString();
	}

	private LinkedHashMap<String, Object> buildIngredientsFromLetters(Inventory inv, Map<ItemStack, String> letters) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();

		Set<String> seen = new HashSet<>();

		for (int i = 0; i < CONTROL_SLOTS.length; i++) {
			ItemStack stack = inv.getItem(CONTROL_SLOTS[i]);
			if (stack == null || stack.getType() == Material.AIR)
				continue;

			String letter = letters.get(stack);
			if (letter == null || seen.contains(letter))
				continue;
			seen.add(letter);

			LinkedHashMap<String, Object> ing = new LinkedHashMap<>();
			ing.put("Material", stack.getType().name());
			ing.put("Identifier", findIngredientIdentifier(stack));
			ing.put("Amount", Math.max(1, stack.getAmount()));
			ing.put("Name", getIngredientNamePlain(stack));
			ing.put("Custom-Model-Data", "none");

			out.put(letter, ing);
		}
		return out;
	}

	private Map<ItemStack, String> setItemLetters(Inventory inventory) {
		int[] ingredientSlots = CONTROL_SLOTS;
		Map<ItemStack, String> itemToLetterMap = new HashMap<>();
		Set<String> usedLetters = new HashSet<>();

		for (int i = 0; i < ingredientSlots.length; i++) {
			ItemStack current = inventory.getItem(ingredientSlots[i]);

			if (current != null && current.getType() != Material.AIR) {
				ItemStack matchingItem = null;
				for (ItemStack key : itemToLetterMap.keySet()) {
					if (key.isSimilar(current)) {
						matchingItem = key;
						break;
					}
				}

				if (matchingItem != null) {
					itemToLetterMap.put(current, itemToLetterMap.get(matchingItem));
				} else {
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
		return "X";
	}

	public List<String> generateItemCrafting(Map<ItemStack, String> ingredients, Inventory inv) {
		List<String> itemCrafting = new ArrayList<>(Arrays.asList("XXX", "XXX", "XXX"));
		int[] ingredientSlots = CONTROL_SLOTS;

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

	private boolean readEnabledToggle(Inventory inv) {
		ItemStack toggle = safeGet(inv, SLOT_ENABLED_TOGGLE);
		if (toggle == null || toggle.getType() == Material.AIR || !toggle.hasItemMeta()) {
			return true; // default to enabled
		}
		String name = ChatColor.stripColor(toggle.getItemMeta().getDisplayName());
		return name != null && name.toLowerCase().contains("enabled") && !name.toLowerCase().contains("disabled");
	}

	private String readLoreTag(Inventory inv) {
		ItemStack converterItem = safeGet(inv, SLOT_CONVERTER);
		if (converterItem == null || converterItem.getType() == Material.AIR || !converterItem.hasItemMeta()
				|| !converterItem.getItemMeta().hasLore()) {
			return "none"; // default to enabled
		}
		String name = ChatColor.stripColor(converterItem.getItemMeta().getLore().get(0));
		return name;
	}

	private boolean readBooleanToggle(Inventory inv, int slot) {
		ItemStack toggle = safeGet(inv, slot);
		if (toggle == null || toggle.getType() == Material.AIR || !toggle.hasItemMeta()) {
			return false;
		}
		String name = ChatColor.stripColor(toggle.getItemMeta().getDisplayName());
		if (name != null) {
			String lc = name.toLowerCase();
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
		if (id != null && !id.isEmpty() && !id.equals("null") && !id.equals("none"))
			return id;

		return fallbackName;
	}

	private String readPermission(Inventory inv, String identifier) {
		String perm = readFirstLoreOrNameStripped(inv, SLOT_PERMISSION);
		if (perm == null || perm.isEmpty() || "none".equalsIgnoreCase(perm)) {
			perm = "none";
		}
		return perm;
	}

	// KEEP colors for Name
	private String readNameColored(Inventory inv) {
		ItemStack item = safeGet(inv, SLOT_RESULT);
		if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null)
			return null;
		ItemMeta im = item.getItemMeta();
		String customName = CompatibilityUtil.hasDisplayname(im) ? CompatibilityUtil.getDisplayname(im) : "none";
		String displayName = im.hasDisplayName() ? im.getDisplayName() : "none";

		// returns displayname if exists, otherwise returns item name or null.
		return displayName != null && !displayName.equals("none") ? displayName : customName;
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
		return "none";
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

	// Ingredient identifier (recipe ref or NBT), default "none"
	private String findIngredientIdentifier(ItemStack item) {
		if (item == null)
			return "none";

		String key = Main.getInstance().getRecipeUtil().getKeyFromResult(item);
		if (key != null)
			return key;

		String nbt = getCustomIdentifier(item);
		return (nbt != null && !"none".equalsIgnoreCase(nbt)) ? nbt : "none";
	}

	// Ingredient plain display name (no colors) or "none"
	private String getIngredientNamePlain(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return "none";
		ItemMeta im = item.getItemMeta();
		String customName = CompatibilityUtil.hasDisplayname(im) ? CompatibilityUtil.getDisplayname(im) : "none";
		String displayName = im.hasDisplayName() ? im.getDisplayName() : "none";

		// returns displayname if exists, otherwise returns item name or null.
		return displayName != null && !displayName.equals("none") ? displayName : customName;
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