package me.mehboss.recipe;

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
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import me.clip.placeholderapi.PlaceholderAPI;

public class RecipeManager {

	@SuppressWarnings("deprecation")
	ItemStack handleItemDamage(ItemStack i, String item, String damage, Optional<XMaterial> type, int amount) {
		if (!getConfig().isSet(item + ".Item-Damage") || damage.equalsIgnoreCase("none")) {
			return new ItemStack(type.get().parseMaterial(), amount);
		} else {
			try {
				return new ItemStack(type.get().parseMaterial(), amount, Short.valueOf(damage));
			} catch (Exception e) {
				Main.getInstance().getLogger().log(Level.WARNING, "Couldn't apply item damage to the recipe " + item
						+ ". Please double check that it is a valid item-damage. Skipping for now.");
				return new ItemStack(type.get().parseMaterial(), amount);
			}
		}
	}

	ItemStack handleIdentifier(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Identifier")) {
			if (getConfig().getBoolean(item + ".Custom-Tagged") == true)
				i = NBTEditor.set(i, getConfig().getString(item + ".Identifier"), "CUSTOM_ITEM_IDENTIFIER");

			identifier().put(getConfig().getString(item + ".Identifier"), i);
		}
		return i;
	}

	@SuppressWarnings("deprecation")
	ItemStack handleDurability(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Durability")) {
			if (!getConfig().getString(item + ".Durability").equals("100"))
				i.setDurability(Short.valueOf(getConfig().getString(item + ".Durability")));
		}
		return i;
	}

	ItemStack handleEnchants(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Enchantments")) {

			try {
				for (String e : getConfig().getStringList(item + ".Enchantments")) {
					String[] breakdown = e.split(":");
					String enchantment = breakdown[0];

					int lvl = Integer.parseInt(breakdown[1]);
					i.addUnsafeEnchantment(Enchantment.getByName(enchantment), lvl);
				}
			} catch (Exception e) {
				debug("Enchantment section for the recipe " + item + " is not valid. Skipping..");
			}
		}
		return i;
	}

	ItemMeta handleFlags(String item, ItemMeta m) {

		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		if (version.contains("1_7") || version.contains("1_8") || version.contains("1_9") || version.contains("1_10")
				|| version.contains("1_11") || version.contains("1_12") || version.contains("1_13"))
			return m;

		if (getConfig().isSet(item + ".Item-Flags")) {
			for (String flag : getConfig().getStringList(item + ".Item-Flags")) {
				try {
					m.addItemFlags(ItemFlag.valueOf(flag));
				} catch (IllegalArgumentException e) {
					debug("Could not add the item flag (" + flag + ") to recipe " + item
							+ ". This item flag could not be found so we will be skipping this flag for now. Please visit https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/ItemFlag.html for a list of valid flags.");
					continue;
				}
			}
		}
		return m;
	}

	ItemMeta handleDisplayname(String item, ItemStack recipe) {
		ItemMeta itemMeta = recipe.getItemMeta();

		if (debug())
			debug("Attempting displayname for: " + item);

		if (getConfig().isSet(item + ".Name")) {
			if (debug()) {
				debug("Applied displayname for: " + item);
				debug("Displayname: " + getConfig().getString(item + ".Name"));
			}

			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(item + ".Name")));
		}
		return itemMeta;
	}

	ItemMeta handleLore(String item, ItemMeta m) {

		ArrayList<String> loreList = new ArrayList<String>();

		if (getConfig().isSet(item + ".Lore")) {

			for (String Item1Lore : getConfig().getStringList(item + ".Lore")) {
				String crateLore = (Item1Lore.replaceAll("(&([a-fk-o0-9]))", "\u00A7$2"));
				loreList.add(crateLore);
			}
			
			if (!(loreList.isEmpty())) {
				m.setLore(loreList);
			}
		}
		return m;
	}

	ItemMeta handleHideEnchants(String item, ItemMeta m) {
		if (getConfig().isSet(item + ".Hide-Enchants") && getConfig().getBoolean(item + ".Hide-Enchants") == true) {
			m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		return m;
	}

	ItemMeta handleCustomModelData(String item, ItemMeta m) {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		if (version.contains("1_7") || version.contains("1_8") || version.contains("1_9") || version.contains("1_10")
				|| version.contains("1_11") || version.contains("1_12") || version.contains("1_13"))
			return m;

		if (getConfig().isSet(item + ".Custom-Model-Data")
				&& isInt(getConfig().getString(item + ".Custom-Model-Data"))) {
			try {
				Integer data = Integer.parseInt(getConfig().getString(item + ".Custom-Model-Data"));
				m.setCustomModelData(data);
			} catch (Exception e) {
				getLogger().log(Level.SEVERE,
						"Error occured while setting custom model data. This feature is only available for MC 1.14 or newer!");
			}
		}
		return m;
	}

	ItemMeta handleAttributes(String item, ItemMeta m) {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		if (version.contains("1_7") || version.contains("1_8") || version.contains("1_9") || version.contains("1_10")
				|| version.contains("1_11"))
			return m;

		if (getConfig().isSet(item + ".Attribute")) {
			for (String split : getConfig().getStringList(item + ".Attribute")) {
				String[] st = split.split(":");
				String attribute = st[0];
				double attributeAmount = Double.valueOf(st[1]);
				String equipmentSlot = null;

				if (st.length > 2)
					equipmentSlot = st[2];

				try {
					AttributeModifier modifier;
					if (equipmentSlot == null)
						modifier = new AttributeModifier(attribute, attributeAmount,
								AttributeModifier.Operation.ADD_NUMBER);
					else
						modifier = new AttributeModifier(UUID.randomUUID(), attribute, attributeAmount,
								AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.valueOf(equipmentSlot));

					m.addAttributeModifier(Attribute.valueOf(attribute), modifier);
				} catch (Exception e) {
					getLogger().log(Level.SEVERE, "Could not add attribute " + attribute + ", " + attributeAmount + ", "
							+ equipmentSlot + ", to the item " + item + ", skipping for now.");
				}
			}
		}
		return m;
	}

	FileConfiguration recipeConfig = null;

	@SuppressWarnings("deprecation")
	public void addItems() {
		disableRecipes();

		File recipeFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipeFolder.exists()) {
			recipeFolder.mkdirs();
		}

		File[] recipeFiles = recipeFolder.listFiles();
		if (recipeFiles == null) {
			return;
		}

		for (File recipeFile : recipeFiles) {
			recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

			String item = recipeFile.getName().replace(".yml", "");

			if (!(recipeConfig.isConfigurationSection(item))) {
				Main.getInstance().getLogger().log(Level.WARNING, "Could not find configuration section " + item
						+ " in the recipe file: " + item + ".yml - (CaSeSeNsItIvE) - Skipping this recipe");
				continue;
			}

			ItemStack i = null;
			ItemMeta m = null;

			ShapedRecipe R = null;
			ShapelessRecipe S = null;

			HashMap<String, String> details = new HashMap<String, String>();
			ArrayList<RecipeAPI.Ingredient> ingredients = new ArrayList<>();

			List<Material> findIngredients = new ArrayList<Material>();

			List<String> loreList = new ArrayList<String>();
			List<String> r = getConfig().getStringList(item + ".ItemCrafting");

			String damage = getConfig().getString(item + ".Item-Damage");
			int amount = getConfig().getInt(item + ".Amount");

			Optional<XMaterial> type = XMaterial.matchXMaterial(getConfig().getString(item + ".Item").toUpperCase());

			if (!type.isPresent()) {
				getLogger().log(Level.SEVERE, "Error loading recipe: " + recipeFile.getName());
				getLogger().log(Level.SEVERE, "We are having trouble matching the material "
						+ getConfig().getString(item + ".Item").toUpperCase()
						+ " to a minecraft item. Please double check you have inputted the correct material enum into the 'Item'"
						+ " section and try again. If this problem persists please contact Mehboss on Spigot!");
				continue;
			}

			if (!recipeConfig.isSet(item + ".Placeable")) {
				recipeConfig.set(item + ".Placeable", true);
				Main.getInstance().saveCustomYml(recipeConfig, recipeFile);
			}

			if (!recipeConfig.isSet(item + ".Ignore-Model-Data")) {
				recipeConfig.set(item + ".Ignore-Model-Data", false);
				Main.getInstance().saveCustomYml(recipeConfig, recipeFile);
			}

			if (!recipeConfig.isSet(item + ".Item-Flags")) {
				recipeConfig.set(item + ".Item-Flags", new ArrayList<String>());
				Main.getInstance().saveCustomYml(recipeConfig, recipeFile);
			}

			i = handleItemDamage(i, item, damage, type, amount); // handles ItemDamage
			i = handleIdentifier(i, item); // handles CustomTag
			i = handleDurability(i, item);
			i = handleEnchants(i, item);

			m = handleDisplayname(item, i); // handle Displayname
			m = handleHideEnchants(item, m); // handles hiding enchants
			m = handleCustomModelData(item, m); // handles custom model data
			m = handleAttributes(item, m);
			m = handleFlags(item, m);
			m = handleLore(item, m);

			i.setItemMeta(m);

			String line1 = r.get(0);
			String line2 = r.get(1);
			String line3 = r.get(2);

			HashMap<String, Material> shape = new HashMap<String, Material>();
			ArrayList<String> shapeletter = new ArrayList<String>();

			menui().add(i);
			giveRecipe().put(item.toLowerCase(), i);
			itemNames().put(item, i);
			configName().put(i, item);

			String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			if (!version.contains("1_7") && !version.contains("1_8") && !version.contains("1_9")
					&& !version.contains("1_10") && !version.contains("1_11")) {
				NamespacedKey key = new NamespacedKey(Main.getInstance(), getConfig().getString(item + ".Identifier"));
				R = new ShapedRecipe(key, i);
				S = new ShapelessRecipe(key, i);
			} else {
				R = new ShapedRecipe(i);
				S = new ShapelessRecipe(i);
			}

			R.shape(line1, line2, line3);
			details.put("X", "null:false:false:1");

			for (String abbreviation : getConfig().getConfigurationSection(item + ".Ingredients").getKeys(false)) {

				ConfigurationSection ingredientSection = getConfig()
						.getConfigurationSection(item + ".Ingredients." + abbreviation);

				String materialString = ingredientSection.getString("Material");
				int inAmount = 1;

				if (ingredientSection.isSet("Amount") && isInt(ingredientSection.getString("Amount")))
					inAmount = Integer.parseInt(ingredientSection.getString("Amount"));

				Optional<XMaterial> ingredientMaterial = XMaterial.matchXMaterial(materialString);

				String identifier = ingredientSection.isSet("Identifier") ? ingredientSection.getString("Identifier")
						: "false";
				String name = ingredientSection.isSet("Name") ? ingredientSection.getString("Name") : "false";

				if (!ingredientMaterial.isPresent()) {
					getLogger().log(Level.SEVERE, "Error loading recipe: " + recipeFile.getName());
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + materialString
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ENUM into the Ingredients section of the config and try again. If this problem persists please contact Mehboss on Spigot!");
					return;
				}

				Material finishedMaterial = ingredientMaterial.get().parseMaterial();

				R.setIngredient(abbreviation.charAt(0), finishedMaterial);
				shape.put(abbreviation, finishedMaterial);

				if (debug()) {
					debug("Ingredient Amount: " + inAmount);
					debug("Ingredient Displayname: " + name);
					debug("Ingredient Identifier: " + identifier);
				}

				if (!(ingredientSection.isSet("Name"))) {
					details.put(abbreviation,
							finishedMaterial.toString() + ":" + name + ":" + identifier + ":" + inAmount);
				} else {
					details.put(abbreviation,
							finishedMaterial.toString() + ":" + name + ":" + identifier + ":" + inAmount);
				}

				findIngredients.add(finishedMaterial);
			}

			ingredients().put(getConfig().getString(item + ".Identifier"), findIngredients);
			recipe().add(R);

			String[] newsplit1 = line1.split("");
			String[] newsplit2 = line2.split("");
			String[] newsplit3 = line3.split("");

			// slot 1
			ArrayList<String> newSlots = new ArrayList<String>();
			newSlots.add(newsplit1[0]);
			newSlots.add(newsplit1[1]);
			newSlots.add(newsplit1[2]);
			newSlots.add(newsplit2[0]);
			newSlots.add(newsplit2[1]);
			newSlots.add(newsplit2[2]);
			newSlots.add(newsplit3[0]);
			newSlots.add(newsplit3[1]);
			newSlots.add(newsplit3[2]);

			int slot = 0;
			for (String handleIngredients : newSlots) {
				slot++;

				Material itemMaterial = details.get(handleIngredients).split(":")[0].equals("null") ? null
						: Material.matchMaterial(details.get(handleIngredients).split(":")[0]);
				String itemName = details.get(handleIngredients).split(":")[1];
				String itemIdentifier = details.get(handleIngredients).split(":")[2];

				int itemAmount = Integer.parseInt(details.get(handleIngredients).split(":")[3]);
				boolean isEmpty = checkAbsent(handleIngredients);

				if (debug())
					debug("HandlingIngredient: " + itemIdentifier);

				ingredients
						.add(api().new Ingredient(itemMaterial, itemName, itemIdentifier, itemAmount, slot, isEmpty));
			}

			if (getConfig().getBoolean(item + ".Shapeless") == true) {

				shapeletter.add(newsplit1[0]);
				shapeletter.add(newsplit1[1]);
				shapeletter.add(newsplit1[2]);
				shapeletter.add(newsplit2[0]);
				shapeletter.add(newsplit2[1]);
				shapeletter.add(newsplit2[2]);
				shapeletter.add(newsplit3[0]);
				shapeletter.add(newsplit3[1]);
				shapeletter.add(newsplit3[2]);

				for (String sl : shapeletter) {
					if (!(sl.equalsIgnoreCase("X"))) {
						S.addIngredient(shape.get(sl));

						if (debug())
							debug("Shapeless: true | Variable: " + sl);
					}
				}

				Bukkit.getServer().addRecipe(S);
			}

			api().addRecipe(item, ingredients);

			if (getConfig().getBoolean(item + ".Shapeless") == false)
				Bukkit.getServer().addRecipe(R);

			if (debug())
				debug("Added Recipe: " + item + " | With Amount: " + i.getAmount());
		}
	}

	void debug(String st) {
		if (debug()) {
			getLogger().log(Level.WARNING, "-----------------");
			getLogger().log(Level.WARNING, "DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
		}

		getLogger().log(Level.WARNING, st);

		if (debug())
			getLogger().log(Level.WARNING, "-----------------");
	}

	boolean checkAbsent(String letterIngredient) {
		if (letterIngredient.equals("X"))
			return true;
		return false;
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	boolean debug() {
		return Main.getInstance().debug;
	}

	void disableRecipes() {
		Main.getInstance().disableRecipes();
	}

	FileConfiguration getConfig() {
		return recipeConfig;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	HashMap<String, List<Material>> ingredients() {
		return Main.getInstance().ingredients;
	}

	HashMap<String, ItemStack> identifier() {
		return Main.getInstance().identifier;
	}

	ArrayList<ItemStack> menui() {
		return Main.getInstance().menui;
	}

	ArrayList<ShapedRecipe> recipe() {
		return Main.getInstance().recipe;
	}

	HashMap<String, ItemStack> itemNames() {
		return Main.getInstance().itemNames;
	}
	
	HashMap<String, ItemStack> giveRecipe() {
		return Main.getInstance().giveRecipe;
	}

	HashMap<ItemStack, String> configName() {
		return Main.getInstance().configName;
	}

	RecipeAPI api() {
		return Main.getInstance().api;
	}
}
