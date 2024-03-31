package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import net.advancedplugins.ae.api.AEAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	private RecipeManager plugin;

	static ManageGUI recipes;
	AddGUI addItem;
	EditGUI editItem;

	ArrayList<UUID> recipeBook = new ArrayList<UUID>();
	ArrayList<Recipe> vanillaRecipes = new ArrayList<Recipe>();

	HashMap<UUID, Inventory> saveInventory = new HashMap<UUID, Inventory>();
	
	HashMap<String, ItemStack> itemNames = new HashMap<String, ItemStack>();
	HashMap<ItemStack, String> configName = new HashMap<ItemStack, String>();
	HashMap<String, ItemStack> giveRecipe = new HashMap<String, ItemStack>();
	HashMap<String, ItemStack> identifier = new HashMap<String, ItemStack>();
	HashMap<String, List<Material>> ingredients = new HashMap<String, List<Material>>();

	ArrayList<ShapedRecipe> recipe = new ArrayList<ShapedRecipe>();
	ArrayList<String> addRecipe = new ArrayList<String>();
	ArrayList<String> disabledrecipe = new ArrayList<String>();
	// add three more shapelessname, amount, and ID specifically for config.

	ArrayList<ItemStack> menui = new ArrayList<ItemStack>();

	File customYml = new File(getDataFolder() + "/blacklisted.yml");
	FileConfiguration customConfig = null;

	File cursedYml = new File(getDataFolder() + "/recipes/CursedPick.yml");
	FileConfiguration cursedConfig = null;

	File swordYml = new File(getDataFolder() + "/recipes/CursedSword.yml");
	FileConfiguration swordConfig = null;

	Boolean hasAE = false;
	Boolean debug = false;
	Boolean uptodate = true;
	Boolean isFirstLoad = true;
	String newupdate = null;

	RecipeAPI api;
	
	public void copyMessagesToConfig() {
		File messagesFile = new File(getDataFolder() + "/messages.yml");
		if (messagesFile.exists()) {
			YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

			// Copy the values from messages.yml to config.yml
			getConfig().set("Debug", messagesConfig.getBoolean("Debug"));
			getConfig().set("Update-Check", messagesConfig.getBoolean("Update-Check"));
			getConfig().set("Messages", messagesConfig.getConfigurationSection("Messages"));
			getConfig().set("action-bar", messagesConfig.getConfigurationSection("action-bar"));
			getConfig().set("chat-message", messagesConfig.getConfigurationSection("chat-message"));
			getConfig().set("add", messagesConfig.getConfigurationSection("add"));
			getConfig().set("gui", messagesConfig.getConfigurationSection("gui"));

			// Set the firstLoad value to false
			getConfig().set("firstLoad", false);
			saveConfig();

			getLogger().log(Level.INFO, "Successfully copied messages.yml to config.yml.");

			// Delete the messages.yml file
			messagesFile.delete();
		}
	}

	void transferRecipesFromConfig() {
		// Get the configuration section containing the recipes in the config.yml
		copyMessagesToConfig();

		ConfigurationSection recipeSection = getConfig().getConfigurationSection("Items");

		// Check if the section exists and contains any recipes
		if (recipeSection == null || recipeSection.getKeys(false).isEmpty()) {
			return;
		}

		// Get the directory for the "recipes" folder
		File recipesFolder = new File(getDataFolder(), "recipes");

		// Check if the folder exists, create it if necessary
		if (!recipesFolder.exists() || !recipesFolder.isDirectory()) {
			recipesFolder.mkdirs();
		}

		// Iterate over each recipe in the config.yml
		for (String recipeKey : recipeSection.getKeys(false)) {
			// Get the recipe configuration from the config.yml
			ConfigurationSection recipeConfig = recipeSection.getConfigurationSection(recipeKey);

			if (recipeConfig.isList("Ingredients"))
				convertRecipeFormat(recipeConfig);

			// Create a new file for the recipe in the "recipes" folder
			File recipeFile = new File(recipesFolder, recipeKey + ".yml");

			// Create a new YamlConfiguration for the recipe
			YamlConfiguration ymlConfig = new YamlConfiguration();

			// Set the recipe values in the YamlConfiguration
			ymlConfig.set(recipeKey + ".Enabled", recipeConfig.getBoolean("Enabled"));
			ymlConfig.set(recipeKey + ".Shapeless", recipeConfig.getBoolean("Shapeless"));
			ymlConfig.set(recipeKey + ".Item", recipeConfig.getString("Item"));
			ymlConfig.set(recipeKey + ".Item-Damage", recipeConfig.getString("Item-Damage"));
			ymlConfig.set(recipeKey + ".Amount", recipeConfig.getInt("Amount"));
			ymlConfig.set(recipeKey + ".Ignore-Data", recipeConfig.getBoolean("Ignore-Data"));
			ymlConfig.set(recipeKey + ".Custom-Tagged", recipeConfig.getBoolean("Custom-Tagged"));
			ymlConfig.set(recipeKey + ".Identifier", recipeConfig.getString("Identifier"));
			ymlConfig.set(recipeKey + ".Permission", recipeConfig.getString("Permission"));
			ymlConfig.set(recipeKey + ".Name", recipeConfig.getString("Name"));
			ymlConfig.set(recipeKey + ".Lore", recipeConfig.getStringList("Lore"));
			ymlConfig.set(recipeKey + ".Effects", recipeConfig.getStringList("Effects"));
			ymlConfig.set(recipeKey + ".Hide-Enchants", recipeConfig.getBoolean("Hide-Enchants"));
			ymlConfig.set(recipeKey + ".Enchantments", recipeConfig.getStringList("Enchantments"));
			ymlConfig.set(recipeKey + ".ItemCrafting", recipeConfig.getStringList("ItemCrafting"));
			ymlConfig.set(recipeKey + ".Attribute", recipeConfig.getStringList("Attribute"));
			ymlConfig.set(recipeKey + ".Custom-Model-Data", recipeConfig.getString("Custom-Model-Data"));

			// Get the ingredients configuration section
			ConfigurationSection ingredientsSection = recipeConfig.getConfigurationSection("Ingredients");
			if (ingredientsSection != null) {
				// Set the ingredients values in the YamlConfiguration
				ymlConfig.set(recipeKey + ".Ingredients", ingredientsSection);
			}

			// Save the recipe configuration to the file
			try {
				ymlConfig.save(recipeFile);
			} catch (IOException e) {
				// Handle any errors that occur during saving
				e.printStackTrace();
			}

			// Remove the transferred recipe from the config.yml
			recipeSection.set(recipeKey, null);
		}

		// Save the updated config.ym
		saveConfig();
		getLogger().log(Level.INFO, "Successfully transfered recipes into their own files.");
	}

	void convertRecipeFormat(ConfigurationSection recipeConfig) {
		// Check if the recipe is in the old format
		if (recipeConfig.contains("Ingredients")) {
			// Get the old ingredients list
			final List<String> oldIngredients = recipeConfig.getStringList("Ingredients");

			// Remove the old "Ingredients" key from the recipe configuration
			recipeConfig.set("Ingredients", null);

			// Create a new "Ingredients" configuration section
			ConfigurationSection newIngredients = recipeConfig.createSection("Ingredients");

			// Convert each old ingredient to the new format
			for (String lists : oldIngredients) {
				String[] parts = lists.split(":");
				if (parts.length >= 2) {
					String ingredientKey = parts[0];
					String material = parts[1];
					int amount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
					String name = parts.length > 3 ? parts[3] : null;

					ConfigurationSection ingredient = newIngredients.createSection(ingredientKey);
					ingredient.set("Material", material);
					ingredient.set("Identifier", "none");
					ingredient.set("Amount", amount);
					ingredient.set("Name", name);
				}
			}
			saveConfig();
		}
	}

	public void saveCustomYml(FileConfiguration ymlConfig, File ymlFile) {
		if (!customYml.exists()) {
			saveResource("blacklisted.yml", false);
		}

		if (isFirstLoad && !cursedYml.exists()) {
			saveResource("recipes/CursedPick.yml", false);
		}

		if (isFirstLoad && !swordYml.exists()) {
			saveResource("recipes/CursedSword.yml", false);
		}

		if (ymlFile.exists() && ymlConfig != null) {
			try {
				ymlConfig.save(ymlFile);
			} catch (IOException e) {
				return;

			}
		}
	}

	public void saveAllCustomYml() {
		// Get the directory for the "recipes" folder
		File recipesFolder = new File(getDataFolder(), "recipes");

		// Check if the folder exists
		if (!recipesFolder.exists() || !recipesFolder.isDirectory()) {
			return;
		}

		// Get all files in the "recipes" folder
		File[] recipeFiles = recipesFolder.listFiles();

		// Iterate over each file
		for (File recipeFile : recipeFiles) {
			// Check if it's a file (not a directory)
			if (recipeFile.isFile()) {
				// Get the file name and extension
				String fileName = recipeFile.getName();
				String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

				if (fileExtension.equalsIgnoreCase("yml")) {
					// Load the file configuration
					FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

					// Save the file
					saveCustomYml(recipeConfig, recipeFile);
				}
			}
		}
	}

	public void initCustomYml() {
		customConfig = YamlConfiguration.loadConfiguration(customYml);
	}

	private static Main instance;

	@Override
	public void onEnable() {

		instance = this;
		api = new RecipeAPI();
		plugin = new RecipeManager();

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
			new Placeholders().register();
		
		if (Bukkit.getPluginManager().getPlugin("AdvancedEnchantments") != null)
			hasAE = true;
		
		getLogger().log(Level.INFO,
				"Made by MehBoss on Spigot. For support please PM me and I will get back to you as soon as possible!");

		new UpdateChecker(this, 36925).getVersion(version -> {

			newupdate = version;

			if (getDescription().getVersion().compareTo(version) >= 0) {
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

		getLogger().log(Level.INFO, "Loading Recipes..");

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

		if (getConfig().isSet("firstLoad"))
			isFirstLoad = getConfig().getBoolean("firstLoad");

		saveCustomYml(customConfig, customYml);
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		saveCustomYml(cursedConfig, cursedYml);
		initCustomYml();

		if (!customConfig.isSet("override-recipes")) {
			customConfig.set("override-recipes", new ArrayList<String>());
			saveCustomYml(customConfig, customYml);
		}

		saveAllCustomYml();
		transferRecipesFromConfig();

		if (isFirstLoad && getConfig().isSet("firstLoad"))
			getConfig().set("firstLoad", false);

		if (!getConfig().isSet("Messages.No-Perm-Place"))
			getConfig().set("Messages.No-Perm-Place", "&cYou cannot place an unplaceable block!");

		saveConfig();

		debug = getConfig().getBoolean("Debug");

		removeRecipes();

		for (Iterator<Recipe> iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
			Recipe type = iterator.next();
			vanillaRecipes.add(type);
		}

		Bukkit.clearRecipes();
		plugin.addItems();

		for (Recipe vanilla : vanillaRecipes)
			Bukkit.addRecipe(vanilla);

		Bukkit.getPluginManager().registerEvents(new ManageGUI(this, null), this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(new CraftManager(), this);
		Bukkit.getPluginManager().registerEvents(new BlockManager(), this);
		Bukkit.getPluginManager().registerEvents(this, this);

		recipes = new ManageGUI(this, null);
		editItem = new EditGUI(Main.getInstance(), null);

		PluginCommand crecipeCommand = getCommand("crecipe");
		crecipeCommand.setExecutor(new GiveRecipe(this));

		if (Main.serverVersionAtLeast(1, 15)) {
			TabCompletion tabCompleter = new TabCompletion();
			crecipeCommand.setTabCompleter(tabCompleter);
		}

		getCommand("edititem").setExecutor(new NBTCommands());
		addItem = new AddGUI(this, null);

		getLogger().log(Level.INFO, "Loaded " + giveRecipe.values().size() + " recipes.");
	}

	public static Main getInstance() {
		return instance;
	}

	public void clear() {

		reloadConfig();
		saveConfig();

		disabledrecipe.clear();
		recipe.clear();
		giveRecipe.clear();
		menui.clear();
		configName.clear();
		addRecipe.clear();
		identifier.clear();
		addItem = null;
		recipes = null;
	}

	@Override
	public void onDisable() {
		clear();
	}

	public void reload() {
		clear();

		initCustomYml();
		saveCustomYml(customConfig, customYml);

		saveAllCustomYml();

		debug = getConfig().getBoolean("Debug");

		Bukkit.clearRecipes();
		plugin.addItems();

		for (Recipe vanilla : vanillaRecipes)
			Bukkit.addRecipe(vanilla);

		removeRecipes();

		recipes = new ManageGUI(this, null);
		editItem = new EditGUI(Main.getInstance(), null);
		addItem = new AddGUI(this, null);
	}

	void removeRecipes() {
		if (customConfig == null || !customConfig.isSet("override-recipes"))
			return;

		for (String recipe : customConfig.getStringList("override-recipes")) {

			if (!(XMaterial.matchXMaterial(recipe).isPresent()))
				continue;

			for (Recipe foundRecipes : Bukkit.getRecipesFor(XMaterial.matchXMaterial(recipe).get().parseItem())) {
				Keyed rKey = (Keyed) foundRecipes;

				if (!(foundRecipes instanceof ShapedRecipe) && !(foundRecipes instanceof ShapelessRecipe))
					continue;

				if (debug)
					getLogger().log(Level.SEVERE, "foundRecipes: " + foundRecipes);

				try {
					Bukkit.removeRecipe(NamespacedKey.minecraft(rKey.getKey().getKey()));
				} catch (Exception e) {
					getLogger().log(Level.SEVERE,
							"Could not find NameSpacedKey for " + NamespacedKey.minecraft(rKey.getKey().getKey())
									+ ", therefore we can not remove this recipe.");
				}
			}
		}
	}

	public void disableRecipes() {
		if (customConfig == null)
			return;

		for (String vanilla : customConfig.getConfigurationSection("vanilla-recipes").getKeys(false)) {
			disabledrecipe.add(vanilla);
		}

		for (String custom : customConfig.getConfigurationSection("custom-recipes").getKeys(false)) {
			disabledrecipe.add(custom);
		}
	}

	public void sendMessages(Player p, String s) {

		String send = null;

		if (s.equalsIgnoreCase("none")) {
			send = "recipe-disabled-message.";
		} else {
			send = "no-permission-message.";
		}

		if (customConfig.getBoolean(send + "actionbar-message.enabled") == true) {

			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						customConfig.getString(send + "actionbar-message.message"));
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message");
			}
		}

		if (customConfig.getBoolean(send + "chat-message.enabled") == true) {

			String message = ChatColor.translateAlternateColorCodes('&',
					customConfig.getString(send + "chat-message.message"));

			p.sendMessage(message);
		}

		if (customConfig.getBoolean(send + "close-inventory") == true)
			p.closeInventory();
	}

	public void sendMessage(Player p) {

		if (getConfig().getBoolean("action-bar.enabled") == true) {
			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						getConfig().getString("action-bar.message"));

				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message");
			}
		}

		if (getConfig().getBoolean("chat-message.enabled") == true) {

			String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString("chat-message.message"));

			if (getConfig().getBoolean("chat-message.close-inventory") == true)
				p.closeInventory();

			p.sendMessage(message);
		}
	}

	@EventHandler
	public void update(PlayerJoinEvent e) {
		if (getConfig().getBoolean("Update-Check") == true && e.getPlayer().hasPermission("crecipe.reload")
				&& getDescription().getVersion().compareTo(newupdate) < 0) {
			e.getPlayer()
					.sendMessage(ChatColor.translateAlternateColorCodes('&',
							"&cCustom-Recipes: &fAn update has been found. Please download version&c " + newupdate
									+ ", &fyou are on version&c " + getDescription().getVersion() + "!"));
		}
	}

	private int[] getServerVersionParts() {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		// ^ such as v1_20_R1
		// which is [3] in dot-delimited "org.bukkit.craftbukkit.v1_20_R1"
		String[] version_parts = version.split("_");
		int version_major = -1;
		int version_minor = -1;
		try {
			if (version_parts[0].startsWith("v")) {
				// This is expected--remove "v" before the version number:
				version_parts[0] = version_parts[0].substring(1);
			}
			version_major = Integer.parseInt(version_parts[0]);
		}
		catch (NumberFormatException e) {
			// leave it at -1. It is not a number.
			Main.getInstance().getLogger().log(Level.WARNING, "The major version number could not be detected in the first number before '_' in \""+version+"\". You may get API errors later.");
		}
		if (version_parts.length > 1) {
			try {
				version_major = Integer.parseInt(version_parts[1]);
			}
			catch (NumberFormatException e) {
				// leave it at -1. It is not a number.
				Main.getInstance().getLogger().log(Level.WARNING, "The minor version number could not be detected in the first number after '_' in \""+version+"\". You may get API errors later.");
			}
		}
		return new int[] {version_major, version_minor};
	}

	public static boolean serverVersionBelow(int majorVersion, int minorVersion) {
		int[] version_numbers = instance.getServerVersionParts();
		if (version_numbers[0] < 0 || version_numbers[1] < 0) {
			// Allow this plugin to run new API calls even if version can't be detected
			// (A warning should already have been shown by getServerVersionParts):
			return false;
		}
		if (version_numbers[0] < majorVersion) {
			return true;
		}
		if (version_numbers[1] < minorVersion) {
			return true;
		}
		return false;
	}

	public static boolean serverVersionAtLeast(int majorVersion, int minorVersion) {
		int[] version_numbers = instance.getServerVersionParts();
		if (version_numbers[0] < 0 || version_numbers[1] < 0) {
			// Allow this plugin to run new API calls even if version can't be detected
			// (A warning should already have been shown by getServerVersionParts):
			return true;
		}
		return (version_numbers[0] >= majorVersion)
			   && (version_numbers[1] >= minorVersion);
	}
}