package me.mehboss.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Ingredient;

public class MetaChecks {
	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
	
	public Boolean itemsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		// Null check first
		if ((ingredient.isEmpty() && item != null) || (!ingredient.isEmpty() && item == null)) {
			logDebug(recipeName + ": Unexpected item found");
			logDebug(recipeName + ": Item - " + item);
			logDebug(recipeName + ": Ingredient - " + ingredient);
			return false;
		}

		// Material checks
		if (item.getType() != ingredient.getMaterial()) {
			logDebug(recipeName + ": Materials do not match");
			logDebug(recipeName + ": Item - " + item);
			logDebug(recipeName + ": Ingredient - " + ingredient);
			return false;
		}

		// Compares against #exactMatch
		if (ingredient.hasIdentifier()) {
			ItemStack foundItem = recipeUtil.getResultFromKey(ingredient.getIdentifier());

			if (foundItem == null) {
				logDebug(recipeName + ": Ingredient identifier does not match to a valid recipe");
				return false;
			}

			if (!(foundItem.isSimilar(item))) {
				logDebug(recipeName + ": Identifier isSimilar returned false");
				return false;
			}

			// Checks displayname requirements only
		} else {

			if (ingredient.hasDisplayName() && (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())) {
				logDebug(recipeName + ": Ingredient has displayname, item in slot does not have one");
				return false;
			}

			if (ingredient.hasDisplayName()
					&& !(item.getItemMeta().getDisplayName().equals(ingredient.getDisplayName()))) {
				logDebug(recipeName + ": Displaynames do not match");
				logDebug(recipeName + ": Slot displayname - " + item.getItemMeta().getDisplayName());
				logDebug(recipeName + ": Ingredient displayname - " + ingredient.getDisplayName());
				return false;
			}
		}
		return true;
	}
	
	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}
}
