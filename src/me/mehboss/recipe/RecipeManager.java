package me.mehboss.recipe;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RecipeManager implements Listener {

	boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {
			if (debug)
				debug("Could not find a recipe to match with!");

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

				int amount = item.getAmount();
				counts.put(key, counts.getOrDefault(key, 0) + amount);
			}
		}
		return counts;
	}

	boolean amountsMatch(CraftingInventory inv, String configName) {
		if (!getConfig().isSet("Items." + configName + ".Ingredients")) {
			return true;
		}

		HashMap<String, Integer> countedAmount = countItemsByMaterial(inv);

		for (String ingredient : getConfig().getConfigurationSection("Items." + configName + ".Ingredients")
				.getKeys(false)) {

			String[] split = ingredient.split(":");
			String abbreviation = split[0];
			ConfigurationSection ingredientSection = getConfig()
					.getConfigurationSection("Items." + configName + ".Ingredients." + abbreviation);

			String materialString = ingredientSection.getString("Material");
			int amountRequired = ingredientSection.isSet("Amount") ? ingredientSection.getInt("Amount") : 1;
			String displayName = ingredientSection.isSet("Name") ? ingredientSection.getString("Name") : null;

			Material material = XMaterial.matchXMaterial(materialString).get().parseMaterial();

			// Generate a unique key for the material and display name combination
			String key = material.toString() + "-" + displayName;

			if (countedAmount.containsKey(key) && countedAmount.get(key) >= amountRequired) {
				continue;
			}
			return false;
		}
		return true;
	}

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

				if (debug)
					debug("Blacklisted Array Size: " + disabledrecipe().size());

				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if ((NBTEditor.contains(inv.getResult(), id) && !identifier().contains(id))
						|| inv.getResult().isSimilar(i)) {

					if (i == null) {
						getPerm = customConfig().getString("custom-recipes." + item + ".permission");
					}

					if (getPerm != null && !(getPerm.equalsIgnoreCase("none"))) {
						if (p.hasPermission("crecipe." + getPerm)) {
							if (debug)
								debug("User DOES have permission!");
							return false;
						}
					}

					if (debug)
						debug("Recipe has been set to air");

					sendMessages(p, getPerm);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler
	void handleShiftClicks(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();

		if (inv.getType() != InventoryType.WORKBENCH || !(matchedRecipe(inv)))
			return;

		if (!(configName().containsKey(inv.getResult())))
			return;

		if (e.getCurrentItem() != null && !(inv.getResult().equals(e.getCurrentItem())))
			return;

		HashMap<String, Integer> countedAmount = countItemsByMaterial(inv);
		String recipeName = configName().get(inv.getResult());
		final ItemStack result = inv.getResult();

		if (e.getCursor() != null && e.getCursor().equals(result) && result.getMaxStackSize() <= 1)
			return;

		ArrayList<RecipeAPI.Ingredient> recipeIngredients = api().getIngredients(recipeName);

		// Calculate the number of times the shift-click should produce the item
		int shiftClickMultiplier = Integer.MAX_VALUE;

		for (RecipeAPI.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			Material material = ingredient.getMaterial();
			String displayName = null;

			if (ingredient.hasDisplayName())
				displayName = ingredient.getDisplayName();

			int requiredAmount = ingredient.getAmount();
			int availableAmount = countedAmount.getOrDefault(material.toString() + "-" + displayName, 0);

			if (requiredAmount > 0) {
				int multiplier = availableAmount / requiredAmount;
				shiftClickMultiplier = Math.min(shiftClickMultiplier, multiplier);
			}
		}

		// Remove the required items from the inventory
		for (RecipeAPI.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			ItemStack removeItem = new ItemStack(ingredient.getMaterial());
			ItemMeta removeItemMeta = removeItem.getItemMeta();

			if (ingredient.hasDisplayName()) {
				removeItemMeta.setDisplayName(ingredient.getDisplayName());
				removeItem.setItemMeta(removeItemMeta);
				removeItem = NBTEditor.set(removeItem, "CUSTOM_ITEM", "CUSTOM_ITEM_IDENTIFIER");
			}

			int requiredAmount = ingredient.getAmount() * shiftClickMultiplier;

			if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && requiredAmount > 0) {
				removeItem.setAmount(requiredAmount);
				inv.removeItem(removeItem);
				continue;
			}
			removeItem.setAmount(ingredient.getAmount());
			inv.removeItem(removeItem);
		}

		// Add the result items to the player's inventory
		Player player = (Player) e.getWhoClicked();

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			e.setCursor(result);
			return;
		}

		for (int i = 0; i < shiftClickMultiplier; i++)
			player.getInventory().addItem(result);

		// Set the cursor item to null (to prevent duplication)
		e.setCursor(null);
	}

	@EventHandler
	void handleCrafting(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Boolean passedCheck = true;
		String recipeName = null;

		if (!(e.getView().getPlayer() instanceof Player)) {
			return;
		}

		Player p = (Player) e.getView().getPlayer();

		if (inv.getType() != InventoryType.WORKBENCH || !(matchedRecipe(inv)) || isBlacklisted(inv, p))
			return;

		if (configName().containsKey(inv.getResult())) {
			recipeName = configName().get(inv.getResult());
		}

		if (debug)
			debug("Recipe Config: " + recipeName);

		if (recipeName == null || !(api().hasRecipe(recipeName)))
			return;

		if (getConfig().isBoolean("Items." + recipeName + ".Ignore-Data")
				&& getConfig().getBoolean("Items." + recipeName + ".Ignore-Data") == true)
			return;

		ArrayList<RecipeAPI.Ingredient> recipeIngredients = api().getIngredients(recipeName);

		if (getConfig().isBoolean("Items." + recipeName + ".Shapeless")
				&& getConfig().getBoolean("Items." + recipeName + ".Shapeless") == true) {
			// runs checks if recipe is shapeless

			ArrayList<String> slotNames = new ArrayList<String>();
			ArrayList<String> recipeNames = new ArrayList<String>();

			for (int slot = 0; slot < 9; slot++) {
				if (inv.getItem(slot) == null || !(inv.getItem(slot).getItemMeta().hasDisplayName())) {
					slotNames.add("false");
					continue;
				}

				if (!(NBTEditor.contains(inv.getItem(slot), "CUSTOM_ITEM_IDENTIFIER")))
					continue;

				slotNames.add(inv.getItem(slot).getItemMeta().getDisplayName());
			}

			for (RecipeAPI.Ingredient names : recipeIngredients) {
				recipeNames.add(names.getDisplayName());
			}

			if (debug)
				debug("ContainsAll: " + slotNames.containsAll(recipeNames) + " | AmountsMatch: "
						+ amountsMatch(inv, recipeName));

			if (!(slotNames.containsAll(recipeNames)) || !(amountsMatch(inv, recipeName)))
				passedCheck = false;

		} else {

			// runs check for non-shapeless recipes
			int i = 0;
			for (RecipeAPI.Ingredient ingredient : recipeIngredients) {
				i++;

				if (inv.getItem(i) == null && !(ingredient.isEmpty())) {
					passedCheck = false;
					break;
				}

				if (inv.getItem(i) != null) {
					ItemMeta meta = inv.getItem(i).getItemMeta();

					// checks for custom tag
					if (meta.hasDisplayName() && !(NBTEditor.contains(inv.getItem(i), "CUSTOM_ITEM_IDENTIFIER"))) {
						passedCheck = false;
						break;
					}

					// checks if displayname is null
					if ((!(meta.hasDisplayName()) && (ingredient.hasDisplayName()))
							|| (meta.hasDisplayName() && !(ingredient.hasDisplayName()))
							|| !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
						passedCheck = false;
						break;
					}

					// checks amounts
					if (!(amountsMatch(inv, recipeName))) {
						passedCheck = false;
						break;
					}
				}
			}
		}

		if (!(passedCheck))
			inv.setResult(new ItemStack(Material.AIR));

		if (passedCheck && (getConfig().getBoolean("Items." + recipeName + ".Enabled") == false
				|| ((getConfig().isSet("Items." + recipeName + ".Permission")
						&& (!(p.hasPermission(getConfig().getString("Items." + recipeName + ".Permission")))))))) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p);
			return;
		}

		if (debug)
			debug("Final Recipe Match: " + passedCheck + "| Recipe Pulled: " + recipeName);
	}

	boolean debug = Main.getInstance().debug;

	void debug(String st) {
		getLogger().log(Level.WARNING, "-----------------");
		getLogger().log(Level.WARNING, "DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
		getLogger().log(Level.WARNING, st);
		getLogger().log(Level.WARNING, "-----------------");
	}

	void sendMessages(Player p, String s) {
		Main.getInstance().sendMessages(p, s);
	}

	void sendNoPermsMessage(Player p) {
		if (debug)
			debug("Sending noPERMS message now");

		Main.getInstance().sendMessage(p);
	}

	ArrayList<String> identifier() {
		return Main.getInstance().identifier;
	}

	ArrayList<String> disabledrecipe() {
		return Main.getInstance().disabledrecipe;
	}

	HashMap<ItemStack, String> configName() {
		return Main.getInstance().configName;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	FileConfiguration customConfig() {
		return Main.getInstance().customConfig;
	}

	FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	RecipeAPI api() {
		return Main.getInstance().api;
	}
}
