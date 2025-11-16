package me.mehboss.utils.data;

import me.mehboss.utils.RecipeUtil.Recipe;

/* ======================================================================
 * Anvil / Stonecutter Recipes - Specialized Subclass
 * ====================================================================== */
public class WorkstationRecipeData extends Recipe {
    private String group = "";
    private int repairCost = 0;
    private float experience = 1.0f;
    
    public WorkstationRecipeData(String name) {
        super(name);
    }

    /** Sets a group identifier for this recipe (used by stonecutter). */
    public void setGroup(String group) {
        this.group = group;
    }

    /** Gets the group identifier for this recipe. */
    public String getGroup() {
        return group;
    }

    /** Sets the repair cost for this recipe (used by anvil). */
    public void setRepairCost(int cost) {
        this.repairCost = cost;
    }

    /** Gets the repair cost for this recipe. */
    public int getRepairCost() {
        return repairCost;
    }

    public boolean hasRepairCost() {
        return repairCost > 0;
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