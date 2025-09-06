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
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CrafterManager implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCollectResult(CrafterCraftEvent e) {
		Container block = (Container) e.getBlock().getState();
		Inventory inv = block.getInventory();
		e.setResult(handleCraftingChecks(inv, e.getResult()));
	}
	
	ItemStack handleCraftingChecks(Inventory inv, ItemStack result) {
		Recipe finalRecipe = null;
		Boolean passedCheck = true;
		Boolean found = false;

		for (String recipes : recipeUtil.getRecipeNames()) {

			Recipe recipe = recipeUtil.getRecipe(recipes);
			finalRecipe = recipe;

			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();

			if (recipe.getType() != RecipeType.SHAPELESS && recipe.getType() != RecipeType.SHAPED)
				continue;

			if (!CraftManager().hasAllIngredients(inv, recipe.getName(), recipeIngredients)) {
				logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not match for recipe "
						+ recipe.getName());
				passedCheck = false;
				continue;
			}

			logDebug("[handleCrafting] Inventory contained all of the ingredients for the recipe " + recipe.getName()
					+ ". Continuing checks..");

			passedCheck = true;
			found = true;

			if (recipe.getType() == RecipeType.SHAPELESS) {
				if (!handleShapelessRecipe(inv, result, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;
				}
			} else {

				if (recipeUtil.getRecipeFromResult(result) != null) {
					recipe = recipeUtil.getRecipeFromResult(result);

					if (recipe.isExactChoice()) {
						logDebug("[handleCrafting] Found recipe " + recipe.getName() + " to handle..");
						logDebug("[handleCrafting] Manual checks and methods skipped because exactChoice is enabled!");

						if (!(amountsMatch(inv, recipe.getName(), recipe.getIngredients(), true))) {
							logDebug("[handleCrafting] Skipping recipe.. ");
							logDebug("The amount check indicates that the requirements have not been met for recipe "
									+ recipe.getName());
							return new ItemStack(Material.AIR);
						}

						break;
					}
				}

				if (!CraftManager().hasShapedIngredients(inv, recipe.getName(), recipeIngredients)) {
					logDebug(
							"[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match for recipe "
									+ recipe.getName());
					found = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched for recipe " + recipe.getName()
						+ ". Continuing system checks..");

				if (recipe.getIgnoreData() == true)
					continue;

				if (!handleShapedRecipe(inv, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;

				}
			}

			if (!(amountsMatch(inv, recipe.getName(), recipeIngredients, true))) {
				logDebug(
						"[handleCrafting] Skipping recipe.. The amount check indicates that the requirements have not been met for recipe "
								+ recipe.getName());
				passedCheck = false;
				continue;
			}

			if (passedCheck && found) {
				break;

			}
		}

		logDebug("[handleCrafting] Final results for recipe " + finalRecipe.getName().toUpperCase() + " (passedChecks: "
				+ passedCheck + ")(foundRecipe: " + found + ")");

		if (CraftManager().hasVanillaIngredients(inv, result) || !found)
			return result;

		if ((!passedCheck) || (passedCheck && !found)) {
			return new ItemStack(Material.AIR);
		}

		if (!finalRecipe.isActive()) {
			logDebug(" Attempt to craft " + finalRecipe.getName() + " was detected, but recipe is disabled!");
			return new ItemStack(Material.AIR);
		}

		if (passedCheck && found) {
			ItemStack item = new ItemStack(finalRecipe.getResult());
			if (!finalRecipe.isExactChoice())
				return item;
		}
		
		return result;
	}
	
	boolean amountsMatch(Inventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {

		logDebug("[amountsMatch] Checking recipe " + recipeName);
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			int slot = ingredient.getSlot();
			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPED) {
				ItemStack invSlot = inv.getContents()[slot - 1];

				if (!CraftManager().validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;
			}

			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPELESS) {
				slot = 0;
				for (ItemStack item : inv.getContents()) {
					if (!CraftManager().validateItem(item, ingredient, recipeName, slot, debug, true))
						return false;
					slot++;
				}
			}
		}
		return true;
	}
	
	boolean handleShapelessRecipe(Inventory inv, ItemStack result, Recipe recipe, List<RecipeUtil.Ingredient> recipeIngredients) {
		// runs checks if recipe is shapeless

		if (recipeUtil.getRecipeFromResult(result) != null) {
			recipe = recipeUtil.getRecipeFromResult(result);

			if (recipe.isExactChoice()) {
				logDebug("[handleShapeless] Found recipe " + recipe.getName() + " to handle..");
				logDebug("[handleShapeless] ExactChoice is enabled, but does not work for shapeless recipes!");
			}
		}

		ArrayList<String> slotNames = new ArrayList<>();
		ArrayList<String> recipeNames = new ArrayList<>();

		ArrayList<Integer> recipeMD = new ArrayList<>();
		ArrayList<Integer> inventoryMD = new ArrayList<>();

		Map<Material, Integer> recipeCount = new HashMap<>();
		Map<Material, Integer> inventoryCount = new HashMap<>();

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty()) {
				recipeCount.put(Material.AIR, recipeCount.getOrDefault(Material.AIR, 0) + 1);
				recipeNames.add("null");
				continue;
			}

			if (ingredient.hasIdentifier() && recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {
				ItemStack exactMatch = recipeUtil.getResultFromKey(ingredient.getIdentifier());
				recipeCount.put(exactMatch.getType(), recipeCount.getOrDefault(exactMatch.getType(), 0) + 1);

				if (Main.getInstance().serverVersionAtLeast(1, 14) && exactMatch.hasItemMeta()
						&& exactMatch.getItemMeta().hasCustomModelData()) {
					recipeMD.add(exactMatch.getItemMeta().getCustomModelData());
				}

				if (exactMatch.getItemMeta().hasDisplayName()) {
					recipeNames.add(exactMatch.getItemMeta().getDisplayName());
				} else {
					recipeNames.add("false");
				}
				continue;
			}

			recipeCount.put(ingredient.getMaterial(), recipeCount.getOrDefault(ingredient.getMaterial(), 0) + 1);

			if (Main.getInstance().serverVersionAtLeast(1, 14))
				recipeMD.add(ingredient.getCustomModelData());

			if (ingredient.hasDisplayName()) {
				recipeNames.add(ingredient.getDisplayName());
			} else {
				recipeNames.add("false");
			}
		}

		for (int slot = 0; slot < 9; slot++) {
			if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.AIR) {
				slotNames.add("null");
				inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
				continue;
			}

			if (Main.getInstance().serverVersionAtLeast(1, 14)) {
				if (inv.getItem(slot).hasItemMeta() && inv.getItem(slot).getItemMeta().hasCustomModelData()) {
					inventoryMD.add(inv.getItem(slot).getItemMeta().getCustomModelData());
				} else {
					inventoryMD.add(-1);
				}
			}

			inventoryCount.put(inv.getItem(slot).getType(),
					inventoryCount.getOrDefault(inv.getItem(slot).getType(), 0) + 1);

			if (!(inv.getItem(slot).getItemMeta().hasDisplayName())) {
				slotNames.add("false");
				continue;
			}

			if (recipeUtil.getRecipeFromResult(inv.getItem(slot)) != null
					&& !(NBTEditor.contains(inv.getItem(slot), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"))) {
				continue;
			}

			slotNames.add(inv.getItem(slot).getItemMeta().getDisplayName());
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
			logDebug("[handleShapeless] The recipe " + recipe.getName()
					+ " does not have all of the required ingredients! Skipping recipe..");
			return false;
		}

		logDebug("[handleShapeless] The recipe " + recipe.getName() + " does have all of the required ingredients!");

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
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory");
				return false;
			}

			if (!recipeModelCount.equals(inventoryModelCount)) {
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory");
				logDebug(" Recipe Model Data Map: " + recipeModelCount);
				logDebug(" Inventory Model Data Map: " + inventoryModelCount);
				return false;
			}
		}

		if (!recipe.getIgnoreData()) {
			Map<String, Integer> recipeNameCount = new HashMap<>();
			Map<String, Integer> slotNameCount = new HashMap<>();

			for (String name : recipeNames) {
				recipeNameCount.put(name, recipeNameCount.getOrDefault(name, 0) + 1);
			}
			for (String name : slotNames) {
				slotNameCount.put(name, slotNameCount.getOrDefault(name, 0) + 1);
			}

			if (!recipeNames.containsAll(slotNames) || !slotNames.containsAll(recipeNames)) {
				logDebug("[handleShapeless] Display name mismatch: recipe vs inventory");
				return false;
			}

			if (!recipeNameCount.equals(slotNameCount)) {
				logDebug("[handleShapeless] Display name count mismatch: recipe vs inventory");
				logDebug(" Recipe Name Map: " + recipeNameCount);
				logDebug(" Inventory Name Map: " + slotNameCount);
				return false;
			}
		}
		return true;
	}

	boolean handleShapedRecipe(Inventory inv, Recipe recipe, List<RecipeUtil.Ingredient> recipeIngredients) {
		int i = -1;

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			i++;

			if (!ingredient.isEmpty() && !(inv.contains(ingredient.getMaterial()))) {
				logDebug("[handleShaped] Initial ingredient check for recipe " + recipe.getName()
						+ " was false. Skipping recipe..");
				return false;
			}

			if (inv.getItem(i) == null && !(ingredient.isEmpty())) {
				logDebug("[handleShaped] Slot " + i + " did not have the required ingredient for recipe "
						+ recipe.getName() + ". Skipping recipe..");
				return false;
			}

			if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
				ItemMeta meta = inv.getItem(i).getItemMeta();

				if (ingredient.hasIdentifier() && recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {

					ItemStack ingredientItem = recipeUtil.getResultFromKey(ingredient.getIdentifier());

					if (!inv.getItem(i).isSimilar(ingredientItem)
							&& !CraftManager().checkCustomItems(ingredientItem, inv.getItem(i), ingredient.getIdentifier(), true)) {
						logDebug("[handleShaped] Skipping recipe.. Recipe: " + recipe.getName());
						logDebug("[handleShaped] Ingredient hasID: " + ingredient.hasIdentifier());
						logDebug("[handleShaped] isSimilar: "
								+ inv.getItem(i).isSimilar(recipeUtil.getResultFromKey(ingredient.getIdentifier())));
						logDebug("[handleShaped] invIngredient ID is "
								+ NBTEditor.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"));
						logDebug("[handleShaped] recipeIngredient ID is " + ingredient.getIdentifier());

						return false;
					}

					logDebug("[handleShaped] Passed all required checks for the recipe ingredient in slot " + i
							+ " for recipe " + recipe.getName());
					continue;

				}

				logDebug("[handleShaped] The recipe " + recipe.getName() + " ingredient slot " + i
						+ " does not have an identifier.");

				// checks if displayname is null
				if ((!meta.hasDisplayName() && ingredient.hasDisplayName())
						|| (meta.hasDisplayName() && !ingredient.hasDisplayName())) {
					logDebug("[handleShaped] Skipping recipe..");
					logDebug(
							"[handleShaped] The recipe ingredient displayname and the inventory slot displayname do not match for recipe "
									+ recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot());
					logDebug("[handleShaped] Does the ingredient have a displayname? " + ingredient.hasDisplayName());
					logDebug("[handleShaped] Does the inventory have a displayname? " + meta.hasDisplayName());
					return false;
				}

				if (ingredient.hasDisplayName() && meta.hasDisplayName()
						&& !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
					logDebug("[handleShaped] Skipping recipe..");
					logDebug(
							"[handleShaped] The ingredient name for the recipe and inventory do not match for recipe "
									+ recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot());
					logDebug("[handleShaped] The ingredient displayname: " + ingredient.getDisplayName());
					logDebug("[handleShaped] The inventory displayname: " + meta.getDisplayName());
					return false;
				}

				logDebug("[handleShaped] Inventory and recipe ingredient displayname matched for slot " + i);

				// checks if displayname is null
				if ((!meta.hasCustomModelData() && ingredient.hasCustomModelData())
						|| (meta.hasCustomModelData() && !ingredient.hasCustomModelData())) {
					logDebug("[handleShaped] Skipping recipe..");
					logDebug(
							"[handleShaped] The recipe ingredient CMD and the inventory slot CMD do not match for recipe "
									+ recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot());
					logDebug("[handleShaped] Does the ingredient have CMD? " + ingredient.hasCustomModelData());
					logDebug("[handleShaped] Does the inventory have CMD? " + meta.hasCustomModelData());
					return false;
				}

				if (ingredient.hasCustomModelData() && meta.hasCustomModelData()
						&& (ingredient.getCustomModelData() != meta.getCustomModelData())) {
					logDebug("[handleShaped] Skipping recipe..");
					logDebug("[handleShaped] The ingredient CMD for the recipe and inventory do not match for recipe "
							+ recipe.getName());
					logDebug("[handleShaped] The inventory slot in question: " + i
							+ ". The ingredient slot in question: " + ingredient.getSlot());
					logDebug("[handleShaped] The ingredient CMD: " + ingredient.getCustomModelData());
					logDebug("[handleShaped] The inventory CMD: " + meta.getCustomModelData());
					return false;
				}
				logDebug("[handleShaped] Inventory and recipe ingredient CMD matched for slot " + i);
			}
		}
		return true;
	}
	
	RecipeUtil recipeUtil = CraftManager().recipeUtil;
	CraftManager CraftManager() {
		return Main.getInstance().craftManager;
	}
	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "][Crafter]" + st);
	}
}
