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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RecipeManager implements Listener {

	boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {
			if (debug) {
				getLogger().log(Level.WARNING,
						"[CRECIPE DEBUG] [2] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
				getLogger().log(Level.WARNING, "COULD NOT FIND A RECIPE FOR THIS!!!");
			}
			return false;
		}
		return true;
	}

	HashMap<Material, Integer> countItemsByMaterial(CraftingInventory inv) {
		HashMap<Material, Integer> counts = new HashMap<>();
		for (ItemStack item : inv.getContents()) {
			if (item != null) {
				Material material = item.getType();
				int amount = item.getAmount();
				counts.put(material, counts.getOrDefault(material, 0) + amount);
			}
		}
		return counts;
	}

	boolean amountsMatch(CraftingInventory inv, String configName) {
		if (!(getConfig().isSet("Items." + configName + ".Ingredients")))
			return true;

		HashMap<Material, Integer> countedAmount = countItemsByMaterial(inv);

		for (String ingredient : getConfig().getStringList("Items." + configName + ".Ingredients")) {
			String[] split = ingredient.split(":");
			Material material = XMaterial.matchXMaterial(split[1]).get().parseMaterial();
			Integer amountRequired = Integer.parseInt(split[2]);

			if (countedAmount.containsKey(material) && countedAmount.get(material) >= amountRequired)
				continue;

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

				if (debug) {
					getLogger().log(Level.WARNING,
							"[CRECIPE DEBUG] [3] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
					getLogger().log(Level.WARNING,
							"CRECIPE DEBUG - BLACKLISTED RECIPE ARRAY SIZE " + disabledrecipe().size());
					getLogger().log(Level.WARNING, "CRECIPE DEBUG - BLACKLISTED RECIPE MATERIAL " + item);
					getLogger().log(Level.SEVERE, "ID: " + id + " BLACKLIST CHECK THIS IS WHAT IT RETURNED: "
							+ NBTEditor.getString(inv.getResult(), "CUSTOM_ITEM_IDENTIFIER"));
					getLogger().log(Level.SEVERE, "ID: " + id + " BLACKLIST CHECK THIS IS WHAT IT RETURNED: "
							+ NBTEditor.getString(inv.getResult(), id));
				}

				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if ((NBTEditor.contains(inv.getResult(), id) && !identifier().contains(id))
						|| inv.getResult().isSimilar(i)) {

					if (i == null) {
						getPerm = customConfig().getString("custom-recipes." + item + ".permission");
					}

					if (getPerm != null && !(getPerm.equalsIgnoreCase("none"))) {
						if (p.hasPermission("crecipe." + getPerm)) {
							if (debug) {
								getLogger().log(Level.WARNING,
										"[CRECIPE DEBUG] [3.25] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
								getLogger().log(Level.WARNING, "CRECIPE DEBUG - USER DOES HAVE PERMISSION");
							}
							return false;
						}
					}

					if (debug) {
						getLogger().log(Level.WARNING,
								"[CRECIPE DEBUG] [3.5] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
						getLogger().log(Level.WARNING, "CRECIPE DEBUG - RECIPE SET TO AIR");

					}

					sendMessages(p, getPerm);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler
	void onCraft(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();

		if (inv.getType() != InventoryType.WORKBENCH || !(matchedRecipe(inv)))
			return;

		if (!(configName().containsKey(inv.getResult())))
			return;

		if (!(inv.getResult().equals(e.getCurrentItem())))
			return;

		HashMap<Material, Integer> countedAmount = countItemsByMaterial(inv);
		for (Material material : countedAmount.keySet()) {
			int amount = countedAmount.get(material);
			if (amount == 1)
				continue;

			inv.removeItem(new ItemStack(material, (amount - 1)));
		}
	}

	@EventHandler
	void check(PrepareItemCraftEvent e) {

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

		if (debug) {
			getLogger().log(Level.WARNING,
					"[CRECIPE DEBUG] [5] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
			getLogger().log(Level.WARNING, "CRECIPE DEBUG - 'recipeName' is set to " + recipeName);
		}

		if (recipeName == null || !(api().hasRecipe(recipeName)))
			return;

		if (getConfig().getBoolean("Items." + recipeName + ".Enabled") == false
				|| ((getConfig().isString("Items." + recipeName + ".Permission")
						&& (!(p.hasPermission(getConfig().getString("Items." + recipeName + ".Permission"))))))) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (getConfig().isBoolean("Items." + recipeName + ".Ignore-Data")
				&& getConfig().getBoolean("Items." + recipeName + ".Ignore-Data") == true)
			return;

		List<RecipeAPI.Ingredient> recipeIngredients = api().getIngredients(recipeName);

		if (getConfig().isBoolean("Items." + configName() + ".Shapeless")
				&& getConfig().getBoolean("Items." + configName() + ".Shapeless") == true) {
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
					if ((!(meta.hasDisplayName()) && !(ingredient.hasDisplayName(null)))
							|| (meta.hasDisplayName() && !(ingredient.hasDisplayName(meta.getDisplayName())))) {
						passedCheck = false;
						break;
					}

					// checks amounts
					if (!(ingredient.hasAmount(inv.getItem(i).getAmount()))) {
						passedCheck = false;
						break;
					}
				}
			}
		}

		if (!(passedCheck))
			inv.setResult(new ItemStack(Material.AIR));

		if (debug) {
			getLogger().log(Level.WARNING,
					"[CRECIPE DEBUG] [10] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
			getLogger().log(Level.WARNING, "CRECIPE DEBUG - END CHECK. FINAL RECIPE MATCH: " + passedCheck);
			getLogger().log(Level.WARNING, "THIS IS WHAT RECIPE IT PULLED FROM -----    " + recipeName);
		}
	}

	boolean debug = Main.getInstance().debug;

	void sendMessages(Player p, String s) {
		Main.getInstance().sendmessages(p, s);
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
