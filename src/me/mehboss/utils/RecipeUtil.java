package me.mehboss.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.material.MaterialData;

import com.cryptomorin.xseries.XMaterial;
import com.nexomc.nexo.api.NexoItems;
import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;

import dev.lone.itemsadder.api.CustomStack;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.th0rgal.oraxen.api.OraxenItems;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.RecipeConditions.ConditionSet;
import me.mehboss.utils.data.CookingRecipeData;
import net.Indyuce.mmoitems.MMOItems;

public class RecipeUtil {

	private HashMap<String, Recipe> recipes = new HashMap<>();
	private ArrayList<String> keyList = new ArrayList<>();
	private HashMap<String, ItemStack> custom_items = new HashMap<>();
	public List<String> SUPPORTED_PLUGINS = List.of("itemsadder", "mythicmobs", "executableitems", "oraxen", "nexo",
			"mmoitems");

	/**
	 * Adds a finished Recipe object to the API
	 * 
	 * @param recipe the Recipe object
	 * @throws InvalidRecipeException if the recipe is null or no NamespacedKey has
	 *                                been set
	 * @throws InvalidRecipeException if the crafting recipe does not have 9
	 *                                ingredients
	 * @throws InvalidRecipeException if the furnace or stone cutter has more than 1
	 *                                ingredient
	 * @throws InvalidRecipeException if the result is null or air
	 * @throws InvalidRecipeException if the row shape of the shaped recipe is null
	 */
	public void createRecipe(Recipe recipe) {
		if (recipe == null || !recipe.hasKey()) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName()
					+ ". Recipe is null or does not have a NameSpacedKey/Identifier set. Key: " + recipe.getKey();
			throw new InvalidRecipeException(errorMessage);
		}

		if ((recipe.getType() == RecipeType.SHAPED || recipe.getType() == RecipeType.SHAPELESS)
				&& recipe.getIngredientSize() != 9) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName() + ". Recipe is "
					+ recipe.getType() + " and does not have 9 ingredients! Ingredients: " + recipe.getIngredientSize();
			throw new InvalidRecipeException(errorMessage);
		}

		if (recipe.getResult() == null || recipe.getResult().getType() == Material.AIR) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName()
					+ ". The recipe result was null or not set.";
			throw new InvalidRecipeException(errorMessage);
		}

		if ((recipe.getType() == RecipeType.SHAPED || recipe.getType() == RecipeType.SHAPELESS)
				&& (recipe.getRow(1) == null || recipe.getRow(2) == null || recipe.getRow(3) == null)) {
			String errorMessage = "[CRAPI] Could not add recipe because shape cannot have null rows. Recipe: "
					+ recipe.getName();
			throw new InvalidRecipeException(errorMessage);
		}

		recipes.put(recipe.getName(), recipe);
		if (!keyList.contains(recipe.getKey()))
			keyList.add(recipe.getKey());
	}

	/**
	 * Removes a recipe registered with CR.
	 * 
	 * @param recipeName the string name of the recipe you wanting to remove.
	 */
	public void removeRecipe(String recipeName) {
		if (recipes.containsKey(recipeName)) {
			Recipe recipe = recipes.get(recipeName);
			clearDuplicates(recipe);
			keyList.remove(recipe.getKey());
			recipes.remove(recipeName);
		}
	}

	/**
	 * Loads the specified recipe to the server. Checks and corrects duplicate
	 * NamespacedKey.
	 * 
	 * @param recipe the recipe you want to load
	 */
	public void registerRecipe(Recipe recipe) {
		this.clearDuplicates(recipe);
		Main.getInstance().recipeManager.addRecipesFromAPI(recipe);
	}

	/**
	 * Resets and reloads all registered recipes, including CR recipes. Checks and
	 * corrects duplicate NamespacedKey.
	 */
	public void reloadRecipes() {
		this.clearDuplicates(null);
		Main.getInstance().recipeManager.addRecipesFromAPI(null);
	}

	public String getCustomItemPlugin(String key) {
		if (key == null)
			return null;

		String[] split = key.split(":");
		if (split.length < 2)
			return null;

		String namespace = split[0];
		return namespace;
	}

	public Boolean isCustomItem(String key) {
		if (key == null)
			return false;

		String pluginID = key.toLowerCase();
		String[] plugins = { "mythicmobs", "itemsadder", "mmoitems", "oraxen", "nexo", "executableitems" };

		for (String plugin : plugins)
			if (plugin.equals(pluginID))
				return true;

		return false;
	}

	/**
	 * Getter for a result from a namespaced key.
	 *
	 * @param key the NamespacedKey as a String (e.g., itemsadder:my_item)
	 * @return the ItemStack if found, can be null
	 */
	public ItemStack getResultFromKey(String key) {

		if (key == null)
			return null;

		// First, try to get the result from a regular recipe
		String[] split = key.split(":");
		String rawkey = split[0];

		if (getRecipeFromKey(rawkey) != null)
			return getRecipeFromKey(rawkey).getResult();
		if (ItemBuilder.get(rawkey) != null)
			return ItemBuilder.get(rawkey);

		// If not found, treat it as a custom item key
		if (split.length < 2)
			return null;

		String namespace = rawkey.toLowerCase();
		String itemId = split[1];
		String recipe = split.length > 2 ? split[2] : "";
		String item = namespace + ":" + itemId;

		if (Main.getInstance().serverVersionLessThan(1, 15)) {
			logError("Issue detected with recipe.. ", recipe);
			logError("Your server version does not support custom items from other plugins!", recipe);
			logError("You must be on 1.15 or higher!", recipe);
			return null;
		}

		switch (namespace) {
		case "itemsadder":
			if (Main.getInstance().hasCustomPlugin("itemsadder")) {
				CustomStack iaItem = CustomStack.getInstance(itemId);
				if (iaItem != null)
					return iaItem.getItemStack();
			}
			break;

		case "mythicmobs":
			if (Main.getInstance().hasCustomPlugin("mythicmobs")) {
				ItemStack mythicItem = MythicBukkit.inst().getItemManager().getItemStack(itemId);
				if (mythicItem != null)
					return mythicItem;
			}
			break;

		case "executableitems":
			if (Main.getInstance().hasCustomPlugin("executableitems")) {
				Optional<ExecutableItemInterface> ei = ExecutableItemsAPI.getExecutableItemsManager()
						.getExecutableItem(itemId);
				if (ei.isPresent()) {
					if (custom_items.containsKey(itemId)) {
						return custom_items.get(itemId);
					} else {
						ItemStack eiItem = ei.get().buildItem(1, Optional.empty());
						custom_items.put(itemId, eiItem);
						return eiItem;
					}
				}
			}
			break;

		case "oraxen":
			if (Main.getInstance().hasCustomPlugin("oraxen")) {
				ItemStack oraxenItem = OraxenItems.exists(itemId) ? OraxenItems.getItemById(itemId).build() : null;
				if (oraxenItem != null)
					return oraxenItem;
			}
			break;

		case "nexo":
			if (Main.getInstance().hasCustomPlugin("nexo")) {
				if (NexoItems.itemFromId(itemId) != null)
					return NexoItems.itemFromId(itemId).build();
			}
			break;

		case "mmoitems":
			if (split.length < 3) {
				logError("Could not complete recipe because MMOItems must specify a type, which can not be found. Key: "
						+ item, recipe);
				return null;
			}

			if (Main.getInstance().hasCustomPlugin("mmoitems")) {
				ItemStack mmoitem = MMOItems.plugin.getItem(MMOItems.plugin.getTypes().get(split[2].toUpperCase()),
						itemId.toUpperCase());
				if (mmoitem != null)
					return mmoitem;
			}
			break;
		}

		if (SUPPORTED_PLUGINS.contains(namespace) && split.length > 2) {
			if (!Main.getInstance().hasCustomPlugin(namespace)) {
				logError("Error loading recipe..", recipe);
				logError("Could not find plugin dependency needed for the referenced item! (" + item + ")", recipe);
			} else {
				logError("Error loading recipe..", recipe);
				logError("Could not find the referenced custom item! (" + item + ")", recipe);
			}
		}
		return null;
	}

	/**
	 * Getter for a key from the result ItemStack
	 * 
	 * @param item the ItemStack
	 * @return the key that is found, can be null
	 */
	public String getKeyFromResult(ItemStack item) {
		if (item == null)
			return null;

		// First, try to get a key from normal recipes
		Recipe recipe = getRecipeFromResult(item);
		if (recipe != null && !recipe.isCustomItem())
			return recipe.getKey();

		// Then, try for a custom item
		String customID = ItemBuilder.get(item);
		if (customID != null && !recipe.isCustomItem())
			return customID;

		if (Main.getInstance().hasCustomPlugin("itemsadder")) {
			CustomStack ia = CustomStack.byItemStack(item);
			if (ia != null) {
				return "itemsadder:" + ia.getId();
			}
		}

		if (Main.getInstance().hasCustomPlugin("mythicmobs")) {
			String mythicId = MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item);
			if (mythicId != null) {
				return "mythicmobs:" + mythicId;
			}
		}

		if (Main.getInstance().hasCustomPlugin("executableitems")) {
			Optional<ExecutableItemInterface> ei = ExecutableItemsAPI.getExecutableItemsManager()
					.getExecutableItem(item);
			if (ei.isPresent())
				return "executableitems:" + ei.get().getId();
		}

		if (Main.getInstance().hasCustomPlugin("oraxen")) {
			if (OraxenItems.exists(item)) {
				return "oraxen:" + OraxenItems.getIdByItem(item);
			}
		}

		if (Main.getInstance().hasCustomPlugin("nexo")) {
			if (NexoItems.idFromItem(item) != null) {
				return "nexo:" + NexoItems.idFromItem(item);
			}
		}

		if (Main.getInstance().hasCustomPlugin("mmoitems")) {
			String id = MMOItems.getID(item);
			String type = MMOItems.getTypeName(item);
			if (id != null && type != null) {
				return "mmoitems:" + id + ":" + type;
			}
		}

		return null;
	}

	/**
	 * Getter for a recipe from Namespacedkey
	 * 
	 * @param key the NamespacedKey
	 * @return the Recipe that is found, can be null
	 */
	public Recipe getRecipeFromKey(String key) {
		for (Recipe recipe : recipes.values()) {
			String recipeTag = recipe.getKey();

			if (key == null)
				return null;

			if (key.equalsIgnoreCase(recipeTag))
				return recipe;
		}
		return null;
	}

	/**
	 * Getter for a recipe from the Name
	 * 
	 * @param recipeName the recipe name
	 * @return the Recipe that is found, can be null
	 */
	public Recipe getRecipe(String recipeName) {
		return recipes.containsKey(recipeName) ? recipes.get(recipeName) : null;
	}

	/**
	 * Getter for a recipe from the result ItemStack
	 * 
	 * @param item the ItemStack
	 * @return the Recipe that is found, can be null
	 */
	public Recipe getRecipeFromResult(ItemStack item) {

		if (item == null || item.getType() == Material.AIR)
			return null;

		String id = NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				? NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				: "none";

		if (!id.equals("none"))
			return getRecipeFromKey(id);

		for (Recipe recipe : recipes.values()) {
			ItemStack result = recipe.getResult();

			if (result.equals(item) || result.isSimilar(item))
				return recipe;
		}
		return null;
	}

	/**
	 * Getter for all recipes registered
	 * 
	 * @return a hashmap of recipes, including CR recipes, can be null
	 */
	public HashMap<String, Recipe> getAllRecipes() {
		return recipes.isEmpty() ? new HashMap<String, Recipe>() : recipes;
	}

	/**
	 * Get all recipes, ordered so that any recipes whose result is similar to the
	 * given result item are first.
	 *
	 * @param shown the result item in the crafting inventory (can be null or AIR)
	 * @return ordered list of recipes
	 */
	public List<Recipe> getAllRecipesSortedByResult(ItemStack shown) {
		if (recipes == null || recipes.isEmpty()) {
			return Collections.emptyList();
		}

		boolean hasShown = shown != null && shown.getType() != Material.AIR;

		return recipes.values().stream().sorted((a, b) -> {
			if (!hasShown)
				return 0; // no prioritization if slot empty
			boolean aMatch = isSimilarSafe(shown, a.getResult());
			boolean bMatch = isSimilarSafe(shown, b.getResult());
			return Boolean.compare(bMatch, aMatch); // true before false
		}).collect(Collectors.toList());
	}

	private boolean isSimilarSafe(ItemStack a, ItemStack b) {
		if (a == null || b == null)
			return false;
		if (a.getType() == Material.AIR || b.getType() == Material.AIR)
			return false;
		return a.isSimilar(b);
	}

	/**
	 * Getter for all recipes matching a type
	 * 
	 * @param type the recipe type
	 * @return the hashmap of recipes found, can be null
	 */
	public HashMap<String, Recipe> getRecipesFromType(RecipeType type) {
		if (recipes.isEmpty())
			return null;

		HashMap<String, Recipe> foundRecipes = new HashMap<String, Recipe>();
		for (Recipe recipe : recipes.values()) {
			if (recipe.getType() == type)
				foundRecipes.put(recipe.getName(), recipe);
		}

		if (foundRecipes.isEmpty())
			return null;

		return foundRecipes;
	}

	/**
	 * Getter for all recipe results
	 * 
	 * @return an arraylist of recipe ItemStacks
	 */
	public ArrayList<ItemStack> getAllResults() {
		ArrayList<ItemStack> results = new ArrayList<>();
		for (Recipe recipe : recipes.values()) {
			ItemStack result = recipe.getResult();
			if (result != null) {
				results.add(result);
			}
		}
		return results;
	}

	/**
	 * Getter for all recipe names
	 * 
	 * @return a set of strings
	 */
	public ArrayList<String> getRecipeNames() {
		return recipes.isEmpty() ? new ArrayList<>() : new ArrayList<>(recipes.keySet());
	}

	/*
	 * ======================================================================
	 * Cooking-Related Accessors (Furnace / etc.)
	 * ======================================================================
	 */

	/**
	 * Getter for a furnace recipe by the source
	 * 
	 * @return recipe the recipe that matches
	 */
	public Recipe getRecipeFromFurnaceSource(ItemStack item) {
		HashMap<String, Recipe> furnaceRecipes = getRecipesFromType(RecipeType.FURNACE);

		if (furnaceRecipes == null)
			return null;

		for (Recipe recipes : furnaceRecipes.values()) {
			if (!(recipes instanceof CookingRecipeData))
				continue;

			CookingRecipeData cookRecipe = (CookingRecipeData) recipes;
			ItemStack sourceItem = cookRecipe.getSource();

			if (sourceItem != null && (item.equals(sourceItem) || item.isSimilar(sourceItem)))
				return recipes;
		}
		return null;
	}

	/*
	 * ======================================================================
	 * Internal Helpers (private)
	 * ======================================================================
	 */

	/**
	 * Clears all or specific registered recipe(s) for duplicate NamespacedKey,
	 * private
	 * 
	 * @param recipe removes the recipe(s) from the bukkit registry, can be null
	 */
	private void clearDuplicates(Recipe recipe) {
		if (Main.getInstance().serverVersionAtLeast(1, 16)) {
			NamespacedKey customKey = null;
			if (recipe != null) {
				String key = recipe.getKey().toLowerCase();
				customKey = NamespacedKey.fromString("customrecipes:" + key);
				if (customKey != null && Bukkit.getRecipe(customKey) != null)
					Bukkit.removeRecipe(customKey);
				return;
			}

			for (String getKey : keyList) {
				if (getKey == null)
					continue;

				String key = getKey.toLowerCase();
				customKey = NamespacedKey.fromString("customrecipes:" + key);

				if (customKey != null && Bukkit.getRecipe(customKey) != null)
					Bukkit.removeRecipe(customKey);
			}
		} else {
			if (recipe != null) {
				for (Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
					org.bukkit.inventory.Recipe r = it.next();
					if (r == null)
						continue;
					ItemStack result = r.getResult();
					if (result != null && result.isSimilar(recipe.getResult())) {
						it.remove();
					}
				}
				return;
			}
		}
	}

	private void logError(String st, String recipe) {
		Logger.getLogger("Minecraft").log(Level.WARNING,
				"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "] " + st);
	}

	/*
	 * ======================================================================
	 * Classes (Recipe, Ingredient)
	 * ======================================================================
	 */

	public static class Recipe {

		private ItemStack result;

		private List<String> commands;
		private ArrayList<String> disabledWorlds = new ArrayList<>();
		private ArrayList<Ingredient> ingredients;

		private String customItem;
		private String name;
		private String key;
		private String permission;

		private boolean exactChoice = false;
		private boolean placeable = true;
		private boolean active = true;
		private boolean ignoreData = false;
		private boolean ignoreModelData = false;
		private boolean ignoreNames = false;
		private boolean isTagged = false;
		private boolean discoverable = false;

		private boolean hasCommands = false;
		private boolean isGrantItem = true;
		private String row1;
		private String row2;
		private String row3;

		private long cooldown = 0;

		public enum RecipeType {
			SHAPELESS(), SHAPED(), STONECUTTER(), FURNACE(), ANVIL(), BLASTFURNACE("BLAST", "BLAST_FURNACE"), SMOKER(),
			CAMPFIRE(), GRINDSTONE(), BREWING_STAND("BREWING");

			private final Set<String> aliases;

			RecipeType(String... aliases) {
				this.aliases = new HashSet<>();
				for (String alias : aliases)
					this.aliases.add(alias.toUpperCase().replace(" ", "_"));
				this.aliases.add(this.name());
			}

			public static RecipeType fromString(String name) {
				if (name == null)
					return null;
				String key = name.toUpperCase().replace(" ", "_");
				for (RecipeType type : values())
					if (type.aliases.contains(key))
						return type;
				return null;
			}
		}

		private RecipeType recipeType = RecipeType.SHAPED;
		private String category = "MISC";

		/**
		 * Parameterized constructor. Initializes a Recipe object with specified name
		 *
		 * @param name the name of the recipe
		 * 
		 */
		public Recipe(String name) {
			this.name = name;
			this.ingredients = new ArrayList<>();
		}

		/**
		 * Getter for setBookCategory
		 * 
		 * @return the Category enum the recipe belongs to in the recipe book.
		 */
		public String getBookCategory() {
			return category;
		}

		/**
		 * Sets the category the recipe will be shown within the vanilla recipe book.
		 * 
		 * @param category CraftingBookCategory or CookingBookCategory, defaults to MISC
		 */
		public void setBookCategory(String category) {
			try {
				if (CraftingBookCategory.valueOf(category.toUpperCase()) != null)
					this.category = category.toUpperCase();
				if (CookingBookCategory.valueOf(category.toUpperCase()) != null)
					this.category = category.toUpperCase();
			} catch (NoClassDefFoundError e) {
			} catch (Exception e) {
			}
		}

		/**
		 * Getter for setDiscoverable
		 * 
		 * @return true if the recipe is discoverable, false otherwise.
		 */
		public boolean isDiscoverable() {
			return discoverable;
		}

		/**
		 * Sets whether or not a recipe is going to be discovered automatically
		 * 
		 * @param discoverable true or false boolean
		 */
		public void setDiscoverable(Boolean discoverable) {
			this.discoverable = discoverable;
		}

		/**
		 * Getter for whether or not a recipe result is of a custom item
		 * 
		 * @return true if the recipe output is custom, false otherwise.
		 */
		public boolean isCustomItem() {
			return customItem != null && customItem.split(":").length > 1;
		}

		/**
		 * Sets an ID for custom item results
		 * 
		 * @param customItem the ID of the custom item
		 */
		public void setCustomItem(String customItem) {
			this.customItem = customItem;
		}

		/**
		 * Getter for the custom ID output
		 * 
		 * @return the customItem ID
		 */
		public String getCustomItemID() {
			return customItem;
		}

		/**
		 * Getter for setExactChoice
		 * 
		 * @return true if the recipe is exactChoice, false otherwise.
		 */
		public boolean isExactChoice() {
			if (Main.getInstance().serverVersionAtLeast(1, 14))
				return exactChoice;

			return false;
		}

		/**
		 * Sets whether or not a recipe is going to be using exactChoice.
		 * 
		 * @param exactChoice true or false boolean
		 */
		public void setExactChoice(Boolean exactChoice) {
			if (Main.getInstance().serverVersionAtLeast(1, 14))
				this.exactChoice = exactChoice;
		}

		/**
		 * Getter for setPlaceable
		 * 
		 * @return true if the recipe can be placed down, false otherwise.
		 */
		public boolean isPlaceable() {
			return placeable;
		}

		/**
		 * Sets whether or not a recipe is allowed to be placed.
		 * 
		 * @param placeable true or false boolean
		 */
		public void setPlaceable(Boolean placeable) {
			this.placeable = placeable;
		}

		/**
		 * Getter for setActive.
		 * 
		 * @return true if the recipe is enabled, false otherwise.
		 */
		public boolean isActive() {
			return active;
		}

		/**
		 * Enables or disables a recipe
		 * 
		 * @param status true or false boolean
		 */
		public void setActive(Boolean status) {
			active = status;
		}

		/**
		 * Getter for the recipe name
		 * 
		 * @return what the recipe name is
		 */
		public String getName() {
			return name;
		}

		/**
		 * Setter for the shaped recipe rows, required
		 * 
		 * @param row   the recipe row
		 * @param shape the shape for the row (example: XDX)
		 * @throws ArrayIndexOutOfBoundsException if the integer specified is not rows
		 *                                        1-3
		 */
		public void setRow(int row, String shape) {

			switch (row) {
			case 1:
				row1 = shape;
				break;
			case 2:
				row2 = shape;
				break;
			case 3:
				row3 = shape;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("Invalid setRow usage: " + row + " (must be 1-3)");
			}
		}

		/**
		 * Getter for the shaped recipe rows
		 * 
		 * @param row the recipe row
		 * @throws ArrayIndexOutOfBoundsException if the integer specified is not rows
		 *                                        1-3
		 */
		public String getRow(int row) {
			switch (row) {
			case 1:
				return row1;
			case 2:
				return row2;
			case 3:
				return row3;
			default:
				throw new ArrayIndexOutOfBoundsException("Invalid getRow usage: " + row + " (must be 1-3)");
			}
		}

		/**
		 * Setter for the RecipeType, not required and defaults to SHAPED
		 * 
		 * @param SHAPELESS   the recipe type will be shapeless
		 * @param SHAPED      the recipe type will be shaped
		 * @param FURNACE     the recipe type will be a furnace recipe
		 * @param STONECUTTER the recipe type will be a stonecutter recipe
		 */
		public void setType(RecipeType type) {
			recipeType = type;
		}

		/**
		 * Getter for the RecipeType
		 * 
		 * @returns the RecipeType RecipeType.SHAPELESS RecipeType.SHAPED
		 *          RecipeType.FURNACE RecipeType.STONECUTTER
		 */
		public RecipeType getType() {
			return recipeType;
		}

		/**
		 * Setter for whether the result is tagged with the key
		 * 
		 * @param tagged tags if true, otherwise does not tag
		 * @throws InvalidRecipeException if the recipe result is null
		 * @throws InvalidRecipeException if there is no key found
		 */
		public void setTagged(boolean tagged) {
			if (result == null) {
				throw new InvalidRecipeException("There was no result found to tag");
			}
			if (key == null) {
				throw new InvalidRecipeException(
						"You must set a NameSpacedKey (setKey()) prior to calling setTagged()");
			} else {
				this.isTagged = tagged;
				result = NBTEditor.set(result, key, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");
			}
		}

		/**
		 * Getter for checking if the recipe is tagged with the key
		 * 
		 * @returns true if tagged, false otherwise
		 */
		public boolean isCustomTagged() {
			return this.isTagged;
		}

		/**
		 * Getter for if the recipe has a NamedspaceKey
		 * 
		 * @returns the NamedspaceKey of the recipe
		 */
		public boolean hasKey() {
			return key == null ? false : true;
		}

		/**
		 * Setter for the NamedspaceKey, required
		 * 
		 * @param key the NamedspaceKey
		 */
		public void setKey(String key) {
			this.key = key;
		}

		/**
		 * Getter for the NamedspaceKey
		 * 
		 * @returns the NamedspaceKey of the recipe
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Getter for if the recipe has a cooldown set
		 * 
		 * @returns true if the cooldown in greater than 0 seconds
		 */
		public boolean hasCooldown() {
			return cooldown > 0;
		}

		/**
		 * Setter for the recipe cooldown
		 * 
		 * @param cooldown the cooldown in seconds
		 */
		public void setCooldown(long cooldown) {
			this.cooldown = cooldown;
		}

		/**
		 * Getter for the cooldown
		 * 
		 * @returns the cooldown in seconds
		 */
		public long getCooldown() {
			return cooldown;
		}

		/**
		 * Setter for if the recipe ignores MetaData
		 * 
		 * @param ignoreData true to ignore, otherwise false
		 */
		public void setIgnoreData(boolean ignoreData) {
			this.ignoreData = ignoreData;
		}

		/**
		 * Getter for ignoreData
		 * 
		 * @returns true of the recipe ignores MetaData, false otherwise
		 */
		public boolean getIgnoreData() {
			return ignoreData;
		}

		/**
		 * Setter for if the recipe ignores names
		 * 
		 * @param ignoreNames true to ignore, otherwise false
		 */
		public void setIgnoreNames(boolean ignoreNames) {
			this.ignoreNames = ignoreNames;
		}

		/**
		 * Getter for ignoreNames
		 * 
		 * @returns true of the recipe ignores names, false otherwise
		 */
		public boolean getIgnoreNames() {
			return ignoreNames;
		}

		/**
		 * Setter for if the recipe ignores CustomModelData
		 * 
		 * @param ignoreModelData true to ignore, otherwise false
		 */
		public void setIgnoreModelData(boolean ignoreModelData) {
			this.ignoreModelData = ignoreModelData;
		}

		/**
		 * Getter for ignoreModelData
		 * 
		 * @returns true of the recipe ignores CustomMetaData, false otherwise
		 */
		public boolean getIgnoreModelData() {
			return this.ignoreModelData;
		}

		/**
		 * Setter for the recipe result, required
		 * 
		 * @param result the ItemStack for the recipe result
		 */
		public void setResult(ItemStack result) {
			if (result == null || result.getType() == Material.AIR) {
				String errorMessage = "[CRAPI] The recipe result can not be set to null or air";
				throw new InvalidRecipeException(errorMessage);
			}

			this.result = result;
		}

		/**
		 * Getter for the recipe result
		 * 
		 * @returns the ItemStack of the recipe result
		 */
		public ItemStack getResult() {
			return result;
		}

		/**
		 * Adds an Ingredient object to the recipe, required Requires 9 ingredients for
		 * crafting, 1 otherwise
		 * 
		 * @param ingredient a finalized Ingredient for the recipe
		 */
		public void addIngredient(Ingredient ingredient) {
			this.ingredients.add(ingredient);
		}

		/**
		 * Compiles all ingredient materials of a recipe
		 * 
		 * @returns an ArrayList of all ingredient types
		 */
		public ArrayList<Material> getAllIngredientTypes() {
			ArrayList<Material> types = new ArrayList<>();
			if (this.ingredients.isEmpty())
				return types;

			for (Ingredient ingredient : this.ingredients) {
				if (ingredient.isEmpty())
					continue;
				types.add(ingredient.getMaterial());

			}
			return types;
		}

		/**
		 * Getter for the recipe ingredients
		 * 
		 * @returns a list of Ingredients
		 */
		public ArrayList<Ingredient> getIngredients() {
			return ingredients;
		}

		/**
		 * Getter for the amount of ingredients the recipe has
		 * 
		 * @returns the amount of ingredients the recipe has, can be null
		 */
		public int getIngredientSize() {
			return ingredients.size();
		}

		/**
		 * Getter for an ingredient slot
		 * 
		 * @param slot integer of the slot (1-9)
		 * @returns the Ingredient found that is in the specified slot
		 * @throws ArrayIndexOutOfBoundsException if param is not 1-9
		 */
		public Ingredient getSlot(int slot) throws ArrayIndexOutOfBoundsException {
			return ingredients.get(slot - 1);
		}

		/**
		 * Setter for the permission needed to craft the recipe
		 * 
		 * @param permission the permission required
		 */
		public void setPerm(String permission) {
			this.permission = permission;
		}

		/**
		 * Getter for the permission needed to craft the recipe, can be null
		 * 
		 * @returns the permission
		 */
		public String getPerm() {
			return permission;
		}

		public boolean hasPerm() {
			return permission != null;
		}

		/**
		 * Adds a world to the noCraft list
		 * 
		 * @param world the world to not allow crafting of the recipe in
		 */
		public void addDisabledWorld(String world) {
			disabledWorlds.add(world);
		}

		/**
		 * Getter for worlds the recipe can not be crafted in
		 * 
		 * @returns an arraylist of worlds
		 */
		public ArrayList<String> getDisabledWorlds() {
			return disabledWorlds;
		}
		
		/**
		 * Sets whether the recipe output should be granted after a command is performed
		 * 
		 * @return true if the recipe is granted, false otherwise.
		 */
		public void setGrantItem(Boolean grantItem) {
			this.isGrantItem = grantItem;
		}

		/**
		 * Gets whether the recipe output should be granted after a command is performed
		 * 
		 * @return true if the recipe is granted, false otherwise.
		 */
		public boolean isGrantItem() {
			return isGrantItem;
		}

		/**
		 * Gets whether the recipe is an item or a command
		 * 
		 * @return true if the recipe is a command, false otherwise
		 */
		public boolean hasCommands() {
			return hasCommands;
		}

		/**
		 * Sets the commands to perform upon crafting a recipe
		 *
		 * @param commands the list of command strings
		 */
		public void setCommands(List<String> commands) {
			this.commands = commands;
			this.hasCommands = true;
		}

		/**
		 * Gets the commands to perform upon crafting a recipe
		 *
		 * @return the list of commands to be performed
		 */
		public List<String> getCommand() {
			return commands;
		}
	}

	public static class Ingredient {
		private ItemStack item;
		private Material material;
		private MaterialData materialData;
		private List<Material> materialChoices = new ArrayList<>();

		private String displayName = "false";
		private String abbreviation;
		private String identifier;
		private int slot = 0;
		private int amount = 1;
		private int modelData = -1;

		/**
		 * Parameterized constructor. Initializes an Ingredient object with specified
		 * abbreviation and material
		 *
		 * @param abbreviation the letter of the ingredient
		 * @param material     the Material type for the ingredient, can be null or air
		 */
		public Ingredient(String abbreviation, Material material) {
			this.material = material;
			this.abbreviation = abbreviation;
		}

		/**
		 * Setter for the itemstack of the ingredient
		 * 
		 * @param item the itemstack for the ingredient to have, not required
		 */
		public void setItem(ItemStack item) {
			if (item == null || item.getType() == Material.AIR) {
				String errorMessage = "[CRAPI] The ingredient result can not be set to null or air";
				throw new InvalidRecipeException(errorMessage);
			}

			this.item = item;
		}

		/**
		 * Getter for an ingredient itemstack
		 * 
		 * @returns the ingredient itemstack, can be null
		 */
		public ItemStack getItem() {
			return item;
		}

		/**
		 * Setter for the ingredients displayname
		 * 
		 * @param displayname the name the ingredient is required to have
		 */
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		/**
		 * Setter for the identifier of the ingredient Requires the ingredient to be
		 * tagged with another Custom Recipe
		 * 
		 * @param identifier the identifier/tag the ingredient is required to have
		 */
		public void setIdentifier(String identifier) {
			if (identifier != null && !(identifier.equalsIgnoreCase("none")) && !identifier.isEmpty())
				this.identifier = identifier;
		}

		/**
		 * Getter for an ingredients identifier
		 * 
		 * @returns the ingredient identifier, can be null
		 */
		public String getIdentifier() {
			return identifier;
		}

		/**
		 * Getter for if the ingredient has an identifier set
		 * 
		 * @returns true if the ingredient has an identifier, false otherwise
		 */
		public boolean hasIdentifier() {
			return identifier != null;
		}

		/**
		 * Setter for the slot the ingredient is set to, not required for shapeless
		 * 
		 * @param slot the inventory slot the ingredient is set to
		 * @throws ArrayIndexOutOfBoundsException if the slot is not within 1-9
		 */
		public void setSlot(int slot) {
			if (slot < 1 || slot > 9)
				throw new ArrayIndexOutOfBoundsException("Invalid setSlot usage: " + slot + " (must be 1-9)");

			this.slot = slot;
		}

		/**
		 * Getter for the ingredient slot
		 * 
		 * @returns the ingredient slot the ingredient is set to
		 */
		public int getSlot() {
			return slot;
		}

		/**
		 * Setter for the amount requirement
		 * 
		 * @param amount the amount of the ingredient the recipe requires
		 */
		public void setAmount(int amount) {
			this.amount = amount;
		}

		/**
		 * Getter for the amount requirement
		 * 
		 * @returns the amount of the ingredient the recipe requires
		 */
		public int getAmount() {
			return amount;
		}

		/**
		 * Getter for checking if the ingredient is empty
		 * 
		 * @returns true if the ingredient is AIR or NULL, false otherwise
		 */
		public boolean isEmpty() {
			return material == null || material == Material.AIR;
		}

		/**
		 * Getter for the ingredient displayname
		 * 
		 * @returns the displayname of the ingredient, can be null
		 */
		public String getDisplayName() {
			if (displayName == null)
				return displayName;

			return ChatColor.translateAlternateColorCodes('&', displayName);
		}

		/**
		 * Getter for if the ingredient has a displayname set
		 * 
		 * @returns true if the ingredient has one, false otherwise
		 */
		public boolean hasDisplayName() {
			return displayName != null && !displayName.equalsIgnoreCase("false")
					&& !displayName.equalsIgnoreCase("none") && !displayName.equalsIgnoreCase("null")
					&& !displayName.isEmpty();
		}

		/**
		 * Getter for the material of the ingredient
		 * 
		 * @returns the material of the ingredient, AIR if null
		 */
		public Material getMaterial() {
			return material == null ? Material.AIR : material;
		}

		/**
		 * Setter for the material of the ingredient
		 *
		 * This should NEVER be used, except internally!
		 * 
		 * @param material the material to be used
		 */
		public void setMaterial(Material material) {
			this.material = material;
		}

		/**
		 * Gets the legacy {@link MaterialData} of this ingredient.
		 *
		 * <p>
		 * Only valid on Minecraft versions prior to 1.13. On newer versions this will
		 * always return {@code null}.
		 * </p>
		 *
		 * @return the legacy {@link MaterialData}, or {@code null} if not set
		 * @deprecated Use modern {@link org.bukkit.inventory.RecipeChoice} or
		 *             {@link org.bukkit.Material} APIs instead. MaterialData was
		 *             removed after Minecraft 1.13.
		 */
		@Deprecated
		public MaterialData getMaterialData() {
			return materialData;
		}

		/**
		 * Sets the legacy {@link MaterialData} for this ingredient.
		 *
		 * <p>
		 * Only valid on Minecraft versions prior to 1.13. On newer versions this call
		 * has no effect.
		 * </p>
		 *
		 * @param data the legacy {@link MaterialData}
		 * @deprecated Use modern {@link org.bukkit.inventory.RecipeChoice} or
		 *             {@link org.bukkit.Material} APIs instead. MaterialData was
		 *             removed after Minecraft 1.13.
		 */
		@Deprecated
		public void setMaterialData(MaterialData data) {
			materialData = data;
		}

		/**
		 * Adds an additional valid {@link Material} for this ingredient.
		 *
		 * <p>
		 * When material choices are defined, the ingredient will match if the input
		 * item's material is any of those added here.
		 * </p>
		 *
		 * @param material a valid material option for this ingredient
		 */
		public void addMaterialChoice(Material material) {
			materialChoices.add(material);
		}

		/**
		 * Gets all additional materials that may satisfy this ingredient.
		 *
		 * @return a list of valid material choices
		 */
		public List<Material> getMaterialChoices() {
			return materialChoices.isEmpty() ? Collections.singletonList(material) : materialChoices;
		}

		/**
		 * Checks whether this ingredient has legacy {@link MaterialData}.
		 *
		 * <p>
		 * Always returns {@code false} on Minecraft versions 1.13 and newer.
		 * </p>
		 *
		 * @return {@code true} if legacy {@link MaterialData} is set and running on a
		 *         legacy server, otherwise {@code false}
		 * @deprecated Use modern {@link org.bukkit.inventory.RecipeChoice} or
		 *             {@link org.bukkit.Material} APIs instead. MaterialData was
		 *             removed after Minecraft 1.13.
		 */
		@Deprecated
		public boolean hasMaterialData() {
			if (materialData == null || Main.getInstance().serverVersionAtLeast(1, 13))
				return false;

			return true;
		}

		/**
		 * Checks whether this ingredient has multiple valid materials.
		 *
		 * @return true if one or more material choices are defined
		 */
		public boolean hasMaterialChoices() {
			return materialChoices.size() > 1;
		}

		/**
		 * Getter for the abbreviation of the ingredient, like G for grass Used when
		 * shaping the recipe
		 * 
		 * @returns the abbreviation of the ingredient
		 */
		public String getAbbreviation() {
			return abbreviation;
		}

		/**
		 * Getter for if an ingredient has CMD
		 * 
		 * @returns true if the recipe has CMD, false otherwise
		 */
		public Boolean hasCustomModelData() {
			if (modelData == -1)
				return false;

			return true;
		}

		/**
		 * Setter for an ingredients custom model data
		 * 
		 * @param data the custom model data of the ingredient
		 */
		public void setCustomModelData(int data) {
			this.modelData = data;
		}

		/**
		 * Getter for an ingredients model data
		 * 
		 * @returns an int representing the custom model data
		 */
		public int getCustomModelData() {
			return modelData;
		}
	}
}