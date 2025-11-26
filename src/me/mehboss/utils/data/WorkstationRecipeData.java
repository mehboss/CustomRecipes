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
    
	/**
	 * Getter for the recipe group.
	 * 
	 * <p>
	 * Available since Spigot 1.13+
	 * </p>
	 * 
	 * @return the group string this recipe belongs to. Empty string means no group.
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Sets the group of the recipe. Recipes with the same group may be grouped
	 * together when displayed in the client.
	 * 
	 * <p>
	 * Available since Spigot 1.13+
	 * </p>
	 * 
	 * @param group the group name. Empty string denotes no group.
	 */
	public void setGroup(String group) {
		if (group == null) {
			this.group = "";
		} else {
			this.group = group;
		}
	}
}