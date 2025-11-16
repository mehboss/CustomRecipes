package me.mehboss.utils.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mehboss.brewing.BrewAction;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;

/* ======================================================================
 * Brewing Recipes - Specialized Subclass
 * ====================================================================== */
public class BrewingRecipeData extends Recipe {
	private ItemStack brewIngredient;
	private ItemStack brewFuel;
	private BrewAction brewAction;
	private boolean brewPerfect = true;
	private int brewFuelSet = 40;
	private int brewFuelCharge = 10;
	private int brewIngredientSlot = 3;
	private int brewFuelSlot = 4;

	private boolean requiresBottles = false;
	private Material requiredBottleType = Material.POTION;

	public BrewingRecipeData(String name) {
		super(name);
	}

	/**
	 * Builds and set the ingredient item (slot 1).
	 */
	public void setBrewIngredient(Ingredient ingredientItem) {
		ItemStack ingredient = null;

		if (ingredientItem.hasIdentifier()) {
			ingredient = Main.getInstance().getRecipeUtil().getResultFromKey(ingredientItem.getIdentifier());
		}

		if (ingredient == null) {
			ingredient = new ItemStack(ingredientItem.getMaterial());
			ItemMeta meta = ingredient.getItemMeta();

			if (ingredientItem.hasDisplayName()) {
				meta.setDisplayName(ingredientItem.getDisplayName());
			}
			if (ingredientItem.hasCustomModelData()) {
				meta.setCustomModelData(ingredientItem.getCustomModelData());
			}
			ingredient.setItemMeta(meta);
		}

		brewIngredient = ingredient;
	}

	/**
	 * Gets the ingredient item (slot 1).
	 */
	public ItemStack getBrewIngredient() {
		return brewIngredient;
	}

	/**
	 * Builds and sets the fuel item (slot 2).
	 */
	public void setBrewFuel(Ingredient fuelItem) {
		ItemStack fuel = null;

		if (fuelItem.hasIdentifier()) {
			fuel = Main.getInstance().getRecipeUtil().getResultFromKey(fuelItem.getIdentifier());
		}

		if (fuel == null) {
			fuel = new ItemStack(fuelItem.getMaterial());
			ItemMeta meta = fuel.getItemMeta();

			if (fuelItem.hasDisplayName()) {
				meta.setDisplayName(fuelItem.getDisplayName());
			}
			if (fuelItem.hasCustomModelData()) {
				meta.setCustomModelData(fuelItem.getCustomModelData());
			}
			fuel.setItemMeta(meta);
		}

		brewFuel = fuel;
	}

	/**
	 * Gets the fuel item (slot 1).
	 */
	public ItemStack getBrewFuel() {
		return brewFuel;
	}

	// --- existing brew settings below ---

	public void setBrewAction(BrewAction action) {
		brewAction = action;
	}

	public BrewAction getBrewAction() {
		return brewAction;
	}

	public void setBrewPerfect(boolean perfect) {
		brewPerfect = perfect;
	}

	public boolean isBrewPerfect() {
		return brewPerfect;
	}

	public void setBrewFuelSet(int value) {
		brewFuelSet = value;
	}

	public int getBrewFuelSet() {
		return brewFuelSet;
	}

	public void setBrewFuelCharge(int value) {
		brewFuelCharge = value;
	}

	public int getBrewFuelCharge() {
		return brewFuelCharge;
	}

	public void setBrewIngredientSlot(int slot) {
		brewIngredientSlot = slot;
	}

	public int getBrewIngredientSlot() {
		return brewIngredientSlot;
	}

	public void setBrewFuelSlot(int slot) {
		brewFuelSlot = slot;
	}

	public int getBrewFuelSlot() {
		return brewFuelSlot;
	}
	
	public void setRequiresBottles(boolean requiresBottles) {
	    this.requiresBottles = requiresBottles;
	}

	public boolean requiresBottles() {
	    return requiresBottles;
	}

	public void setRequiredBottleType(Material type) {
	    this.requiredBottleType = type;
	}

	public Material getRequiredBottleType() {
	    return requiredBottleType;
	}
}