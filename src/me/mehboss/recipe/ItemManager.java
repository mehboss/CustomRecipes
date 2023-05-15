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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemManager {

	@SuppressWarnings("deprecation")
	public void addItems() {
		disableRecipes();

		for (String item : getConfig().getConfigurationSection("Items").getKeys(false)) {

			ItemStack i = null;
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

			if (getConfig().isSet("Items." + item + ".Item-Damage") && damage.equalsIgnoreCase("none")) {
				i = new ItemStack(type.get().parseMaterial(), amount);
			} else {
				i = new ItemStack(type.get().parseMaterial(), amount, Short.valueOf(damage));
			}

			identifier().add(getConfig().getString("Items." + item + ".Identifier"));

			if (getConfig().isSet("Items." + item + ".Identifier")
					&& (getConfig().getBoolean("Items." + item + ".Custom-Tagged") == true)) {
				i = NBTEditor.set(i, getConfig().getString("Items." + item + ".Identifier"), "CUSTOM_ITEM_IDENTIFIER");
			}

			ItemMeta m = i.getItemMeta();

			if (getConfig().isSet("Items." + item + ".Name")) {
				m.setDisplayName(
						ChatColor.translateAlternateColorCodes('&', getConfig().getString("Items." + item + ".Name")));
			}

			if (getConfig().isSet("Items." + item + ".Lore")) {

				for (String Item1Lore : getConfig().getStringList("Items." + item + ".Lore")) {
					String crateLore = (Item1Lore.replaceAll("(&([a-fk-o0-9]))", "\u00A7$2"));
					loreList.add(crateLore);
				}
				if (!(loreList.isEmpty())) {
					m.setLore(loreList);
				}
			}

			if (getConfig().isSet("Items." + item + ".Durability")) {
				if (!getConfig().getString("Items." + item + ".Durability").equals("100"))
					i.setDurability(Short.valueOf(getConfig().getString("Items." + item + ".Durability")));
			}

			if (getConfig().isSet("Items." + item + ".Hide-Enchants")
					&& getConfig().getBoolean("Items." + item + ".Hide-Enchants") == true) {
				m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}

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

						i.getItemMeta().addAttributeModifier(Attribute.valueOf(attribute), modifier);
					} catch (Exception e) {
						getLogger().log(Level.SEVERE, "Could not add attribute " + attribute + ", " + attributeAmount
								+ ", " + equipmentSlot + ", to the item " + item + ", skipping for now.");
					}
				}
			}

			i.setItemMeta(m);

			if (getConfig().isSet("Items." + item + ".Enchantments")) {

				for (String e : getConfig().getStringList("Items." + item + ".Enchantments")) {
					String[] breakdown = e.split(":");
					String enchantment = breakdown[0];

					int lvl = Integer.parseInt(breakdown[1]);
					i.addUnsafeEnchantment(Enchantment.getByName(enchantment), lvl);
				}
			}

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
			details.put("X", "false:1");

			for (String I : getConfig().getStringList("Items." + item + ".Ingredients")) {

				String[] b = I.split(":");
				char lin1 = b[0].charAt(0);

				String lin2 = b[1];
				int inAmount = 1;

				if (b.length > 2 && isInt(b[2]))
					inAmount = Integer.parseInt(b[2]);

				Optional<XMaterial> mi = XMaterial.matchXMaterial(lin2);

				if (!mi.isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + lin2
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the Ingredients section of the config and try again. If this problem persists please contact Mehboss on Spigot!");
					return;
				}
				R.setIngredient(lin1, mi.get().parseMaterial(), inAmount);
				shape.put(b[0], mi.get().parseMaterial());

				if (b.length != 4) {
					details.put(b[0], "false" + ":" + inAmount);
				} else {
					details.put(b[0], b[3] + ":" + inAmount);
				}
			}

			recipe().add(R);

			String[] newsplit1 = line1.split("");
			String[] newsplit2 = line2.split("");
			String[] newsplit3 = line3.split("");

			// slot 1
			ingredients.add(api().new Ingredient(details.get(newsplit1[0]).split(":")[0],
					Integer.parseInt(details.get(newsplit1[0]).split(":")[1]), checkAbsent(newsplit1[0])));

			// slot 2
			ingredients.add(api().new Ingredient(details.get(newsplit1[1]).split(":")[0],
					Integer.parseInt(details.get(newsplit1[1]).split(":")[1]), checkAbsent(newsplit1[1])));

			// slot 3
			ingredients.add(api().new Ingredient(details.get(newsplit1[2]).split(":")[0],
					Integer.parseInt(details.get(newsplit1[2]).split(":")[1]), checkAbsent(newsplit1[2])));

			// slot 4
			ingredients.add(api().new Ingredient(details.get(newsplit2[0]).split(":")[0],
					Integer.parseInt(details.get(newsplit2[0]).split(":")[1]), checkAbsent(newsplit2[0])));

			// slot 5
			ingredients.add(api().new Ingredient(details.get(newsplit2[1]).split(":")[0],
					Integer.parseInt(details.get(newsplit2[1]).split(":")[1]), checkAbsent(newsplit2[1])));

			// slot 6
			ingredients.add(api().new Ingredient(details.get(newsplit2[2]).split(":")[0],
					Integer.parseInt(details.get(newsplit2[2]).split(":")[1]), checkAbsent(newsplit2[2])));

			// slot 7
			ingredients.add(api().new Ingredient(details.get(newsplit3[0]).split(":")[0],
					Integer.parseInt(details.get(newsplit3[0]).split(":")[1]), checkAbsent(newsplit3[0])));

			// slot 8
			ingredients.add(api().new Ingredient(details.get(newsplit3[1]).split(":")[0],
					Integer.parseInt(details.get(newsplit3[1]).split(":")[1]), checkAbsent(newsplit3[1])));

			// slot 9
			ingredients.add(api().new Ingredient(details.get(newsplit3[2]).split(":")[0],
					Integer.parseInt(details.get(newsplit3[2]).split(":")[1]), checkAbsent(newsplit3[2])));

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

						if (debug() == true) {
							getLogger().log(Level.WARNING,
									"[CRECIPE DEBUG] [1] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
							getLogger().log(Level.WARNING, "SHAPELESS IS SET TO TRUE. VARIABLE: " + sl);
						}
					}
				}

				Bukkit.getServer().addRecipe(S);
			}

			api().addRecipe(item, ingredients);

			if (getConfig().getBoolean("Items." + item + ".Shapeless") == false)
				Bukkit.getServer().addRecipe(R);
		}
	}

	boolean checkAbsent(String letterIngredient) {
		if (letterIngredient.equals("X"))
			return true;
		return false;
	}

	static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
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

	ArrayList<String> identifier() {
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

	Boolean debug() {
		return Main.getInstance().debug;
	}
}
