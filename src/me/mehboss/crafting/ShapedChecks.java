package me.mehboss.crafting;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.CompatibilityUtil;

public class ShapedChecks {

	public class AlignedResult {
		public final ItemStack[] getMatrix;
		public final int[] invSlotMap; // maps aligned index → real inventory slot

		public AlignedResult(ItemStack[] alignedGrid, int[] invSlotMap) {
			this.getMatrix = alignedGrid;
			this.invSlotMap = invSlotMap;
		}
	}

	public AlignedResult getAlignedGrid(Inventory inv, List<Ingredient> recipeIngredients) {
		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;

		ItemStack[] invGrid = new ItemStack[9];
		int[] invSlotMap = new int[9]; // map each index to real inventory slot

		if (inv.getType() == InventoryType.CRAFTING) {
			// 2×2 player crafting grid
			int[] map = { 1, 2, -1, 3, 4, -1, -1, -1, -1 };

			for (int i = 0; i < 9; i++) {
				int real = map[i];
				if (real != -1) {
					ItemStack item = inv.getItem(real);
					invGrid[i] = (item == null ? new ItemStack(Material.AIR) : item);
					invSlotMap[i] = real;
				}
			}
		} else {
			int idx = 0;
			for (int slot = 0; slot < inv.getContents().length && idx < 9; slot++) {
				if (isCraftingInventory && slot == 0)
					continue;

				ItemStack item = inv.getItem(slot);
				invGrid[idx] = (item == null ? new ItemStack(Material.AIR) : item);
				invSlotMap[idx] = slot; // << real inventory slot
				idx++;
			}
		}

		// Build 3×3 recipe grid
		ItemStack[] recipeGrid = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			Ingredient ing = recipeIngredients.get(i);
			if (ing.isEmpty())
				recipeGrid[i] = new ItemStack(Material.AIR);
			else if (ing.hasIdentifier())
				recipeGrid[i] = getRecipeUtil().getResultFromKey(ing.getIdentifier());
			else
				recipeGrid[i] = new ItemStack(ing.getMaterialChoices().get(0));
		}

		// Trim recipe bounding box
		int rTop = 3, rBottom = -1, rLeft = 3, rRight = -1;
		for (int i = 0; i < 9; i++) {
			if (recipeGrid[i].getType() != Material.AIR) {
				int r = i / 3;
				int c = i % 3;
				rTop = Math.min(rTop, r);
				rBottom = Math.max(rBottom, r);
				rLeft = Math.min(rLeft, c);
				rRight = Math.max(rRight, c);
			}
		}

		int recipeHeight = rBottom - rTop + 1;
		int recipeWidth = rRight - rLeft + 1;

		// Try all shifts
		for (int shiftRow = 0; shiftRow <= 3 - recipeHeight; shiftRow++) {
			for (int shiftCol = 0; shiftCol <= 3 - recipeWidth; shiftCol++) {

				boolean match = true;

				for (int rr = 0; rr < recipeHeight && match; rr++) {
					for (int rc = 0; rc < recipeWidth && match; rc++) {

						int invIndex = (shiftRow + rr) * 3 + (shiftCol + rc);
						int recIndex = (rTop + rr) * 3 + (rLeft + rc);

						ItemStack invItem = invGrid[invIndex];
						Ingredient ing = recipeIngredients.get(recIndex);

						if (ing.isEmpty())
							continue;

						if (invItem == null || !ing.getMaterialChoices().contains(invItem.getType()))
							match = false;
					}
				}

				if (match) {
					ItemStack[] aligned = new ItemStack[9];
					int[] map = new int[9];

					Arrays.fill(aligned, new ItemStack(Material.AIR));
					Arrays.fill(map, -1);

					// Copy matched region
					for (int rr = 0; rr < recipeHeight; rr++) {
						for (int rc = 0; rc < recipeWidth; rc++) {

							int invIndex = (shiftRow + rr) * 3 + (shiftCol + rc);
							int recIndex = (rTop + rr) * 3 + (rLeft + rc);

							aligned[recIndex] = invGrid[invIndex];
							map[recIndex] = invSlotMap[invIndex]; // <<< real inventory slot
						}
					}

					return new AlignedResult(aligned, map);
				}
			}
		}

		return null;
	}

	public boolean handleShapedRecipe(InventoryType type, AlignedResult alignedGrid, Recipe recipe,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		int i = -1;
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

			i++;
			ItemStack stack = alignedGrid.getMatrix[i];
			if (ingredient.isEmpty()) {
				if (stack.getType() != Material.AIR)
					return false;

				continue;
			}

			ItemMeta meta = stack.getItemMeta();
			// IDENTIFIER CHECK
			if (ingredient.hasIdentifier()) {
				String invID = getRecipeUtil().getKeyFromResult(stack);

				if (invID == null || !invID.equals(ingredient.getIdentifier())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] IDs do not match", recipe.getName());
					logDebug("[handleShaped] IngID = " + ingredient.getIdentifier(), recipe.getName());
					logDebug("[handleShaped] InvID = " + invID, recipe.getName());
					return false;
				}
				logDebug("[handleShaped] Passed 'ID' checks for the ingredient in slot " + i, recipe.getName());
				continue;
			}

			if (ingredient.hasItem()) {
				if (!ingredient.getItem().isSimilar(stack)) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] Ingredient has item, and item found does not match.", recipe.getName());
					return false;
				}
				logDebug("[handleShaped] Passed 'ITEM' checks for the ingredient in slot " + i, recipe.getName());
				continue;
			}

			logDebug("[handleShaped] Ingredient slot " + i + " does not have an identifier..", recipe.getName());

			if (recipe.getIgnoreData()) {
				logDebug("[handleShaped] Skipping data checks.. Ignore-Data is enabled..", recipe.getName());
				continue;
			}

			// NAME CHECKS
			if (!recipe.getIgnoreNames()) {
				if ((!CompatibilityUtil.hasDisplayname(meta, recipe.isLegacyNames()) && ingredient.hasDisplayName())
						|| (CompatibilityUtil.hasDisplayname(meta, recipe.isLegacyNames())
								&& !ingredient.hasDisplayName())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug(
							"[handleShaped] The recipe ingredient displayname and the inventory slot displayname do not match",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] Does the ingredient have a displayname? " + ingredient.hasDisplayName(),
							recipe.getName());
					logDebug("[handleShaped] Does the inventory have a displayname? " + meta.hasDisplayName(),
							recipe.getName());
					return false;
				}

				if (ingredient.hasDisplayName() && CompatibilityUtil.hasDisplayname(meta, recipe.isLegacyNames())
						&& !(ingredient.getDisplayName()
								.equals(CompatibilityUtil.getDisplayname(meta, recipe.isLegacyNames())))) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] The ingredient name for the recipe and inventory do not match",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] The ingredient displayname: " + ingredient.getDisplayName(),
							recipe.getName());
					logDebug("[handleShaped] The inventory displayname: " + meta.getDisplayName(), recipe.getName());
					return false;
				}

				logDebug("[handleShaped] Inventory and recipe ingredient displayname matched for slot " + i,
						recipe.getName());
			}

			// MODEL DATA CHECKS
			if (!recipe.getIgnoreModelData()) {
				if (!CompatibilityUtil.supportsCustomModelData()) {
					logDebug("[handleShaped] Skipping CMD checks.. Version is less than 1.14..", recipe.getName());
					continue;
				}

				if ((!meta.hasCustomModelData() && ingredient.hasCustomModelData())
						|| (meta.hasCustomModelData() && !ingredient.hasCustomModelData())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] The recipe ingredient CMD and the inventory slot CMD do not match..",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] Does the ingredient have CMD? " + ingredient.hasCustomModelData(),
							recipe.getName());
					logDebug("[handleShaped] Does the inventory have CMD? " + meta.hasCustomModelData(),
							recipe.getName());
					return false;
				}

				if (ingredient.hasCustomModelData() && meta.hasCustomModelData()
						&& (ingredient.getCustomModelData() != meta.getCustomModelData())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] The ingredient CMD for the recipe and inventory do not match..",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] The ingredient CMD: " + ingredient.getCustomModelData(), recipe.getName());
					logDebug("[handleShaped] The inventory CMD: " + meta.getCustomModelData(), recipe.getName());
					return false;
				}

				logDebug("[handleShaped] Inventory and recipe ingredient CMD matched for slot " + i, recipe.getName());
			}

			// ITEM MODEL CHECKS
			if (!recipe.getIgnoreItemModel()) {
				if (!CompatibilityUtil.supportsItemModel()) {
					logDebug("[handleShaped] Skipping IM checks.. Version is less than 1.21.4..", recipe.getName());
					continue;
				}

				if ((!meta.hasItemModel() && ingredient.hasItemModel())
						|| (meta.hasItemModel() && !ingredient.hasItemModel())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] The recipe ingredient IM and the inventory slot IM do not match..",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] Does the ingredient have IM? " + ingredient.hasItemModel(),
							recipe.getName());
					logDebug("[handleShaped] Does the inventory have IM? " + meta.hasItemModel(), recipe.getName());
					return false;
				}

				if (ingredient.hasItemModel() && meta.hasItemModel()
						&& (ingredient.getItemModel() != meta.getItemModel().toString())) {
					logDebug("[handleShaped] Skipping recipe..", recipe.getName());
					logDebug("[handleShaped] The ingredient IM for the recipe and inventory do not match..",
							recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot(), recipe.getName());
					logDebug("[handleShaped] The ingredient IM: " + ingredient.getItemModel(), recipe.getName());
					logDebug("[handleShaped] The inventory IM: " + meta.getItemModel(), recipe.getName());
					return false;
				}

				logDebug("[handleShaped] Inventory and recipe ingredient IM matched for slot " + i, recipe.getName());
			}
		}

		return true;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	void logDebug(String st, String recipeName, UUID id) {
		if (Main.getInstance().debug && (id == null || (!Main.getInstance().inInventory.contains(id))))
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}

	void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}

}
