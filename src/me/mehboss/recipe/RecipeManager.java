package me.mehboss.recipe;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XItemStack;
import com.cryptomorin.xseries.XItemStack.Deserializer;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.bananapuncher714.nbteditor.NBTEditor.NBTCompound;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.RecipeConditions;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.BrewingRecipeData;
import me.mehboss.utils.data.CookingRecipeData;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.data.WorkstationRecipeData;
import net.advancedplugins.ae.api.AEAPI;
import valorless.havenbags.api.HavenBagsAPI;
import valorless.havenbags.datamodels.Data;

public class RecipeManager {

	List<String> delayedRecipes = new ArrayList<>();
	boolean allFinished = false;
	FileConfiguration recipeConfig = null;

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

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

	public ItemStack handleBagCreation(Material bagMaterial, int bagSize, int bagCMD, String canBind, String bagTexture,
			String item) {

		if (item != null) {
			bagTexture = getConfig().isString(item + ".Bag-Texture") ? getConfig().getString(item + ".Bag-Texture")
					: "none";
			canBind = getConfig().getBoolean(item + ".Can-Bind") ? "null" : "ownerless";
			bagSize = getConfig().isInt(item + ".Bag-Size") ? getConfig().getInt(item + ".Bag-Size") : 9;
			bagCMD = 0;

			if (Main.getInstance().serverVersionAtLeast(1, 14) && getConfig().isSet(item + ".Custom-Model-Data")
					&& isInt(getConfig().getString(item + ".Custom-Model-Data"))) {
				bagCMD = getConfig().getInt(item + ".Custom-Model-Data");
			}
		}

		Data bagData = new Data("null", canBind);
		bagData.setSize(bagSize);
		bagData.setMaterial(bagMaterial);
		bagData.setTexture(bagTexture);
		bagData.setModeldata(bagCMD);

		ItemStack bagItem = HavenBagsAPI.generateBagItem(bagData);
		bagItem = handleIdentifier(bagItem, item);
		return bagItem;
	}

	void handleCommand(String item, Recipe recipe) {
		String path = item + ".Commands.Run-Commands";
		String grantItem = item + ".Commands.Give-Item";

		// Check if the list exists and is not empty
		if (!getConfig().isSet(path) || getConfig().getStringList(path).isEmpty())
			return;

		if (getConfig().isSet(grantItem))
			recipe.setGrantItem(getConfig().getBoolean(grantItem));

		List<String> commands = getConfig().getStringList(path);
		recipe.setCommands(commands);
		logDebug("Successfully set commands: " + commands, item);
	}

	void handleMaterialChoice(Recipe recipe, Ingredient ingredient, String path) {
		List<String> list = getConfig().getStringList(path + ".Materials");
		if (list == null || list.isEmpty())
			return;
		for (String material : list) {
			Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);
			if (!validMaterial(recipe.getName(), material, rawMaterial))
				continue;
			ingredient.addMaterialChoice(rawMaterial.get().get());
		}
	}

	void handleRecipeFlags(String item, Recipe recipe) {
		if (getConfig().getBoolean(item + ".Custom-Tagged"))
			recipe.setTagged(true);

		if (getConfig().isBoolean(item + ".Enabled"))
			recipe.setActive(getConfig().getBoolean(item + ".Enabled"));

		if (getConfig().isString(item + ".Permission"))
			recipe.setPerm(getConfig().getString(item + ".Permission"));

		if (getConfig().isInt(item + ".Cooldown") && getConfig().getInt(item + ".Cooldown") != -1)
			recipe.setCooldown(getConfig().getInt(item + ".Cooldown"));

		if (getConfig().isBoolean(item + ".Placeable"))
			recipe.setPlaceable(getConfig().getBoolean(item + ".Placeable"));

		if (getConfig().isSet(item + ".Disabled-Worlds")) {
			for (String world : getConfig().getStringList(item + ".Disabled-Worlds"))
				recipe.addDisabledWorld(world);
		}

		recipe.setIgnoreData(getConfig().getBoolean(item + ".Flags.Ignore-Data"));
		recipe.setIgnoreNames(getConfig().getBoolean(item + ".Flags.Ignore-Name"));
		recipe.setIgnoreModelData(getConfig().getBoolean(item + ".Flags.Ignore-Model-Data"));
		recipe.setExactChoice(getConfig().getBoolean(item + ".Exact-Choice"));
		recipe.setDiscoverable(getConfig().getBoolean(item + ".Auto-Discover-Recipe"));
		recipe.setLegacyNames(getConfig().getBoolean(item + ".Use-Display-Name", true));

		if (getConfig().isSet(item + ".Book-Category")) {
			try {
				recipe.setBookCategory(getConfig().getString(item + ".Book-Category").toUpperCase());
			} catch (NoClassDefFoundError e) {
			}
		}
	}

	void handleCraftingData(Recipe recipe, String configPath) {
		CraftingRecipeData workbench = (CraftingRecipeData) recipe;
		if (getConfig().getBoolean(configPath + ".Use-Conditions")) {
			ConfigurationSection rSec = getConfig().getConfigurationSection(configPath);
			workbench.setConditionSet(RecipeConditions.parseConditionSet(rSec));
		}

		if (getConfig().isSet(configPath + ".ItemsLeftover"))
			for (String leftOver : getConfig().getStringList(configPath + ".ItemsLeftover")) {
				if (leftOver.equalsIgnoreCase("none"))
					return;
				workbench.addLeftoverItem(leftOver);
			}

		if (Main.getInstance().serverVersionAtLeast(1, 13))
			if (getConfig().isSet(configPath + ".Group")
					&& !getConfig().getString(configPath + ".Group").equalsIgnoreCase("none")) {
				try {
					workbench.setGroup(getConfig().getString(configPath + ".Group"));
				} catch (NoClassDefFoundError e) {
				}
			}
	}

	void handleFurnaceData(Recipe recipe, String configPath) {
		CookingRecipeData furnace = (CookingRecipeData) recipe;
		if (getConfig().isInt(configPath + ".Cooktime"))
			furnace.setCookTime(getConfig().getInt(configPath + ".Cooktime"));
		if (getConfig().isInt(configPath + ".Experience"))
			furnace.setExperience(getConfig().getInt(configPath + ".Experience"));
	}

	void handleAnvilData(Recipe recipe, String configPath) {
		WorkstationRecipeData anvil = (WorkstationRecipeData) recipe;
		if (getConfig().isInt(configPath + ".Repair-Cost"))
			anvil.setRepairCost(getConfig().getInt(configPath + ".Repair-Cost"));
	}

	void handleGrindstoneData(Recipe recipe, String configPath) {
		WorkstationRecipeData grindstone = (WorkstationRecipeData) recipe;
		if (getConfig().isInt(configPath + ".Experience"))
			grindstone.setExperience(getConfig().getInt(configPath + ".Experience"));
	}

	void handleStonecutterData(Recipe recipe, String configPath) {
		WorkstationRecipeData stonecutter = (WorkstationRecipeData) recipe;
		if (getConfig().isSet(configPath + ".Group")
				&& !getConfig().getString(configPath + ".Group").equalsIgnoreCase("none"))
			stonecutter.setGroup(getConfig().getString(configPath + ".Group"));
	}

	void handleBrewingData(Recipe recipe, String configPath) {
		BrewingRecipeData brew = (BrewingRecipeData) recipe;
		String newPath = (configPath + ".Required-Item");

		if (getConfig().getBoolean(configPath + ".Requires-Items")
				&& getConfig().getConfigurationSection(configPath + ".Required-Item") != null) {
			ItemStack potionItem = handlePotions(newPath);
			brew.setRequiredBottleType(potionItem.getType());
			brew.setRequiresBottles(true);
		}

		brew.setBrewPerfect(getConfig().getBoolean(configPath + ".Exact-Choice", true));

		if (getConfig().isSet(configPath + ".FuelSet")) {
			brew.setBrewFuelSet(getConfig().getInt(configPath + ".FuelSet"));
		}

		if (getConfig().isSet(configPath + ".FuelCharge")) {
			brew.setBrewFuelCharge(getConfig().getInt(configPath + ".FuelCharge"));
		}
	}

	@SuppressWarnings("deprecation")
	ItemStack handlePotions(String configPath) {
		// Material
		String matName = getConfig().getString(configPath + ".Material", getConfig().getString(configPath + ".Item"));
		Material matType = XMaterial.matchXMaterial(matName).isPresent() ? XMaterial.matchXMaterial(matName).get().get()
				: XMaterial.POTION.get();

		boolean vanilla = getConfig().getBoolean(configPath + ".UseVanillaType", false);
		Color color = parseColor(getConfig().getString(configPath + ".Color"));

		List<Map<?, ?>> list = getConfig().getMapList(configPath + ".Effects");
		List<PotionEffect> effects = new ArrayList<>();

		if (!list.isEmpty()) {
			for (Map<?, ?> entry : list) {
				String typeName = String.valueOf(entry.get("Type"));
				int dur = entry.containsKey("Duration") ? Integer.parseInt(String.valueOf(entry.get("Duration"))) : 200;
				int amp = entry.containsKey("Amplifier") ? Integer.parseInt(String.valueOf(entry.get("Amplifier"))) : 0;

				// XPotion lookup ONLY
				Optional<XPotion> xp = XPotion.of(typeName);
				if (!xp.isPresent() || xp.get().getPotionEffectType() == null)
					continue;

				PotionEffect pe = xp.get().buildPotionEffect(dur * 20, amp + 1);
				if (pe != null)
					effects.add(pe);
			}
		} else {
			String potionName = getConfig().getString(configPath + ".PotionType", "NONE");
			int durationSeconds = getConfig().getInt(configPath + ".Duration", 3600);
			int amplifier = getConfig().getInt(configPath + ".Amplifier", 0);

			Optional<XPotion> match = XPotion.of(potionName);
			if (!match.isPresent() || match.get().getPotionEffectType() == null)
				return new ItemStack(Material.POTION);

			PotionEffect effect = match.get().buildPotionEffect(durationSeconds * 20, amplifier + 1);
			if (effect != null)
				effects.add(effect);
		}

		if (effects.isEmpty())
			return new ItemStack(Material.POTION);

		if (vanilla) {
			PotionEffect effect = effects.get(0);
			XPotion xp = XPotion.of(effect.getType());

			PotionType base = normalize(xp.getPotionType());
			if (base == null) {
				Bukkit.getLogger().warning("[DEBUG] Invalid vanilla potion type '" + xp + "'. Falling back to custom.");
				return XPotion.buildItemWithEffects(matType, color, effects.toArray(new PotionEffect[0]));
			}

			ItemStack item = new ItemStack(matType);
			PotionMeta meta = (PotionMeta) item.getItemMeta();

			boolean isExtended = effect.getDuration() > (3600 * 20);
			boolean isUpgraded = effect.getAmplifier() > 0;

			try {
				meta.setBasePotionData(new PotionData(base, isExtended, isUpgraded));
			} catch (Throwable ignored) {
			}

			if (color != null)
				meta.setColor(color);

			item.setItemMeta(meta);
			return item;
		}
		return XPotion.buildItemWithEffects(matType, color, effects.toArray(new PotionEffect[0]));
	}

	private PotionType normalize(PotionType type) {
		if (type == null)
			return null;
		if (!Main.getInstance().serverVersionAtLeast(1, 20))
			return type;
		// New API since 1.20.5 only accepts base types.
		try {
			return PotionType.valueOf(type.name().replace("LONG_", "").replace("STRONG_", ""));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Parses a color string like "RED", "BLUE", or "255,0,0" to a Bukkit Color.
	 */
	private Color parseColor(String input) {
		if (input == null)
			return null;

		try {
			// Try named colors first (RED, BLUE, GREEN, etc.)
			switch (input.toUpperCase()) {
			case "RED":
				return Color.RED;
			case "BLUE":
				return Color.BLUE;
			case "GREEN":
				return Color.GREEN;
			case "YELLOW":
				return Color.YELLOW;
			case "PURPLE":
				return Color.PURPLE;
			case "AQUA":
				return Color.AQUA;
			case "WHITE":
				return Color.WHITE;
			case "BLACK":
				return Color.BLACK;
			case "FUCHSIA":
				return Color.FUCHSIA;
			case "ORANGE":
				return Color.ORANGE;
			case "GRAY":
				return Color.GRAY;
			}

			// Otherwise, try parsing RGB manually (e.g., "255,0,0")
			if (input.contains(",")) {
				String[] parts = input.split(",");
				if (parts.length == 3) {
					int r = Integer.parseInt(parts[0].trim());
					int g = Integer.parseInt(parts[1].trim());
					int b = Integer.parseInt(parts[2].trim());
					return Color.fromRGB(r, g, b);
				}
			}
		} catch (Exception ignored) {
		}

		return null;
	}

	ItemStack handleHeadTexture(String material) {
		if (material == null || material.isEmpty() || !XMaterial.matchXMaterial(material.split(":")[0]).isPresent())
			return null;

		String[] split = material.split(":");
		String item = split[0];
		ItemStack head = XMaterial.matchXMaterial(item).get().parseItem();

		// If it's not a player head, just return the item
		if (split.length < 2 || XMaterial.matchXMaterial(item).get() != XMaterial.PLAYER_HEAD)
			return head;

		String texture = split[1];
		UUID uuid = UUID.randomUUID();
		SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

		try {
			if (Main.getInstance().serverVersionAtLeast(1, 21)) {
				// Modern API (Spigot 1.21+)
				PlayerProfile profile = Bukkit.createPlayerProfile(uuid);
				PlayerTextures textures = profile.getTextures();
				textures.setSkin(new URL(CompatibilityUtil.extractUrlFromBase64(texture)));
				profile.setTextures(textures);

				logDebug("Applying player head texture (modern API)", "");
				logDebug("Texture: ", texture);
				logDebug("Extracted URL: " + CompatibilityUtil.extractUrlFromBase64(texture), "");

				skullMeta.setOwnerProfile(profile);

			} else {
				// Legacy method (1.8–1.20.6): set GameProfile manually
				GameProfile profile = new GameProfile(uuid, "Player");
				profile.getProperties().put("textures", new Property("textures", texture));

				if (Main.getInstance().serverVersionAtLeast(1, 13)) {
					// Try to use setProfile(GameProfile)
					Method mtd = CompatibilityUtil.getMethod(skullMeta.getClass(), "setProfile", GameProfile.class);
					if (mtd != null) {
						CompatibilityUtil.invokeMethod(mtd, skullMeta, profile);
					} else {
						// Fallback: directly set the private profile field
						CompatibilityUtil.setFieldValue(skullMeta, "profile", profile);
					}
				} else {
					// 1.12 or lower (including 1.8): always set the private field directly
					CompatibilityUtil.setFieldValue(skullMeta, "profile", profile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		head.setItemMeta(skullMeta);
		return head;
	}

	@SuppressWarnings("unchecked")
	ItemStack applyCustomTags(ItemStack item, String recipe) {
		try {
			List<Map<String, Object>> customTags = getCustomTags(recipe);
			for (Map<String, Object> tagEntry : customTags) {
				List<String> path = (List<String>) tagEntry.get("path");
				Object value = tagEntry.get("value");

				logDebug("Handling tag path " + path.get(0) + "..", recipe);

				// Check if this is an AttributeModifier entry
				if (path.get(0).equalsIgnoreCase("AttributeModifiers")) {
					List<Map<String, Object>> modifiers = (List<Map<String, Object>>) value;

					for (Map<String, Object> modifier : modifiers) {
						// Apply attribute modifier
						item = applyAttributeModifier(item, modifier);
					}
				} else if (path.get(0).equalsIgnoreCase("SpawnerID")) {
					logDebug("Attempting to apply path tags now..", recipe);

					if (XMaterial.matchXMaterial(item.getType()) != XMaterial.SPAWNER)
						return item;

					EntityType entity = EntityType.valueOf((String) value);
					logDebug("Attempting to apply entity type " + entity + " to a spawner..", recipe);

					BlockStateMeta bsm = (BlockStateMeta) item.getItemMeta();
					CreatureSpawner cs = (CreatureSpawner) bsm.getBlockState();

					if (entity == null) {
						logDebug("Could not find entitytype " + entity + "! Skipping application of tag.", recipe);
						return item;
					}

					logDebug("Successfully set " + entity + " to the spawner..", recipe);
					cs.setSpawnedType(EntityType.valueOf((String) value));
					bsm.setBlockState(cs);
					item.setItemMeta(bsm);
					return item;

				} else if (path.get(0).equalsIgnoreCase("Potion")) {
					if (!Main.getInstance().serverVersionAtLeast(1, 9))
						return item;

					ItemStack potion = item;
					PotionMeta meta = (PotionMeta) potion.getItemMeta();

					boolean extended = tagEntry.containsKey("extended") && (boolean) tagEntry.get("extended");
					boolean upgraded = tagEntry.containsKey("upgraded") && (boolean) tagEntry.get("upgraded");

					meta.setBasePotionData(new PotionData(PotionType.valueOf((String) value), extended, upgraded));
					potion.setItemMeta(meta);

					return potion;
				} else if (path.get(0).equalsIgnoreCase("Enchantments")) {
					// Check if it's an enchantment book
					if (item.getType() == XMaterial.ENCHANTED_BOOK.get()) {
						Map<String, Integer> enchants = (Map<String, Integer>) value;

						// Get the EnchantmentMeta to apply enchantments to the enchanted book
						EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();

						for (Map.Entry<String, Integer> enchantEntry : enchants.entrySet()) {
							Enchantment enchantment = !XEnchantment.matchXEnchantment(enchantEntry.getKey()).isPresent()
									? null
									: XEnchantment.matchXEnchantment(enchantEntry.getKey()).get().getEnchant();
							int level = enchantEntry.getValue();

							// Apply enchantment if it's valid
							if (enchantment != null) {
								meta.addStoredEnchant(enchantment, level, true);
							}
						}

						// Apply the modified enchantment meta to the item
						item.setItemMeta(meta);
					}
				} else if ((item.getType() == XMaterial.ITEM_FRAME.get()
						|| item.getType() == XMaterial.GLOW_ITEM_FRAME.get())
						&& path.get(0).equalsIgnoreCase("EntityTag")) {

					try {
						if (value instanceof Map) {
							Map<String, Object> compound = (Map<String, Object>) value;

							// Invisible (boolean)
							if (compound.containsKey("Invisible")) {
								boolean invisible = Boolean.parseBoolean(String.valueOf(compound.get("Invisible")));
								item = NBTEditor.set(item, invisible, "EntityTag", "Invisible");
							}

							// Glowing (boolean)
							if (compound.containsKey("Glowing")) {
								boolean glowing = Boolean.parseBoolean(String.valueOf(compound.get("Glowing")));
								item = NBTEditor.set(item, glowing, "EntityTag", "Glowing");
							}

							// Fixed (boolean)
							if (compound.containsKey("Fixed")) {
								boolean fixed = Boolean.parseBoolean(String.valueOf(compound.get("Fixed")));
								item = NBTEditor.set(item, fixed, "EntityTag", "Fixed");
							}

							// Facing (byte)
							if (compound.containsKey("Facing")) {
								byte facing = ((Number) compound.get("Facing")).byteValue();
								item = NBTEditor.set(item, facing, "EntityTag", "Facing");
							}
						} else {
							logDebug("EntityTag value must be a map. Skipping.", recipe);
						}

						return item;

					} catch (Throwable t) {
						logError("Failed to apply EntityTag to item frame with NBTEditor.", recipe);
						t.printStackTrace();
						return item;
					}
				} else {
					// Apply general NBT tag
					item = applyNBT(item, value, path.toArray(new String[0]));
				}
			}
			return item;
		} catch (Exception e) {
			logError("Could not apply custom tags! This generally happens when the operations are incorrect.", recipe);
			e.printStackTrace();
			return item;
		}
	}

	ItemStack applyAttributeModifier(ItemStack item, Map<String, Object> modifier) {
		// Retrieve necessary fields
		String name = (String) modifier.get("Name");
		String attributeName = (String) modifier.get("AttributeName");

		double amount = (double) modifier.get("Amount");
		int operation = (int) modifier.get("Operation");
		String slot = (String) modifier.get("Slot");

		// Debugging output
		logDebug("Applying attribute modifier:", "");
		logDebug("Name: " + name + ", AttributeName: " + attributeName + ", Amount: " + amount + ", Operation: "
				+ operation + ", Slot: " + slot, "");

		int[] uuid = { 0, 0, 0, 0 };

		NBTCompound compound = NBTEditor.getNBTCompound(item);
		compound = NBTEditor.set(compound, attributeName, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers",
				NBTEditor.NEW_ELEMENT, "AttributeName");
		compound = NBTEditor.set(compound, name, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0, "Name");
		compound = NBTEditor.set(compound, amount, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0, "Amount");
		compound = NBTEditor.set(compound, operation, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0,
				"Operation");

		if (slot != null)
			compound = NBTEditor.set(compound, slot, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0, "Slot");

		compound = NBTEditor.set(compound, uuid, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0, "UUID");
		compound = NBTEditor.set(compound, 99L, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0, "UUIDMost");
		compound = NBTEditor.set(compound, 77530600L, NBTEditor.ITEMSTACK_COMPONENTS, "AttributeModifiers", 0,
				"UUIDLeast");

		item = NBTEditor.getItemFromTag(compound);
		return item;
	}

	ItemStack applyNBT(ItemStack item, Object value, String... path) {
		if (path == null || path.length == 0)
			throw new IllegalArgumentException("NBT path cannot be null or empty");

		if (value instanceof Number) {
			Number num = (Number) value;
			if (num.intValue() == 0 || num.intValue() == 1)
				value = num.byteValue();
		}

		try {
			return NBTEditor.set(item, value, (Object[]) path);
		} catch (Exception ex) {
			throw new RuntimeException(
					"Failed to apply NBT at path " + String.join(".", path) + " with value: " + value, ex);
		}
	}

	@SuppressWarnings("unchecked")
	List<Map<String, Object>> getCustomTags(String recipe) {
		List<Map<?, ?>> rawList = getConfig().getMapList(recipe + ".Custom-Tags");
		List<Map<String, Object>> castedList = new ArrayList<>();

		for (Map<?, ?> rawMap : rawList) {
			castedList.add((Map<String, Object>) (Map<?, ?>) rawMap);
		}

		return castedList;
	}

	ItemStack handleIdentifier(ItemStack i, String item) {
		if (!getConfig().isSet(item + ".Identifier"))
			return i;

		String identifier = getConfig().getString(item.replaceAll(".Result", "") + ".Identifier");

		if (getConfig().getBoolean(item + ".Custom-Tagged"))
			i = NBTEditor.set(i, identifier, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");

		if (identifier.equalsIgnoreCase("LifeStealHeart")) {
			i.getItemMeta().getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "heart"),
					PersistentDataType.INTEGER, 1);
		}

		return i;
	}

	@SuppressWarnings("deprecation")
	public ItemStack handleDurability(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Durability")) {
			String durability = getConfig().getString(item + ".Durability");
			if (!durability.isEmpty() && !durability.equals("100") && !durability.equals("0")
					&& !durability.equals("none"))
				i.setDurability(Short.valueOf(getConfig().getString(item + ".Durability")));
		}
		return i;
	}

	ItemStack handleEnchants(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Enchantments")) {
			if (!getConfig().isSet(item + ".Enchantments")) {
				logError("Enchantment section is not valid, skipping..", item);
				return i;
			}

			for (String e : getConfig().getStringList(item + ".Enchantments")) {
				String[] breakdown = e.split(":");
				String enchantment = breakdown[0];

				if (enchantment.equalsIgnoreCase("none"))
					break;

				if (breakdown.length >= 3)
					continue;

				if (!(XEnchantment.matchXEnchantment(enchantment).isPresent())) {
					logError("Enchantment " + enchantment + " is not valid, skipping..", item);
					continue;
				}

				Enchantment parsedEnchant = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
				int lvl = Integer.parseInt(breakdown[1]);
				i.addUnsafeEnchantment(parsedEnchant, lvl);
			}
		}
		return i;
	}

	ItemStack handleCustomEnchants(ItemStack i, String item) {
		if (getConfig().isSet(item + ".Enchantments")) {

			try {
				for (String e : getConfig().getStringList(item + ".Enchantments")) {
					Boolean applied = false;
					String[] breakdown = e.split(":");

					if (breakdown.length != 3)
						continue;

					String enchantment = breakdown[1].toLowerCase();
					int lvl = Integer.parseInt(breakdown[2]);

					if (Main.getInstance().hasAE && AEAPI.isAnEnchantment(enchantment)) {
						i = AEAPI.applyEnchant(enchantment, lvl, i);
						applied = true;
						continue;
					}

					if (Main.getInstance().hasEE) {
						NamespacedKey enchantKey = NamespacedKey.fromString("minecraft:" + enchantment);
						if (Enchantment.getByKey(enchantKey) != null) {
							Enchantment enchant = Enchantment.getByKey(enchantKey);
							i.addUnsafeEnchantment(enchant, lvl);
							applied = true;
							continue;
						}
					}

					if (!applied && !XEnchantment.matchXEnchantment(enchantment).isPresent())
						logError("Could not find custom enchantment " + enchantment + "..", item);
				}
			} catch (Exception e) {
				logError("Enchantment section is not valid. Skipping..", item);
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
					logError("Could not add the item flag (" + flag
							+ ").. This item flag could not be found so we will be skipping this flag for now. Please visit https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/ItemFlag.html for a list of valid flags.",
							item);
					continue;
				}
			}
		}
		return m;
	}

	ItemMeta handleDisplayname(String item, ItemStack recipe, boolean useLegacyNames) {
		ItemMeta itemMeta = recipe.getItemMeta();

		if (getConfig().isSet(item + ".Name")) {
			String name = getConfig().getString(item + ".Name");

			if (name.equalsIgnoreCase("none"))
				return itemMeta;

			logDebug("Applying displayname..", item);
			logDebug("Displayname: " + name, item);

			itemMeta = CompatibilityUtil.setDisplayname(recipe, name, useLegacyNames);
		}
		return itemMeta;
	}

	ItemMeta handleLore(String item, ItemMeta m) {

		ArrayList<String> loreList = new ArrayList<String>();

		if (!getConfig().isSet(item + ".Lore"))
			return m;

		if (m.hasLore() && !getConfig().getBoolean(item + ".Hide-Enchants")) {
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
				logError(
						"Error occured while setting custom model data. This feature is only available for MC 1.14 or newer!",
						item);
			}
		}
		return m;
	}

	ItemMeta handleAttributes(String item, ItemMeta m) {
		String logName = item.replaceAll(".Result", "");
		if (!(Main.getInstance().serverVersionAtLeast(1, 14))) {
			logError(
					"Failed to apply attributes due to unsupported version. You must use NBT to apply the attributes. Skipping for now..",
					logName);
			return m;
		}

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
					if (equipmentSlot == null) {
						modifier = new AttributeModifier(attribute, attributeAmount,
								AttributeModifier.Operation.ADD_NUMBER);
					} else {
						modifier = new AttributeModifier(UUID.randomUUID(), attribute, attributeAmount,
								AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.valueOf(equipmentSlot));
					}

					m.addAttributeModifier(Attribute.valueOf(attribute), modifier);
				} catch (Exception e) {
					logError("Could not add attribute " + attribute + ", " + attributeAmount + ", " + equipmentSlot
							+ ", skipping for now..", logName);
				}
			}
		}
		return m;
	}

	void checkIdentifiers() {
		for (Recipe recipe : new ArrayList<>(getRecipeUtil().getAllRecipes().values())) {
			for (Ingredient ingredient : recipe.getIngredients()) {
				if (!ingredient.hasIdentifier())
					continue;

				// recipe name hyphen-argument is strictly for debugging, and is needed for
				// attaching a recipe during getResultFromKey()
				ItemStack matchedItem = getRecipeUtil()
						.getResultFromKey(ingredient.getIdentifier() + ":" + recipe.getName());
				if (matchedItem == null) {
					logError("Please double check the IDs of the ingredients matches that of a custom item or recipe.",
							recipe.getName());
					logError("Skipping recipe..", recipe.getName());
					getRecipeUtil().removeRecipe(recipe.getName());
					break;
				} else {
					if (ingredient.getMaterial() != matchedItem.getType()) {
						ingredient.setMaterial(matchedItem.getType());
					}
				}
			}
		}
	}

	public void addRecipes(String name) {
		File recipeFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipeFolder.exists()) {
			recipeFolder.mkdirs();
		}

		File[] recipeFiles = recipeFolder.listFiles();
		if (recipeFiles == null) {
			logError("Could not add recipes because none were found to load!", "");
			return;
		}

		if (name != null) {
			if (!name.endsWith(".yml")) {
				name = name + ".yml";
			}

			File single = new File(recipeFolder, name);
			if (single.exists() && single.isFile()) {
				recipeFiles = new File[] { single };
			} else {
				logError("Recipe file " + name + " not found!", name);
				return;
			}
		}

		recipeLoop: for (File recipeFile : recipeFiles) {
			recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
			String item = recipeFile.getName().replace(".yml", "");

			if (!(recipeConfig.isConfigurationSection(item))) {
				logError("Could not find configuration section " + item
						+ " in the recipe file that must match its filename: " + item
						+ ".yml - (CaSeSeNsItIvE) - Skipping this recipe", item);
				continue;
			}

			Recipe recipe;

			List<String> gridRows = getConfig().getStringList(item + ".ItemCrafting");
			String converter = getConfig().isString(item + ".Converter")
					? getConfig().getString(item + ".Converter").toLowerCase()
					: "converterNotDefined";
			int amountRequirement = 9;

			logDebug("Attempting to add recipe..", item);

			if (!getConfig().isConfigurationSection(item + ".Ingredients")) {
				logError("Error loading recipe..", item);
				logError("Could not locate the ingredients section. Please double check formatting. Skipping recipe..",
						item);
				continue;
			}

			RecipeType type = RecipeType.fromString(converter);
			switch (type) {
			case STONECUTTER:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleStonecutterData(recipe, item);
				amountRequirement = 1;
				break;
			case FURNACE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case BLASTFURNACE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case SMOKER:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case CAMPFIRE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 1;
				break;
			case ANVIL:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleAnvilData(recipe, item);
				amountRequirement = 2;
				break;
			case GRINDSTONE:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleGrindstoneData(recipe, item);
				amountRequirement = 2;
				break;
			case BREWING_STAND:
				recipe = new BrewingRecipeData(item);
				recipe.setType(type);
				handleBrewingData(recipe, item);
				amountRequirement = 2;
				break;
			default:
				recipe = new CraftingRecipeData(item);
				recipe.setType(getConfig().getBoolean(item + ".Shapeless") ? RecipeType.SHAPELESS : RecipeType.SHAPED);
				handleCraftingData(recipe, item);
				break;
			}

			// Checks version req. of recipe types
			if (!isVersionSupported(recipe.getType(), item))
				continue;

			// HavenBag detected, but converter is not SHAPED or SHAPELESS
			if (recipe.getType() != RecipeType.SHAPED && recipe.getType() != RecipeType.SHAPELESS) {
				if (isHavenBag(item)) {
					logError("Error loading recipe..", recipe.getName());
					logError("Got " + recipe.getType() + ", but the recipe is a havenbag recipe! Skipping..",
							recipe.getName());
					continue;
				}

				if (name == null) {
					delayedRecipes.add(item);
					continue;
				}
			}
			boolean useLegacyNames = getConfig().getBoolean(item + ".Use-Display-Name", true);
			String resultPath = getConfig().isConfigurationSection(item + ".Result") ? item + ".Result" : item;
			int amount = getConfig().isInt(resultPath + ".Amount") ? getConfig().getInt(resultPath + ".Amount") : 1;
			String identifier = getConfig().getString(item + ".Identifier");
			recipe.setKey(identifier);

			// Checks for a custom item and attempts to set it
			String rawItem = getConfig().getString(resultPath + ".Item") != null
					? getConfig().getString(resultPath + ".Item")
					: null;
			if (rawItem == null) {
				logError("Error loading recipe..", recipe.getName());
				logError("The 'Item' section is missing or improperly formatted! Skipping..", recipe.getName());
				continue;
			}

			// Attach recipe name ONLY for debug purposes
			ItemStack i = getRecipeUtil().getResultFromKey(rawItem + ":" + recipe.getName());

			// handle custom item stacks
			if (i != null)
				recipe.setCustomItem(rawItem);

			if (i == null) {
				if (getConfig().getItemStack(resultPath + ".Item") != null) {
					// handle itemstack checks
					i = getConfig().getItemStack(resultPath + ".Item");
				} else {
					// handle material checks
					Optional<ItemStack> built = buildItem(resultPath, getConfig(), useLegacyNames);
					if (!built.isPresent())
						continue;

					i = built.get();
				}
			}

			i = handleDurability(i, resultPath);
			i.setAmount(amount);

			recipe.setResult(i);
			handleRecipeFlags(item, recipe);
			handleCommand(item, recipe);

			ArrayList<String> slotsAbbreviations = new ArrayList<String>();
			String row1 = gridRows.get(0);
			String row2 = gridRows.get(1);
			String row3 = gridRows.get(2);

			slotsAbbreviations.add(row1.split("")[0]);
			slotsAbbreviations.add(row1.split("")[1]);
			slotsAbbreviations.add(row1.split("")[2]);

			slotsAbbreviations.add(row2.split("")[0]);
			slotsAbbreviations.add(row2.split("")[1]);
			slotsAbbreviations.add(row2.split("")[2]);

			slotsAbbreviations.add(row3.split("")[0]);
			slotsAbbreviations.add(row3.split("")[1]);
			slotsAbbreviations.add(row3.split("")[2]);

			recipe.setRow(1, row1);
			recipe.setRow(2, row2);
			recipe.setRow(3, row3);

			int slot = 0;
			int count = 0;

			for (String abbreviation : slotsAbbreviations) {
				// Iterate through the specified 9x9 grid and get it from the Ingredients
				// section..
				slot++;

				RecipeUtil.Ingredient recipeIngredient;

				// adds the abbreviation for AIR automatically to the recipe
				if (abbreviation.equalsIgnoreCase("X")) {
					recipeIngredient = new RecipeUtil.Ingredient("X", Material.AIR);
					recipeIngredient.setSlot(slot);
					recipe.addIngredient(recipeIngredient);
					continue;
				}

				count++;
				if (count > amountRequirement) {
					logError("Error loading recipe..", recipe.getName());
					logError(
							"Found " + count + " slots but converter is " + converter + " so use only "
									+ amountRequirement + " slot(s) (X for others) for 'ItemCrafting'",
							recipe.getName());
					continue recipeLoop;
				}

				// Try to deserialize using XItemstack (Deserializer)
				Optional<RecipeUtil.Ingredient> recipeIngredientOptional = tryDeserializeFromXItemstack(item,
						abbreviation);
				if (recipeIngredientOptional.isPresent()) {
					recipeIngredient = recipeIngredientOptional.get();
					logDebug("Loading ingredient '" + abbreviation + "' from ItemStack..", recipe.getName());
				} else {
					recipeIngredient = tryDeserializeFromConfig(recipe, item, abbreviation);
					if (recipeIngredient == null) {
						continue recipeLoop;
					}
					logDebug("Loading ingredient '" + abbreviation + "' from configuration settings..",
							recipe.getName());
				}

				recipeIngredient.setSlot(slot);
				recipe.addIngredient(recipeIngredient);

				if (count == amountRequirement)
					break;
			}

			logDebug("Recipe Type: " + recipe.getType(), recipe.getName());
			logDebug("Successfully added " + item + " with the amount output of " + i.getAmount(), recipe.getName());

			getRecipeUtil().createRecipe(recipe);
			if (delayedRecipes.isEmpty() && name != null) {
				getRecipeUtil().registerRecipe(recipe);
				return;
			}
		}

		if (!delayedRecipes.isEmpty() && name == null) {
			for (String recipe : delayedRecipes)
				addRecipes(recipe);

			delayedRecipes.clear();
		}

		if (delayedRecipes.isEmpty()) {
			getRecipeUtil().reloadRecipes();
		}
	}

	private Optional<RecipeUtil.Ingredient> tryDeserializeFromXItemstack(String recipe, String abbreviation) {
		String ingredientPath = recipe + ".Ingredients." + abbreviation;
		ConfigurationSection path = getConfig().getConfigurationSection(ingredientPath);

		if (path == null)
			return Optional.empty();

		try {
			Deserializer deserializer = new Deserializer();
			deserializer.withConfig(path);
			ItemStack item = deserializer.read();
			if (item.getType() == XMaterial.BARRIER.get())
				return Optional.empty();

			RecipeUtil.Ingredient ingredient = new RecipeUtil.Ingredient(abbreviation, item.getType());
			ingredient.setItem(item);
			return Optional.of(ingredient);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	// Method for deserializing from config
	private RecipeUtil.Ingredient tryDeserializeFromConfig(Recipe recipe, String item, String abbreviation) {
		String configPath = item + ".Ingredients." + abbreviation;
		List<String> list = getConfig().getStringList(configPath + ".Materials");
		String material = !list.isEmpty() ? list.get(0) : getConfig().getString(configPath + ".Material");

		if (material == null) {
			logError("Error loading recipe..", recipe.getName());
			logError("Could not find valid ingredient 'Material(s)' section! Skipping..", recipe.getName());
			return null;
		}

		Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);
		if (!validMaterial(recipe.getName(), material, rawMaterial)) {
			logError("Error loading recipe..", recipe.getName());
			logError("Invalid ingredient material/item found. Skipping..", recipe.getName());
			return null;
		}

		Material ingredientMaterial = rawMaterial.get().get();
		String ingredientName = getConfig().isString(configPath + ".Name") ? getConfig().getString(configPath + ".Name")
				: null;
		String ingredientIdentifier = getConfig().isString(configPath + ".Identifier")
				? getConfig().getString(configPath + ".Identifier")
				: null;
		int ingredientAmount = getConfig().isInt(configPath + ".Amount") ? getConfig().getInt(configPath + ".Amount")
				: 1;
		int ingredientCMD = getConfig().isInt(configPath + ".Custom-Model-Data")
				? getConfig().getInt(configPath + ".Custom-Model-Data")
				: -1;

		RecipeUtil.Ingredient ingredient = new RecipeUtil.Ingredient(abbreviation, ingredientMaterial);
		ingredient.setDisplayName(ingredientName);
		ingredient.setCustomModelData(ingredientCMD);
		ingredient.setIdentifier(ingredientIdentifier);
		ingredient.setAmount(ingredientAmount);

		// Set material data if available
		if (rawMaterial.get().getData() != 0) {
			logDebug("[LegacyID] Found legacy ID - " + ingredientMaterial + ", with a short value of "
					+ rawMaterial.get().getData(), recipe.getName());
			ingredient.setMaterialData(new MaterialData(ingredientMaterial, rawMaterial.get().getData()));
		}

		// Handle material choice inputs for ingredients
		handleMaterialChoice(recipe, ingredient, configPath);

		return ingredient;
	}

	boolean hasItemDamage(String item, Optional<XMaterial> type) {
		// no need for data values in item IDs anymore. These have been given their own
		// IDs since 1.13
		if (Main.getInstance().serverVersionAtLeast(1, 13))
			return false;

		if (!getConfig().isSet(item + ".Item-Damage")
				|| getConfig().getString(item + ".Item-Damage").equalsIgnoreCase("none"))
			return false;

		return true;
	}

	@SuppressWarnings("deprecation")
	ItemStack handleItemDamage(String item, Optional<XMaterial> type) {
		try {
			Short damage = Short.valueOf(getConfig().getString(item + ".Item-Damage"));
			return new ItemStack(type.get().parseMaterial(), 1, damage);
		} catch (Exception e) {
			logError(
					"Couldn't apply item damage, please double check that it is a valid item-damage. Skipping for now.",
					item);
			return new ItemStack(type.get().parseMaterial(), 1);
		}
	}

	public Optional<ItemStack> buildItem(String item, FileConfiguration path, boolean useLegacyNames) {
		Optional<XMaterial> type = path.isString(item + ".Item")
				? XMaterial.matchXMaterial(path.getString(item + ".Item").split(":")[0].toUpperCase())
				: null;

		recipeConfig = path;

		// not a valid material
		if (!(validMaterial(item, path.getString(item + ".Item"), type)))
			return Optional.empty();

		ItemStack i = hasItemDamage(item, type) ? handleItemDamage(item, type) : new ItemStack(type.get().get(), 1);
		ItemMeta m = i.getItemMeta();

		if (isHavenBag(item)) {
			if (!(hasHavenBag())) {
				logError("Error loading recipe..", item);
				logError("Found a havenbag recipe, but no havenbags plugin found. Skipping recipe..", item);
				return Optional.empty();
			}
			return Optional.of(handleBagCreation(i.getType(), 0, 0, "null", null, item));
		}

		// handle head textures
		ItemStack texture = handleHeadTexture(path.getString(item + ".Item"));
		if (texture != null) {
			i = texture;
		}

		// handle potions
		if (type.get() == XMaterial.POTION) {
			if (Main.getInstance().serverVersionLessThan(1, 9)) {
				logError("Error loading recipe..", item);
				logError("Potions are only supported on > 1.9! Skipping recipe..", item);
				return Optional.empty();
			}
			i = handlePotions(item);
		}

		i = handleEnchants(i, item);
		i = handleCustomEnchants(i, item);
		i = applyCustomTags(i, item);
		m = handleDisplayname(item, i, useLegacyNames);
		m = handleHideEnchants(item, m);
		m = handleCustomModelData(item, m);
		m = handleAttributes(item, m);
		m = handleFlags(item, m);
		m = handleLore(item, m);
		i.setItemMeta(m);

		i = handleIdentifier(i, item);

		return Optional.of(i);
	}

	public void addRecipesFromAPI(Recipe specificRecipe) {
		RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
		HashMap<String, Recipe> recipeList = recipeUtil.getAllRecipes() == null ? null
				: new HashMap<>(recipeUtil.getAllRecipes());

		if (recipeList == null || recipeList.isEmpty()) {
			logError("No recipes were found to load..", "");
			return;
		}
		if (specificRecipe != null) {
			recipeList.clear();
			recipeList.put(specificRecipe.getName(), specificRecipe);
		}

		for (Recipe recipe : recipeList.values()) {
			try {
				ShapedRecipe shapedRecipe = null;
				ShapelessRecipe shapelessRecipe = null;
				FurnaceRecipe furnaceRecipe = null;
				StonecuttingRecipe sCutterRecipe = null;
				BlastingRecipe blastRecipe = null;
				SmokingRecipe smokerRecipe = null;
				CampfireRecipe campfireRecipe = null;

				// Create recipes based on type
				switch (recipe.getType()) {
				case SHAPELESS:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						shapelessRecipe = Main.getInstance().exactChoice
								.createShapelessRecipe((CraftingRecipeData) recipe);
						break;
					}

					shapelessRecipe = createShapelessRecipe((CraftingRecipeData) recipe);
					break;

				case SHAPED:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						shapedRecipe = Main.getInstance().exactChoice.createShapedRecipe((CraftingRecipeData) recipe);
						break;
					}

					shapedRecipe = createShapedRecipe((CraftingRecipeData) recipe);
					break;

				case FURNACE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						furnaceRecipe = Main.getInstance().exactChoice.createFurnaceRecipe((CookingRecipeData) recipe);
						break;
					}

					furnaceRecipe = createFurnaceRecipe((CookingRecipeData) recipe);
					break;

				case BLASTFURNACE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						blastRecipe = Main.getInstance().exactChoice
								.createBlastFurnaceRecipe((CookingRecipeData) recipe);
					}
					break;

				case SMOKER:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						smokerRecipe = Main.getInstance().exactChoice.createSmokerRecipe((CookingRecipeData) recipe);
					}
					break;

				case STONECUTTER:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						sCutterRecipe = Main.getInstance().exactChoice
								.createStonecuttingRecipe((WorkstationRecipeData) recipe);
					}
					break;

				case CAMPFIRE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						campfireRecipe = Main.getInstance().exactChoice
								.createCampfireRecipe((CookingRecipeData) recipe);
					}
					break;

				case BREWING_STAND:
					setBrewingItems((BrewingRecipeData) recipe);
					break;
				default:
					break;
				}

				// Register recipes with the server
				if (shapedRecipe != null)
					Bukkit.getServer().addRecipe(shapedRecipe);
				if (shapelessRecipe != null)
					Bukkit.getServer().addRecipe(shapelessRecipe);
				if (furnaceRecipe != null)
					Bukkit.getServer().addRecipe(furnaceRecipe);
				if (blastRecipe != null)
					Bukkit.getServer().addRecipe(blastRecipe);
				if (sCutterRecipe != null)
					Bukkit.getServer().addRecipe(sCutterRecipe);
				if (smokerRecipe != null)
					Bukkit.getServer().addRecipe(smokerRecipe);
				if (campfireRecipe != null)
					Bukkit.getServer().addRecipe(campfireRecipe);

			} catch (Exception e) {
				Main.getInstance().getLogger().log(Level.SEVERE, "Error loading recipe: " + e.getMessage(), e);
			}
		}

		checkIdentifiers();
	}

	@SuppressWarnings("deprecation")
	private ShapelessRecipe createShapelessRecipe(CraftingRecipeData recipe) {
		ShapelessRecipe shapelessRecipe;

		if (Main.getInstance().serverVersionAtLeast(1, 12)) {
			shapelessRecipe = new ShapelessRecipe(createNamespacedKey(recipe), recipe.getResult());
		} else {
			shapelessRecipe = new ShapelessRecipe(recipe.getResult());
		}

		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			// Added material data for legacy minecraft versions
			if (ingredient.hasMaterialData()) {
				shapelessRecipe.addIngredient(ingredient.getMaterialData());
			} else {
				shapelessRecipe.addIngredient(ingredient.getMaterial());
			}
		}

		return shapelessRecipe;
	}

	@SuppressWarnings("deprecation")
	private ShapedRecipe createShapedRecipe(CraftingRecipeData recipe) {
		ShapedRecipe shapedRecipe;

		ArrayList<String> ingredients = new ArrayList<String>();

		if (Main.getInstance().serverVersionAtLeast(1, 12)) {
			shapedRecipe = new ShapedRecipe(createNamespacedKey(recipe), recipe.getResult());
		} else {
			shapedRecipe = new ShapedRecipe(recipe.getResult());
		}

		shapedRecipe.shape(recipe.getRow(1), recipe.getRow(2), recipe.getRow(3));
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty() || ingredient.getMaterial() == Material.AIR
					|| ingredients.contains(ingredient.getAbbreviation()))
				continue;

			ingredients.add(ingredient.getAbbreviation());

			// Added material data for legacy minecraft versions
			if (ingredient.hasMaterialData()) {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), ingredient.getMaterialData());
			} else {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), ingredient.getMaterial());
			}
		}

		return shapedRecipe;
	}

	@SuppressWarnings("deprecation")
	private FurnaceRecipe createFurnaceRecipe(CookingRecipeData recipe) {

		FurnaceRecipe furnaceRecipe;

		if (Main.getInstance().serverVersionAtLeast(1, 13)) {
			furnaceRecipe = new FurnaceRecipe(createNamespacedKey(recipe), recipe.getResult(),
					recipe.getSlot(1).getMaterial(), recipe.getExperience(), recipe.getCookTime());
		} else {
			furnaceRecipe = new FurnaceRecipe(recipe.getResult(), recipe.getSlot(1).getMaterial());
		}

		setFurnaceSource(recipe);
		return furnaceRecipe;
	}

	NamespacedKey createNamespacedKey(Recipe recipe) {
		return new NamespacedKey(Main.getInstance(), recipe.getKey());
	}

	private boolean validMaterial(String recipe, String materialInput, Optional<XMaterial> type) {
		if (type == null || !type.isPresent() || type.get().get() == null) {
			if (isCustomItem(materialInput))
				return false;

			logError("Error loading recipe..", recipe);
			logError("We are having trouble matching the material " + materialInput.toUpperCase()
					+ " to a minecraft item or custom item. Please double check you have inputted the correct material enum into the 'Item'"
					+ " section and try again. If this problem persists please contact Mehboss on Spigot!", recipe);
			return false;
		}
		return true;
	}

	void setBrewingItems(BrewingRecipeData recipe) {
		recipe.setBrewIngredient(recipe.getSlot(1));
		recipe.setBrewFuel(recipe.getSlot(2));
	}

	void setFurnaceSource(CookingRecipeData recipe) {
		Ingredient sourceItem = recipe.getSlot(1);
		ItemStack source = null;

		if (sourceItem.hasIdentifier()) {
			source = getRecipeUtil().getResultFromKey(sourceItem.getIdentifier());
		}

		if (source == null) {
			source = new ItemStack(sourceItem.getMaterial());

			if (sourceItem.hasDisplayName())
				source.getItemMeta().setDisplayName(sourceItem.getDisplayName());
			if (sourceItem.hasCustomModelData())
				source.getItemMeta().setCustomModelData(sourceItem.getCustomModelData());
		}

		recipe.setSource(source);
	}

	boolean isVersionSupported(RecipeType type, String item) {
		List<RecipeType> v16_Types = Arrays.asList(RecipeType.GRINDSTONE);
		List<RecipeType> legacyTypes = Arrays.asList(RecipeType.ANVIL, RecipeType.SHAPED, RecipeType.SHAPELESS,
				RecipeType.FURNACE);

		if (Main.getInstance().serverVersionLessThan(1, 14) && !legacyTypes.contains(type)
				&& !v16_Types.contains(type)) {
			logError("Error loading recipe..", item);
			logError(">= 1.14 is required for " + type.toString() + " recipes!", item);
			return false;
		}
		if (Main.getInstance().serverVersionLessThan(1, 16) && v16_Types.contains(type)) {
			logError("Error loading recipe..", item);
			logError(">= 1.16 is required for " + type.toString() + " recipes!", item);
			return false;
		}
		return true;
	}

	boolean isCustomItem(String id) {
		String[] key = id.split(":");
		if (key.length < 2)
			return false;

		String plugin = key[0];

		if (getRecipeUtil().SUPPORTED_PLUGINS.contains(plugin.toLowerCase())) {
			return true;
		}

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

	private void logError(String st, String recipe) {
		Logger.getLogger("Minecraft").log(Level.WARNING,
				"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "] " + st);
	}

	private void logDebug(String st, String recipe) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][" + recipe.replaceAll(".Result", "") + "] " + st);
	}

	private FileConfiguration getConfig() {
		return recipeConfig;
	}
}
