package me.mehboss.utils.data;

import java.util.ArrayList;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.RecipeConditions.ConditionSet;

/**
 * Stores additional data for custom crafting recipes.
 * <p>
 * Extends {@link Recipe} with fields used only by crafting systems,
 * including leftover item rules, conditional requirements, and
 * optional recipe grouping. This class acts only as a data container
 * and does not enforce how a recipe is created.
 */
public class CraftingRecipeData extends Recipe {
	private ArrayList<String> leftoverItems = new ArrayList<>();
	private ConditionSet conditionSet = new ConditionSet();
	private String group = "";
	
	public CraftingRecipeData(String name) {
		super(name);
	}
	
	/**
	 * Setter for adding an ingredient as a leftover
	 * 
	 * @param id the id of the ingredient to be leftover.
	 */
	public void addLeftoverItem(String id) {
		leftoverItems.add(id);
	}

	/**
	 * Checks if a MATERIAL is marked as leftover.
	 * 
	 * @returns true or false boolean
	 */
	public Boolean isLeftover(String id) {
		if (leftoverItems.isEmpty())
			return false;

		if (XMaterial.matchXMaterial(id).isPresent())
			id = XMaterial.matchXMaterial(id).get().parseMaterial().toString();

		if (leftoverItems.contains(id))
			return true;

		return false;
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
	
	/**
	 * Gets the ConditionSet attached to this recipe. Conditions define extra
	 * requirements for crafting (e.g. world, time, weather).
	 *
	 * @return the ConditionSet for this recipe, never null
	 */
	public ConditionSet getConditionSet() {
		return conditionSet;
	}

	/**
	 * Sets the ConditionSet for this recipe. If {@code cs} is null, an empty
	 * ConditionSet is applied instead.
	 *
	 * @param cs the new ConditionSet for this recipe, can be null
	 */
	public void setConditionSet(ConditionSet cs) {
		this.conditionSet = (cs == null) ? new ConditionSet() : cs;
	}
}
