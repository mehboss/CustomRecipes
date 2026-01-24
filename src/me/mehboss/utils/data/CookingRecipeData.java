package me.mehboss.utils.data;

import org.bukkit.inventory.ItemStack;
import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Stores extra data for custom furnace-based recipes.
 * <p>
 * Extends {@link Recipe} by adding furnace-specific fields such as source item,
 * cook time, and experience yield. This class is a simple data container and
 * does not enforce how recipes are created.
 */
public class CookingRecipeData extends Recipe {

	private ItemStack source;
	private int cookTime = 200;
	private float experience = 1.0f;

	public CookingRecipeData(String name) {
		super(name);
	}

	/**
	 * Stores the source item used in a furnace.
	 *
	 * @param item the item used as the furnace source.
	 */
	public void setFurnaceSource(ItemStack item) {
		this.source = item;
	}

	/**
	 * Gets the input item used by this cooking recipe.
	 *
	 * @return the source item
	 */
	public ItemStack getSource() {
		return source;
	}

	/**
	 * Sets the cook time in ticks. (20 ticks = 1 second)
	 *
	 * @param cookTime the number of ticks required to cook
	 */
	public void setCookTime(int cookTime) {
		this.cookTime = cookTime;
	}

	/**
	 * Gets how long the recipe takes to cook.
	 *
	 * @return cook time in ticks
	 */
	public int getCookTime() {
		return cookTime;
	}

	/**
	 * Sets the experience granted when the recipe is completed.
	 *
	 * @param experience the XP amount awarded
	 */
	public void setExperience(float experience) {
		this.experience = experience;
	}

	/**
	 * Gets the experience value awarded when the item is cooked.
	 *
	 * @return experience amount
	 */
	public float getExperience() {
		return experience;
	}
}