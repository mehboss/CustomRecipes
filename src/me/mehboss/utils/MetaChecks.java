package me.mehboss.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Ingredient;

public class MetaChecks {
	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	public Boolean itemsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
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

			if (ingredient.hasDisplayName()
					&& (!item.hasItemMeta() || !CompatibilityUtil.hasDisplayname(item.getItemMeta()))) {
				logDebug("Ingredient has displayname, item in slot does not have one", recipeName);
				return false;
			}

			if (ingredient.hasDisplayName()
					&& !(CompatibilityUtil.getDisplayname(item.getItemMeta()).equals(ingredient.getDisplayName()))) {
				logDebug("Displaynames do not match", recipeName);
				logDebug("Slot displayname - " + CompatibilityUtil.getDisplayname(item.getItemMeta()), recipeName);
				logDebug("Ingredient displayname - " + ingredient.getDisplayName(), recipeName);
				return false;
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
