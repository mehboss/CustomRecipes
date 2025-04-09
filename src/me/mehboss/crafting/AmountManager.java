package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class AmountManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;

	private CraftManager craftManager;

	public AmountManager(CraftManager craftManager) {
		this.craftManager = craftManager;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	private boolean hasAllIngredients(CraftingInventory inv, String recipes, List<Ingredient> recipeIngredients) {
		return craftManager.hasAllIngredients(inv, recipes, recipeIngredients);
	}

	private boolean matchedRecipe(CraftingInventory inv) {
		return craftManager.matchedRecipe(inv);
	}

	private boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean b) {
		return craftManager.hasMatchingDisplayName(recipeName, item, displayName, identifier, hasIdentifier, b);
	}

	// Helper method for the handleShiftClick method
	void handlesItemRemoval(CraftItemEvent e, CraftingInventory inv, Recipe recipe, ItemStack item,
			RecipeUtil.Ingredient ingredient, int slot, int itemsToRemove, int itemsToAdd, int requiredAmount) {

		String recipeName = recipe.getName();
		logDebug("[handleShiftClicks] Checking slot " + slot + " for the recipe " + recipeName);

		if (matchesIngredient(item, recipeName, ingredient, ingredient.getMaterial(), ingredient.getDisplayName(),
				ingredient.hasIdentifier())) {

			if (item.getAmount() < requiredAmount)
				return;

			itemsToRemove = itemsToAdd * requiredAmount;

			int availableItems = item.getAmount();

			logDebug("[handleShiftClicks] Handling recipe " + recipeName);
			logDebug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove);
			logDebug("[handleShiftClicks] ItemAmount: " + availableItems);
			logDebug("[handleShiftClicks] RequiredAmount: " + requiredAmount);
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier());
			logDebug("[handleShiftClicks] HasIdentifier: " + ingredient.hasIdentifier());
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString());
			logDebug("[handleShiftClicks] Displayname: " + ingredient.getDisplayName());

			int itemAmount = item.getAmount();
			if (itemAmount < requiredAmount)
				return;

			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (item.getType().toString().contains("_BUCKET") && !(recipe.isConsume())) {
					item.setType(XMaterial.BUCKET.parseMaterial());
				} else {
					if ((item.getAmount() + 1) - requiredAmount == 0)
						inv.setItem(slot, null);
					else
						item.setAmount((item.getAmount() + 1) - requiredAmount);
				}
			} else {
				if (item.getType().toString().contains("_BUCKET") && !(recipe.isConsume())) {
					item.setType(XMaterial.BUCKET.parseMaterial());
				} else {
					if ((item.getAmount() - itemsToRemove) <= 0) {
						inv.setItem(slot, null);
						return;
					}

					item.setAmount(item.getAmount() - itemsToRemove);
				}
			}
		}
	}

	// Helper method for the handleShiftClick method
	boolean matchesIngredient(ItemStack item, String recipeName, RecipeUtil.Ingredient ingredient, Material material,
			String displayName, boolean hasIdentifier) {
		Recipe exactMatch = recipeUtil.getRecipeFromKey(ingredient.getIdentifier());
		ItemStack customItem = ingredient.hasIdentifier() ? recipeUtil.getResultFromKey(ingredient.getIdentifier())
				: null;

		return ((ingredient.hasIdentifier() && NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				&& ingredient.getIdentifier()
						.equals(NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
				|| (customItem != null && item.isSimilar(customItem))
				|| (ingredient.hasIdentifier() && exactMatch != null && item.isSimilar(exactMatch.getResult()))
				|| (item.getType() == material && hasMatchingDisplayName(recipeName, item, displayName,
						ingredient.getIdentifier(), hasIdentifier, false)));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void handleShiftClicks(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();

		if (!(matchedRecipe(inv)))
			return;

		if (recipeUtil.getRecipeFromResult(inv.getResult()) == null)
			return;

		logDebug("[handleShiftClicks] Passed containsValue boolean check.");

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		String findName = recipeUtil.getRecipeFromResult(inv.getResult()) != null
				? recipeUtil.getRecipeFromResult(inv.getResult()).getName()
				: null;

		final ItemStack result = inv.getResult();

		logDebug("[handleShiftClicks] Initial recipe found recipe '" + findName + "' to handle..");
		for (String recipes : recipeUtil.getRecipeNames()) {
			List<RecipeUtil.Ingredient> recipeIngredients = recipeUtil.getRecipe(recipes).getIngredients();

			if (hasAllIngredients(inv, recipes, recipeIngredients)
					&& recipeUtil.getRecipe(recipes).getType() == RecipeType.SHAPELESS) {
				findName = recipes;
				break;
			}
			if (hasAllIngredients(inv, recipes, recipeIngredients)
					&& recipeUtil.getRecipe(recipes).getType() == RecipeType.SHAPED) {
				findName = recipes;
				break;
			}
		}

		if (NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = NBTEditor.getString(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");

			if (recipeUtil.getRecipeFromKey(foundID) != null) {
				findName = recipeUtil.getRecipeFromKey(foundID).getName();
			}
		}

		logDebug("[handleShiftClicks] Actual found recipe '" + findName + "' to handle..");

		if (findName == null)
			return;

		logDebug("[handleShiftClicks] Paired it to a custom recipe. Running crafting amount calculations..");

		final String recipeName = findName;
		int itemsToAdd = Integer.MAX_VALUE;
		int itemsToRemove = 0;

		if (e.isCancelled()) {
			logDebug("Couldn't complete craftItemEvent for recipe " + recipeName
					+ ", the event was unexpectedly cancelled.");
			logDebug("Please seek support or open a ticket https://github.com/mehboss/CustomRecipes/issues");
			return;
		}

		Recipe recipe = recipeUtil.getRecipe(recipeName);

		logDebug("[handleShiftClicks] Checking amount requirements for " + recipeName);
		ArrayList<String> handledIngredients = new ArrayList<String>();
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			Material material = ingredient.getMaterial();
			String displayName = ingredient.getDisplayName();
			int requiredAmount = ingredient.getAmount();
			boolean hasIdentifier = ingredient.hasIdentifier();

			for (int highest = 1; highest < 10; highest++) {
				ItemStack slot = inv.getItem(highest);

				if (slot == null || slot.getType() == Material.AIR)
					continue;

				if (matchesIngredient(slot, recipeName, ingredient, material, displayName, hasIdentifier)) {
					if (slot.getAmount() < requiredAmount)
						continue;

					int availableItems = slot.getAmount();
					int possibleItemsToRemove = availableItems / requiredAmount;

					// Keep track of the lowest possible items to add.
					itemsToAdd = Math.min(itemsToAdd, possibleItemsToRemove);
				}
			}

			if (itemsToAdd == Integer.MAX_VALUE) {
				logDebug("[handleShiftClicks] Could not craft " + findName
						+ ".. An issue has occurred with the amount deductions..");
				e.setResult(null);
				e.setCancelled(true);
				return;
			}

			// Handle SHAPELESS recipes by looping through the inventory
			if (recipe.getType() == RecipeType.SHAPELESS) {
				logDebug("[handleShiftClicks] Found shapeless recipe to handle..");

				for (int i = 1; i < 10; i++) {
					ItemStack item = inv.getItem(i);
					int slot = i;

					if (item == null || item.getType() == Material.AIR)
						continue;

					if (!handledIngredients.contains(ingredient.getAbbreviation())) {
						handlesItemRemoval(e, inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd,
								requiredAmount);
					}
				}
				if (!handledIngredients.contains(ingredient.getAbbreviation()))
					handledIngredients.add(ingredient.getAbbreviation());

			} else {
				logDebug("[handleShiftClicks] Found other recipe type to handle..");

				ItemStack item = inv.getItem(ingredient.getSlot());
				int slot = ingredient.getSlot();

				if (item == null || item.getType() == Material.AIR)
					continue;

				handlesItemRemoval(e, inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd, requiredAmount);

			}
		}

		// Add the result items to the player's inventory
		Player player = (Player) e.getWhoClicked();
		Main.getInstance().cooldownManager.setCooldown(player.getUniqueId(), recipe.getKey(),
				System.currentTimeMillis());

		if (recipe.isCommand()) {
			logDebug("Cancelling craft of " + recipeName + " because a command to perform was found instead.");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), recipe.getCommand());

			Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
				@Override
				public void run() {
					e.getWhoClicked().setItemOnCursor(null);
				}
			}, 2L);

			return;
		}

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			logDebug("[handleShiftClicks] Didn't detect shift click from inventory.. Ignoring..");

		} else {
			e.setCancelled(true);

			if (recipe.isCommand())
				return;

			inv.setResult(new ItemStack(Material.AIR));

			for (int i = 0; i < itemsToAdd; i++) {
				if (player.getInventory().firstEmpty() == -1) {
					player.getLocation().getWorld().dropItem(player.getLocation(), result);
					continue;
				}
				player.getInventory().addItem(result);
			}

			logDebug("[handleShiftClicks] Shift click detected. Adding " + itemsToAdd + " to inventory.");
			logDebug("[handleShiftClicks] Added " + itemsToAdd + " items and removed items from table.");
		}
	}
}
