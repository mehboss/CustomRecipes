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
import org.bukkit.potion.PotionData;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.gui.framework.GuiButton;
import me.mehboss.gui.framework.GuiButton.GuiStringButton;
import me.mehboss.gui.framework.GuiButton.GuiToggleButton;
import me.mehboss.gui.framework.GuiView;
import me.mehboss.gui.framework.RecipeLayout;
import me.mehboss.recipe.Main;
import me.mehboss.recipe.RecipeBuilder;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.libs.CompatibilityUtil;
import me.mehboss.utils.libs.XItemStack;
import me.mehboss.utils.libs.XItemStack.Serializer;

public class RecipeSaver {

	/**
	 * Saves the contents of a recipe GUI to its YAML file.
	 *
	 * @param view   The active GuiView (editing view)
	 * @param player The player who clicked "Update"
	 * @param recipe The logical recipe being edited/created
	 */
	public void saveRecipe(GuiView view, Player player, Recipe recipe) {

		Inventory inv = view.getInventory();
		RecipeType type = recipe.getType();

		if (type == null) {
			player.sendMessage(ChatColor.RED + "Recipe has no type set; cannot save.");
			return;
		}

		RecipeLayout layout = RecipeLayout.forType(type);
		int[] ingredientSlots = layout.getIngredientSlots();
		int resultSlot = layout.getResultSlot();

		String recipeName = readStringField(view, "Name", null);
		if (recipeName == null || recipeName.trim().isEmpty() || recipeName.equalsIgnoreCase("none")
				|| recipeName.trim().equals("New Recipe")) {
			player.sendMessage(ChatColor.RED + "Recipe name is empty; cannot save.");
			return;
		}

		String id = readStringField(view, "Identifier", null);
		if (id == null || id.isEmpty() || id.equalsIgnoreCase("null") || id.equalsIgnoreCase("none")) {
			player.sendMessage(ChatColor.RED + "Identifier must be set; cannot save.");
			return;
		}

		// Ensure there is at least one ingredient and a result
		boolean matrixEmpty = true;
		for (int slot : ingredientSlots) {
			if (!isEmpty(inv, slot)) {
				matrixEmpty = false;
				break;
			}
		}

		if (matrixEmpty || isEmpty(inv, resultSlot)) {
			player.sendMessage(ChatColor.RED + "Recipe matrix or result is empty; nothing to save.");
			return;
		}

		// Prepare recipes folder
		File recipesFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipesFolder.exists()) {
			recipesFolder.mkdirs();
		}

		File recipeFile = new File(recipesFolder, recipeName + ".yml");
		FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

		Map<String, Object> recipeData = convertToRecipeConfig(view, inv, recipe, type, layout);
		recipeConfig.set(recipeName, recipeData);

		try {
			recipeConfig.save(recipeFile);
			player.sendMessage(ChatColor.GREEN + "Recipe saved successfully to " + recipeFile.getName() + "!");
		} catch (IOException e) {
			e.printStackTrace();
			player.sendMessage(ChatColor.RED + "Failed to save the recipe to " + recipeFile.getName() + ".");
		}

		player.closeInventory();
		if (getRecipeUtil().getRecipe(recipeName) != null)
			getRecipeUtil().removeRecipe(recipeName);

		getRecipeBuilder().addRecipes(recipeName);
	}

	private Map<String, Object> convertToRecipeConfig(GuiView view, Inventory inventory, Recipe recipe, RecipeType type,
			RecipeLayout layout) {

		LinkedHashMap<String, Object> cfg = new LinkedHashMap<>();

		int[] ingredientSlots = layout.getIngredientSlots();
		int resultSlot = layout.getResultSlot();

		// ====== Result item ======
		ItemStack resultItem = safeGet(inventory, resultSlot);
		int resultAmount = (resultItem != null && resultItem.getAmount() > 0) ? resultItem.getAmount() : 1;

		// ====== High-level flags ======
		boolean enabled = readEnabledToggle(view);
		boolean shapeless = readBooleanToggle(view, "Shapeless", false);
		boolean exactChoice = readBooleanToggle(view, "Exact-Choice", true);
		boolean placeable = readBooleanToggle(view, "Placeable", false);
		boolean isLegacyNames = readBooleanToggle(view, "Legacy-Names", true);
		boolean isCustomTagged = readBooleanToggle(view, "Custom-Tagged", true);

		// ====== Textual fields ======
		String identifier = readIdentifier(view, recipe.getName());
		String permission = readStringField(view, "Permission", "none");
		String group = readStringField(view, "Group", "none");
		if ("none".equalsIgnoreCase(group))
			group = "";

		String displayNameColored = readNameColored(resultItem, isLegacyNames);
		// ====== Effects (currently unused, keep as empty list) ======
		List<String> effects = new ArrayList<>(); // or from button "Effects" later

		// ====== Enchantments & lore ======
		List<String> enchantList = readEnchantList(resultItem);
		AtomicBoolean resultHasID = new AtomicBoolean(false);
		String material = getItemValue(resultItem, identifier, resultHasID);
		List<String> lore = getItemLoreList(resultItem);
		if (lore == null)
			lore = new ArrayList<>();

		// ====== Ingredient mapping / pattern ======
		Map<ItemStack, String> letters = setItemLetters(inventory, ingredientSlots);
		List<String> itemCrafting = generateItemCrafting(letters, inventory, ingredientSlots);
		LinkedHashMap<String, Object> ingredients = buildIngredientsFromLetters(inventory, letters, ingredientSlots,
				isLegacyNames);

		String converterType = (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS) ? "none" : type.toString();

		boolean isCooking = type == RecipeType.FURNACE || type == RecipeType.BLASTFURNACE || type == RecipeType.SMOKER
				|| type == RecipeType.CAMPFIRE;
		boolean hasRepairCost = type == RecipeType.GRINDSTONE || type == RecipeType.ANVIL;
		boolean isBrewing = type == RecipeType.BREWING_STAND;
		boolean isCrafting = type == RecipeType.SHAPED || type == RecipeType.SHAPELESS;

		// ====== Base fields (mostly unchanged from original) ======
		cfg.put("Enabled", enabled);

		if (isCrafting)
			cfg.put("Shapeless", shapeless);
		if (isCrafting || type == RecipeType.STONECUTTER)
			cfg.put("Group", group);

		cfg.put("Cooldown", -1);
		cfg.put("Placeable", placeable);
		cfg.put("Use-Display-Name", isLegacyNames);
		cfg.put("Exact-Choice", exactChoice);
		cfg.put("Identifier", identifier);

		// Converter: use recipe type; crafting types use "none" like before
		cfg.put("Converter", converterType);

		// ---- Cooking: Cook-Time & Experience ----
		if (isCooking) {
			Integer cookTime = readIntField(view, "Cook-Time");
			if (cookTime != null) {
				cfg.put("Cook-Time", cookTime);
			}
		}

		if (isCooking || type == RecipeType.GRINDSTONE) {
			Double exp = readDoubleField(view, "Experience");
			if (exp != null) {
				cfg.put("Experience", exp);
			}
		}

		// ---- Workstation: Repair-Cost ----
		if (hasRepairCost) {
			Integer repairCost = readIntField(view, "Repair-Cost");
			if (repairCost != null) {
				cfg.put("Repair-Cost", repairCost);
			}
		}

		// ---- Brewing: Required-Items, Fuel-Set, Fuel-Charge ----
		if (isBrewing) {
			Integer fuelSet = readIntField(view, "Fuel-Set");
			if (fuelSet != null) {
				cfg.put("FuelSet", fuelSet);
			}

			Integer fuelCharge = readIntField(view, "Fuel-Charge");
			if (fuelCharge != null) {
				cfg.put("FuelCharge", fuelCharge);
			}

			boolean requiresItems = readBooleanToggle(view, "Required-Items", false);
			cfg.put("Required-Items", requiresItems);
		}

		cfg.put("Permission", permission);
		cfg.put("Auto-Discover-Recipe", true);
		cfg.put("Book-Category", "MISC");

		// handles "result" configuration section
		Map<String, Object> result = new LinkedHashMap<>();
		Serializer serializer = XItemStack.serializer().fromItem(resultItem);
		if (resultHasID.get()) {
			result.put("Item", material);

		} else if (serializer != null) {
			Map<String, Object> serializedMap = serializer.writeToMap();
			serializedMap.put("custom-tagged", isCustomTagged);
			result = serializedMap;

		} else {
			// DEPRECATED
			// MARKED FOR REMOVAL
			result.put("Item", material);
			if (!resultHasID.get()) {
				result.put("Item-Damage", "none");
				result.put("Durability", (resultItem != null) ? resultItem.getDurability() : 0);
				result.put("Amount", resultAmount);
				result.put("Name",
						(displayNameColored != null && !displayNameColored.isEmpty()) ? displayNameColored : "none");
				result.put("Lore", lore);
				result.put("Hide-Enchants", false);
				result.put("Enchantments", enchantList);
				result.put("Custom-Tagged", isCustomTagged);
				result.put("Custom-Model-Data", "none");
				result.put("Item-Flags", new ArrayList<>());
				result.put("Attribute", new ArrayList<>());
				result.put("Custom-Tags", new ArrayList<>());
			}
		}

		// adds the section back to parent
		cfg.put("Result", result);

		if (!isBrewing) {
			cfg.put("Effects", effects);

			Map<String, Object> commands = new LinkedHashMap<>();
			commands.put("Give-Item", true);
			commands.put("Run-Commands", new ArrayList<>());
			cfg.put("Commands", commands);
		}

		cfg.put("ItemCrafting", itemCrafting);
		cfg.put("Ingredients", ingredients);

		cfg.put("ItemsLeftover", new ArrayList<>());

		Map<String, Object> flags = new LinkedHashMap<>();
		flags.put("Ignore-Data", false);
		flags.put("Ignore-Model-Data", false);
		flags.put("Ignore-Name", false);
		cfg.put("Flags", flags);

		cfg.put("Use-Conditions", false);
		cfg.put("Conditions_All", new ArrayList<>());
		cfg.put("Disabled-Worlds", new ArrayList<>());

		return cfg;
	}

	private GuiStringButton getStringButton(GuiView view, String fieldName) {
		Inventory inv = view.getInventory();
		int size = inv.getSize();
		for (int slot = 0; slot < size; slot++) {
			GuiButton b = view.getButton(slot);
			GuiStringButton sb = b instanceof GuiStringButton ? (GuiStringButton) b : null;
			if (sb != null && sb.getFieldName().equalsIgnoreCase(fieldName)) {
				return sb;
			}
		}
		return null;
	}

	private GuiToggleButton getToggleButton(GuiView view, String fieldName) {
		Inventory inv = view.getInventory();
		int size = inv.getSize();
		for (int slot = 0; slot < size; slot++) {
			GuiButton b = view.getButton(slot);
			GuiToggleButton tb = b instanceof GuiToggleButton ? (GuiToggleButton) b : null;
			if (tb != null && tb.getFieldName().equalsIgnoreCase(fieldName)) {
				return tb;
			}
		}
		return null;
	}

	private boolean readEnabledToggle(GuiView view) {
		// Enabled uses field name "Enabled" and a SLIME_BALL/SNOWBALL icon
		GuiToggleButton btn = getToggleButton(view, "Enabled");
		if (btn == null) {
			return true; // default to enabled
		}
		ItemStack icon = btn.getIcon();
		if (icon == null || !icon.hasItemMeta()) {
			return true;
		}
		String name = ChatColor.stripColor(icon.getItemMeta().getDisplayName());
		if (name == null)
			return true;
		String lc = name.toLowerCase();
		if (lc.contains(getParsedValue("Buttons.Toggle-False", "&cfalse")))
			return false;
		if (lc.contains(getParsedValue("Buttons.Toggle-True", "&atrue")))
			return true;
		return true;
	}

	private boolean readBooleanToggle(GuiView view, String fieldName, boolean defaultVal) {
		GuiToggleButton btn = getToggleButton(view, fieldName);
		if (btn == null)
			return defaultVal;
		ItemStack icon = btn.getIcon();
		if (icon == null || !icon.hasItemMeta())
			return defaultVal;
		String name = ChatColor.stripColor(icon.getItemMeta().getDisplayName());
		if (name == null)
			return defaultVal;
		String lc = name.toLowerCase();
		if (lc.contains(getParsedValue("Buttons.Toggle-True", "&atrue")))
			return true;
		if (lc.contains(getParsedValue("Buttons.Toggle-False", "&cfalse")))
			return false;
		return defaultVal;
	}

	private String readIdentifier(GuiView view, String fallbackName) {
		String id = readStringField(view, "Identifier", null);
		if (id != null && !id.isEmpty() && !id.equalsIgnoreCase("null") && !id.equalsIgnoreCase("none"))
			return id;
		return fallbackName;
	}

	private String readStringField(GuiView view, String fieldName, String defaultVal) {
		GuiStringButton btn = getStringButton(view, fieldName);
		if (btn == null)
			return defaultVal;

		ItemStack icon = btn.getIcon();
		if (icon == null || !icon.hasItemMeta())
			return defaultVal;
		ItemMeta im = icon.getItemMeta();

		if (im.hasLore() && im.getLore() != null && !im.getLore().isEmpty()) {
			return ChatColor.stripColor(im.getLore().get(0));
		}
		if (im.hasDisplayName()) {
			return ChatColor.stripColor(im.getDisplayName());
		}
		return defaultVal;
	}

	private Integer readIntField(GuiView view, String fieldName) {
		GuiStringButton btn = getStringButton(view, fieldName);
		if (btn == null)
			return null;
		ItemStack icon = btn.getIcon();
		if (icon == null || !icon.hasItemMeta())
			return null;
		ItemMeta im = icon.getItemMeta();
		String src = null;

		if (im.hasLore() && im.getLore() != null && !im.getLore().isEmpty()) {
			src = ChatColor.stripColor(im.getLore().get(0));
		} else if (im.hasDisplayName()) {
			src = ChatColor.stripColor(im.getDisplayName());
		}

		if (src == null)
			return null;

		src = src.replaceAll("[^0-9-]", ""); // strip non-digits
		if (src.isEmpty())
			return null;

		try {
			return Integer.parseInt(src);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double readDoubleField(GuiView view, String fieldName) {
		GuiStringButton btn = getStringButton(view, fieldName);
		if (btn == null)
			return null;
		ItemStack icon = btn.getIcon();
		if (icon == null || !icon.hasItemMeta())
			return null;
		ItemMeta im = icon.getItemMeta();
		String src = null;

		if (im.hasLore() && im.getLore() != null && !im.getLore().isEmpty()) {
			src = ChatColor.stripColor(im.getLore().get(0));
		} else if (im.hasDisplayName()) {
			src = ChatColor.stripColor(im.getDisplayName());
		}

		if (src == null)
			return null;

		src = src.replaceAll("[^0-9+\\-\\.]", "");
		if (src.isEmpty())
			return null;

		try {
			return Double.parseDouble(src);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String readNameColored(ItemStack item, boolean isLegacyNames) {
		if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null)
			return null;
		ItemMeta im = item.getItemMeta();
		String customName = CompatibilityUtil.hasDisplayname(im, isLegacyNames)
				? CompatibilityUtil.getDisplayname(im, isLegacyNames)
				: "none";
		String displayName = im.hasDisplayName() ? im.getDisplayName() : "none";

		return (displayName != null && !"none".equals(displayName)) ? displayName : customName;
	}

	private LinkedHashMap<String, Object> buildIngredientsFromLetters(Inventory inv, Map<ItemStack, String> letters,
			int[] ingredientSlots, boolean isLegacyNames) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		Set<String> seen = new HashSet<>();

		for (int i = 0; i < ingredientSlots.length; i++) {
			int slotIndex = ingredientSlots[i];
			ItemStack stack = inv.getItem(slotIndex);

			if (stack == null || stack.getType() == Material.AIR)
				continue;

			String letter = letters.get(stack);

			if (letter == null || seen.contains(letter))
				continue;
			seen.add(letter);

			// Get the identifier for the ingredient
			String identifier = findIngredientIdentifier(stack);
			if (identifier == null || identifier.isEmpty() || identifier.equals("none")) {
				Serializer serializer = XItemStack.serializer().fromItem(stack);
				if (serializer == null)
					continue;

				Map<String, Object> serializedMap = serializer.writeToMap();
				out.put(letter, serializedMap);
			} else {
				LinkedHashMap<String, Object> ing = new LinkedHashMap<>();
				ing.put("Material", stack.getType().name());
				ing.put("Identifier", identifier);
				out.put(letter, ing);
			}
		}
		return out;
	}

	private Map<ItemStack, String> setItemLetters(Inventory inventory, int[] ingredientSlots) {
		Map<ItemStack, String> itemToLetterMap = new HashMap<>();
		Set<String> usedLetters = new HashSet<>();

		for (int slotIndex : ingredientSlots) {
			ItemStack current = inventory.getItem(slotIndex);

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

	public List<String> generateItemCrafting(Map<ItemStack, String> ingredients, Inventory inv, int[] ingredientSlots) {
		List<String> itemCrafting = new ArrayList<>(Arrays.asList("XXX", "XXX", "XXX"));

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

	private boolean isEmpty(Inventory inv, int slot) {
		ItemStack it = safeGet(inv, slot);
		return it == null || it.getType() == Material.AIR;
	}

	private ItemStack safeGet(Inventory inv, int slot) {
		try {
			return inv.getItem(slot);
		} catch (Exception ex) {
			return null;
		}
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

	String getValue(String path, String def) {
		String val = getConfig().getString("gui." + path);
		return (val == null || val.isEmpty()) ? def : val;
	}

	String getParsedValue(String msg, String def) {
		return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getValue(msg, def)));
	}

	RecipeBuilder getRecipeBuilder() {
		return Main.getInstance().recipeBuilder;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
}