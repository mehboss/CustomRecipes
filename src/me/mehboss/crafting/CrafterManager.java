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

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;
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

			if (recipe.getType() == RecipeType.SHAPELESS) {
				if (!handleShapelessRecipe(inv, result, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;
				}
			} else {

				if (!CraftManager().hasShapedIngredients(inv, recipe.getName(), recipeIngredients)) {
					logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match..",
							recipe.getName());
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched. Continuing checks..", recipe.getName());

				if (recipe.getIgnoreData() == true)
					continue;

				if (!handleShapedRecipe(inv, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;

				}
			}

			if (!(amountsMatch(inv, recipe.getName(), recipeIngredients, true))) {
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

		// checks recipe conditions
		ConditionSet cs = finalRecipe.getConditionSet();
		if (cs != null && !cs.isEmpty()) {
			if (!cs.test(e.getBlock().getLocation(), null, null)) {
				logDebug("Preventing craft of due to failing required recipe conditions!", finalRecipe.getName());
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

	boolean amountsMatch(Inventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {

		logDebug("[amountsMatch] Checking recipe amounts..", recipeName);
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			int slot = ingredient.getSlot();
			if (getRecipeUtil().getRecipe(recipeName).getType() == RecipeType.SHAPED) {
				ItemStack invSlot = inv.getContents()[slot - 1];

				if (!CraftManager().validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;
			}

			if (getRecipeUtil().getRecipe(recipeName).getType() == RecipeType.SHAPELESS) {
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

	boolean handleShapelessRecipe(Inventory inv, ItemStack result, Recipe recipe,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		// runs checks if recipe is shapeless

		if (getRecipeUtil().getRecipeFromResult(result) != null) {
			recipe = getRecipeUtil().getRecipeFromResult(result);

			if (recipe.isExactChoice()) {
				logDebug("[handleShapeless] Handling recipe..", recipe.getName());
				logDebug("[handleShapeless] ExactChoice is enabled, but does not work for shapeless recipes!",
						recipe.getName());
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

			if (ingredient.hasIdentifier() && getRecipeUtil().getResultFromKey(ingredient.getIdentifier()) != null) {
				ItemStack exactMatch = getRecipeUtil().getResultFromKey(ingredient.getIdentifier());
				recipeCount.put(exactMatch.getType(), recipeCount.getOrDefault(exactMatch.getType(), 0) + 1);

				if (Main.getInstance().serverVersionAtLeast(1, 14)) {
					if (exactMatch.hasItemMeta() && exactMatch.getItemMeta().hasCustomModelData()) {
						recipeMD.add(exactMatch.getItemMeta().getCustomModelData());
					} else {
						recipeMD.add(-1);
					}
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

		for (int i = 0; i < 9; i++) {
			ItemStack it = inv.getItem(i);

			if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0) {
				slotNames.add("null");
				inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
				continue;
			}

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

			if (getRecipeUtil().getRecipeFromResult(it) != null
					&& getRecipeUtil().getRecipeFromResult(it).isCustomTagged()
					&& !NBTEditor.contains(it, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
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
				logDebug("[handleShapeless] Display name mismatch: recipe vs inventory", recipe.getName());
				logDebug(slotNames.toString(), recipe.getName());
				logDebug(recipeNames.toString(), recipe.getName());
				return false;
			}

			if (!recipeNameCount.equals(slotNameCount)) {
				logDebug("[handleShapeless] Display name count mismatch: recipe vs inventory", recipe.getName());
				logDebug(" Recipe Name Map: " + recipeNameCount, recipe.getName());
				logDebug(" Inventory Name Map: " + slotNameCount, recipe.getName());
				return false;
			}
		}
		return true;
	}

	boolean handleShapedRecipe(Inventory inv, Recipe recipe, List<RecipeUtil.Ingredient> recipeIngredients) {
		int i = -1;

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			i++;

			if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
				ItemMeta meta = inv.getItem(i).getItemMeta();

				if (ingredient.hasIdentifier()
						&& getRecipeUtil().getResultFromKey(ingredient.getIdentifier()) != null) {

					ItemStack ingredientItem = getRecipeUtil().getResultFromKey(ingredient.getIdentifier());

					if (!inv.getItem(i).isSimilar(ingredientItem) && !CraftManager().checkCustomItems(ingredientItem,
							inv.getItem(i), ingredient.getIdentifier(), true)) {
						logDebug("[handleShaped] Skipping recipe..", recipe.getName());
						logDebug("[handleShaped] Ingredient hasID: " + ingredient.hasIdentifier(), recipe.getName());
						logDebug(
								"[handleShaped] isSimilar: " + inv.getItem(i)
										.isSimilar(getRecipeUtil().getResultFromKey(ingredient.getIdentifier())),
								recipe.getName());
						logDebug("[handleShaped] invIngredient ID is "
								+ NBTEditor.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"),
								recipe.getName());
						logDebug("[handleShaped] recipeIngredient ID is " + ingredient.getIdentifier(),
								recipe.getName());

						return false;
					}

					logDebug("[handleShaped] Passed all checks for the ingredient in slot " + i, recipe.getName());
					continue;

				}

				logDebug("[handleShaped] Ingredient slot " + i + " does not have an identifier..", recipe.getName());

				// checks if displayname is null
				if ((!meta.hasDisplayName() && ingredient.hasDisplayName())
						|| (meta.hasDisplayName() && !ingredient.hasDisplayName())) {
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

				if (ingredient.hasDisplayName() && meta.hasDisplayName()
						&& !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
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

				// checks if displayname is null
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
		}
		return true;
	}

	void handleAmountDeductions(CrafterCraftEvent e, String recipeName, Inventory inv) {
		Recipe recipe = getRecipeUtil().getRecipe(recipeName);
		ArrayList<String> handledIngredients = new ArrayList<String>();

		int itemsToAdd = 1;
		int itemsToRemove = 0;
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			final Material material = ingredient.getMaterial();
			final String displayName = ingredient.getDisplayName();
			final int requiredAmount = Math.max(1, ingredient.getAmount());
			final boolean hasIdentifier = ingredient.hasIdentifier();

			// === Your existing per-type logic ===
			if (recipe.getType() == RecipeType.SHAPELESS) {
				logDebug("[handleShiftClicks] Found shapeless recipe to handle..", recipeName);

				for (int i = 0; i < 9; i++) {
					ItemStack item = inv.getItem(i);
					int slot = i;

					if (item == null || item.getType() == Material.AIR)
						continue;

					if (!AmountManager().matchesIngredient(item, recipeName, ingredient, material, displayName,
							hasIdentifier))
						continue;

					// If your original removal didn’t require an explicit matchesIngredient()
					// here, leave it as-is. (You earlier said only Pass 1 needs matching.)
					if (!handledIngredients.contains(ingredient.getAbbreviation())) {
						handlesItemRemoval(inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd,
								requiredAmount);
					}
				}

				if (!handledIngredients.contains(ingredient.getAbbreviation()))
					handledIngredients.add(ingredient.getAbbreviation());

			} else {
				logDebug("[handleShiftClicks] Found other recipe type to handle..", recipeName);
				int slot = ingredient.getSlot() - 1;
				ItemStack item = inv.getItem(slot);

				if (item == null || item.getType() == Material.AIR)
					continue;

				// Same note: keep your original behavior.
				handlesItemRemoval(inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd, requiredAmount);
			}
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
