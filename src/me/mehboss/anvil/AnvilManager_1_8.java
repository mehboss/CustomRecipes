package me.mehboss.anvil;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.CooldownManager;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.WorkstationRecipeData;

public class AnvilManager_1_8 implements Listener {

	@EventHandler
	void onPlace(InventoryClickEvent e) {

		// Only run in an anvil
		if (!(e.getInventory() instanceof AnvilInventory))
			return;

		if (Main.getInstance().serverVersionAtLeast(1, 9))
			return;

		// Only respond to clicks inside the anvil input/result slots
		if (e.getRawSlot() > 2)
			return;

		Player p = (Player) e.getWhoClicked();
		World w = p.getWorld();
		AnvilInventory inv = (AnvilInventory) e.getInventory();

		// Delay by 1 tick so the inventory updates like PrepareAnvilEvent would
		Bukkit.getScheduler().runTask(Main.getInstance(), () -> {

			Recipe matchedRecipe = null;
			HashMap<String, Recipe> anvilRecipes = getRecipeUtil().getRecipesFromType(RecipeType.ANVIL);

			if (anvilRecipes == null || anvilRecipes.isEmpty())
				return;

			for (Recipe recipe : anvilRecipes.values()) {
				Boolean matchedToRecipe = true;

				int slot = 0;
				for (Ingredient ingredient : recipe.getIngredients()) {
					if (!itemsMatch(recipe, inv.getItem(slot), ingredient)
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
				boolean hasPerms = p == null || !matchedRecipe.hasPerm() || p.hasPermission(matchedRecipe.getPerm());
				boolean allowWorld = w == null || !matchedRecipe.getDisabledWorlds().contains(w.getName());
				boolean hasCooldown = p != null && matchedRecipe.hasCooldown()
						&& getCooldownManager().hasCooldown(p.getUniqueId(), matchedRecipe.getKey())
						&& !(matchedRecipe.hasPerm() && p.hasPermission(matchedRecipe.getPerm() + ".bypass"));

				if (!matchedRecipe.isActive() || !hasPerms || !allowWorld) {
					sendNoPermsMessage(p, matchedRecipe.getName());
					inv.setItem(2, null);
					return;
				}

				if (hasCooldown) {
					Long timeLeft = Main.getInstance().cooldownManager.getTimeLeft(p.getUniqueId(),
							matchedRecipe.getKey());
					sendMessages(p, "crafting-limit", timeLeft);
					inv.setItem(2, null);
					return;
				}

				WorkstationRecipeData anvil = (WorkstationRecipeData) matchedRecipe;

				logDebug(anvil.getName() + ": Requirements met.. Successfully set anvil result slot");

				inv.setItem(2, anvil.getResult()); // prepareAnvilEvent#setResult equivalent
				inv.setRepairCost(anvil.getRepairCost());
				p.updateInventory();
			}
		});
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

	CooldownManager getCooldownManager() {
		return Main.getInstance().cooldownManager;
	}

	/**
	 * Checks if the given ItemStack matches the ingredient's item type and
	 * metadata.
	 *
	 * @param recipeName Name of the recipe (used for debug logging).
	 * @param item       ItemStack from the anvil input.
	 * @param ingredient Ingredient to compare against.
	 * @return true if items match according to MetaChecks; false otherwise.
	 */
	boolean itemsMatch(Recipe recipe, ItemStack item, Ingredient ingredient) {
		return Main.getInstance().metaChecks.itemsMatch(recipe, item, ingredient);
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	void sendMessages(Player p, String s, long seconds) {
		Main.getInstance().sendMessages(p, s, seconds);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe " + recipe);
		Main.getInstance().sendnoPerms(p);
	}
}
