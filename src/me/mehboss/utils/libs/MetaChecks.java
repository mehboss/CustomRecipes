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
	@SuppressWarnings("deprecation")
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
				logDebug("Ingredient identifier does not match to a valid recipe/item", recipeName);
				return false;
			}

			if (!(foundItem.isSimilar(item))) {
				logDebug("Identifier isSimilar returned false", recipeName);
				return false;
			}

			// Checks displayname requirements only
		} else {
			if (!recipe.hasIgnoreTag() && ingredient.hasItem() && ingredient.getItem().isSimilar(item))
				return true;

			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();

				if (!recipe.getIgnoreNames()) {
					if ((ingredient.hasItemName() && !meta.hasItemName())
							|| (ingredient.hasDisplayName() && !meta.hasDisplayName())) {
						logDebug("Ingredient has displayname, item in slot does not have one", recipeName);
						return false;
					}

					if ((ingredient.hasItemName() && !meta.getItemName().equals(ingredient.getDisplayName()))
							|| (ingredient.hasDisplayName()
									&& !meta.getDisplayName().equals(ingredient.getDisplayName()))) {
						logDebug("Skipping recipe..", recipeName);
						logDebug("Displaynames do not match:", recipeName);
						logDebug("Ingredient: " + ingredient.getDisplayName(), recipeName);
						logDebug("Item: " + meta.getDisplayName(), recipeName);
						return false;
					}
				}

				if (!recipe.getIgnoreModelData()) {
					int cmd = ingredient.getCustomModelData();
					if (ingredient.hasCustomModelData() && meta.getCustomModelData() != cmd)
						return false;
				}

				if (CompatibilityUtil.supportsItemModel() && !recipe.getIgnoreItemModel()) {
					String itemModel = ingredient.getItemModel();
					if (ingredient.hasItemModel() && !itemModel.equals(meta.getItemModel().getKey()))
						return false;
				}
			}
		}
		return true;

	}

	/**
	 * Checks whether the provided item satisfies the ingredient's required amount.
	 *
	 * @param recipeName Name of the recipe (used for debugging messages).
	 * @param item       ItemStack inside the anvil.
	 * @param ingredient Ingredient definition being compared to.
	 * @return true if the item meets or exceeds the amount required; false
	 *         otherwise.
	 */
	public Boolean amountsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		if (item == null || ingredient == null) {
			logDebug("Item or Ingredient is null", recipeName);
			logDebug("Item - " + item, recipeName);
			logDebug("Ingredient - " + ingredient, recipeName);
			return false;
		}

		if (item.getAmount() < ingredient.getAmount()) {
			logDebug("Amount requirements not met", recipeName);
			logDebug("Slot amount - " + item.getAmount(), recipeName);
			logDebug("Ingredient amount - " + ingredient.getAmount(), recipeName);
			return false;
		}

		return true;
	}
	
	private void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Metachecks][" + recipeName + "] " + st);
	}
}
