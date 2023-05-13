package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {

	Recipes recipes;
	addMenu addItem;
	editMenu editItem;

	ItemStack i = null;
	ShapedRecipe R = null;
	ShapelessRecipe S = null;

	HashMap<ItemStack, String> configName = new HashMap<ItemStack, String>();
	HashMap<String, ItemStack> giveRecipe = new HashMap<String, ItemStack>();

	ArrayList<ShapedRecipe> recipe = new ArrayList<ShapedRecipe>();
	ArrayList<String> addRecipe = new ArrayList<String>();
	ArrayList<String> disabledrecipe = new ArrayList<String>();
	// add three more shapelessname, amount, and ID specifically for config.

	ArrayList<String> identifier = new ArrayList<String>();
	ArrayList<ItemStack> menui = new ArrayList<ItemStack>();

	File customYml = new File(getDataFolder() + "/blacklisted.yml");
	FileConfiguration customConfig = null;

	File messagesYml = new File(getDataFolder() + "/messages.yml");
	FileConfiguration messagesConfig = null;

	Boolean debug = null;
	Boolean uptodate = true;
	String newupdate = null;

	RecipeAPI api;

	public void saveCustomYml(FileConfiguration ymlConfig, File ymlFile) {
		if (!customYml.exists()) {
			saveResource("blacklisted.yml", false);
		}

		if (!messagesYml.exists()) {
			saveResource("messages.yml", false);
		}

		if (ymlFile.exists() && ymlConfig != null) {
			try {
				ymlConfig.save(ymlFile);
			} catch (IOException e) {
				return;

			}
		}
	}

	public void initCustomYml() {

		messagesConfig = YamlConfiguration.loadConfiguration(messagesYml);
		customConfig = YamlConfiguration.loadConfiguration(customYml);
	}

	private static Main instance;

	@Override
	public void onEnable() {

		instance = this;
		api = new RecipeAPI();

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		reloadConfig();
		getLogger().log(Level.INFO,
				"Made by MehBoss on Spigot. For support please PM me and I will get back to you as soon as possible!");

		new UpdateChecker(this, 36925).getVersion(version -> {

			newupdate = version;

			if (getDescription().getVersion().equals(version)) {
				getLogger().log(Level.INFO, "Checking for updates..");
				getLogger().log(Level.INFO,
						"We are all up to date with the latest version. Thank you for using custom recipes :)");
			} else {
				getLogger().log(Level.INFO, "Checking for updates..");
				getLogger().log(Level.WARNING,
						"An update has been found! This could be bug fixes or additional features. Please update CustomRecipes at https://www.spigotmc.org/resources/authors/mehboss.139036/");
				uptodate = false;

			}
		});

		int pluginId = 17989;
		Metrics metrics = new Metrics(this, pluginId);
		metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", new Callable<Map<String, Integer>>() {

			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("servers", 1);
				valueMap.put("players", Bukkit.getOnlinePlayers().size());
				return valueMap;
			}
		}));

		saveCustomYml(customConfig, customYml);
		saveCustomYml(messagesConfig, messagesYml);
		initCustomYml();

		debug = messagesConfig.getBoolean("Debug");

		addItems();

		Bukkit.getPluginManager().registerEvents(new Recipes(this, null), this);
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new Effects(), this);
		recipes = new Recipes(this, null);
		editItem = new editMenu(Main.getInstance(), null);

		getCommand("crecipe").setExecutor(new GiveRecipe(this));
		getCommand("edititem").setExecutor(new Manager());
		addItem = new addMenu(this, null);

	}

	public static Main getInstance() {
		return instance;
	}

	public void clear() {

		reloadConfig();
		saveConfig();
		getServer().resetRecipes();
		disabledrecipe.clear();
		recipe.clear();
		giveRecipe.clear();
		menui.clear();
		configName.clear();
		addRecipe.clear();
		identifier.clear();
		addItem = null;
		recipes = null;

		i = null;
		R = null;
		S = null;

		debug = messagesConfig.getBoolean("Debug");

	}

	@Override
	public void onDisable() {
		clear();
	}

	public void reload() {
		clear();

		saveCustomYml(customConfig, customYml);
		saveCustomYml(messagesConfig, messagesYml);
		initCustomYml();

		addItems();

		recipes = new Recipes(this, null);
		editItem = new editMenu(Main.getInstance(), null);
		addItem = new addMenu(this, null);
	}

	public void disableRecipes() {
		for (String vanilla : customConfig.getConfigurationSection("vanilla-recipes").getKeys(false)) {
			disabledrecipe.add(vanilla);
		}

		for (String custom : customConfig.getConfigurationSection("custom-recipes").getKeys(false)) {
			disabledrecipe.add(custom);
		}
	}

	public void sendmessages(Player p, String s) {

		String send = null;

		if (s.equalsIgnoreCase("none")) {
			send = "recipe-disabled-message.";
		} else {
			send = "no-permission-message.";
		}

		if (customConfig.getString(send + "actionbar-message.enabled").equalsIgnoreCase("true")) {

			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						customConfig.getString(send + "actionbar-message.message"));
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message");
			}
		}

		if (customConfig.getString(send + "chat-message.enabled").equalsIgnoreCase("true")) {

			String message = ChatColor.translateAlternateColorCodes('&',
					customConfig.getString(send + "chat-message.message"));

			p.sendMessage(message);
		}

		if (customConfig.getString(send + "close-inventory").equalsIgnoreCase("true"))
			p.closeInventory();
	}

	public void sendmessage(Player p) {

		if (messagesConfig.getString("action-bar.enabled").equalsIgnoreCase("true")) {
			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						messagesConfig.getString("action-bar.message"));

				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message");
			}
		}

		if (messagesConfig.getString("chat-message.enabled").equalsIgnoreCase("true")) {

			String message = ChatColor.translateAlternateColorCodes('&',
					messagesConfig.getString("chat-message.message"));

			p.closeInventory();
			p.sendMessage(message);
		}
	}

	@SuppressWarnings("deprecation")
	public void addItems() {
		disableRecipes();

		for (String item : getConfig().getConfigurationSection("Items").getKeys(false)) {

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

			if (damage.equalsIgnoreCase("none")) {
				i = new ItemStack(type.get().parseMaterial(), amount);
			} else {
				i = new ItemStack(type.get().parseMaterial(), amount, Short.valueOf(damage));
			}

			identifier.add(getConfig().getString("Items." + item + ".Identifier"));

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

			configName.put(i, item);
			menui.add(i);
			giveRecipe.put(item.toLowerCase(), i);

			String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			if (!version.contains("1_7") && !version.contains("1_8") && !version.contains("1_9")
					&& !version.contains("1_10") && !version.contains("1_11")) {
				NamespacedKey key = new NamespacedKey(this, getConfig().getString("Items." + item + ".Identifier"));
				R = new ShapedRecipe(key, i);
				S = new ShapelessRecipe(key, i);
			} else {
				R = new ShapedRecipe(i);
				S = new ShapelessRecipe(i);
			}

			R.shape(line1, line2, line3);

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

			recipe.add(R);

			String[] newsplit1 = line1.split("");
			String[] newsplit2 = line2.split("");
			String[] newsplit3 = line3.split("");

			ingredients.add(api.new Ingredient(details.get(newsplit1[0]).split(":")[0], 1,
					Integer.parseInt(details.get(newsplit1[0]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit1[1]).split(":")[0], 2,
					Integer.parseInt(details.get(newsplit1[1]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit1[2]).split(":")[0], 3,
					Integer.parseInt(details.get(newsplit1[2]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit2[0]).split(":")[0], 4,
					Integer.parseInt(details.get(newsplit2[0]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit2[1]).split(":")[0], 5,
					Integer.parseInt(details.get(newsplit2[1]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit2[2]).split(":")[0], 6,
					Integer.parseInt(details.get(newsplit2[2]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit3[0]).split(":")[0], 7,
					Integer.parseInt(details.get(newsplit3[0]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit3[1]).split(":")[0], 8,
					Integer.parseInt(details.get(newsplit3[1]).split(":")[1]), false));
			ingredients.add(api.new Ingredient(details.get(newsplit3[2]).split(":")[0], 9,
					Integer.parseInt(details.get(newsplit3[2]).split(":")[1]), false));

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

						if (debug == true) {
							getLogger().log(Level.WARNING,
									"[CRECIPE DEBUG] [1] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
							getLogger().log(Level.WARNING, "SHAPELESS IS SET TO TRUE. VARIABLE: " + sl);
						}
					}
				}

				Bukkit.getServer().addRecipe(S);
			}

			api.addRecipe(item, ingredients);

			if (getConfig().getBoolean("Items." + item + ".Shapeless") == false)
				Bukkit.getServer().addRecipe(R);
		}
	}

	public static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	@EventHandler
	public void update(PlayerJoinEvent e) {
		if (messagesConfig.getBoolean("Update-Check") == true && e.getPlayer().hasPermission("crecipe.reload")
				&& !getDescription().getVersion().equals(newupdate)) {
			e.getPlayer()
					.sendMessage(ChatColor.translateAlternateColorCodes('&',
							"&cCustom-Recipes: &fAn update has been found. Please download version&c " + newupdate
									+ ", &fyou are on version&c " + getDescription().getVersion() + "!"));
		}
	}

	public Boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {
			if (debug == true) {
				getLogger().log(Level.WARNING,
						"[CRECIPE DEBUG] [2] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
				getLogger().log(Level.WARNING, "COULD NOT FIND A RECIPE FOR THIS!!!");
			}
			return false;
		}
		return true;
	}

	public boolean isBlacklisted(CraftingInventory inv, Player p) {
		if (customConfig.getBoolean("blacklist-recipes") == true) {
			for (String item : disabledrecipe) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				if (customConfig.getString("vanilla-recipes." + split[0]) != null
						&& !XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
				}

				if (XMaterial.matchXMaterial(split[0]).isPresent())
					i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				if (debug == true) {
					getLogger().log(Level.WARNING,
							"[CRECIPE DEBUG] [3] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
					getLogger().log(Level.WARNING,
							"CRECIPE DEBUG - BLACKLISTED RECIPE ARRAY SIZE " + disabledrecipe.size());
					getLogger().log(Level.WARNING, "CRECIPE DEBUG - BLACKLISTED RECIPE MATERIAL " + item);
					getLogger().log(Level.SEVERE, "ID: " + id + " BLACKLIST CHECK THIS IS WHAT IT RETURNED: "
							+ NBTEditor.getString(inv.getResult(), "CUSTOM_ITEM_IDENTIFIER"));
					getLogger().log(Level.SEVERE, "ID: " + id + " BLACKLIST CHECK THIS IS WHAT IT RETURNED: "
							+ NBTEditor.getString(inv.getResult(), id));
				}

				String getPerm = customConfig.getString("vanilla-recipes." + item + ".permission");

				if ((NBTEditor.contains(inv.getResult(), id) && !identifier.contains(id))
						|| inv.getResult().isSimilar(i)) {

					if (i == null) {
						getPerm = customConfig.getString("custom-recipes." + item + ".permission");
					}

					if (getPerm != null && !(getPerm.equalsIgnoreCase("none"))) {
						if (p.hasPermission("crecipe." + getPerm)) {
							if (debug == true) {
								getLogger().log(Level.WARNING,
										"[CRECIPE DEBUG] [3.25] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
								getLogger().log(Level.WARNING, "CRECIPE DEBUG - USER DOES HAVE PERMISSION");
							}
							break;
						}
					}

					if (debug == true) {
						getLogger().log(Level.WARNING,
								"[CRECIPE DEBUG] [3.5] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
						getLogger().log(Level.WARNING, "CRECIPE DEBUG - RECIPE SET TO AIR");

					}

					sendmessages(p, getPerm);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler
	public void check(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Boolean passedCheck = true;
		String recipeName = null;

		if (!(e.getView().getPlayer() instanceof Player)) {
			return;
		}

		Player p = (Player) e.getView().getPlayer();

		if (inv.getType() != InventoryType.WORKBENCH || !(matchedRecipe(inv)) || isBlacklisted(inv, p))
			return;

		if (configName.containsKey(inv.getResult())) {
			recipeName = configName.get(inv.getResult());
		}

		if (debug == true) {
			getLogger().log(Level.WARNING,
					"[CRECIPE DEBUG] [5] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
			getLogger().log(Level.WARNING, "CRECIPE DEBUG - 'recipeName' is set to " + recipeName);
		}

		if (recipeName == null || !(api.hasRecipe(recipeName)))
			return;

		if (getConfig().getBoolean("Items." + recipeName + ".Enabled") == false
				|| (getConfig().isString("Items." + recipeName + ".Permission")
						&& (!(p.hasPermission(getConfig().getString("Items." + recipeName + ".Permission")))))) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		List<RecipeAPI.Ingredient> recipeIngredients = api.getIngredients(recipeName);

		if (getConfig().isBoolean("Items." + configName + ".Shapeless")
				&& getConfig().getBoolean("Items." + configName + ".Shapeless") == true) {

			ArrayList<String> slotNames = new ArrayList<String>();
			ArrayList<String> recipeNames = new ArrayList<String>();

			for (int slot = 0; slot < 9; slot++) {
				if (inv.getItem(slot) == null || !(inv.getItem(slot).getItemMeta().hasDisplayName())) {
					slotNames.add("false");
					continue;
				}
				slotNames.add(inv.getItem(slot).getItemMeta().getDisplayName());
			}

			for (RecipeAPI.Ingredient names : recipeIngredients) {
				recipeNames.add(names.getDisplayName());
			}

			if (!(slotNames.containsAll(recipeNames)))
				passedCheck = false;
		} else {

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

		if (passedCheck == false)
			inv.setResult(new ItemStack(Material.AIR));

		if (debug == true) {
			getLogger().log(Level.WARNING,
					"[CRECIPE DEBUG] [10] DEBUG IS TURNED ON! PLEASE CONTACT MEHBOSS ON SPIGOT FOR ASSISTANCE");
			getLogger().log(Level.WARNING, "CRECIPE DEBUG - END CHECK. FINAL RECIPE MATCH: " + passedCheck);
			getLogger().log(Level.WARNING, "THIS IS WHAT RECIPE IT PULLED FROM -----    " + recipeName);
		}
	}
}