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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.willfp.ecoenchants.enchant.EcoEnchant;
import com.willfp.ecoenchants.enchant.EcoEnchants;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.recipe.RecipeAPI;
import net.advancedplugins.ae.api.AEAPI;
import valorless.havenbags.hooks.CustomRecipes;
import valorless.havenbags.hooks.CustomRecipes.BagInfo;

public class RecipeManager {

	boolean hasHavenBag() {
		if (Main.getInstance().hasHavenBags)
			return true;
		return false;
	}

	boolean isHavenBag(String item) {
		if (getConfig().isSet(item + ".Identifier")
				&& getConfig().getString(item + ".Identifier").split("-")[0].contains("havenbags"))
			return true;

		return false;
	}

	ItemStack handleBagCreation(Material bagMaterial, String item) {
		String bagTexture = getConfig().isString(item + ".Bag-Texture") ? getConfig().getString(item + ".Bag-Texture")
				: "none";
		boolean canBind = getConfig().isBoolean(item + ".Can-Bind") ? getConfig().getBoolean(item + ".Can-Bind") : true;
		int bagSize = getConfig().isInt(item + ".Bag-Size") ? getConfig().getInt(item + ".Bag-Size") : 1;
		int bagCMD = 0;

		CustomRecipes havenBags = new CustomRecipes();
		BagInfo bInfo = havenBags.new BagInfo(null, bagMaterial, bagSize, canBind, bagTexture);

		if (Main.getInstance().serverVersionAtLeast(1, 14) && getConfig().isSet(item + ".Custom-Model-Data")
				&& isInt(getConfig().getString(item + ".Custom-Model-Data"))) {
			bagCMD = getConfig().getInt(item + ".Custom-Model-Data");
		}

		bInfo.setModelData(bagCMD);

		ItemStack bag = CustomRecipes.CreateBag(bInfo);
		return bag;
	}

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

	ItemStack handleCustomTags(ItemStack i, String item) {
		if (!getConfig().isSet(item + ".Custom-Tags"))
			return i;

		for (String tag : getConfig().getStringList(item + ".Custom-Tags")) {
			String[] customTags = tag.split(":");

			if (customTags.length != 2)
				return i;

			try {
				i = NBTEditor.set(i, customTags[1], NBTEditor.CUSTOM_DATA, customTags[0]);
			} catch (Exception e) {
			}
		}
		return i;
	}

	ItemStack handleIdentifier(ItemStack i, String item) {
		if (!getConfig().isSet(item + ".Identifier"))
			return i;

		identifier().put(getConfig().getString(item + ".Identifier"), i);

		if (getConfig().getBoolean(item + ".Custom-Tagged") == true)
			i = NBTEditor.set(i, getConfig().getString(item + ".Identifier"), NBTEditor.CUSTOM_DATA,
					"CUSTOM_ITEM_IDENTIFIER");

		if (getConfig().getString(item + ".Identifier").equalsIgnoreCase("LifeStealHeart"))
			i.getItemMeta().getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "heart"),
					PersistentDataType.INTEGER, 1);

		return i;
	}

	@SuppressWarnings("deprecation")
	ItemStack handleDurability(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Durability"))
			if (!getConfig().getString(item + ".Durability").equals("100"))
				i.setDurability(Short.valueOf(getConfig().getString(item + ".Durability")));

		return i;
	}

	ItemStack handleEnchants(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Enchantments")) {
			try {
				for (String e : getConfig().getStringList(item + ".Enchantments")) {
					String[] breakdown = e.split(":");
					String enchantment = breakdown[0];

					if (!(XEnchantment.matchXEnchantment(enchantment).isPresent()))
						continue;

					Enchantment parsedEnchant = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
					int lvl = Integer.parseInt(breakdown[1]);
					i.addUnsafeEnchantment(parsedEnchant, lvl);
				}
			} catch (Exception e) {
				debug("Enchantment section for the recipe " + item + " is not valid. Skipping..");
			}
		}
		return i;
	}

	@SuppressWarnings("deprecation")
	ItemStack handleCustomEnchants(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Enchantments")) {

			try {
				for (String e : getConfig().getStringList(item + ".Enchantments")) {
					String[] breakdown = e.split(":");
					String enchantment = breakdown[0];
					int lvl = Integer.parseInt(breakdown[1]);

					if (Main.getInstance().hasAE && AEAPI.isAnEnchantment(enchantment.toLowerCase())) {
						i = AEAPI.applyEnchant(enchantment.toLowerCase(), lvl, i);
						continue;
					}

					if (Main.getInstance().hasEE) {

						EcoEnchants ee = EcoEnchants.INSTANCE;

						if (ee.getByName(enchantment) != null) {
							EcoEnchant name = ee.getByName(enchantment);

							Enchantment enchant = Enchantment.getByKey(name.getEnchantmentKey());
							i.addEnchantment(enchant, lvl);
							continue;
						}
					}

					if (Enchantment.getByName(enchantment) == null
							&& !(XEnchantment.matchXEnchantment(enchantment).isPresent()))
						debug("Enchantment - " + enchantment + " for the recipe " + item + " is not valid. Skipping..");
				}
			} catch (Exception e) {
				debug("Enchantment section for the recipe " + item + " is not valid. Skipping..");
			}
		}
		return i;
	}

	ItemMeta handleFlags(String item, ItemMeta m) {

		if (!(Main.getInstance().serverVersionAtLeast(1, 14)))
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

		if (getConfig().isSet(item + ".Name")) {
			if (debug()) {
				debug("Applying displayname..");
				debug("Displayname: " + getConfig().getString(item + ".Name"));
			}

			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(item + ".Name")));
		}
		return itemMeta;
	}

	ItemMeta handleLore(String item, ItemMeta m) {

		ArrayList<String> loreList = new ArrayList<String>();

		if (!getConfig().isSet(item + ".Lore"))
			return m;

		if (m.hasLore() && getConfig().getBoolean(item + ".Hide-Enchants") == false) {
			loreList = (ArrayList<String>) m.getLore();
		}

		for (String Item1Lore : getConfig().getStringList(item + ".Lore")) {
			String crateLore = (Item1Lore.replaceAll("(&([a-fk-o0-9]))", "\u00A7$2"));
			loreList.add(crateLore);
		}

		if (!(loreList.isEmpty())) {
			m.setLore(loreList);
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
		if (!(Main.getInstance().serverVersionAtLeast(1, 14)))
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
		if (!(Main.getInstance().serverVersionAtLeast(1, 12)))
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
			debug("No recipes were found to load");
			return;
		}

		for (File recipeFile : recipeFiles) {
			recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

			String item = recipeFile.getName().replace(".yml", "");

			if (!(recipeConfig.isConfigurationSection(item))) {
				Main.getInstance().getLogger().log(Level.WARNING,
						"Could not find configuration section " + item
								+ " in the recipe file that must match its filename: " + item
								+ ".yml - (CaSeSeNsItIvE) - Skipping this recipe");
				continue;
			}

			ItemStack i = null;
			ItemMeta m = null;

			ShapedRecipe R = null;
			ShapelessRecipe S = null;
			FurnaceRecipe fRecipe = null;

			HashMap<String, String> details = new HashMap<String, String>();
			ArrayList<RecipeAPI.Ingredient> ingredients = new ArrayList<>();

			List<Material> findIngredients = new ArrayList<Material>();
			List<String> gridRows = getConfig().getStringList(item + ".ItemCrafting");

			String converter = getConfig().getString(item + ".Converter");
			int amountRequirement = 9;

			if (debug())
				debug("Attempting to add the recipe " + item + "..");

			if (isHavenBag(item) && !(hasHavenBag())) {
				getLogger().log(Level.SEVERE, "Error loading recipe: " + recipeFile.getName());
				getLogger().log(Level.SEVERE,
						"Found a havenbag recipe, but can not find the havenbags plugin. Skipping recipe..");
				continue;
			}

			if (converter != null && isHavenBag(item)) {
				getLogger().log(Level.SEVERE,
						"Error loading recipe. Got " + converter + ", but the recipe is a havenbag recipe! Skipping..");
				continue;
			}

			if (converter != null && converter.equalsIgnoreCase("stonecutter")) {
				if (Main.getInstance().serverVersionAtLeast(1, 14)) {
					getLogger().log(Level.SEVERE, "Error loading recipe. Got " + converter
							+ ", but your server version is below 1.14. Expected furnace or no converter (for regular crafting) in: "
							+ recipeFile.getName());
					continue;
				}
				amountRequirement = 1;
			}

			if (converter != null && converter.equalsIgnoreCase("furnace"))
				amountRequirement = 1;

			if (converter == null)
				converter = "null (crafting)";

			String damage = getConfig().getString(item + ".Item-Damage");
			int amount = getConfig().isInt(item + ".Amount") ? getConfig().getInt(item + ".Amount") : 1;

			Optional<XMaterial> type = getConfig().isString(item + ".Item")
					? XMaterial.matchXMaterial(getConfig().getString(item + ".Item").toUpperCase())
					: null;

			if (type == null || !type.isPresent()) {
				getLogger().log(Level.SEVERE, "Error loading recipe: " + recipeFile.getName());
				getLogger().log(Level.SEVERE, "We are having trouble matching the material "
						+ getConfig().getString(item + ".Item").toUpperCase()
						+ " to a minecraft item. Please double check you have inputted the correct material enum into the 'Item'"
						+ " section and try again. If this problem persists please contact Mehboss on Spigot!");
				continue;
			}

			i = handleItemDamage(i, item, damage, type, amount);
			i = handleDurability(i, item);
			i = handleEnchants(i, item);
			i = handleCustomEnchants(i, item);

			m = handleDisplayname(item, i);
			m = handleHideEnchants(item, m);
			m = handleCustomModelData(item, m);
			m = handleAttributes(item, m);
			m = handleFlags(item, m);
			m = handleLore(item, m);

			i.setItemMeta(m);

			if (isHavenBag(item))
				i = handleBagCreation(i.getType(), item);

			i = handleIdentifier(i, item);

			if (i == null || i.getType() == Material.AIR) {
				getLogger().log(Level.SEVERE, "Error loading recipe: " + recipeFile.getName());
				getLogger().log(Level.SEVERE,
						"The itemstack is null. Please double check your recipe.yml or contact Mehboss on Spigot for support.");
				continue;
			}

			String line1 = gridRows.get(0);
			String line2 = gridRows.get(1);
			String line3 = gridRows.get(2);

			HashMap<String, Material> shape = new HashMap<String, Material>();
			ArrayList<String> shapeletter = new ArrayList<String>();

			giveRecipe().put(item.toLowerCase(), i);
			itemNames().put(item, i);
			configName().put(i, item);

			NamespacedKey key = null;
			if (Main.getInstance().serverVersionAtLeast(1, 12)) {
				key = new NamespacedKey(Main.getInstance(), getConfig().getString(item + ".Identifier"));
				R = new ShapedRecipe(key, i);
				S = new ShapelessRecipe(key, i);
			} else {
				R = new ShapedRecipe(i);
				S = new ShapelessRecipe(i);
			}

			R.shape(line1, line2, line3);
			details.put("X", "null:false:false:1");
			float experience = 1.0f;
			int cookTime = 200;

			for (String abbreviation : getConfig().getConfigurationSection(item + ".Ingredients").getKeys(false)) {
				ConfigurationSection ingredientSection = getConfig()
						.getConfigurationSection(item + ".Ingredients." + abbreviation);
				// Iterate through the specified item declaration.

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
					debug("Ingredient Type: " + materialString);
				}

				details.put(abbreviation, finishedMaterial.toString() + ":" + name + ":" + identifier + ":" + inAmount);
				findIngredients.add(finishedMaterial);

				if (findIngredients.size() > amountRequirement) {
					getLogger().log(Level.SEVERE,
							"Error loading recipe. Got " + finishedMaterial.toString()
									+ " but expected only one entry in Ingredients since using " + converter
									+ " as Converter in: " + recipeFile.getName());
					continue;
				}

				if (converter.equalsIgnoreCase("furnace")) {
					if (key != null)
						fRecipe = new FurnaceRecipe(key, i, finishedMaterial, experience, cookTime);
					else
						fRecipe = new FurnaceRecipe(i, finishedMaterial);

				}
			}

			ingredients().put(getConfig().getString(item + ".Identifier"), findIngredients);
			recipe().add(R);

			String[] newsplit1 = line1.split("");
			String[] newsplit2 = line2.split("");
			String[] newsplit3 = line3.split("");

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
			int count = 0;
			for (String handleIngredients : newSlots) {
				// Iterate through the specified 9x9 grid.
				slot++;
				Material itemMaterial = details.get(handleIngredients).split(":")[0].equals("null") ? null
						: Material.matchMaterial(details.get(handleIngredients).split(":")[0]);

				if (itemMaterial != null) {
					count++;

					if (count > amountRequirement) {
						getLogger().log(Level.SEVERE,
								"Error loading recipe. Got " + handleIngredients + " but converter is " + converter
										+ " so use only one slot" + " (X for others) for ItemCrafting in: "
										+ recipeFile.getName());
						continue;
					}
				}

				String itemName = details.get(handleIngredients).split(":")[1];
				String itemIdentifier = details.get(handleIngredients).split(":")[2];

				int itemAmount = Integer.parseInt(details.get(handleIngredients).split(":")[3]);
				boolean isEmpty = checkAbsent(handleIngredients);

				if (debug()) {
					debug("[HandlingIngredient] Ingredient Identifier: " + itemIdentifier);
					debug("[HandlingIngredient] Ingredient Type: " + itemMaterial);
				}

				ingredients
						.add(api().new Ingredient(itemMaterial, itemName, itemIdentifier, itemAmount, slot, isEmpty));
			}

			if (converter.equalsIgnoreCase("furnace")) {
				Bukkit.getServer().addRecipe(fRecipe);

				if (debug())
					debug("Added " + converter + " Recipe: " + item + " " + i.getAmount());

			}

			if (converter.equalsIgnoreCase("stonecutter")) {
				if (!(Main.getInstance().serverVersionAtLeast(1, 14))) {
					getLogger().log(Level.SEVERE, "Error loading recipe. Your API version is >= 1.14"
							+ " but does not support NameSpacedKey or the plugin failed to create one.");
					break;
				}

				if (debug())
					debug("Successfully added " + converter + " recipe " + item + ".. (" + i.getAmount() + ")");

				StonecuttingRecipe scRecipe = new StonecuttingRecipe(key, i, findIngredients.get(0));
				Bukkit.getServer().addRecipe(scRecipe);

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
							debug("Adding shapeless letters.. Letter: " + sl);
					}
				}

				Bukkit.getServer().addRecipe(S);
			}

			if (getConfig().getBoolean(item + ".Shapeless") == false)

				try {
					Bukkit.getServer().addRecipe(R);
				} catch (Exception e) {
					Main.getInstance().getLogger().log(Level.SEVERE,
							"An exception has occurred. Possible duplicate identifier " + key
									+ " has been found, skipping..");
					continue;
				}

			if (debug())
				debug("Successfully added " + item + " with the amount output of " + i.getAmount());

			api().addRecipe(item, ingredients);
		}
	}

	void debug(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
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
