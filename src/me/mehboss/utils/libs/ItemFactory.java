package me.mehboss.utils.libs;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.cryptomorin.xseries.XAttribute;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.XItemStack.UnknownMaterialCondition;
import me.mehboss.utils.libs.XItemStack.UnAcceptableMaterialCondition;
import net.advancedplugins.ae.api.AEAPI;
import valorless.havenbags.api.HavenBagsAPI;
import valorless.havenbags.datamodels.Data;

public class ItemFactory {

	FileConfiguration file;

	FileConfiguration getConfig() {
		return file;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
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

	public boolean isHavenBag(String item) {
		if (getConfig().isSet(item + ".Identifier")
				&& getConfig().getString(item + ".Identifier").split("-")[0].contains("havenbags"))
			return true;

		return false;
	}

	boolean hasHavenBag() {
		if (Main.getInstance().hasHavenBags)
			return true;
		return false;
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

	private boolean validMaterial(String recipe, String materialInput, Optional<XMaterial> type) {
		String name = recipe.replaceAll(".Result", "");
		if (type == null || !type.isPresent() || type.get().get() == null) {
			if (type == null) {
				logError("Error loading recipe..", name);
				logError("Could not find a material for the result!", name);
				return false;
			}

			if (isCustomItem(materialInput))
				return false;

			logError("Error loading recipe..", name);
			logError(
					"Invalid material '" + materialInput.toUpperCase()
							+ "'. Please double check that the material is valid before reaching out for support.",
					name);
			return false;
		}
		return true;
	}

	public Optional<ItemStack> buildItem(String item, FileConfiguration path) {
		Optional<ItemStack> result = deserializeItemFromPath(path, item);
		file = path;

		if (result.isPresent()) {
			logDebug("Loading result from ItemStack..", item);
			return Optional.of(result.get());
		}

		logDebug("Loading result from configuration settings..", item);
		Optional<XMaterial> type = path.isString(item + ".Item")
				? XMaterial.matchXMaterial(path.getString(item + ".Item").split(":")[0].toUpperCase())
				: null;

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
		m = handleDisplayname(item, i);
		m = handleHideEnchants(item, m);
		m = handleCustomModelData(item, m);
		m = handleItemModel(item, m);
		m = handleAttributes(item, m);
		m = handleFlags(item, m);
		m = handleLore(item, m);
		i.setItemMeta(m);

		return Optional.of(i);
	}

	public Optional<ItemStack> deserializeItemFromPath(FileConfiguration file, String path) {
		ConfigurationSection section = file.getConfigurationSection(path);
		if (section == null) {
			return Optional.empty();
		}

		try {
			XItemStack.Deserializer deserializer = XItemStack.deserializer();
			deserializer.fromConfig(section);
			ItemStack item = deserializer.deserialize();

			// Optional: treat AIR or BARRIER as "no item"
			if (item == null || item.getType() == Material.AIR || item.getType() == Material.BARRIER) {
				return Optional.empty();
			}

			return Optional.of(item);
		} catch (UnAcceptableMaterialCondition | UnknownMaterialCondition e) {
			String material = file.getString(path + ".material");
			validMaterial(path.split(",")[0], material, XMaterial.matchXMaterial(material));
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	// Method for deserializing from config
	public RecipeUtil.Ingredient deserializeItemFromConfig(FileConfiguration file, Recipe recipe, String item,
			String abbreviation) {
		String configPath = item + ".Ingredients." + abbreviation;
		List<String> list = file.getStringList(configPath + ".Materials");
		String material = !list.isEmpty() ? list.get(0) : file.getString(configPath + ".Material");

		if (material == null) {
			logError("Error loading recipe..", recipe.getName());
			logError("Could not deserialize recipe! Skipping..", recipe.getName());
			return null;
		}

		Optional<XMaterial> rawMaterial = XMaterial.matchXMaterial(material);
		if (!validMaterial(recipe.getName(), material, rawMaterial)) {
			logError("Error loading recipe..", recipe.getName());
			logError("Invalid ingredient material/item found. Skipping..", recipe.getName());
			return null;
		}

		Material ingredientMaterial = rawMaterial.get().get();

		ArrayList<String> lore = new ArrayList<String>();
		for (String line : file.getStringList(configPath + ".Lore")) {
			lore.add(ChatColor.translateAlternateColorCodes('&', line));
		}

		String displayName = Optional.ofNullable(file.getString(configPath + ".Name"))
				.filter(s -> !s.isEmpty() && !s.equals("none")).orElse("false");
		String itemName = file.getString(configPath + ".Item-Name", "false");
		String ingredientIdentifier = file.getString(configPath + ".Identifier");
		int ingredientAmount = file.getInt(configPath + ".Amount", 1);
		int ingredientCMD = file.getInt(configPath + ".Custom-Model-Data", -1);
		String ingredientIM = file.getString(configPath + ".Item-Model");

		RecipeUtil.Ingredient ingredient = new RecipeUtil.Ingredient(abbreviation, ingredientMaterial);
		ingredient.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
		ingredient.setItemName(ChatColor.translateAlternateColorCodes('&', itemName));
		ingredient.setCustomModelData(ingredientCMD);
		ingredient.setItemModel(ingredientIM);
		ingredient.setIdentifier(ingredientIdentifier);
		ingredient.setAmount(ingredientAmount);
		ingredient.setLore(lore);

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

	boolean hasItemDamage(String item, Optional<XMaterial> type) {
		if (Main.getInstance().serverVersionAtLeast(1, 13))
			return false;

		if (!getConfig().isSet(item + ".Item-Damage")
				|| getConfig().getString(item + ".Item-Damage").equalsIgnoreCase("none"))
			return false;

		return true;
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

	ItemMeta handleDisplayname(String item, ItemStack recipe) {
		ItemMeta itemMeta = recipe.getItemMeta();

		String name = getConfig().getString(item + ".Name", "none");
		if (!name.equalsIgnoreCase("none")) {
			logDebug("Applying display-name..", item);
			logDebug("Name: " + name, item);
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		}

		if (!CompatibilityUtil.supportsItemName())
			return itemMeta;

		String itemName = getConfig().getString(item + ".Item-Name", "none");
		if (!itemName.equalsIgnoreCase("none")) {
			logDebug("Applying item-name..", item);
			logDebug("Name: " + name, item);
			itemMeta.setItemName(ChatColor.translateAlternateColorCodes('&', itemName));
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

	ItemMeta handleItemModel(String item, ItemMeta m) {
		if (!CompatibilityUtil.supportsItemModel())
			return m;

		if (getConfig().isSet(item + ".Item-Model")) {
			String model = getConfig().getString(item + ".Item-Model");
			if (model == null || model.isEmpty() || model.equalsIgnoreCase("none"))
				return m;
			try {
				m.setItemModel(NamespacedKey.fromString(model));
			} catch (Exception e) {
				logError("Error occured while setting item model..", item);
			}
		}
		return m;
	}

	ItemMeta handleAttributes(String item, ItemMeta m) {
		String logName = item.replaceAll(".Result", "");
		if (getConfig().isSet(item + ".Attribute")) {
			for (String split : getConfig().getStringList(item + ".Attribute")) {
				if (!(Main.getInstance().serverVersionAtLeast(1, 14))) {
					logError(
							"Failed to apply attributes due to unsupported version. You must use NBT to apply the attributes. Skipping for now..",
							logName);
					return m;
				}

				String[] st = split.split(":");
				String rawAt = XAttribute.of(st[0]).isPresent() ? XAttribute.of(st[0]).get().name() : null;
				double attributeAmount = Double.valueOf(st[1]);
				String equipmentSlot = null;
				Operation operation = AttributeModifier.Operation.ADD_NUMBER;

				if (st.length == 3) {
					equipmentSlot = st[2];
				} else if (st.length > 3) {
					equipmentSlot = st[3];
					operation = AttributeModifier.Operation.valueOf(st[2]);
				}

				if (rawAt == null || operation == null) {
					logError("Could not find attribute " + st[0] + ".. skipping attribute application.", logName);
					continue;
				}

				try {
					AttributeModifier modifier = XAttribute.createModifier(rawAt, attributeAmount, operation,
							EquipmentSlot.valueOf(equipmentSlot));
					Optional<XAttribute> attribute = XAttribute.of(rawAt);
					if (modifier == null || !attribute.isPresent())
						continue;

					XAttribute.createModifier(rawAt, attributeAmount, operation, EquipmentSlot.valueOf(equipmentSlot));
					m.addAttributeModifier(attribute.get().get(), modifier);
				} catch (Exception e) {
					logError("Could not add attribute " + rawAt + ", " + attributeAmount + ", " + equipmentSlot
							+ ", skipping for now..", logName);
					e.printStackTrace();
				}
			}
		}
		return m;
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
	List<Map<String, Object>> getCustomTags(String recipe) {
		List<Map<?, ?>> rawList = getConfig().getMapList(recipe + ".Custom-Tags");
		List<Map<String, Object>> castedList = new ArrayList<>();

		for (Map<?, ?> rawMap : rawList) {
			castedList.add((Map<String, Object>) (Map<?, ?>) rawMap);
		}

		return castedList;
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
				if (path.get(0).equalsIgnoreCase("SpawnerID")) {
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
							NBT.modify(item, nbt -> {
								// Get or create the EntityTag compound
								ReadWriteNBT entityTag = nbt.getOrCreateCompound("EntityTag");

								// Invisible (boolean)
								if (compound.containsKey("Invisible")) {
									boolean invisible = Boolean.parseBoolean(String.valueOf(compound.get("Invisible")));
									entityTag.setBoolean("Invisible", invisible);
								}

								// Glowing (boolean)
								if (compound.containsKey("Glowing")) {
									boolean glowing = Boolean.parseBoolean(String.valueOf(compound.get("Glowing")));
									entityTag.setBoolean("Glowing", glowing);
								}

								// Fixed (boolean)
								if (compound.containsKey("Fixed")) {
									boolean fixed = Boolean.parseBoolean(String.valueOf(compound.get("Fixed")));
									entityTag.setBoolean("Fixed", fixed);
								}

								// Facing (byte)
								if (compound.containsKey("Facing")) {
									byte facing = ((Number) compound.get("Facing")).byteValue();
									entityTag.setByte("Facing", facing);
								}
							});
						}
					} catch (Throwable t) {
						logError("Failed to apply EntityTag to item frame with NBTEditor.", recipe);
						t.printStackTrace();
						return item;
					}
				}
			}
			return item;
		} catch (Exception e) {
			logError("Could not apply custom tags! This generally happens when the operations are incorrect.", recipe);
			e.printStackTrace();
			return item;
		}
	}

	@SuppressWarnings("deprecation")
	public ItemStack handlePotions(String configPath) {
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
		return bagItem;
	}
}
