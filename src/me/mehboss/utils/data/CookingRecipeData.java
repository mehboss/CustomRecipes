package me.mehboss.utils.data;

import org.bukkit.inventory.ItemStack;

import me.mehboss.utils.RecipeUtil.Recipe;

/* ======================================================================
 * Cooking Recipes - Specialized Subclass
 * ====================================================================== */
public class CookingRecipeData extends Recipe {
    private ItemStack source;
    private int cookTime = 200;
    private float experience = 1.0f;

    public CookingRecipeData(String name) {
        super(name);
    }

    /** Sets the source item for this cooking recipe. */
    public void setSource(ItemStack source) {
        this.source = source;
    }

    /** Gets the source item used in this cooking recipe. */
    public ItemStack getSource() {
        return source;
    }

    /** Sets the cook time in ticks (20 ticks = 1 second). */
    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }

    /** Gets the cook time in ticks. */
    public int getCookTime() {
        return cookTime;
    }

    /** Sets the experience yield of the result. */
    public void setExperience(float experience) {
        this.experience = experience;
    }

    /** Gets the experience yield of the result. */
    public float getExperience() {
        return experience;
    }
}