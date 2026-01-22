package me.mehboss.utils.libs;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;

/**
 * Handles all metadata comparison logic used during special recipe matching.
 * <p>
 * Provides utility checks for matching item materials, display names, and
 * custom identifiers. This class is used internally by RecipeUtil and recipe
 * handlers to determine whether an input item matches a defined ingredient.
 */
public class MetaChecks {
	private RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	/**
	 * Compares an input item against a recipe ingredient definition.
	 * <p>
	 * Validates:
	 * <ul>
	 * <li>null vs non-null mismatches</li>
	 * <li>material type</li>
	 * <li>custom identifier match (if present)</li>
	 * <li>display name match (if present)</li>
	 * </ul>
	 *
	 * @param recipeName name of the recipe (used for debug output)
	 * @param item       the actual input item
	 * @param ingredient expected ingredient definition
	 * @return true if the item satisfies all required checks
	 */
	public Boolean itemsMatch(Recipe recipe, ItemStack item, Ingredient ingredient) {
		String recipeName = recipe.getName();
		// Null check first
		if ((ingredient.isEmpty() && item != null) || (!ingredient.isEmpty() && item == null)) {
			logDebug("Unexpected item found", recipeName);
			logDebug("Item - " + item, recipeName);
			logDebug("Ingredient - " + ingredient, recipeName);
			return false;
		}

		// Material checks
		if (item.getType() != ingredient.getMaterial()) {
			logDebug("Materials do not match", recipeName);
			logDebug("Item - " + item, recipeName);
			logDebug("Ingredient - " + ingredient, recipeName);
			return false;
		}

		// Compares against #exactMatch
		if (ingredient.hasIdentifier()) {
			ItemStack foundItem = getRecipeUtil().getResultFromKey(ingredient.getIdentifier());

			if (foundItem == null) {
				logDebug("Ingredient identifier does not match to a valid recipe", recipeName);
				return false;
			}

			if (!(foundItem.isSimilar(item))) {
				logDebug("Identifier isSimilar returned false", recipeName);
				return false;
			}

			// Checks displayname requirements only
		} else {
			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();

				if ((ingredient.hasItemName() && !meta.hasItemName())
						|| (ingredient.hasDisplayName() && !meta.hasDisplayName())) {
					logDebug("Ingredient has displayname, item in slot does not have one", recipeName);
					return false;
				}

				if ((ingredient.hasItemName() && !meta.getDisplayName().equals(ingredient.getDisplayName()))
						|| (ingredient.hasDisplayName()
								&& !meta.getDisplayName().equals(ingredient.getDisplayName()))) {
					logDebug("Skipping recipe..", recipeName);
					logDebug("Displaynames do not match:", recipeName);
					logDebug("Ingredient: " + ingredient.getDisplayName(), recipeName);
					logDebug("Item: " + meta.getDisplayName(), recipeName);
					return false;
				}
			}
		}
		return true;
	}

	private void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Metachecks][" + recipeName + "] " + st);
	}
}
