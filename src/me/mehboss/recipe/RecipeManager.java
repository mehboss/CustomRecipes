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
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class RecipeManager {

	@SuppressWarnings("deprecation")
	ItemStack handleItemDamage(ItemStack i, String item, String damage, Optional<XMaterial> type, int amount) {
		if (getConfig().isSet("Items." + item + ".Item-Damage") && damage.equalsIgnoreCase("none")) {
			return new ItemStack(type.get().parseMaterial(), amount);
		} else {
			return new ItemStack(type.get().parseMaterial(), amount, Short.valueOf(damage));
		}
	}

	ItemStack handleIdentifier(ItemStack i, String item) {
		if (getConfig().isSet("Items." + item + ".Identifier")) {
			if (getConfig().getBoolean("Items." + item + ".Custom-Tagged") == true)
				i = NBTEditor.set(i, "CUSTOM_ITEM", "CUSTOM_ITEM_IDENTIFIER");

			identifier().put(getConfig().getString("Items." + item + ".Identifier"), i);
		}
		return i;
	}

	@SuppressWarnings("deprecation")
	ItemStack handleDurability(ItemStack i, String item) {
		if (getConfig().isSet("Items." + item + ".Durability")) {
			if (!getConfig().getString("Items." + item + ".Durability").equals("100"))
				i.setDurability(Short.valueOf(getConfig().getString("Items." + item + ".Durability")));
		}
		return i;
	}

	ItemStack handleEnchants(ItemStack i, String item) {
		if (getConfig().isSet("Items." + item + ".Enchantments")) {

			for (String e : getConfig().getStringList("Items." + item + ".Enchantments")) {
				String[] breakdown = e.split(":");
				String enchantment = breakdown[0];

				int lvl = Integer.parseInt(breakdown[1]);
				i.addUnsafeEnchantment(Enchantment.getByName(enchantment), lvl);
			}
		}
		return i;
	}

	ItemMeta handleDisplayname(String item, ItemStack recipe) {
		ItemMeta itemMeta = recipe.getItemMeta();
		if (getConfig().isSet("Items." + item + ".Name")) {
			itemMeta.setDisplayName(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Items." + item + ".Name")));
		}
		return itemMeta;
	}

	ItemMeta handleLore(String item, ItemMeta m, List<String> loreList) {
		if (getConfig().isSet("Items." + item + ".Lore")) {

			for (String Item1Lore : getConfig().getStringList("Items." + item + ".Lore")) {
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
		if (getConfig().isSet("Items." + item + ".Hide-Enchants")
				&& getConfig().getBoolean("Items." + item + ".Hide-Enchants") == true) {
			m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		return m;
	}

	ItemMeta handleCustomModelData(String item, ItemMeta m) {
		if (getConfig().isSet("Items." + item + ".Custom-Model-Data")
				&& isInt(getConfig().getString("Items." + item + ".Custom-Model-Data"))) {
			try {
				Integer data = Integer.parseInt(getConfig().getString("Items." + item + ".Custom-Model-Data"));
				m.setCustomModelData(data);
			} catch (Exception e) {
				getLogger().log(Level.SEVERE,
						"Error occured while setting custom model data. This feature is only available for MC 1.14 or newer!");
			}
		}
		return m;
	}

	ItemMeta handleAttributes(String item, ItemMeta m) {
		if (getConfig().isSet("Items." + item + ".Attribute")) {
			for (String split : getConfig().getStringList("Items." + item + ".Attribute")) {
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

	@SuppressWarnings("deprecation")
	public void addItems() {
		disableRecipes();

		for (String item : getConfig().getConfigurationSection("Items").getKeys(false)) {

			ItemStack i = null;
			ItemMeta m = null;

			ShapedRecipe R = null;
			ShapelessRecipe S = null;

			HashMap<String, String> details = new HashMap<String, String>();
			ArrayList<RecipeAPI.Ingredient> ingredients = new ArrayList<>();

			List<String> loreList = new ArrayList<String>();
			List<String> r = getConfig().getStringList("Items." + item + ".ItemCrafting");

			String damage = getConfig().getString("Items." + item + ".Item-Damage");
			int amount = getConfig().getInt("Items." + item + ".Amount");

			Optional<XMaterial> type = XMaterial
					.matchXMaterial(getConfig().getString("Items." + item + ".Item").toUpperCase());

			if (!type.isPresent()) {
				getLogger().log(Level.SEVERE,
						"Dear message from Custom-Recipes: We are having trouble matching the material " + type
								+ " to a minecraft item. Please double check you have inputted the correct material "
								+ "ID in the config and try again. If this problem persists please contact Mehboss on Spigot!");
				return;
			}

			i = handleItemDamage(i, item, damage, type, amount); // handles ItemDamage
			i = handleDurability(i, item);
			i = handleEnchants(i, item);

			m = handleDisplayname(item, i); // handle Displayname
			m = handleLore(item, m, loreList); // handle Lore
			m = handleHideEnchants(item, m); // handles hiding enchants
			m = handleCustomModelData(item, m); // handles custom model data
			m = handleAttributes(item, m);

			i.setItemMeta(m);
			i = handleIdentifier(i, item); // handles CustomTag

			String line1 = r.get(0);
			String line2 = r.get(1);
			String line3 = r.get(2);

			HashMap<String, Material> shape = new HashMap<String, Material>();
			ArrayList<String> shapeletter = new ArrayList<String>();

			configName().put(i, item);
			menui().add(i);
			giveRecipe().put(item.toLowerCase(), i);

			String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			if (!version.contains("1_7") && !version.contains("1_8") && !version.contains("1_9")
					&& !version.contains("1_10") && !version.contains("1_11")) {
				NamespacedKey key = new NamespacedKey(Main.getInstance(),
						getConfig().getString("Items." + item + ".Identifier"));
				R = new ShapedRecipe(key, i);
				S = new ShapelessRecipe(key, i);
			} else {
				R = new ShapedRecipe(i);
				S = new ShapelessRecipe(i);
			}

			R.shape(line1, line2, line3);
			details.put("X", "null:false:false:1");

			for (String abbreviation : getConfig().getConfigurationSection("Items." + item + ".Ingredients")
					.getKeys(false)) {

				ConfigurationSection ingredientSection = getConfig()
						.getConfigurationSection("Items." + item + ".Ingredients." + abbreviation);

				String materialString = ingredientSection.getString("Material");
				int inAmount = 1;

				if (ingredientSection.isSet("Amount") && isInt(ingredientSection.getString("Amount")))
					inAmount = Integer.parseInt(ingredientSection.getString("Amount"));

				Optional<XMaterial> ingredientMaterial = XMaterial.matchXMaterial(materialString);

				if (!ingredientMaterial.isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + materialString
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the Ingredients section of the config and try again. If this problem persists please contact Mehboss on Spigot!");
					return;
				}

				String identifier = ingredientSection.isSet("Identifier") ? ingredientSection.getString("Identifier")
						: "false";
				String name = ingredientSection.isSet("Name") ? ingredientSection.getString("Name") : "false";

				Material finishedMaterial = ingredientMaterial.get().parseMaterial();
				R.setIngredient(abbreviation.charAt(0), finishedMaterial, inAmount);
				shape.put(abbreviation, finishedMaterial);

				if (!(ingredientSection.isSet("Name"))) {
					details.put(abbreviation,
							finishedMaterial.toString() + ":" + name + ":" + identifier + ":" + inAmount);
				} else {
					details.put(abbreviation,
							finishedMaterial.toString() + ":" + name + ":" + identifier + ":" + inAmount);
				}
			}

			recipe().add(R);

			String[] newsplit1 = line1.split("");
			String[] newsplit2 = line2.split("");
			String[] newsplit3 = line3.split("");

			// slot 1
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit1[0]).split(":")[0]),
					details.get(newsplit1[0]).split(":")[1], details.get(newsplit1[0]).split(":")[2],
					Integer.parseInt(details.get(newsplit1[0]).split(":")[3]), 0, checkAbsent(newsplit1[0])));

			// slot 2
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit1[1]).split(":")[0]),
					details.get(newsplit1[1]).split(":")[1], details.get(newsplit1[1]).split(":")[2],
					Integer.parseInt(details.get(newsplit1[1]).split(":")[3]), 1, checkAbsent(newsplit1[1])));

			// slot 3
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit1[2]).split(":")[0]),
					details.get(newsplit1[2]).split(":")[1], details.get(newsplit1[2]).split(":")[2],
					Integer.parseInt(details.get(newsplit1[2]).split(":")[3]), 2, checkAbsent(newsplit1[2])));

			// slot 4
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit2[0]).split(":")[0]),
					details.get(newsplit2[0]).split(":")[1], details.get(newsplit2[0]).split(":")[2],
					Integer.parseInt(details.get(newsplit2[0]).split(":")[3]), 3, checkAbsent(newsplit2[0])));

			// slot 5
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit2[1]).split(":")[0]),
					details.get(newsplit2[1]).split(":")[1], details.get(newsplit2[1]).split(":")[2],
					Integer.parseInt(details.get(newsplit2[1]).split(":")[3]), 4, checkAbsent(newsplit2[1])));

			// slot 6
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit2[2]).split(":")[0]),
					details.get(newsplit2[2]).split(":")[1], details.get(newsplit2[2]).split(":")[2],
					Integer.parseInt(details.get(newsplit2[2]).split(":")[3]), 5, checkAbsent(newsplit2[2])));

			// slot 7
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit3[0]).split(":")[0]),
					details.get(newsplit3[0]).split(":")[1], details.get(newsplit3[0]).split(":")[2],
					Integer.parseInt(details.get(newsplit3[0]).split(":")[3]), 6, checkAbsent(newsplit3[0])));

			// slot 8
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit3[1]).split(":")[0]),
					details.get(newsplit3[1]).split(":")[1], details.get(newsplit3[1]).split(":")[2],
					Integer.parseInt(details.get(newsplit3[1]).split(":")[3]), 7, checkAbsent(newsplit3[1])));

			// slot 9
			ingredients.add(api().new Ingredient(Material.matchMaterial(details.get(newsplit3[2]).split(":")[0]),
					details.get(newsplit3[2]).split(":")[1], details.get(newsplit3[2]).split(":")[2],
					Integer.parseInt(details.get(newsplit3[2]).split(":")[3]), 8, checkAbsent(newsplit3[2])));

			if (getConfig().getBoolean("Items." + item + ".Shapeless") == true) {

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

			if (getConfig().getBoolean("Items." + item + ".Shapeless") == false)
				Bukkit.getServer().addRecipe(R);
		}
	}

	void debug(String st) {
		getLogger().log(Level.WARNING, "-----------------");
		getLogger().log(Level.WARNING, "DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
		getLogger().log(Level.WARNING, st);
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
		return Main.getInstance().getConfig();
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
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
