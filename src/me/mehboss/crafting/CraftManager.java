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

	FileConfiguration getCnfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	void sendMessages(Player p, String s) {
		Main.getInstance().sendMessages(p, s);
	}

	void sendNoPermsMessage(Player p) {
		logDebug("Player " + p.getName() + " does not have required recipe crafting permissions for recipe");
		Main.getInstance().sendMessage(p);
	}

	boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {

			logDebug("[matchedRecipe] Could not find a recipe to match with!");

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

				logDebug("[countItemsByMaterial] Key found is " + key);

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

	boolean validateItem(ItemStack item, RecipeUtil.Ingredient ingredient, String recipeName, int slot, boolean debug,
			boolean returnType) {
		if (item != null && item.getType() == ingredient.getMaterial() && hasMatchingDisplayName(recipeName, item,
				ingredient.getDisplayName(), ingredient.getIdentifier(), ingredient.hasIdentifier(), false)) {

			if (debug)
				logDebug("[amountsMatch] Checking slot " + slot + " for required amounts.. ");

			if (item.getAmount() < ingredient.getAmount()) {

				if (debug) {
					logDebug("[amountsMatch] Item amount: " + item.getAmount());
					logDebug("[amountsMatch] Required amount: " + ingredient.getAmount());
				}
				return false;
			}

			if (debug)
				logDebug("[amountsMatch] Item amount and ingredient amounts matched (#" + ingredient.getAmount() + ")");

			return true;
		}
		return returnType;
	}

	boolean amountsMatch(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			int slot = ingredient.getSlot();

			ItemStack invSlot = inv.getMatrix()[slot - 1];

			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPED)
				if (!validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;

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

	@SuppressWarnings("deprecation")
	boolean isBlacklisted(CraftingInventory inv, Player p) {
		if (customConfig().getBoolean("blacklist-recipes") == true) {
			for (String item : disabledrecipe()) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				if (customConfig().isString("vanilla-recipes." + split[0]))
					return false;

				if (!XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
					return false;
				}
				i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				logDebug("[isBlacklisted] Found " + disabledrecipe().size() + " disabled recipes.");
				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if (i == null)
					return false;

				if ((NBTEditor.contains(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id)
						&& NBTEditor.getString(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id).equals(id)
						&& recipeUtil.getRecipe(id) == null) || inv.getResult().isSimilar(i)) {

					if (getPerm != null && !(getPerm.equalsIgnoreCase("none"))) {
						if (p.hasPermission("crecipe." + getPerm)) {

							logDebug("[isBlacklisted] Player " + p.getName() + " does have required permission "
									+ getPerm + " for item " + item);
							return false;
						}
					}

					logDebug("[isBlacklisted] Player " + p.getName() + " does not have required permission " + getPerm
							+ " for item " + item);

					sendMessages(p, getPerm);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
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
			logDebug("[hasMatchingDisplayName] Checking displayname..");

		if (displayName == null || displayName.equals("false") || displayName.equals("none")) {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found that ingredient does not have a displayname to check");
			return !item.hasItemMeta() || !itemM.hasDisplayName();

		} else {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found displayname " + displayName
						+ " and the match check came back: " + Boolean.valueOf(item.hasItemMeta()
								&& item.getItemMeta().hasDisplayName() && itemM.getDisplayName().equals(displayName)));
			return item.hasItemMeta() && itemM.hasDisplayName() && itemM.getDisplayName().equals(displayName);
		}
	}

	// Checks if inventory slots and recipe materials match
	boolean hasShapedIngredients(CraftingInventory inv, String recipeName,
			List<RecipeUtil.Ingredient> recipeIngredients) {
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

				logDebug("[hasShapedIngredients] Required ingredient not found in slot " + slot + " for the recipe "
						+ recipeName);
				return false;
			}
			if (!ingredient.isEmpty() && !invMaterials.get(slot).equals(ingredient.getMaterial().toString())) {

				logDebug("[hasShapedIngredients] Recipe ingredient requirement for slot " + slot
						+ " does not match the ingredient for the recipe " + recipeName);

				return false;
			}

			slot++;
		}

		if (invMaterials.size() != 9) {

			logDebug(
					"[hasShapedIngredients] An internal error has occurred.. Please contact mehboss on spigot! Found size "
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

	// Checks all inv materials for recipe (shaped or shapeless)
	boolean hasAllIngredients(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();
		ArrayList<String> ingMaterials = new ArrayList<>();

		for (ItemStack invSlot : inv.getMatrix()) {

			if (invMaterials.size() == 9)
				break;

			if (invSlot == null || invSlot.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(invSlot.getType().toString());
		}

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (!ingredient.isEmpty()) {
				ingMaterials.add(ingredient.getMaterial().toString());
				continue;
			}
			ingMaterials.add("null");
		}

		// handles crafting table
		if (inv.getType() == InventoryType.WORKBENCH
				&& (invMaterials.size() != 9 || ingMaterials.size() != 9 || !invMaterials.containsAll(ingMaterials))) {

			logDebug("[hasAllIngredients] Ingredients size is " + ingMaterials.size() + ", Inventory size is "
					+ invMaterials.size() + " for the recipe " + recipeName);

			return false;
		}

		// handles 4x4 slot
		if (inv.getType() == InventoryType.CRAFTING && !invMaterials.containsAll(ingMaterials)) {
			logDebug("[hasAllIngredients] Recipe ingredient requirements not met for the recipe " + recipeName);
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

		Recipe finalRecipe = null;
		Boolean passedCheck = true;
		Boolean found = false;

		logDebug("[handleCrafting] Fired craft event!");
		if (!(e.getView().getPlayer() instanceof Player))
			return;

		Player p = (Player) e.getView().getPlayer();
		
		if ((inv.getType() != InventoryType.WORKBENCH && inv.getType() != InventoryType.CRAFTING)
				|| !(matchedRecipe(inv)) || isBlacklisted(inv, p))
			return;

		recipeLoop: for (String recipes : recipeUtil.getRecipeNames()) {

			Recipe recipe = recipeUtil.getRecipe(recipes);
			finalRecipe = recipe;

			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();

			if (!hasAllIngredients(inv, recipe.getName(), recipeIngredients)) {
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

						if (Main.getInstance().serverVersionAtLeast(1, 14) && exactMatch.getResult().hasItemMeta()
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

					if (ingredient.hasDisplayName())
						recipeNames.add(ingredient.getDisplayName());
					else {
						recipeNames.add("false");
					}
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

					if (Main.getInstance().serverVersionAtLeast(1, 14) && inv.getItem(slot).hasItemMeta()
							&& inv.getItem(slot).getItemMeta().hasCustomModelData()) {
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
					logDebug("[handleCrafting] The recipe " + recipe.getName()
							+ " does not have all of the required ingredients! Skipping recipe..");
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] The recipe " + recipe.getName()
						+ " does have all of the required ingredients!");
				logDebug("[handleCrafting] Ingredient contains all: " + slotNames.containsAll(recipeNames));

				if (!slotNames.containsAll(recipeNames)) {
					logDebug("[handleCrafting] Debugging recipe " + recipe.getName());
					logDebug("[handleCrafting] Slot name size: " + slotNames.size());
					logDebug("[handleCrafting] Recipe name size: " + recipeNames.size());
					logDebug("[handleCrafting] Sending slot name results and recipe name results..");
					for (String names : slotNames)
						logDebug(names);
					for (String names : recipeNames)
						logDebug(names);
				}

				if (recipe.getIgnoreModelData() == false && recipeMD.size() != inventoryMD.size()
						&& (!recipeMD.containsAll(inventoryMD) || !inventoryMD.containsAll(recipeMD))) {
					passedCheck = false;
					continue;
				}

				if ((recipe.getIgnoreData() == false) && (slotNames.size() != 9 || recipeNames.size() != 9
						|| !(slotNames.containsAll(recipeNames)))) {
					passedCheck = false;
					continue;
				}

			} else {

				if (!hasShapedIngredients(inv, recipe.getName(), recipeIngredients)) {

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

				int i = 0;

				for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
					i++;

					if (ingredient.getMaterial() != null && !(inv.contains(ingredient.getMaterial()))) {

						logDebug("[handleCrafting] Initial ingredient check for recipe " + recipe.getName()
								+ " was false. Skipping recipe..");

						passedCheck = false;
						break;
					}

					if (inv.getItem(i) == null && !(ingredient.isEmpty())) {
						passedCheck = false;

						logDebug("[handleCrafting] Slot " + i + " did not have the required ingredient for recipe "
								+ recipe.getName() + ". Skipping recipe..");
						break;
					}

					if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
						ItemMeta meta = inv.getItem(i).getItemMeta();

						if (ingredient.hasIdentifier()
								&& recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null
								&& (NBTEditor.contains(inv.getItem(i), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
										&& NBTEditor.getString(inv.getItem(i), NBTEditor.CUSTOM_DATA,
												"CUSTOM_ITEM_IDENTIFIER").equals(ingredient.getIdentifier()))) {

							logDebug("[handleCrafting] Passed all required checks for the recipe ingredient in slot "
									+ i + " for recipe " + recipe.getName());
							continue;

						} else if (ingredient.hasIdentifier()) {

							logDebug(
									"[handleCrafting] Skipping recipe.. We should never reach this line of code.. please reach out for support.. Recipe: "
											+ recipe.getName());

							logDebug("[handleCrafting] Ingredient hasID: " + ingredient.hasIdentifier());

							logDebug("[handleCrafting] Invoke getRecipeFromKey:	"
									+ recipeUtil.getRecipeFromKey(ingredient.getIdentifier()));
							logDebug("[handleCrafting] Inventory Item == Ingredient Item?: "
									+ recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult()
											.isSimilar(inv.getItem(i)));
							logDebug("[handleCrafting] invIngredient ID is " + NBTEditor.getString(inv.getItem(i),
									NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"));
							logDebug("[handleCrafting] recipeIngredient ID is " + ingredient.getIdentifier());

							passedCheck = false;
							continue recipeLoop;
						}

						logDebug("[handleCrafting] The recipe " + recipe.getName() + " ingredient slot " + i
								+ " does not have an identifier.");

						// checks if displayname is null
						if ((!meta.hasDisplayName() && ingredient.hasDisplayName())
								|| (meta.hasDisplayName() && !ingredient.hasDisplayName())) {
							passedCheck = false;
							logDebug("[handleCrafting] Skipping recipe..");
							logDebug(
									"[handleCrafting] The recipe ingredient displayname and the inventory slot displayname do not match for recipe "
											+ recipe.getName());
							logDebug("[handleCrafting] The inventory slot in question: " + i
									+ ". The ingredient slot in question: " + ingredient.getSlot());
							logDebug("[handleCrafting] Does the ingredient have a displayname? "
									+ ingredient.hasDisplayName());
							logDebug(
									"[handleCrafting] Does the inventory have a displayname? " + meta.hasDisplayName());
							continue recipeLoop;
						}

						if (ingredient.hasDisplayName() && meta.hasDisplayName()
								&& !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
							passedCheck = false;
							logDebug("[handleCrafting] Skipping recipe..");
							logDebug(
									"[handleCrafting] The ingredient name for the recipe and inventory do not match for recipe "
											+ recipe.getName());
							logDebug("[handleCrafting] The inventory slot in question: " + i
									+ ". The ingredient slot in question: " + ingredient.getSlot());
							logDebug("[handleCrafting] The ingredient displayname: " + ingredient.getDisplayName());
							logDebug("[handleCrafting] The inventory displayname: " + meta.getDisplayName());
							continue recipeLoop;
						}

						logDebug("[handleCrafting] Inventory and recipe ingredient displayname matched for slot " + i);
					}
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

		if (hasVanillaIngredients(inv) && !found)
			return;

		if ((!passedCheck) || (found) || (passedCheck && !found))
			inv.setResult(new ItemStack(Material.AIR));

		if ((!(finalRecipe.isActive())
				|| ((finalRecipe.getPerm() != null && (!(p.hasPermission(finalRecipe.getPerm()))))))) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p);
			return;
		}

		if (!(finalRecipe.getDisabledWorlds().isEmpty())) {
			for (String string : finalRecipe.getDisabledWorlds()) {
				if (p.getWorld().getName().equalsIgnoreCase(string)) {
					inv.setResult(new ItemStack(Material.AIR));
					sendMessages(p, "none");
					return;
				}
			}
		}

		String recipeName = finalRecipe.getName();
		if (passedCheck && found && getRecipe().containsKey(finalRecipe.getName().toLowerCase())) {

			List<String> withPlaceholders = null;
			ItemStack item = new ItemStack(getRecipe().get(recipeName.toLowerCase()));

			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
				withPlaceholders = item.hasItemMeta() && item.getItemMeta().hasLore()
						? PlaceholderAPI.setPlaceholders(p, item.getItemMeta().getLore())
						: null;

			ItemMeta itemMeta = item.getItemMeta();

			if (withPlaceholders != null) {
				itemMeta.setLore(withPlaceholders);
				item.setItemMeta(itemMeta);
			}

			inv.setResult(item);

			if (!inInventory.contains(p))
				inInventory.add(p);
		}
	}
}
