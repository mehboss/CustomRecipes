package me.mehboss.utils.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mehboss.brewing.BrewAction;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Stores all extra data required for a custom brewing-stand recipe.
 * <p>
 * Extends {@link Recipe} by adding fields used specifically by brewing logic:
 * ingredient item, fuel item, fuel behavior, bottle requirements, brew-action
 * behavior, and slot indexing.
 * <p>
 * This is a flexible data container. Recipes may come from config, code,
 * integrations, or any other source.
 */
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
	 * Builds and stores the ingredient item used for brewing. Supports custom
	 * identifiers or vanilla material/meta.
	 *
	 * @param ingredientItem ingredient definition
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

	/** @return the brewing ingredient item. */
	public ItemStack getBrewIngredient() {
		return brewIngredient;
	}

	/**
	 * Builds and stores the fuel item used for brewing. Supports custom identifiers
	 * or vanilla material/meta.
	 *
	 * @param fuelItem fuel definition
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

	/** @return the brewing fuel item. */
	public ItemStack getBrewFuel() {
		return brewFuel;
	}

	/** Sets how this recipe behaves when brewing completes. */
	public void setBrewAction(BrewAction action) {
		brewAction = action;
	}

	/** @return the brew action assigned to this recipe. */
	public BrewAction getBrewAction() {
		return brewAction;
	}

	/** Enables or disables exact matching for ingredient and fuel. */
	public void setBrewPerfect(boolean perfect) {
		brewPerfect = perfect;
	}

	/** @return true if ingredient/fuel must match exactly. */
	public boolean isBrewPerfect() {
		return brewPerfect;
	}

	/** Sets the fuel amount added when consuming one fuel item. */
	public void setBrewFuelSet(int value) {
		brewFuelSet = value;
	}

	/** @return fuel provided per fuel item. */
	public int getBrewFuelSet() {
		return brewFuelSet;
	}

	/** Sets how much fuel is consumed per brew cycle. */
	public void setBrewFuelCharge(int value) {
		brewFuelCharge = value;
	}

	/** @return fuel consumed per brew cycle. */
	public int getBrewFuelCharge() {
		return brewFuelCharge;
	}

	/** Sets which slot is used as the ingredient slot. */
	public void setBrewIngredientSlot(int slot) {
		brewIngredientSlot = slot;
	}

	/** @return the ingredient slot index. */
	public int getBrewIngredientSlot() {
		return brewIngredientSlot;
	}

	/** Sets which slot is used as the fuel slot. */
	public void setBrewFuelSlot(int slot) {
		brewFuelSlot = slot;
	}

	/** @return the fuel slot index. */
	public int getBrewFuelSlot() {
		return brewFuelSlot;
	}

	/** Enables or disables bottle validation for brew output. */
	public void setRequiresBottles(boolean requiresBottles) {
		this.requiresBottles = requiresBottles;
	}

	/** @return true if brewing requires valid bottle items. */
	public boolean requiresBottles() {
		return requiresBottles;
	}

	/** Sets the bottle material required for this recipe's output. */
	public void setRequiredBottleType(Material type) {
		this.requiredBottleType = type;
	}

	/** @return the required bottle material. */
	public Material getRequiredBottleType() {
		return requiredBottleType;
	}
}