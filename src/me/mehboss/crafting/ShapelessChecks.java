package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;

public class ShapelessChecks {

	public boolean handleShapelessRecipe(CraftingInventory inv, Recipe recipe,
			List<RecipeUtil.Ingredient> recipeIngredients, UUID id) {

		logDebug("[handleShapeless] Handling shapeless checks..!", recipe.getName(), id);

		ArrayList<String> slotNames = new ArrayList<>();
		ArrayList<String> recipeNames = new ArrayList<>();

		ArrayList<Integer> recipeMD = new ArrayList<>();
		ArrayList<Integer> inventoryMD = new ArrayList<>();

		ArrayList<String> slotIDs = new ArrayList<>();
		ArrayList<String> recipeIDs = new ArrayList<>();

		Map<Material, Integer> recipeCount = new HashMap<>();
		Map<Material, Integer> inventoryCount = new HashMap<>();

		if (inv.getType() == InventoryType.CRAFTING) {
			inventoryCount.put(Material.AIR, 5);
			Collections.addAll(slotNames, "null", "null", "null", "null", "null");
			Collections.addAll(slotIDs, "null", "null", "null", "null", "null");
			Collections.addAll(inventoryMD, -1, -1, -1, -1, -1);
		}

		ItemStack[] matrix = inv.getMatrix();

		for (int i = 0; i < matrix.length; i++) {
			ItemStack it = matrix[i];

			if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0) {
				slotNames.add("null");
				slotIDs.add("null");
				inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
				continue;
			}

			String key = getRecipeUtil().getKeyFromResult(it);
			if (key == null)
				key = "null";
			slotIDs.add(key);

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

		// working copy used for consumption while matching
		Map<Material, Integer> availableCount = new HashMap<>(inventoryCount);

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty()) {
				recipeCount.put(Material.AIR, recipeCount.getOrDefault(Material.AIR, 0) + 1);
				recipeNames.add("null");
				recipeIDs.add("null");
				continue;
			}

			if (ingredient.hasIdentifier() && getRecipeUtil().getResultFromKey(ingredient.getIdentifier()) != null) {
				ItemStack exactMatch = getRecipeUtil().getResultFromKey(ingredient.getIdentifier());
				Material type = exactMatch.getType();

				recipeCount.put(type, recipeCount.getOrDefault(type, 0) + 1);
				availableCount.put(type, availableCount.getOrDefault(type, 0) - 1);

				recipeIDs.add(ingredient.getIdentifier());

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

			if (ingredient.hasMaterialChoices()) {
				boolean matched = false;
				for (Material choice : ingredient.getMaterialChoices()) {
					int avail = availableCount.getOrDefault(choice, 0);
					if (avail > 0) {
						availableCount.put(choice, avail - 1);
						recipeCount.put(choice, recipeCount.getOrDefault(choice, 0) + 1);
						matched = true;
						break;
					}
				}
				if (!matched) {
					return false;
				}
			} else {
				Material mat = ingredient.getMaterial();
				recipeCount.put(mat, recipeCount.getOrDefault(mat, 0) + 1);
				availableCount.put(mat, availableCount.getOrDefault(mat, 0) - 1);
			}

			recipeIDs.add("null");

			if (Main.getInstance().serverVersionAtLeast(1, 14))
				recipeMD.add(ingredient.getCustomModelData());

			if (ingredient.hasDisplayName()) {
				recipeNames.add(ingredient.getDisplayName());
			} else {
				recipeNames.add("false");
			}
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
			logDebug("[handleShapeless] Missing required ingredients! Skipping recipe..", recipe.getName(), id);
			return false;
		}
		logDebug("[handleShapeless] All required ingredients found..", recipe.getName(), id);

		if (!recipeIDs.equals(slotIDs) || !slotIDs.equals(recipeIDs)) {
			logDebug("[handleShapeless] NBT tags mismatch: recipe vs inventory", recipe.getName(), id);
			logDebug("[handleShapeless] recipeIDS: " + recipeIDs, recipe.getName(), id);
			logDebug("[handleShapeless] slotIDS: " + slotIDs, recipe.getName(), id);
			return false;
		}

		if (recipe.getIgnoreData())
			return true;

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
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory", recipe.getName(), id);
				return false;
			}

			if (!recipeModelCount.equals(inventoryModelCount)) {
				logDebug("[handleShapeless] Model data mismatch: recipe vs inventory", recipe.getName(), id);
				logDebug(" RM: " + recipeModelCount, recipe.getName(), id);
				logDebug(" IM: " + inventoryModelCount, recipe.getName(), id);
				return false;
			}
		}

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
				logDebug("[handleShapeless] Display name mismatch: recipe vs inventory", recipe.getName(), id);
				logDebug(slotNames.toString(), recipe.getName(), id);
				logDebug(recipeNames.toString(), recipe.getName(), id);
				return false;
			}

			if (!recipeNameCount.equals(slotNameCount)) {
				logDebug("[handleShapeless] Display name count mismatch: recipe vs inventory", recipe.getName(), id);
				logDebug(" RN: " + recipeNameCount, recipe.getName(), id);
				logDebug(" IN: " + slotNameCount, recipe.getName(), id);
				return false;
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
