package me.mehboss.crafting;

import java.io.File;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CraftManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
	ArrayList<Player> inInventory = new ArrayList<Player>();

	boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {
			if (isDebug())
				debug("[matchedRecipe] Could not find a recipe to match with!");

			return false;
		}
		return true;
	}

	HashMap<String, Integer> countItemsByMaterial(CraftingInventory inv) {
		HashMap<String, Integer> counts = new HashMap<>();

		for (ItemStack item : inv.getContents()) {
			if (item != null && item.getType() != Material.AIR && !(item.isSimilar(inv.getResult()))) {
				Material material = item.getType();
				String displayName = null; // Default to null if no display name is present

				if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					displayName = item.getItemMeta().getDisplayName();
				}

				// Generate a unique key for the material and display name combination
				String key = material.toString() + "-" + displayName;

				if (isDebug())
					debug("[countItemsByMaterial] Key found is " + key);

				int amount = item.getAmount();
				counts.put(key, counts.getOrDefault(key, 0) + amount);
			}
		}

		// Combine counts for items with the same material and display name
		HashMap<String, Integer> combinedCounts = new HashMap<>();
		for (HashMap.Entry<String, Integer> entry : counts.entrySet()) {
			String key = entry.getKey();
			int amount = entry.getValue();
			combinedCounts.put(key, combinedCounts.getOrDefault(key, 0) + amount);
		}

		return combinedCounts;
	}

	boolean amountsMatch(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			Material material = ingredient.getMaterial();
			String displayName = ingredient.getDisplayName();
			int requiredAmount = ingredient.getAmount();

			if (isDebug())
				debug("[amountsMatch] Recipe name: " + recipeName);

			int i = 0;
			for (ItemStack item : inv.getMatrix()) {
				i++;

				if (isDebug() && debug == true)
					debug("[hasMatchingDisplayName] Checking slot " + i + " for the recipe " + recipeName + ".. ");

				if (item != null && item.getType() == material && hasMatchingDisplayName(recipeName, item, displayName,
						ingredient.getIdentifier(), ingredient.hasIdentifier(), true)) {

					if (item.getAmount() < requiredAmount) {
						if (isDebug() && debug == true) {
							debug("[amountsMatch] Item amount: " + item.getAmount());
							debug("[amountsMatch] Required amount: " + requiredAmount);
						}
						return false;
					}
				}
			}
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	boolean isBlacklisted(CraftingInventory inv, Player p) {
		if (customConfig().getBoolean("blacklist-recipes") == true) {
			for (String item : disabledrecipe()) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				if (customConfig().getString("vanilla-recipes." + split[0]) != null
						&& !XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
				}

				if (XMaterial.matchXMaterial(split[0]).isPresent())
					i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				if (isDebug())
					debug("[isBlacklisted] Found " + disabledrecipe().size() + " disabled recipes.");

				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if (i == null)
					return false;

				if ((NBTEditor.contains(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id)
						&& NBTEditor.getString(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id).equals(id)
						&& recipeUtil.getRecipe(id) == null) || inv.getResult().isSimilar(i)) {

					if (getPerm != null && !(getPerm.equalsIgnoreCase("none"))) {
						if (p.hasPermission("crecipe." + getPerm)) {
							if (isDebug())
								debug("[isBlacklisted] Player " + p.getName() + " does have required permission "
										+ getPerm + " for item " + item);
							return false;
						}
					}

					if (isDebug())
						debug("[isBlacklisted] Player " + p.getName() + " does not have required permission " + getPerm
								+ " for item " + item);

					sendMessages(p, getPerm);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void handleShiftClicks(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();

		if (!(matchedRecipe(inv)))
			return;

		if (!(getRecipe().containsValue(inv.getResult())))
			return;

		if (isDebug())
			debug("[handleShiftClicks] Passed containsValue boolean check.");

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		String findName = recipeUtil.getRecipeFromResult(inv.getResult()).getName();
		boolean found = false;
		final ItemStack result = inv.getResult();

		for (String recipes : recipeUtil.getRecipeNames()) {
			List<RecipeUtil.Ingredient> recipeIngredients = recipeUtil.getRecipe(findName).getIngredients();

			if (hasIngredients(inv, recipes, recipeIngredients)
					&& recipeUtil.getRecipe(findName).getType() == RecipeType.SHAPELESS) {
				findName = recipes;
				found = true;
				break;
			}
			if (checkIngredients(inv, recipes, recipeIngredients)
					&& recipeUtil.getRecipe(findName).getType() == RecipeType.SHAPED) {
				findName = recipes;
				found = true;
				break;
			}
		}

		if (NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = NBTEditor.getString(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");

			if (recipeUtil.getRecipeFromKey(foundID) != null)
				findName = recipeUtil.getRecipeFromKey(foundID).getName();

		}

		if (isDebug())
			debug("[handleShiftClicks] Found recipe " + findName + " to handle..");

		if (!found)
			return;

		if (isDebug())
			debug("[handleShiftClicks] Paired it to a custom recipe. Running crafting amount calculations..");

		final String recipeName = findName;
		int itemsToAdd = Integer.MAX_VALUE;
		int itemsToRemove = 0;

		if (e.isCancelled()) {
			debug("Couldn't complete craftItemEvent for recipe " + recipeName
					+ ", the event was unexpectedly cancelled.");
			debug("Please seek support or open a ticket https://github.com/mehboss/CustomRecipes/issues");
			return;
		}

		if (isDebug())
			debug("[handleShiftClicks] Checking amount requirements for " + recipeName);

		for (RecipeUtil.Ingredient ingredient : recipeUtil.getRecipe(recipeName).getIngredients()) {
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

				if ((ingredient.hasIdentifier()
						&& NBTEditor.contains(slot, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
						&& ingredient.getIdentifier()
								.equals(NBTEditor.getString(slot, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
						|| (ingredient.hasIdentifier()
								&& slot.isSimilar(recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult()))
						|| (slot.getType() == material && hasMatchingDisplayName(recipeName, slot, displayName,
								ingredient.getIdentifier(), hasIdentifier, false))) {

					if (slot.getAmount() < requiredAmount)
						continue;

					int availableItems = slot.getAmount();
					int possibleItemsToRemove = availableItems / requiredAmount;

					// Keep track of the lowest possible items to add.
					itemsToAdd = Math.min(itemsToAdd, possibleItemsToRemove);
				}
			}

			if (itemsToAdd == Integer.MAX_VALUE) {
				e.setResult(null);
				e.setCancelled(true);
				return;
			}

			for (int i = 1; i < 10; i++) {
				ItemStack item = inv.getItem(i);
				int slot = i;

				if (item == null || item.getType() == Material.AIR)
					continue;

				if (isDebug())
					debug("[handleShiftClicks] Checking slot " + i + " for the recipe " + recipeName);

				if ((ingredient.hasIdentifier()
						&& NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
						&& ingredient.getIdentifier()
								.equals(NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
						|| (ingredient.hasIdentifier()
								&& item.isSimilar(recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult()))
						|| (item.getType() == material && hasMatchingDisplayName(recipeName, item, displayName,
								ingredient.getIdentifier(), hasIdentifier, true))) {

					if (item.getAmount() < requiredAmount)
						continue;

					itemsToRemove = itemsToAdd * requiredAmount;

					int availableItems = item.getAmount();

					if (isDebug()) {
						debug("[handleShiftClicks] Handling recipe " + recipeName);
						debug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove);
						debug("[handleShiftClicks] ItemAmount: " + availableItems);
						debug("[handleShiftClicks] RequiredAmount: " + requiredAmount);
						debug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier());
						debug("[handleShiftClicks] HasIdentifier: " + hasIdentifier);
						debug("[handleShiftClicks] Material: " + material.toString());
						debug("[handleShiftClicks] Displayname: " + displayName);
					}

					int itemAmount = item.getAmount();
					if (itemAmount < requiredAmount)
						continue;

					if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
						if (item.getType().toString().contains("_BUCKET")
								&& getConfig(recipeName).isSet(recipeName + ".Consume-Bucket")
								&& !getConfig(recipeName).getBoolean(recipeName + ".Consume-Bucket")) {
							item.setType(XMaterial.BUCKET.parseMaterial());
						} else {
							if ((item.getAmount() + 1) - requiredAmount == 0)
								inv.setItem(slot, null);
							else
								item.setAmount((item.getAmount() + 1) - requiredAmount);
						}
					} else {
						if (item.getType().toString().contains("_BUCKET")
								&& getConfig(recipeName).isSet(recipeName + ".Consume-Bucket")
								&& !getConfig(recipeName).getBoolean(recipeName + ".Consume-Bucket")) {
							item.setType(XMaterial.BUCKET.parseMaterial());
						} else {
							if ((item.getAmount() - itemsToRemove) <= 0) {
								inv.setItem(slot, null);
								continue;
							}

							item.setAmount(item.getAmount() - itemsToRemove);
						}
					}
				}
			}
		}

		// Add the result items to the player's inventory
		Player player = (Player) e.getWhoClicked();

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (isDebug())
				debug("[handleShiftClicks] Didn't detect shift click from inventory.. Ignoring..");
		} else {

			e.setCancelled(true);
			inv.setResult(new ItemStack(Material.AIR));

			if (isDebug())
				debug("[handleShiftClicks] Shift click detected. Adding " + itemsToAdd + " to inventory.");

			for (int i = 0; i < itemsToAdd; i++) {
				if (player.getInventory().firstEmpty() == -1) {
					player.getLocation().getWorld().dropItem(player.getLocation(), result);
					continue;
				}

				player.getInventory().addItem(result);
				if (isDebug()) {
					debug("[handleShiftClicks] Detected shift click and successfully removed items.");
					debug("[handleShiftClicks] Added " + itemsToAdd + " items and removed items from table.");
				}
			}
		}
	}

	boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean debug) {
		if (getConfig(recipeName).isSet(recipeName + ".Ignore-Data")
				&& getConfig(recipeName).getBoolean(recipeName + ".Ignore-Data") == true)
			return true;

		String recipeIdentifier = NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				? NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				: "none";

		if (hasIdentifier && identifier.equals(recipeIdentifier))
			return true;

		if (displayName == null || displayName.equals("false")) {
			if (isDebug() && debug == true)
				debug("[hasMatchingDisplayName] Found that recipe does not have a displayname to check");
			return !item.hasItemMeta() || !item.getItemMeta().hasDisplayName();
		} else {
			if (isDebug() && debug == true) {
				debug("[hasMatchingDisplayName] Found displayname " + displayName + " and the match check came back: "
						+ Boolean.valueOf(item.hasItemMeta() && item.getItemMeta().hasDisplayName()
								&& item.getItemMeta().getDisplayName().equals(displayName)));
			}
			return item.hasItemMeta() && item.getItemMeta().hasDisplayName()
					&& item.getItemMeta().getDisplayName().equals(displayName);
		}
	}

	// Checks if inventory slots and recipe materials match
	boolean checkIngredients(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();

		for (ItemStack contents : inv.getMatrix()) {
			if (invMaterials.size() == 9)
				break;

			if (contents == null || contents.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(contents.getType().toString());
		}

		int slot = 0;

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

			if (ingredient.isEmpty() && !invMaterials.get(slot).equals("null")) {
				if (isDebug())
					debug("[findIngredients] Required ingredient not found in slot " + slot + " for the recipe "
							+ recipeName);
				return false;
			}
			if (!ingredient.isEmpty() && !invMaterials.get(slot).equals(ingredient.getMaterial().toString())) {

				if (isDebug())
					debug("[findIngredients] Recipe ingredient requirement for slot " + slot
							+ " does not match the ingredient for the recipe " + recipeName);

				return false;
			}

			slot++;
		}

		if (invMaterials.size() != 9) {

			if (isDebug())
				debug("[findIngredients] An internal error has occurred.. Please contact mehboss on spigot! Found size "
						+ invMaterials.size() + "but should be 9 for the recipe " + recipeName);

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

	// Method for specifically shaped recipes only (checks all inv materials)
	boolean hasIngredients(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();
		ArrayList<String> ingMaterials = new ArrayList<>();

		for (ItemStack contents : inv.getMatrix()) {

			if (invMaterials.size() == 9)
				break;

			if (contents == null || contents.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(contents.getType().toString());
		}

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				ingMaterials.add("null");

			if (!ingredient.isEmpty())
				ingMaterials.add(ingredient.getMaterial().toString());
		}

		if (inv.getType() == InventoryType.WORKBENCH
				&& (invMaterials.size() != 9 || ingMaterials.size() != 9 || !invMaterials.containsAll(ingMaterials))) {

			if (isDebug()) {
				debug("[hasIngredients] Recipe ingredient requirements not met for the recipe " + recipeName);
				debug("Ingredients size is " + ingMaterials.size() + ", Inventory size is " + invMaterials.size()
						+ " for the recipe " + recipeName);
			}

			return false;
		}

		if (inv.getType() == InventoryType.CRAFTING && !invMaterials.containsAll(ingMaterials)) {

			if (isDebug())
				debug("[hasIngredients] Recipe ingredient requirements not met for the recipe " + recipeName);

			return false;
		}

		return true;
	}

	boolean hasVanillaIngredients(CraftingInventory inv) {

		if (inv.getResult().hasItemMeta()
				&& (inv.getResult().getItemMeta().hasDisplayName() || inv.getResult().getItemMeta().hasLore()
						|| NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
			return false;

		for (ItemStack item : inv.getContents()) {
			if (item == null || item.getType() == Material.AIR)
				continue;

			if (item.hasItemMeta() && (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore()))
				return false;
		}
		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void handleCrafting(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Boolean passedCheck = true;
		Boolean found = false;
		String recipeName = null;

		if (!(e.getView().getPlayer() instanceof Player))
			return;

		Player p = (Player) e.getView().getPlayer();

		if ((inv.getType() != InventoryType.WORKBENCH && inv.getType() != InventoryType.CRAFTING)
				|| !(matchedRecipe(inv)) || isBlacklisted(inv, p))
			return;

		for (String recipes : recipeUtil.getRecipeNames()) {

			List<RecipeUtil.Ingredient> recipeIngredients = recipeUtil.getRecipe(recipes).getIngredients();

			passedCheck = true;
			recipeName = recipes;

			if (!hasIngredients(inv, recipes, recipeIngredients)) {
				if (isDebug())
					debug("[handleCrafting] Skipping to the next recipe! Ingredients did not match for recipe "
							+ recipeName);
				passedCheck = false;
				continue;
			}

			if (isDebug())
				debug("[handleCrafting] Inventory contained all of the ingredients for the recipe " + recipeName
						+ " (findIngredients: " + Boolean.valueOf(checkIngredients(inv, recipeName, recipeIngredients))
						+ "). Continuing checks..");

			passedCheck = true;
			found = true;

			if (getConfig(recipeName).isBoolean(recipeName + ".Shapeless")
					&& getConfig(recipeName).getBoolean(recipeName + ".Shapeless") == true) {
				// runs checks if recipe is shapeless

				ArrayList<String> slotNames = new ArrayList<String>();
				ArrayList<String> recipeNames = new ArrayList<String>();

				ArrayList<Integer> recipeMD = new ArrayList<Integer>();
				ArrayList<Integer> inventoryMD = new ArrayList<Integer>();

				Map<Material, Integer> recipeCount = new HashMap<>();
				Map<Material, Integer> inventoryCount = new HashMap<>();

				for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

					if (ingredient.getMaterial() != null && !(inv.contains(ingredient.getMaterial())))
						break;

					if (ingredient.isEmpty()) {
						recipeCount.put(Material.AIR, recipeCount.getOrDefault(Material.AIR, 0) + 1);
						recipeNames.add("null");
						continue;
					}

					recipeCount.put(ingredient.getMaterial(),
							recipeCount.getOrDefault(ingredient.getMaterial(), 0) + 1);

					if (ingredient.hasIdentifier() && recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null) {
						Recipe exactMatch = recipeUtil.getRecipeFromKey(ingredient.getIdentifier());

						if (exactMatch.getResult().hasItemMeta()
								&& exactMatch.getResult().getItemMeta().hasCustomModelData()) {
							recipeMD.add(exactMatch.getResult().getItemMeta().getCustomModelData());
							// grab ingredient model data
						}

						if (exactMatch.getResult().getItemMeta().hasDisplayName()) {
							recipeNames.add(exactMatch.getResult().getItemMeta().getDisplayName());

						} else {
							recipeNames.add("false");
						}
						continue;
					}
					recipeNames.add(ingredient.getDisplayName());
				}

				for (int slot = 1; slot < 10; slot++) {
					if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.AIR) {
						slotNames.add("null");
						inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
						continue;
					}

					inventoryCount.put(inv.getItem(slot).getType(),
							inventoryCount.getOrDefault(inv.getItem(slot).getType(), 0) + 1);

					if (!(inv.getItem(slot).getItemMeta().hasDisplayName())) {
						slotNames.add("false");
						continue;
					}

					if (inv.getItem(slot).hasItemMeta() && inv.getItem(slot).getItemMeta().hasCustomModelData()) {
						inventoryMD.add(inv.getItem(slot).getItemMeta().getCustomModelData());
					}

					if (!(NBTEditor.contains(inv.getItem(slot), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
						continue;

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

					if (isDebug())
						debug("[handleCrafting] The recipe " + recipeName
								+ " does not have all of the required ingredients! Skipping recipe..");

					passedCheck = false;
					continue;
				}

				if (isDebug()) {
					debug("[handleCrafting] The recipe " + recipeName + " does have all of the required ingredients!");
					debug("[handleCrafting] Ingredient contains all: " + slotNames.containsAll(recipeNames));
				}

				if (getConfig(recipeName).isSet(recipeName + ".Ignore-Model-Data")
						&& getConfig(recipeName).getBoolean(recipeName + ".Ignore-Model-Data") == false
						&& recipeMD.size() != inventoryMD.size()
						&& (!recipeMD.containsAll(inventoryMD) || !inventoryMD.containsAll(recipeMD))) {
					passedCheck = false;
					continue;
				}

				if ((getConfig(recipeName).isSet(recipeName + ".Ignore-Data")
						&& getConfig(recipeName).getBoolean(recipeName + ".Ignore-Data") == false)
						&& (slotNames.size() != 9 || recipeNames.size() != 9
								|| !(slotNames.containsAll(recipeNames)))) {
					passedCheck = false;
					continue;
				}

				if (passedCheck == false && isDebug()) {
					debug("[handleCrafting] Debugging recipe " + recipeName);
					debug("[handleCrafting] Slot name size: " + slotNames.size());
					debug("[handleCrafting] Recipe name size: " + recipeNames.size());
					debug("[handleCrafting] Sending slot name results and recipe name results..");
					for (String names : slotNames)
						getLogger().log(Level.SEVERE, names);
					for (String names : recipeNames)
						getLogger().log(Level.SEVERE, names);
				}

			} else {

				if (!checkIngredients(inv, recipeName, recipeIngredients)) {
					if (isDebug())
						debug("[handleCrafting] Skipping to the next recipe! Ingredients did not match for recipe "
								+ recipeName);
					found = false;
					continue;
				}

				if (isDebug())
					debug("[handleCrafting] Ingredients matched for recipe " + recipeName
							+ ". Continuing system checks..");

				if (getConfig(recipeName).isSet(recipeName + ".Ignore-Data")
						&& getConfig(recipeName).getBoolean(recipeName + ".Ignore-Data") == true)
					continue;

				int i = 0;

				for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
					i++;

					if (ingredient.getMaterial() != null && !(inv.contains(ingredient.getMaterial()))) {

						if (isDebug())
							debug("[handleCrafting] Initial ingredient check for recipe " + recipeName
									+ " was false. Skipping recipe..");

						passedCheck = false;
						break;
					}

					if (inv.getItem(i) == null && !(ingredient.isEmpty())) {
						passedCheck = false;

						if (isDebug())
							debug("[handleCrafting] Slot " + i + " did not have the required ingredient for recipe "
									+ recipeName + ". Skipping recipe..");
						break;
					}

					if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
						ItemMeta meta = inv.getItem(i).getItemMeta();

						if (ingredient.hasIdentifier()
								&& recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null
								&& (NBTEditor.contains(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
										&& NBTEditor.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA,
												"CUSTOM_ITEM_IDENTIFIER").equals(ingredient.getIdentifier()))) {

							if (isDebug())
								debug("[handleCrafting] Passed all required checks for the recipe ingredient in slot "
										+ i + " for recipe " + recipeName);
							continue;

						} else if (ingredient.hasIdentifier()) {

							debug("[handleCrafting] Skipping recipe.. We should never reach this line of code.. please reach out for support.. Recipe: "
									+ recipeName);
							if (isDebug()) {
								debug("[handleCrafting] identifierAPI.contains(): "
										+ Boolean.valueOf(recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null));
								debug("[handleCrafting] identifierAPI.isSimilar(): "
										+ recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult().isSimilar(inv.getItem(i)));
								debug("[handleCrafting] recipeIngredient identifier is " + NBTEditor
										.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"));
								debug("[handleCrafting] ingredientIdentifier is " + ingredient.getIdentifier());
							}

							passedCheck = false;
							break;
						}

						if (isDebug())
							debug("[handleCrafting] The recipe " + recipeName + " ingredient slot " + i
									+ " does not have an identifier. Continuing more checks..");

						// checks if displayname is null
						if ((!meta.hasDisplayName() && ingredient.hasDisplayName())
								|| (meta.hasDisplayName() && !ingredient.hasDisplayName())) {
							passedCheck = false;
							if (isDebug()) {
								debug("[handleCrafting] Skipping recipe..");
								debug("[handleCrafting] The recipe ingredient displayname and the inventory slot displayname do not match for recipe "
										+ recipeName);
								debug("[handleCrafting] The inventory slot in question: " + i
										+ ". The ingredient slot in question: " + ingredient.getSlot());
								debug("[handleCrafting] Does the ingredient have a displayname? "
										+ ingredient.hasDisplayName());
								debug("[handleCrafting] Does the inventory have a displayname? "
										+ meta.hasDisplayName());
							}
							break;
						}

						if (ingredient.hasDisplayName() && meta.hasDisplayName()
								&& !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
							passedCheck = false;
							if (isDebug()) {
								debug("[handleCrafting] Skipping recipe..");
								debug("[handleCrafting] The ingredient name for the recipe and inventory do not match for recipe "
										+ recipeName);
								debug("[handleCrafting] The inventory slot in question: " + i
										+ ". The ingredient slot in question: " + ingredient.getSlot());
								debug("[handleCrafting] The ingredient displayname: " + ingredient.getDisplayName());
								debug("[handleCrafting] The inventory displayname: " + meta.getDisplayName());
								break;
							}
						}
					}
				}
			}

			if (!(amountsMatch(inv, recipeName, recipeIngredients, true))) {
				if (isDebug())
					debug("[handleCrafting] Skipping recipe.. The amount check indicates that the requirements have not been met for recipe "
							+ recipeName);
				passedCheck = false;
				continue;
			}

			if (passedCheck && found) {
				break;
			}
		}

		if (passedCheck && (getConfig(recipeName).getBoolean(recipeName + ".Enabled") == false
				|| ((getConfig(recipeName).isSet(recipeName + ".Permission")
						&& (!(p.hasPermission(getConfig(recipeName).getString(recipeName + ".Permission")))))))) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p);
			return;
		}

		if (hasVanillaIngredients(inv))
			return;

		if (getConfig(recipeName).isSet(recipeName + ".Disabled-Worlds")) {
			for (String string : getConfig(recipeName).getStringList(recipeName + ".Disabled-Worlds")) {
				if (p.getWorld().getName().equalsIgnoreCase(string)) {
					inv.setResult(new ItemStack(Material.AIR));
					sendMessages(p, "none");
					return;
				}
			}
		}

		if ((!passedCheck) || (found) || (passedCheck && !found))
			inv.setResult(new ItemStack(Material.AIR));

		if (passedCheck && found && getRecipe().containsKey(recipeName.toLowerCase())) {

			List<String> withPlaceholders = null;

			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
				withPlaceholders = getRecipe().get(recipeName.toLowerCase()).hasItemMeta()
						&& getRecipe().get(recipeName.toLowerCase()).getItemMeta().hasLore()
								? PlaceholderAPI.setPlaceholders(p,
										getRecipe().get(recipeName.toLowerCase()).getItemMeta().getLore())
								: null;

			ItemStack finalItem = new ItemStack(getRecipe().get(recipeName.toLowerCase()));
			ItemMeta finalItemm = finalItem.getItemMeta();

			if (withPlaceholders != null) {
				finalItemm.setLore(withPlaceholders);
				finalItem.setItemMeta(finalItemm);
			}

			inv.setResult(finalItem);

			if (!inInventory.contains(p))
				inInventory.add(p);
		}

		if (isDebug())
			debug("[handleCrafting] Final results for recipe " + recipeName + " (passedCheck: " + passedCheck
					+ ")(found: " + found + ")");
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	boolean isDebug() {
		return Main.getInstance().debug;
	}

	void debug(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	void sendMessages(Player p, String s) {
		Main.getInstance().sendMessages(p, s);
	}

	void sendNoPermsMessage(Player p) {
		if (isDebug())
			debug("Player " + p.getName() + " does not have required recipe crafting permissions for recipe");

		Main.getInstance().sendMessage(p);
	}

	ArrayList<String> disabledrecipe() {
		return Main.getInstance().disabledrecipe;
	}

	HashMap<String, ItemStack> getRecipe() {
		return Main.getInstance().giveRecipe;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	FileConfiguration customConfig() {
		return Main.getInstance().customConfig;
	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
	}
}
