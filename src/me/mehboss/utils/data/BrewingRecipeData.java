package me.mehboss.utils.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.mehboss.brewing.BrewAction;
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
	 * Stores the ingredient item used for brewing.
	 *
	 * @param item the item used as the ingredient
	 */
	public void setBrewIngredient(ItemStack item) {
		this.brewIngredient = item;
	}

	/**
	 * Gets the brewing ingredient item.
	 *
	 * @return ingredient item created for this recipe
	 */
	public ItemStack getBrewIngredient() {
		return brewIngredient;
	}

	/**
	 * Stores the fuel item used for brewing.
	 *
	 * @param fuel the fuel item
	 */
	public void setBrewFuel(ItemStack fuel) {
		this.brewFuel = fuel;
	}

	/**
	 * Gets the brewing fuel item.
	 *
	 * @return fuel item created for this recipe
	 */
	public ItemStack getBrewFuel() {
		return brewFuel;
	}

	/**
	 * Sets how this recipe behaves when brewing completes.
	 *
	 * @param action the brewing action to perform on completion
	 */
	public void setBrewAction(BrewAction action) {
		brewAction = action;
	}

	/**
	 * Gets the brew action executed when brewing finishes.
	 *
	 * @return assigned brew action
	 */
	public BrewAction getBrewAction() {
		return brewAction;
	}

	/**
	 * Enables or disables exact matching for ingredient and fuel items.
	 *
	 * @param perfect true to require exact item matching
	 */
	public void setBrewPerfect(boolean perfect) {
		brewPerfect = perfect;
	}

	/**
	 * Checks if ingredient and fuel must match exactly.
	 *
	 * @return true if strict matching is enabled
	 */
	public boolean isBrewPerfect() {
		return brewPerfect;
	}

	/**
	 * Sets how much internal fuel is added when consuming a fuel item.
	 *
	 * @param value amount of internal fuel to add
	 */
	public void setBrewFuelSet(int value) {
		brewFuelSet = value;
	}

	/**
	 * Gets how much internal fuel is added per fuel item.
	 *
	 * @return fuel added per item
	 */
	public int getBrewFuelSet() {
		return brewFuelSet;
	}

	/**
	 * Sets how much internal fuel is consumed per brewing cycle.
	 *
	 * @param value amount of fuel consumed
	 */
	public void setBrewFuelCharge(int value) {
		brewFuelCharge = value;
	}

	/**
	 * Gets how much fuel is consumed per brew cycle.
	 *
	 * @return fuel consumption amount
	 */
	public int getBrewFuelCharge() {
		return brewFuelCharge;
	}

	/**
	 * Sets which slot is used for the ingredient.
	 *
	 * @param slot inventory slot index
	 */
	public void setBrewIngredientSlot(int slot) {
		brewIngredientSlot = slot;
	}

	/**
	 * Gets the slot index used for the ingredient.
	 *
	 * @return ingredient slot index
	 */
	public int getBrewIngredientSlot() {
		return brewIngredientSlot;
	}

	/**
	 * Sets which slot is used for the fuel.
	 *
	 * @param slot inventory slot index
	 */
	public void setBrewFuelSlot(int slot) {
		brewFuelSlot = slot;
	}

	/**
	 * Gets the slot index used for the fuel.
	 *
	 * @return fuel slot index
	 */
	public int getBrewFuelSlot() {
		return brewFuelSlot;
	}

	/**
	 * Enables or disables bottle validation for brewing results.
	 *
	 * @param requiresBottles true to require bottle-type slots
	 */
	public void setRequiresBottles(boolean requiresBottles) {
		this.requiresBottles = requiresBottles;
	}

	/**
	 * Checks whether bottle validation is required.
	 *
	 * @return true if valid bottle items must be present
	 */
	public boolean requiresBottles() {
		return requiresBottles;
	}

	/**
	 * Sets the type of bottle required for valid brewing output.
	 *
	 * @param type the material representing the required bottle
	 */
	public void setRequiredBottleType(Material type) {
		this.requiredBottleType = type;
	}

	/**
	 * Gets the required bottle type for this recipe.
	 *
	 * @return bottle material required
	 */
	public Material getRequiredBottleType() {
		return requiredBottleType;
	}
}