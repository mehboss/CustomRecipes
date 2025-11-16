package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Container;
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
import me.mehboss.utils.RecipeConditions.ConditionSet;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CrafterManager implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(CrafterCraftEvent e) {
		Container block = (Container) e.getBlock().getState();
		Inventory inv = block.getInventory();
		e.setResult(handleCraftingChecks(e, inv, e.getResult()));
	}

	ItemStack handleCraftingChecks(CrafterCraftEvent e, Inventory inv, ItemStack result) {
		Recipe finalRecipe = null;
		Boolean passedCheck = true;
		Boolean found = false;
		AlignedResult grid = null;

		if (CraftManager().hasVanillaIngredients(inv, result))
			return result;

		for (Recipe recipe : getRecipeUtil().getAllRecipesSortedByResult(result)) {

			finalRecipe = recipe;
			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();

			if (recipe.getType() != RecipeType.SHAPELESS && recipe.getType() != RecipeType.SHAPED)
				continue;

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

				if (!handleShapelessRecipe(inv, result, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;
				}

			}
			// ----------------------
			// SHAPED RECIPE CHECKS
			// ----------------------
			else {
				AlignedResult alignedGrid = shapedChecks().getAlignedGrid(inv, recipeIngredients);
				
				// NEW: get shape match + aligned grid
				if (alignedGrid == null) {
					logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match..",
							recipe.getName());
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched. Continuing checks..", recipe.getName());

				// NEW: run strict NBT-name-CMD checks on aligned grid
				if (!recipe.getIgnoreData())
					if (!shapedChecks().handleShapedRecipe(inv.getType(), alignedGrid, recipe, recipeIngredients)) {
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
				+ ")", finalRecipe.getName());

		if (!found)
			return result;

		if ((!passedCheck) || (passedCheck && !found)) {
			return new ItemStack(Material.AIR);
		}

		if (!finalRecipe.isActive() || finalRecipe.getDisabledWorlds().contains(e.getBlock().getWorld().getName())
				|| Main.getInstance().disabledrecipe.contains(finalRecipe.getKey())) {

			logDebug(" Attempt to craft recipe was detected, but recipe is disabled!", finalRecipe.getName());
			return new ItemStack(Material.AIR);
		}

		// CONDITIONS
		ConditionSet cs = finalRecipe.getConditionSet();
		if (cs != null && !cs.isEmpty()) {
			if (!cs.test(e.getBlock().getLocation(), null, null)) {
				logDebug("Preventing craft due to failing required recipe conditions!", finalRecipe.getName());
				return new ItemStack(Material.AIR);
			}
		}

		if (passedCheck && found) {
			ItemStack item = new ItemStack(finalRecipe.getResult());
			handleAmountDeductions(e, finalRecipe.getName(), inv);
			return item;
		}

		return result;
	}

	boolean handleShapelessRecipe(Inventory inv, ItemStack result, Recipe recipe,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		// runs checks if recipe is shapeless

		logDebug("[handleShapeless] Handling shapeless checks..!", recipe.getName());
		if (getRecipeUtil().getRecipeFromResult(result) != null)
			recipe = getRecipeUtil().getRecipeFromResult(result);

		ArrayList<String> slotNames = new ArrayList<>();
		ArrayList<String> recipeNames = new ArrayList<>();

		ArrayList<Integer> recipeMD = new ArrayList<>();
		ArrayList<Integer> inventoryMD = new ArrayList<>();

		ArrayList<String> slotIDs = new ArrayList<>();
		ArrayList<String> recipeIDs = new ArrayList<>();

		Map<Material, Integer> recipeCount = new HashMap<>();
		Map<Material, Integer> inventoryCount = new HashMap<>();

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty()) {
				recipeCount.put(Material.AIR, recipeCount.getOrDefault(Material.AIR, 0) + 1);
				recipeNames.add("null");
				continue;
			}

			if (ingredient.hasIdentifier() && getRecipeUtil().getResultFromKey(ingredient.getIdentifier()) != null) {
				ItemStack exactMatch = getRecipeUtil().getResultFromKey(ingredient.getIdentifier());
				recipeCount.put(exactMatch.getType(), recipeCount.getOrDefault(exactMatch.getType(), 0) + 1);
				recipeIDs.add(ingredient.getIdentifier());

				// recipe model data
				if (Main.getInstance().serverVersionAtLeast(1, 14)) {
					if (exactMatch.hasItemMeta() && exactMatch.getItemMeta().hasCustomModelData()) {
						recipeMD.add(exactMatch.getItemMeta().getCustomModelData());
					} else {
						recipeMD.add(-1);
					}
				}

				// recipe names
				if (exactMatch.getItemMeta().hasDisplayName()) {
					recipeNames.add(exactMatch.getItemMeta().getDisplayName());
				} else {
					recipeNames.add("false");
				}
				continue;
			}

			recipeCount.put(ingredient.getMaterial(), recipeCount.getOrDefault(ingredient.getMaterial(), 0) + 1);
			recipeIDs.add("null");

			if (Main.getInstance().serverVersionAtLeast(1, 14))
				recipeMD.add(ingredient.getCustomModelData());

			if (ingredient.hasDisplayName()) {
				recipeNames.add(ingredient.getDisplayName());
			} else {
				recipeNames.add("false");
			}
		}

		for (int i = 0; i < 9; i++) {
			ItemStack it = inv.getItem(i);

			if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0) {
				slotNames.add("null");
				inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
				continue;
			}

			// check ids
			String key = getRecipeUtil().getKeyFromResult(it);
			if (key == null)
				key = "null";
			slotIDs.add(key);

			// inv model data
			if (Main.getInstance().serverVersionAtLeast(1, 14)) {
				if (it.hasItemMeta() && it.getItemMeta().hasCustomModelData()) {
					inventoryMD.add(it.getItemMeta().getCustomModelData());
				} else {
					inventoryMD.add(-1);
				}
			}

			inventoryCount.put(it.getType(), inventoryCount.getOrDefault(it.getType(), 0) + 1);

			if (!it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) {
				slotNames.add("false");
				continue;
			}

			slotNames.add(it.getItemMeta().getDisplayName());
		}

		boolean hasIngredients = true;
		for (Map.Entry<Material, Integer> entry : recipeCount.entrySet()) {
			Material material = entry.getKey();
			int requiredCount = entry.getValue();
			int invCount = inventoryCount.getOrDefault(material, 0);

			if (invCount < requiredCount) {
				hasIngredients = false;
				break;
			}
		}

		if (!hasIngredients) {
			logDebug("[handleShapeless] Missing required ingredients! Skipping recipe..", recipe.getName());
			return false;
		}

		logDebug("[handleShapeless] All required ingredients found..", recipe.getName());
		// checks hardcoded nbt tags
		if (!recipeIDs.equals(slotIDs) || !slotIDs.equals(recipeIDs)) {
			logDebug("[handleShapeless] NBT tags mismatch: recipe vs inventory", recipe.getName());
			return false;
		}

		// ignore data
		if (recipe.getIgnoreData())
			return true;

		// ignore model data
		if (!recipe.getIgnoreModelData()) {
			Map<Integer, Integer> recipeModelCount = new HashMap<>();
			Map<Integer, Integer> inventoryModelCount = new HashMap<>();

			for (int model : recipeMD) {
				recipeModelCount.put(model, recipeModelCount.getOrDefault(model, 0) + 1);
			}
			for (int model : inventoryMD) {
				inventoryModelCount.put(model, inventoryModelCount.getOrDefault(model, 0) + 1);
			}

			if (!recipeMD.containsAll(inventoryMD) || !inventoryMD.containsAll(recipeMD)) {
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory", recipe.getName());
				return false;
			}

			if (!recipeModelCount.equals(inventoryModelCount)) {
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory", recipe.getName());
				logDebug(" Recipe Model Data Map: " + recipeModelCount, recipe.getName());
				logDebug(" Inventory Model Data Map: " + inventoryModelCount, recipe.getName());
				return false;
			}
		}

		// ignore names
		if (!recipe.getIgnoreNames()) {
			Map<String, Integer> recipeNameCount = new HashMap<>();
			Map<String, Integer> slotNameCount = new HashMap<>();

			for (String name : recipeNames) {
				recipeNameCount.put(name, recipeNameCount.getOrDefault(name, 0) + 1);
			}
			for (String name : slotNames) {
				slotNameCount.put(name, slotNameCount.getOrDefault(name, 0) + 1);
			}

			if (!recipeNames.containsAll(slotNames) || !slotNames.containsAll(recipeNames)) {
				logDebug("[handleShapeless] Display name mismatch: recipe vs inventory", recipe.getName());
				logDebug(slotNames.toString(), recipe.getName());
				logDebug(recipeNames.toString(), recipe.getName());
				return false;
			}

			if (!recipeNameCount.equals(slotNameCount)) {
				logDebug("[handleShapeless] Display name count mismatch: recipe vs inventory", recipe.getName());
				logDebug(" RN: " + recipeNameCount, recipe.getName());
				logDebug(" IN: " + slotNameCount, recipe.getName());
				return false;
			}
		}
		return true;
	}

	void handleAmountDeductions(CrafterCraftEvent e, String recipeName, Inventory inv) {
		Recipe recipe = getRecipeUtil().getRecipe(recipeName);
		ArrayList<String> handledIngredients = new ArrayList<>();

		final int itemsToAdd = 1; // Crafter always crafts exactly one
		int itemsToRemove = 0;

		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			final Material mat = ingredient.getMaterial();
			final String displayName = ingredient.getDisplayName();
			final int requiredAmount = Math.max(1, ingredient.getAmount());
			final boolean hasIdentifier = ingredient.hasIdentifier();

			logDebug("[CrafterAmount] Handling ingredient: " + ingredient.getAbbreviation(), recipeName);

			// === Unified loop for all 9 crafting-grid slots ===
			for (int i = 0; i < 9; i++) {
				ItemStack item = inv.getItem(i);
				int slot = i;

				if (item == null || item.getType() == Material.AIR)
					continue;

				if (!AmountManager().matchesIngredient(item, recipeName, ingredient, mat, displayName, hasIdentifier))
					continue;

				// Same double-handling protection as player amount manager
				if (!handledIngredients.contains(ingredient.getAbbreviation())) {
					handlesItemRemoval(inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd, requiredAmount);
				}
			}

			handledIngredients.add(ingredient.getAbbreviation());
		}
	}

	// Helper method for the handleShiftClick method
	void handlesItemRemoval(Inventory inv, Recipe recipe, ItemStack item, RecipeUtil.Ingredient ingredient, int slot,
			int itemsToRemove, int itemsToAdd, int requiredAmount) {

		String recipeName = recipe.getName();
		logDebug("[handleShiftClicks] Checking slot " + slot + " for the recipe ", recipeName);

		if (AmountManager().matchesIngredient(item, recipeName, ingredient, ingredient.getMaterial(),
				ingredient.getDisplayName(), ingredient.hasIdentifier())) {

			itemsToRemove = (itemsToAdd * requiredAmount) - 1;

			int availableItems = item.getAmount();

			logDebug("[handleShiftClicks] Handling recipe " + recipeName, "");
			logDebug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove + " - ItemsToAdd: " + itemsToAdd, "");
			logDebug("[handleShiftClicks] ItemAmount: " + availableItems, "");
			logDebug("[handleShiftClicks] RequiredAmount: " + requiredAmount, "");
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " - HasIdentifier: "
					+ ingredient.hasIdentifier(), "");
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString(), "");
			logDebug("[handleShiftClicks] Displayname: " + ingredient.getDisplayName(), "");

			if (availableItems < requiredAmount)
				return;

			String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
			if (recipe.isLeftover(id)) {
				if (item.getType().toString().contains("_BUCKET"))
					item.setType(XMaterial.BUCKET.parseMaterial());
				return;
			}

			if ((item.getAmount() - itemsToRemove) <= 0) {
				inv.setItem(slot, null);
				return;
			}

			item.setAmount(item.getAmount() - itemsToRemove);
		}
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().shapedChecks;
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
