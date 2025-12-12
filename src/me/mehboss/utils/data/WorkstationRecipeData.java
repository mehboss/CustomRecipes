package me.mehboss.utils.data;

import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Stores additional data for workstation-based recipes such as
 * anvils or stonecutters.
 * <p>
 * Extends {@link Recipe} by adding fields related to workstation
 * behavior, including repair cost, experience output, and optional
 * grouping. This class is a flexible data container and does not
 * define how recipes must be created.
 */
public class WorkstationRecipeData extends Recipe {
    private String group = "";
    private int repairCost = 0;
    private float experience = 1.0f;
    
    public WorkstationRecipeData(String name) {
        super(name);
    }

    /**
     * Sets the repair cost for this recipe (used by anvil mechanics).
     *
     * @param cost the repair cost to assign
     */
    public void setRepairCost(int cost) {
        this.repairCost = cost;
    }

    /**
     * Gets the repair cost assigned to this workstation recipe.
     *
     * @return numerical repair cost
     */
    public int getRepairCost() {
        return repairCost;
    }

    /**
     * Checks whether this recipe has a defined repair cost.
     *
     * @return true if repairCost > 0
     */
    public boolean hasRepairCost() {
        return repairCost > 0;
    }

    /**
     * Sets the amount of experience granted by this recipe.
     *
     * @param experience the XP reward value
     */
    public void setExperience(float experience) {
        this.experience = experience;
    }

    /**
     * Gets the experience reward assigned to this recipe.
     *
     * @return experience value
     */
    public float getExperience() {
        return experience;
    }

    /**
     * Gets the group tag assigned to this recipe.
     * <p>
     * Available since Spigot 1.13+.
     *
     * @return the recipe group name, or empty string if ungrouped
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the group this recipe belongs to.
     * Recipes with the same group may be visually grouped in the client.
     * <p>
     * Available since Spigot 1.13+.
     *
     * @param group the group identifier, or empty string for no group
     */
    public void setGroup(String group) {
        this.group = (group == null ? "" : group);
    }
}