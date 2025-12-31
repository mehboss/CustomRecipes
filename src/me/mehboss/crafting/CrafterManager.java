package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.crafting.ShapedChecks.AlignedResult;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.libs.RecipeConditions.ConditionSet;

public class CrafterManager implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(CrafterCraftEvent e) {
		Block block = e.getBlock();

		if (!(block.getState() instanceof Crafter))
			return;

		Crafter crafter = (Crafter) block.getState();
		Inventory inv = crafter.getInventory();
		e.setResult(handleCraftingChecks(crafter, inv, e.getResult()));
	}

	ItemStack handleCraftingChecks(Crafter crafter, Inventory inv, ItemStack result) {
		CraftingRecipeData recipe = null;
		Boolean passedCheck = true;
		Boolean found = false;
		AlignedResult grid = null;

		if (CraftManager().hasVanillaIngredients(inv, result))
			return result;

		for (Recipe data : getRecipeUtil().getAllRecipesSortedByResult(result)) {

			if (data.getType() != RecipeType.SHAPELESS && data.getType() != RecipeType.SHAPED)
				continue;

			recipe = (CraftingRecipeData) data;
			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();
			if (!CraftManager().hasAllIngredients(inv, recipe.getName(), recipeIngredients, null)) {
				logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not match..", recipe.getName());
				passedCheck = false;
				continue;
			}

			logDebug("[handleCrafting] Inventory contained all of the ingredients. Continuing checks..",
					recipe.getName());

			passedCheck = true;
			found = true;

			// -------------------------
			// SHAPELESS RECIPE CHECKS
			// -------------------------
			if (recipe.getType() == RecipeType.SHAPELESS) {

				if (!shapelessChecks().handleShapelessRecipe(inv, recipe, recipeIngredients, null)) {
					passedCheck = false;
					continue;
				}

			}
			// ----------------------
			// SHAPED RECIPE CHECKS
			// ----------------------
			else {

				grid = shapedChecks().getAlignedGrid(inv, recipeIngredients);
				if (grid == null) {
					logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match..",
							recipe.getName());
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched. Continuing checks..", recipe.getName());

				// NEW: run strict NBT-name-CMD checks on aligned grid
				if (!recipe.getIgnoreData())
					if (!shapedChecks().handleShapedRecipe(inv.getType(), grid, recipe, recipeIngredients)) {
						passedCheck = false;
						continue;
					}
			}

			// -----------------------
			// AMOUNT CHECK
			// -----------------------
			if (!(CraftManager().amountsMatch(grid, inv, recipe.getName(), recipeIngredients, true, null))) {
				logDebug(
						"[handleCrafting] Skipping recipe.. The amount check indicates that the requirements have not been met..",
						recipe.getName());
				passedCheck = false;
				continue;
			}

			if (passedCheck && found) {
				break;
			}
		}

		logDebug("[handleCrafting] Final crafting results: (passedChecks: " + passedCheck + ")(foundRecipe: " + found
				+ ")", recipe.getName());

		if (!found)
			return result;

		if ((!passedCheck) || (passedCheck && !found)) {
			return new ItemStack(Material.AIR);
		}

		Location location = crafter.getBlock().getLocation();
		if (!recipe.isActive() || recipe.getDisabledWorlds().contains(location.getWorld().getName())
				|| Main.getInstance().disabledrecipe.contains(recipe.getKey()) || recipe.hasCommands()) {

			logDebug(" Attempt to craft recipe was detected, but recipe is disabled!", recipe.getName());
			return new ItemStack(Material.AIR);
		}

		// CONDITIONS
		ConditionSet cs = recipe.getConditionSet();
		if (cs != null && !cs.isEmpty()) {
			if (!cs.test(location, null, null)) {
				logDebug("Preventing craft due to failing required recipe conditions!", recipe.getName());
				return new ItemStack(Material.AIR);
			}
		}

		if (passedCheck && found) {
			ItemStack item = new ItemStack(recipe.getResult());
			handleAmountDeductions(grid, crafter, recipe.getName(), inv);
			return item;
		}

		return result;
	}

	void handleAmountDeductions(AlignedResult aligned, Crafter crafter, String recipeName, Inventory inv) {
		CraftingRecipeData recipe = (CraftingRecipeData) getRecipeUtil().getRecipe(recipeName);
		if (recipe == null)
			return;

		ArrayList<String> handledIngredients = new ArrayList<>();
		boolean[] handledSlots = new boolean[9];

		final int itemsToAdd = 1;
		int itemsToRemove = 0;

		RecipeType type = recipe.getType();

		if (type == RecipeType.SHAPED) {
			if (aligned == null)
				return;

			for (int i = 0; i < recipe.getIngredients().size(); i++) {
				RecipeUtil.Ingredient ing = recipe.getIngredients().get(i);
				if (ing.isEmpty())
					continue;

				int slot = aligned.invSlotMap[i];
				ItemStack item = inv.getItem(slot);
				if (item == null || item.getType() == Material.AIR)
					continue;

				int requiredAmount = Math.max(1, ing.getAmount());
				handlesItemRemoval(inv, recipe, item, ing, slot, itemsToRemove, itemsToAdd, requiredAmount);
			}

		} else {
			// Shapeless
			for (RecipeUtil.Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;

				final Material mat = ing.getMaterial();
				final String name = ing.getDisplayName();
				final boolean hasID = ing.hasIdentifier();
				int requiredAmount = Math.max(1, ing.getAmount());

				for (int i = 0; i < 9; i++) {
					if (handledSlots[i])
						continue;

					ItemStack item = inv.getItem(i);
					if (item == null || item.getType() == Material.AIR || crafter.isSlotDisabled(i))
						continue;

					if (!AmountManager().matchesIngredient(item, recipeName, ing, mat, name, hasID))
						continue;

					handlesItemRemoval(inv, recipe, item, ing, i, itemsToRemove, itemsToAdd, requiredAmount);
					handledSlots[i] = true;
				}

				handledIngredients.add(ing.getAbbreviation());
			}
		}
	}

	void handlesItemRemoval(Inventory inv, CraftingRecipeData recipe, ItemStack item, RecipeUtil.Ingredient ingredient,
			int slot, int itemsToRemove, int itemsToAdd, int requiredAmount) {

		String recipeName = recipe.getName();

		if (!AmountManager().matchesIngredient(item, recipeName, ingredient, ingredient.getMaterial(),
				ingredient.getDisplayName(), ingredient.hasIdentifier()))
			return;

		itemsToRemove = (itemsToAdd * requiredAmount) - 1; // remove exact amount for the crafter

		int availableItems = item.getAmount();
		if (availableItems < requiredAmount)
			return;

		logDebug("[handleShiftClicks] Handling recipe " + recipeName, "");
		logDebug("[handleShiftClicks] ItemsToRemove: " + (itemsToRemove + 1) + " - ItemsToAdd: " + itemsToAdd, "");
		logDebug("[handleShiftClicks] ItemAmount: " + availableItems, "");
		logDebug("[handleShiftClicks] RequiredAmount: " + requiredAmount, "");
		logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " - HasIdentifier: "
				+ ingredient.hasIdentifier(), "");
		logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString(), "");
		logDebug("[handleShiftClicks] Displayname: " + ingredient.getDisplayName(), "");

		String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
		if (recipe.isLeftover(id)) {
			if (item.getType().toString().contains("_BUCKET")) {
				item.setType(XMaterial.BUCKET.parseMaterial());
			}
			return;
		}

		if ((item.getAmount() - itemsToRemove) <= 0) {
			inv.setItem(slot, null);
			return;
		}

		item.setAmount(item.getAmount() - itemsToRemove);
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().shapedChecks;
	}

	ShapelessChecks shapelessChecks() {
		return Main.getInstance().shapelessChecks;
	}

	CraftManager CraftManager() {
		return Main.getInstance().craftManager;
	}

	AmountManager AmountManager() {
		return Main.getInstance().amountManager;
	}

	void logDebug(String st, String recipeName) {
		if (Main.getInstance().crafterdebug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}
}
