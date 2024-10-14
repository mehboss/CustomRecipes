package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
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

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.commands.GiveRecipe;
import me.mehboss.commands.NBTCommands;
import me.mehboss.commands.TabCompletion;
import me.mehboss.crafting.CraftManager;
import me.mehboss.crafting.RecipeManager;
import me.mehboss.gui.AddGUI;
import me.mehboss.gui.EditGUI;
import me.mehboss.gui.ManageGUI;
import me.mehboss.listeners.BlockManager;
import me.mehboss.listeners.EffectsManager;
import me.mehboss.utils.Metrics;
import me.mehboss.utils.Placeholders;
import me.mehboss.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	private RecipeManager plugin;

	public static ManageGUI recipes;
	AddGUI addItem;
	EditGUI editItem;

	public ArrayList<UUID> recipeBook = new ArrayList<UUID>();
	public ArrayList<Recipe> vanillaRecipes = new ArrayList<Recipe>();

	public HashMap<UUID, Inventory> saveInventory = new HashMap<UUID, Inventory>();

	public HashMap<String, ItemStack> itemNames = new HashMap<String, ItemStack>();
	public HashMap<ItemStack, String> configName = new HashMap<ItemStack, String>();
	public HashMap<String, ItemStack> giveRecipe = new HashMap<String, ItemStack>();
	public HashMap<String, ItemStack> identifier = new HashMap<String, ItemStack>();
	public HashMap<String, List<Material>> ingredients = new HashMap<String, List<Material>>();

	public ArrayList<ShapedRecipe> recipe = new ArrayList<ShapedRecipe>();
	public ArrayList<String> addRecipe = new ArrayList<String>();
	public ArrayList<String> disabledrecipe = new ArrayList<String>();
	// add three more shapelessname, amount, and ID specifically for config.

	File customYml = new File(getDataFolder() + "/blacklisted.yml");
	public FileConfiguration customConfig = null;

	File cursedYml = new File(getDataFolder() + "/recipes/CursedPick.yml");
	FileConfiguration cursedConfig = null;

	File swordYml = new File(getDataFolder() + "/recipes/CursedSword.yml");
	FileConfiguration swordConfig = null;

	File bagYml = new File(getDataFolder() + "/recipes/HavenBag.yml");
	FileConfiguration bagConfig = null;

	public Boolean hasAE = false;
	public Boolean hasEE = false;
	Boolean hasEEnchants = false;
	public Boolean hasHavenBags = false;

	public Boolean debug = false;
	Boolean uptodate = true;
	Boolean isFirstLoad = true;
	String newupdate = null;

	public RecipeAPI api;

	void registerCommands() {
		PluginCommand crecipeCommand = getCommand("crecipe");
		crecipeCommand.setExecutor(new GiveRecipe(this));

		if (serverVersionAtLeast(1, 15)) {
			TabCompletion tabCompleter = new TabCompletion();
			crecipeCommand.setTabCompleter(tabCompleter);
		}

		getCommand("edititem").setExecutor(new NBTCommands());
	}

	void registerUpdateChecker() {
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

	}

	void registerBstats() {
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

		if (!bagYml.exists()) {
			saveResource("recipes/HavenBag.yml", false);
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

		if (Bukkit.getPluginManager().getPlugin("EcoEnchants") != null)
			hasEE = true;

		if (Bukkit.getPluginManager().getPlugin("ExcellentEnchants") != null)
			hasEEnchants = true;

		if (Bukkit.getPluginManager().getPlugin("HavenBags") != null) {
			hasHavenBags = true;
		}

		getLogger().log(Level.INFO,
				"Made by MehBoss on Spigot. For support please PM me and I will get back to you as soon as possible!");
		getLogger().log(Level.INFO, "Loading Recipes..");

		if (getConfig().isSet("firstLoad"))
			isFirstLoad = getConfig().getBoolean("firstLoad");

		saveCustomYml(customConfig, customYml);
		saveCustomYml(cursedConfig, cursedYml);
		saveCustomYml(swordConfig, swordYml);
		saveCustomYml(bagConfig, bagYml);
		initCustomYml();

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();

		if (!customConfig.isSet("override-recipes")) {
			customConfig.set("override-recipes", new ArrayList<String>());
			saveCustomYml(customConfig, customYml);
		}

		if (isFirstLoad && getConfig().isSet("firstLoad"))
			getConfig().set("firstLoad", false);

		if (!getConfig().isSet("Messages.No-Perm-Place"))
			getConfig().set("Messages.No-Perm-Place", "&cYou cannot place an unplaceable block!");

		debug = getConfig().getBoolean("Debug");

		saveAllCustomYml();
		saveConfig();
		registerUpdateChecker();
		registerBstats();
		removeRecipes();
		plugin.addItems();

		Bukkit.getPluginManager().registerEvents(new ManageGUI(this, null), this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(new CraftManager(), this);
		Bukkit.getPluginManager().registerEvents(new BlockManager(), this);
		Bukkit.getPluginManager().registerEvents(this, this);

		recipes = new ManageGUI(this, null);
		editItem = new EditGUI(Main.getInstance(), null);
		addItem = new AddGUI(this, null);

		registerCommands();
		getLogger().log(Level.INFO, "Loaded " + giveRecipe.values().size() + " recipes.");
	}

	public static Main getInstance() {
		return instance;
	}

	public void clear() {

		reloadConfig();
		saveConfig();

		if (serverVersionAtLeast(1, 12))
			for (String getKey : identifier.keySet()) {
				if (getKey == null)
					continue;

				String key = getKey.toLowerCase();
				NamespacedKey customKey = NamespacedKey.fromString("customrecipes:" + key);

				if (customKey != null && Bukkit.getRecipe(customKey) != null)
					Bukkit.removeRecipe(customKey);

				if (debug) {
					debug("Reloading recipe: " + key);
					debug("Foundkey: " + customKey);
				}
			}

		recipeBook.clear();
		vanillaRecipes.clear();
		saveInventory.clear();
		itemNames.clear();
		configName.clear();
		giveRecipe.clear();
		identifier.clear();
		ingredients.clear();
		recipe.clear();
		addRecipe.clear();
		disabledrecipe.clear();
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

		plugin.addItems();
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
					debug("foundRecipes: " + foundRecipes);

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
		String version = Bukkit.getServer().getBukkitVersion();
		// Example version: 1.14-R0.1-SNAPSHOT
		int version_major = -1;
		int version_minor = -1;

		// Split by "-" to get the version part before the hyphen
		String[] parts = version.split("-");
		if (parts.length > 0) {
			String[] version_parts = parts[0].split("\\."); // Split by dot to separate major and minor versions
			try {
				if (version_parts.length > 0) {
					version_major = Integer.parseInt(version_parts[0]);
				}
				if (version_parts.length > 1) {
					version_minor = Integer.parseInt(version_parts[1]);
				}
			} catch (NumberFormatException e) {
				Main.getInstance().getLogger().log(Level.WARNING,
						"Error parsing version numbers from Bukkit version: " + version);
			}
		}

		return new int[] { version_major, version_minor };
	}

	public boolean serverVersionAtLeast(int majorVersion, int minorVersion) {
		int[] version_numbers = instance.getServerVersionParts();
		if (version_numbers[0] < 0 || version_numbers[1] < 0) {
			return true; // Allow plugin to continue even if version detection failed
		}
		return (version_numbers[0] > majorVersion)
				|| (version_numbers[0] == majorVersion && version_numbers[1] >= minorVersion);
	}

	void debug(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);

	}
}