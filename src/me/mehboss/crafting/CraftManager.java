package me.mehboss.crafting;

/*
 * Mozilla Public License v2.0
 * 
 * Author: Mehboss
 * Copyright (c) 2023 Mehboss
 * Spigot: https://www.spigotmc.org/resources/authors/mehboss.139036/
 *
 * DO NOT REMOVE THIS SECTION IF YOU WISH TO UTILIZE ANY PORTIONS OF THIS CODE
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import dev.lone.itemsadder.api.CustomStack;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CraftManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
	ArrayList<Player> inInventory = new ArrayList<Player>();

	ArrayList<String> disabledrecipe() {
		return Main.getInstance().disabledrecipe;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	FileConfiguration customConfig() {
		return Main.getInstance().customConfig;
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}

	void sendMessages(Player p, String s, long seconds) {
		Main.getInstance().sendMessages(p, s, seconds);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe", recipe);
		Main.getInstance().sendnoPerms(p);
	}

	public boolean matchedRecipe(CraftingInventory inv) {
//		if (result == null || result == new ItemStack(Material.AIR)) {
		if (Main.getInstance().serverVersionAtLeast(1, 13)) {
			if (inv.isEmpty()) {
				logDebug("[matchedRecipe] Could not find a recipe to match with!", "");
				return false;
			}
		} else {
			boolean isEmpty = true;
			for (ItemStack item : inv.getContents())
				if (item != null && item.getType() != Material.AIR) {
					isEmpty = false;
					break;
				}
			if (isEmpty) {
				logDebug("[matchedRecipe] Could not find a recipe to match with!", "");
				return false;
			}
		}
		return true;
	}

	boolean validateItem(ItemStack item, RecipeUtil.Ingredient ingredient, String recipeName, int slot, boolean debug,
			boolean returnType) {

		ItemStack recipe = recipeUtil.getRecipeFromResult(item) != null
				? recipeUtil.getRecipeFromResult(item).getResult()
				: null;
		ItemStack exactMatch = ingredient.hasIdentifier() ? recipeUtil.getResultFromKey(ingredient.getIdentifier())
				: recipe;

		if (item == null || (ingredient.hasIdentifier() && exactMatch == null))
			return returnType;

		// Attempt to match the ingredient to a recipe using the identifier
		// Attempt to match the ingredient to an itemsadder item
		// If all else fail, match the itemstack to the regular ingredient requirements
		if ((ingredient.hasIdentifier() && item.isSimilar(exactMatch))
				|| (ingredient.hasIdentifier() && checkCustomItems(exactMatch, item, ingredient.getIdentifier(), false))
				|| (item.getType() == ingredient.getMaterial() && hasMatchingDisplayName(recipeName, item,
						ingredient.getDisplayName(), ingredient.getIdentifier(), ingredient.hasIdentifier(), false))) {

			if (debug)
				logDebug("[amountsMatch] Checking slot " + slot + " for required amounts.. ", recipeName);

			if (item.getAmount() < ingredient.getAmount()) {

				if (debug) {
					logDebug("[amountsMatch] Item amount: " + item.getAmount(), recipeName);
					logDebug("[amountsMatch] Required amount: " + ingredient.getAmount(), recipeName);
				}
				return false;
			}

			if (debug)
				logDebug("[amountsMatch] Item amount and ingredient amounts matched (#" + ingredient.getAmount() + ")",
						recipeName);

			return true;
		}
		return returnType;
	}

	boolean amountsMatch(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {

		logDebug("[amountsMatch] Checking recipe amounts..", recipeName);
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			int slot = ingredient.getSlot();
			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPED) {
				ItemStack invSlot = inv.getMatrix()[slot - 1];

				if (!validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;
			}

			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPELESS) {
				slot = 0;
				for (ItemStack item : inv.getMatrix()) {
					slot++;
					if (!validateItem(item, ingredient, recipeName, slot, debug, true))
						return false;
				}
			}
		}
		return true;
	}

	boolean hasCooldown(Player p, Recipe recipe) {
		if (recipe.hasCooldown())
			if (!(Main.getInstance().cooldownManager.cooldownExpired(p.getUniqueId(), recipe.getKey())))
				return true;

		return false;
	}

	@SuppressWarnings("deprecation")
	boolean isBlacklisted(ItemStack result, Player p) {
		if (customConfig().getBoolean("blacklist-recipes")) {
			for (String item : disabledrecipe()) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				if (result == null)
					return false;

				if (!customConfig().isConfigurationSection("vanilla-recipes." + split[0]))
					continue;

				if (!XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
					continue;
				}
				i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				logDebug("[isBlacklisted] Found " + disabledrecipe().size() + " disabled recipes.", "");
				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if (i == null)
					continue;

				if ((NBTEditor.contains(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id)
						&& NBTEditor.getString(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id).equals(id)
						&& recipeUtil.getRecipe(id) == null) || result.isSimilar(i)) {

					if (getPerm != null && !getPerm.equalsIgnoreCase("none") && p.hasPermission("crecipe." + getPerm)) {
						logDebug("[isBlacklisted] Player " + p.getName() + " does have required permission '" + getPerm
								+ "' for item " + item, "");
						return false;
					}

					logDebug("[isBlacklisted] Player " + p.getName() + " does not have required permission '" + getPerm
							+ "' for item " + item + " or this recipe has been globally blacklisted!", "");

					sendMessages(p, getPerm, 0);
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean debug) {
		if (recipeUtil.getRecipe(recipeName).getIgnoreData() == true)
			return true;

		ItemMeta itemM = item.getItemMeta();
		String recipeIdentifier = NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				? NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				: "none";

		if (hasIdentifier && identifier.equals(recipeIdentifier))
			return true;

		if (debug)
			logDebug("[hasMatchingDisplayName] Checking displayname..", recipeName);

		if (displayName == null || displayName.equals("false") || displayName.equals("none")) {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found that ingredient does not have a displayname to check",
						recipeName);
			return !item.hasItemMeta() || !itemM.hasDisplayName();

		} else {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found displayname " + displayName
						+ " and the match check came back: " + Boolean.valueOf(item.hasItemMeta()
								&& item.getItemMeta().hasDisplayName() && itemM.getDisplayName().equals(displayName)),
						recipeName);
			return item.hasItemMeta() && itemM.hasDisplayName() && itemM.getDisplayName().equals(displayName);
		}
	}

	// Checks if inventory slots and recipe materials match
	public boolean hasShapedIngredients(Inventory inv, String recipeName,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		int slot = 0;
		for (ItemStack contents : inv.getContents()) {
			if (isCraftingInventory && slot == 0) {
				slot++;
				continue;
			}
			if (invMaterials.size() == 9)
				break;

			if (contents == null || contents.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(contents.getType().toString());
		}

		slot = 0;
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

			if (ingredient.isEmpty() && !invMaterials.get(slot).equals("null")) {
				logDebug("[hasShapedIngredients] Required ingredient not found in slot " + slot, recipeName);
				return false;
			}

			if (!ingredient.isEmpty()) {
				if (ingredient.hasIdentifier()) {
					String ingMat = recipeUtil.getResultFromKey(ingredient.getIdentifier()).getType().toString();
					if (!invMaterials.get(slot).equals(ingMat)) {
						logDebug("[hasShapedIngredients] Recipe ingredient requirement for slot " + slot
								+ " does not match the ID ingredient", recipeName);
						logDebug("[hasShapedIngredients] Required ingredient: " + ingMat + " - Found: "
								+ invMaterials.get(slot), recipeName);
						return false;
					}
					slot++;
					continue;
				}

				if (!invMaterials.get(slot).equals(ingredient.getMaterial().toString())) {
					logDebug("[hasShapedIngredients] Recipe ingredient requirement for slot " + slot
							+ " does not match the ingredient", recipeName);
					logDebug("[hasShapedIngredients] Required ingredient: " + ingredient.getMaterial().toString()
							+ " - Found: " + invMaterials.get(slot), recipeName);
					return false;
				}
			}

			slot++;
		}

		if (invMaterials.size() != 9) {

			logDebug(
					"[hasShapedIngredients] An internal error has occurred.. Please contact mehboss on spigot! Found size "
							+ invMaterials.size() + "but should be 9",
					recipeName);

			return false;
		}
		return true;
	}

	// Method for specifically shapeless recipes only (checks all inv materials)
	public ArrayList<RecipeUtil.Ingredient> getIngredients(String recipeName,
			List<RecipeUtil.Ingredient> allIngredients) {
		ArrayList<RecipeUtil.Ingredient> newIngredients = new ArrayList<>();

		for (RecipeUtil.Ingredient ingredient : allIngredients) {
			// Ignore the slot field and only save material, amount, and slot
			if (!ingredient.isEmpty() && ingredient.getMaterial() != null) {
				if (!newIngredients.contains(ingredient)) {
					newIngredients.add(ingredient);
				}
			}
		}

		return newIngredients;
	}

	// Checks all inv materials for recipe (shaped or shapeless)
	public boolean hasAllIngredients(Inventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();
		ArrayList<String> ingMaterials = new ArrayList<>();

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		int slot = 0;
		for (ItemStack invSlot : inv.getContents()) {

			if (invMaterials.size() == 9)
				break;

			if (isCraftingInventory && slot == 0) {
				slot++;
				continue;
			}
			if (invSlot == null || invSlot.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(invSlot.getType().toString());
		}

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (!ingredient.isEmpty()) {

				if (recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null) {
					ingMaterials.add(
							recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult().getType().toString());
				} else if (recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {
					ingMaterials.add(recipeUtil.getResultFromKey(ingredient.getIdentifier()).getType().toString());
				} else {
					ingMaterials.add(ingredient.getMaterial().toString());
				}

				continue;
			}
			ingMaterials.add("null");
		}

		// handles crafting table
		if ((inv.getType() == InventoryType.WORKBENCH || inv.getType() == InventoryType.CRAFTER)
				&& (invMaterials.size() != 9 || ingMaterials.size() != 9 || !invMaterials.containsAll(ingMaterials)
						|| !ingMaterials.containsAll(invMaterials))) {

			logDebug("[hasAllIngredients] Ingredients size is " + ingMaterials.size() + ", Inventory size is "
					+ invMaterials.size(), recipeName);

			return false;
		}

		// handles 4x4 slot
		if (inv.getType() == InventoryType.CRAFTING
				&& (!invMaterials.containsAll(ingMaterials) || !ingMaterials.containsAll(invMaterials))) {
			logDebug("[hasAllIngredients] Recipe ingredient requirements not met..", recipeName);
			return false;
		}

		return true;
	}

	public boolean hasVanillaIngredients(Inventory inv, ItemStack result) {

		if (result == null)
			return false;

		if (result.hasItemMeta() && (result.getItemMeta().hasDisplayName() || result.getItemMeta().hasLore()
				|| NBTEditor.contains(result, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
			return false;

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		int slot = 0;
		for (ItemStack item : inv.getContents()) {

			if (isCraftingInventory && slot == 0) {
				slot++;
				continue;
			}

			if (item == null || item.getType() == Material.AIR)
				continue;
			if (item.hasItemMeta() && (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore()))
				return false;

			if (recipeUtil.getRecipeFromResult(result) != null)
				return false;
		}
		return true;
	}

	boolean checkCustomItems(ItemStack exactMatch, ItemStack item, String identifier, Boolean logDebug) {
		ItemStack ingredientItem = exactMatch != null ? exactMatch.clone() : null;
		ItemMeta ingredientMeta = ingredientItem != null ? ingredientItem.getItemMeta() : null;
		ItemStack inventoryItem = item.clone();
		ItemMeta inventoryMeta = item.getItemMeta();

		String customItem = recipeUtil.getCustomItemPlugin(identifier);
		if (customItem != null
				&& (customItem.toLowerCase().equals("mythicmobs") || customItem.toLowerCase().equals("itemsadder"))) {
			for (Attribute am : Attribute.values()) {
				inventoryMeta.removeAttributeModifier(am);
				if (ingredientItem != null)
					ingredientMeta.removeAttributeModifier(am);
			}

			ingredientItem.setItemMeta(ingredientMeta);
			inventoryItem.setItemMeta(inventoryMeta);
		}

		if (logDebug) {
			logDebug(" MM Ingredient: " + ingredientItem.toString(), "");
			logDebug(" MM Inventory: " + inventoryItem.toString(), "");
		}

		if (ingredientItem == null || inventoryItem == null)
			return false;
		if (ingredientItem.isSimilar(inventoryItem))
			return true;

		if (customItem != null) {

			if (customItem.toLowerCase().equals("itemsadder")) {
				CustomStack stack = CustomStack.byItemStack(inventoryItem);
				if (stack == null) {
					logDebug("[checkCustomItems] ItemsAdder stack came back null, by error.", "");
					logDebug("Item in question: " + identifier, "");
					return false;
				}

				String iaID = identifier.split(":")[1].toLowerCase();
				String invID = stack.getNamespacedID().split(":")[1].toLowerCase();

				if (invID.equals(iaID))
					return true;
			}
		}

		return false;
	}

	boolean handleShapelessRecipe(CraftingInventory inv, Recipe recipe, List<RecipeUtil.Ingredient> recipeIngredients) {
		// runs checks if recipe is shapeless

		if (recipeUtil.getRecipeFromResult(inv.getResult()) != null) {
			recipe = recipeUtil.getRecipeFromResult(inv.getResult());

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

			if (ingredient.hasIdentifier() && recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {
				ItemStack exactMatch = recipeUtil.getResultFromKey(ingredient.getIdentifier());
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

		ItemStack[] matrix = inv.getMatrix(); // len 4 or 9

		for (int i = 0; i < matrix.length; i++) {
			ItemStack it = matrix[i];

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

			if (recipeUtil.getRecipeFromResult(it) != null && recipeUtil.getRecipeFromResult(it).isCustomTagged()
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

	boolean handleShapedRecipe(CraftingInventory inv, Recipe recipe, List<RecipeUtil.Ingredient> recipeIngredients) {
		int i = 0;

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			i++;

			if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
				ItemMeta meta = inv.getItem(i).getItemMeta();

				if (ingredient.hasIdentifier() && recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {

					ItemStack ingredientItem = recipeUtil.getResultFromKey(ingredient.getIdentifier());

					if (!inv.getItem(i).isSimilar(ingredientItem)
							&& !checkCustomItems(ingredientItem, inv.getItem(i), ingredient.getIdentifier(), true)) {
						logDebug("[handleShaped] Skipping recipe..", recipe.getName());
						logDebug("[handleShaped] Ingredient hasID: " + ingredient.hasIdentifier(), recipe.getName());
						logDebug(
								"[handleShaped] isSimilar: " + inv.getItem(i)
										.isSimilar(recipeUtil.getResultFromKey(ingredient.getIdentifier())),
								recipe.getName());
						logDebug("[handleShaped] invIngredient ID is "
								+ NBTEditor.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"),
								recipe.getName());
						logDebug("[handleShaped] recipeIngredient ID is " + ingredient.getIdentifier(),
								recipe.getName());

						return false;
					}

					logDebug("[handleShaped] Passed all required checks for the recipe ingredient in slot " + i,
							recipe.getName());
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
				
				if (!Main.getInstance().serverVersionAtLeast(1, 14)) {
					logDebug("[handleShaped] Skipping CMD checks.. Version is less than 1.14..", recipe.getName());
					return true;
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
		}
		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void handleCrafting(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Object view = CompatibilityUtil.getInventoryView(e);
		Player p = CompatibilityUtil.getPlayerFromView(view);

		logDebug("[handleCrafting] Fired craft event!", "");
		if (!(p instanceof Player) || p == null)
			return;

		if ((inv.getType() != InventoryType.WORKBENCH && inv.getType() != InventoryType.CRAFTING)
				|| !(matchedRecipe(inv)))
			return;
		if (isBlacklisted(inv.getResult(), p)) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (inv.getType() == InventoryType.CRAFTING) {
			if (!(inv.getRecipe() instanceof ShapelessRecipe))
				if (recipeUtil.getRecipeFromResult(inv.getResult()) != null)
					inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		handleCraftingChecks(inv, p);
	}

	public void handleCraftingChecks(CraftingInventory inv, Player p) {
		Recipe finalRecipe = null;
		Boolean passedCheck = true;
		Boolean found = false;

		for (String recipes : recipeUtil.getRecipeNames()) {

			Recipe recipe = recipeUtil.getRecipe(recipes);
			finalRecipe = recipe;

			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();

			if (recipe.getType() != RecipeType.SHAPELESS && recipe.getType() != RecipeType.SHAPED)
				continue;

			if (!hasAllIngredients(inv, recipe.getName(), recipeIngredients)) {
				logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not match..", recipe.getName());
				passedCheck = false;
				continue;
			}

			logDebug("[handleCrafting] Inventory contained all of the ingredients. Continuing checks..",
					recipe.getName());

			passedCheck = true;
			found = true;

			if (recipe.getType() == RecipeType.SHAPELESS) {
				if (!handleShapelessRecipe(inv, recipe, recipeIngredients)) {
					passedCheck = false;
					continue;
				}
			} else {

//				if (recipeUtil.getRecipeFromResult(inv.getResult()) != null) {
//					recipe = recipeUtil.getRecipeFromResult(inv.getResult());
//
//					if (recipe.isExactChoice()) {
//						logDebug("[handleCrafting] Handling recipe..", recipe.getName());
//						logDebug("[handleCrafting] Manual checks and methods skipped because exactChoice is enabled!",
//								recipe.getName());
//
//						if (!(amountsMatch(inv, recipe.getName(), recipe.getIngredients(), true))) {
//							logDebug("[handleCrafting] Skipping recipe.. ", recipe.getName());
//							logDebug("The amount check indicates that the requirements have not been met..",
//									recipe.getName());
//							inv.setResult(new ItemStack(Material.AIR));
//							passedCheck = false;
//						}
//
//						break;
//					}
//				}

				if (!hasShapedIngredients(inv, recipe.getName(), recipeIngredients)) {
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

		if (hasVanillaIngredients(inv, inv.getResult()) || !found)
			return;

		if ((!passedCheck) || (passedCheck && !found)) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (!finalRecipe.isActive()) {
			inv.setResult(new ItemStack(Material.AIR));
			logDebug(" Attempt to craft disabled recipe detected..", finalRecipe.getName());
			return;
		}

		// p is null for crafter. Add check
		if (finalRecipe.getPerm() != null && !p.hasPermission(finalRecipe.getPerm())) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p, finalRecipe.getName());
			return;
		}

		if (!(finalRecipe.getDisabledWorlds().isEmpty())) {
			for (String string : finalRecipe.getDisabledWorlds()) {
				if (p.getWorld().getName().equalsIgnoreCase(string)) {
					inv.setResult(new ItemStack(Material.AIR));
					sendMessages(p, "none", 0);
					return;
				}
			}
		}

		if ((finalRecipe.getPerm() != null && !(p.hasPermission(finalRecipe.getPerm() + ".bypass")))
				&& hasCooldown(p, finalRecipe)) {
			Long timeLeft = Main.getInstance().cooldownManager.getTimeLeft(p.getUniqueId(), finalRecipe.getKey());

			sendMessages(p, "crafting-limit", timeLeft);
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (passedCheck && found) {
			ItemStack item = new ItemStack(finalRecipe.getResult());

			List<String> withPlaceholders = null;

			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
				withPlaceholders = item.hasItemMeta() && item.getItemMeta().hasLore()
						? PlaceholderAPI.setPlaceholders(p, item.getItemMeta().getLore())
						: null;

			ItemMeta itemMeta = item.getItemMeta();

			if (withPlaceholders != null) {
				itemMeta.setLore(withPlaceholders);
				item.setItemMeta(itemMeta);
			}
			if (!inInventory.contains(p))
				inInventory.add(p);

//			if (!finalRecipe.isExactChoice())
			inv.setResult(item);
		}
	}
}
