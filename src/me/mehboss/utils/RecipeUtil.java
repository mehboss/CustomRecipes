package me.mehboss.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;

public class RecipeUtil {
	private HashMap<String, Recipe> recipes = new HashMap<>();
	private ArrayList<String> keyList = new ArrayList<>();

	public void createRecipe(Recipe recipe) {
		if (recipe == null || !recipe.hasKey()) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName()
					+ ". Recipe is null or does not have a NameSpacedKey set. Key: " + recipe.getKey();
			throw new InvalidRecipeException(errorMessage);
		}

		if (recipe.getType() != RecipeUtil.Recipe.RecipeType.SHAPED
				&& recipe.getType() != RecipeUtil.Recipe.RecipeType.SHAPELESS && recipe.getIngredientSize() != 9) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName() + ". Recipe is "
					+ recipe.getType() + " and does not have 9 ingredients! Ingredients: " + recipe.getIngredientSize();
			throw new InvalidRecipeException(errorMessage);
		}

		if ((recipe.getType() == RecipeUtil.Recipe.RecipeType.FURNACE
				|| recipe.getType() == RecipeUtil.Recipe.RecipeType.STONECUTTER) && recipe.getIngredientSize() != 1) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName() + ". Recipe is "
					+ recipe.getType() + " and has more than 1 ingredient! Ingredients: " + recipe.getIngredientSize();
			throw new InvalidRecipeException(errorMessage);
		}

		if (recipe.getResult() == null || recipe.getResult().getType() == Material.AIR) {
			String errorMessage = "[CRAPI] Could not add recipe: " + recipe.getName()
					+ ". The recipe result was null or not set.";
			throw new InvalidRecipeException(errorMessage);
		}

		if (recipe.getRow(1) == null || recipe.getRow(2) == null || recipe.getRow(3) == null) {
			String errorMessage = "[CRAPI] Could not add recipe because shape cannot have null rows. Recipe: "
					+ recipe.getName();
			throw new InvalidRecipeException(errorMessage);
		}

		recipes.put(recipe.getName(), recipe);
		keyList.add(recipe.getKey());
	}

	public void removeRecipe(String recipeName) {
		recipes.remove(recipeName);
	}

	public Recipe getRecipeFromResult(ItemStack item) {
		for (Recipe recipe : recipes.values()) {
			ItemStack result = recipe.getResult();

			if (result.equals(item))
				return recipe;
		}
		return null;
	}

	public Recipe getRecipeFromKey(String key) {
		for (Recipe recipe : recipes.values()) {
			String recipeTag = recipe.getKey();

			if (key.equals(recipeTag))
				return recipe;
		}
		return null;
	}

	public Recipe getRecipe(String recipeName) {
		return recipes.get(recipeName);
	}

	public HashMap<String, Recipe> getAllRecipes() {
		return recipes;
	}

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

	public Set<String> getRecipeNames() {
		return recipes.keySet();
	}

	public void addRecipes() {
		Main.getInstance().plugin.addRecipesFromAPI();
	}

	public void reloadRecipes() {
		this.clearDuplicates();
		this.addRecipes();
	}

	private void clearDuplicates() {
		if (Main.getInstance().serverVersionAtLeast(1, 12))
			for (String getKey : keyList) {
				if (getKey == null)
					continue;

				String key = getKey.toLowerCase();
				NamespacedKey customKey = NamespacedKey.fromString("customrecipes:" + key);

				if (customKey != null && Bukkit.getRecipe(customKey) != null)
					Bukkit.removeRecipe(customKey);
			}
	}

	public static class Recipe {

		private ItemStack result;

		private ArrayList<String> disabledWorlds = new ArrayList<>();
		private ArrayList<Ingredient> ingredients;

		private String name;
		private String key;
		private String permission;

		private boolean bucketConsume = true;
		private boolean active = true;
		private boolean ignoreData = false;
		private boolean ignoreModelData = false;
		private boolean isTagged = false;

		private String row1;
		private String row2;
		private String row3;

		private int cookTime = 200;
		private float furnaceExperience = 1.0f;

		public enum RecipeType {
			SHAPELESS, SHAPED, STONECUTTER, FURNACE;
		}

		private RecipeType recipeType = RecipeType.SHAPED;

		public Recipe(String name) {
			this.name = name;
			this.ingredients = new ArrayList<>();
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(Boolean status) {
			active = status;
		}

		public String getName() {
			return name;
		}

		public void setRow(int i, String row) {

			switch (i) {
			case 1:
				row1 = row;
				break;
			case 2:
				row2 = row;
				break;
			case 3:
				row3 = row;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("Invalid setRow usage: " + i + " (must be 1-3)");
			}
		}

		public String getRow(int i) {
			switch (i) {
			case 1:
				return row1;
			case 2:
				return row2;
			case 3:
				return row3;
			default:
				throw new ArrayIndexOutOfBoundsException("Invalid getRow usage: " + i + " (must be 1-3)");
			}
		}

		public void setType(RecipeType type) {
			recipeType = type;
		}

		public RecipeType getType() {
			return recipeType;
		}

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

		public boolean isCustomTagged(boolean tagged) {
			return this.isTagged;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public boolean hasKey() {
			return key == null ? false : true;
		}

		public void setIgnoreData(boolean ignoreData) {
			this.ignoreData = ignoreData;
		}

		public boolean getIgnoreData() {
			return ignoreData;
		}

		public void setIgnoreModelData(boolean ignoreModelData) {
			this.ignoreModelData = ignoreModelData;
		}

		public boolean getIgnoreModelData() {
			return this.ignoreModelData;
		}

		public void setResult(ItemStack result) {
			if (result == null || result.getType() == Material.AIR) {
				String errorMessage = "[CRAPI] The recipe result can not be set to null or air";
				throw new InvalidRecipeException(errorMessage);
			}

			this.result = result;
		}

		public ItemStack getResult() {
			return result;
		}

		public void addIngredient(Ingredient ingredient) {
			this.ingredients.add(ingredient);
		}

		public ArrayList<Ingredient> getIngredients() {
			return ingredients;
		}

		public int getIngredientSize() {
			return ingredients.size();
		}

		public void setCookTime(int cooktime) {
			this.cookTime = cooktime;
		}

		public int getCookTime() {
			return cookTime;
		}

		public void setExperience(float experience) {
			this.furnaceExperience = experience;
		}

		public float getExperience() {
			return furnaceExperience;
		}

		public Ingredient getSlot(int i) throws ArrayIndexOutOfBoundsException {
			return ingredients.get(i - 1);
		}

		public void setPerm(String permission) {
			this.permission = permission;
		}

		public String getPerm() {
			return permission;
		}

		public void addDisabledWorld(String world) {
			disabledWorlds.add(world);
		}

		public ArrayList<String> getDisabledWorlds() {
			return disabledWorlds;
		}

		public void setConsume(Boolean arg) {
			this.bucketConsume = arg;
		}

		public Boolean isConsume() {
			return bucketConsume;
		}
	}

	public static class Ingredient {
		private Material material;

		private String abbreviation;
		private String displayName;
		private String identifier;
		private int slot = 0;
		private int amount = 1;

		public Ingredient(String abbreviation, Material material) {
			this.material = material;
			this.abbreviation = abbreviation;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public void setIdentifier(String identifier) {
			if (!(identifier.equalsIgnoreCase("none")))
				this.identifier = identifier;
		}

		public void setSlot(int slot) {
			this.slot = slot;
		}

		public int getSlot() {
			// subtracts one to grab the actual slot (0-8 rather than 1-9)
			return slot;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public int getAmount() {
			return amount;
		}

		public boolean isEmpty() {
			return material == null || material == Material.AIR;
		}

		public String getDisplayName() {
			return ChatColor.translateAlternateColorCodes('&', displayName);
		}

		public boolean hasDisplayName() {
			return displayName == null ? false : true;
		}

		public String getIdentifier() {
			return identifier;
		}

		public boolean hasIdentifier() {
			return identifier == null ? false : true;
		}

		public Material getMaterial() {
			return material;
		}

		public String getAbbreviation() {
			return abbreviation;
		}
	}
}