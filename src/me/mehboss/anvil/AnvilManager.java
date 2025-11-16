package me.mehboss.anvil;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.WorkstationRecipeData;

public class AnvilManager implements Listener {

	public HashMap<UUID, Integer> runTimes = new HashMap<UUID, Integer>();

	@EventHandler
	public void onPlace(PrepareAnvilEvent e) {
		AnvilInventory inv = e.getInventory();
		Recipe matchedRecipe = null;
		Player p = (Player) e.getView().getPlayer();

		if (getRecipeUtil().getAllRecipes() == null)
			return;

		for (Recipe recipe : getRecipeUtil().getAllRecipes().values()) {
			Boolean matchedToRecipe = true;

			if (recipe.getType() != RecipeType.ANVIL) {
				logDebug("Skipping " + recipe.getName() + ".. Type is not anvil - " + recipe.getType());
				continue;
			}

			int slot = 0;
			for (Ingredient ingredient : recipe.getIngredients()) {
				if (!itemsMatch(recipe.getName(), inv.getItem(slot), ingredient)
						|| !amountsMatch(recipe.getName(), inv.getItem(slot), ingredient)) {
					matchedToRecipe = false;
					break;
				}
				slot++;
			}

			if (matchedToRecipe) {
				matchedRecipe = recipe;
				break;
			}

			logDebug(recipe.getName() + ": Requirements not met.. Skipping to next recipe");
		}

		if (matchedRecipe != null) {
			if (!matchedRecipe.isActive()
					|| (matchedRecipe.getPerm() != null && !p.hasPermission(matchedRecipe.getPerm()))
					|| matchedRecipe.getDisabledWorlds().contains(p.getWorld().getName())) {
				sendNoPermsMessage(p, matchedRecipe.getName());
				return;
			}
			
			WorkstationRecipeData anvil = (WorkstationRecipeData) matchedRecipe;

			logDebug(anvil.getName() + ": Requirements met.. Successfully set anvil result slot");
			e.setResult(anvil.getResult());
			inv.setRepairCost(anvil.getRepairCost());
			p.updateInventory();
		}
	}

	Boolean amountsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		if (item == null || ingredient == null) {
			logDebug(recipeName + ": Item or Ingredient is null");
			logDebug(recipeName + ": Item - " + item);
			logDebug(recipeName + ": Ingredient - " + ingredient);
			return false;
		}

		if (item.getAmount() < ingredient.getAmount()) {
			logDebug(recipeName + ": Amount requirements not met");
			logDebug(recipeName + ": Slot amount - " + item.getAmount());
			logDebug(recipeName + ": Ingredient amount - " + ingredient.getAmount());
			return false;
		}

		return true;
	}

	RecipeUtil getRecipeUtil() {
	    return Main.getInstance().recipeUtil;
	}
	
	boolean itemsMatch(String recipeName, ItemStack item, Ingredient ingredient) {
		return Main.getInstance().metaChecks.itemsMatch(recipeName, item, ingredient);
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe " + recipe);
		Main.getInstance().sendnoPerms(p);
	}
}
