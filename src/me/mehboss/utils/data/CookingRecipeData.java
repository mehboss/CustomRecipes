package me.mehboss.utils.data;

import org.bukkit.inventory.ItemStack;


import me.mehboss.utils.RecipeUtil.Recipe;
/**
 * Stores extra data for custom furnace-based recipes.
 * <p>
 * Extends {@link Recipe} by adding furnace-specific fields such as
 * source item, cook time, and experience yield. This class is a simple
 * data container and does not enforce how recipes are created.
 */
public class CookingRecipeData extends Recipe {

    private ItemStack source;
    private int cookTime = 200;
    private float experience = 1.0f;

    public CookingRecipeData(String name) {
        super(name);
    }

    /** Sets the input item that this recipe smelts or cooks. */
    public void setSource(ItemStack source) {
        this.source = source;
    }

    /** @return the item used as the furnace input. */
    public ItemStack getSource() {
        return source;
    }

    /** Sets the cook time in ticks (20 ticks = 1 second). */
    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }

    /** @return the configured cook time in ticks. */
    public int getCookTime() {
        return cookTime;
    }

    /** Sets the experience granted when the recipe is completed. */
    public void setExperience(float experience) {
        this.experience = experience;
    }

    /** @return the experience value of the cooked result. */
    public float getExperience() {
        return experience;
    }
}