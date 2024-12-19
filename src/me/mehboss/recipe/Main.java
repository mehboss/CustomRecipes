package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
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
import me.mehboss.crafting.AmountManager;
import me.mehboss.crafting.CooldownManager;
import me.mehboss.crafting.CraftManager;
import me.mehboss.crafting.RecipeManager;
import me.mehboss.gui.EditGUI;
import me.mehboss.gui.InventoryManager;
import me.mehboss.gui.RecipesGUI;
import me.mehboss.listeners.BlockManager;
import me.mehboss.listeners.EffectsManager;
import me.mehboss.utils.Metrics;
import me.mehboss.utils.Placeholders;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	public RecipeManager plugin;

	public RecipesGUI recipes;
	EditGUI editItem;

	public ArrayList<UUID> recipeBook = new ArrayList<UUID>();
	ArrayList<Recipe> vanillaRecipes = new ArrayList<Recipe>();

	public HashMap<UUID, Inventory> saveInventory = new HashMap<UUID, Inventory>();
	public HashMap<String, ItemStack> giveRecipe = new HashMap<String, ItemStack>();

	ArrayList<ShapedRecipe> recipe = new ArrayList<ShapedRecipe>();
	public ArrayList<String> addRecipe = new ArrayList<String>();
	public ArrayList<String> disabledrecipe = new ArrayList<String>();
	// add three more shapelessname, amount, and ID specifically for config.

	File customYml = new File(getDataFolder() + "/blacklisted.yml");
	public FileConfiguration customConfig = null;

	File cooldownYml = new File(getDataFolder() + "/cooldowns.yml");
	FileConfiguration cooldownConfig = null;

	File cursedYml = new File(getDataFolder() + "/recipes/CursedPick.yml");
	File swordYml = new File(getDataFolder() + "/recipes/CursedSword.yml");
	File bagYml = new File(getDataFolder() + "/recipes/HavenBag.yml");

	public Boolean hasAE = false;
	public Boolean hasEE = false;
	public Boolean hasHavenBags = false;
	Boolean hasEEnchants = false;
	Boolean uptodate = true;
	Boolean isFirstLoad = true;
	String newupdate = null;

	public CooldownManager cooldownManager;
	public RecipeUtil recipeUtil;
	public InventoryManager guiUtil;
	public Boolean debug = false;

	public RecipeUtil getRecipeUtil() {
		return recipeUtil;
	}

	private static Main instance;

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

		if (!cooldownYml.exists()) {
			saveResource("cooldowns.yml", false);
		}

		if (isFirstLoad && !cursedYml.exists()) {
			saveResource("recipes/CursedPick.yml", false);
		}

		if (isFirstLoad && !swordYml.exists()) {
			saveResource("recipes/CursedSword.yml", false);
		}

		if (isFirstLoad && !bagYml.exists()) {
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

	@Override
	public void onEnable() {

		instance = this;
		cooldownManager = new CooldownManager();
		recipeUtil = new RecipeUtil();
		plugin = new RecipeManager();
		guiUtil = new InventoryManager();

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
		saveCustomYml(cooldownConfig, cooldownYml);

		customConfig = YamlConfiguration.loadConfiguration(customYml);
		cooldownConfig = YamlConfiguration.loadConfiguration(cooldownYml);

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();

		if (!customConfig.isSet("override-recipes")) {
			customConfig.set("override-recipes", new ArrayList<String>());
			saveCustomYml(customConfig, customYml);
		}

		if (!customConfig.isSet("disable-all-vanilla")) {
			customConfig.set("disable-all-vanilla", false);
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
		addCooldowns();
		plugin.addRecipes();

		CraftManager craftManager = new CraftManager();
		Bukkit.getPluginManager().registerEvents(new EditGUI(this, null), this);
		Bukkit.getPluginManager().registerEvents(new RecipesGUI(this), this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(craftManager, this);
		Bukkit.getPluginManager().registerEvents(new AmountManager(craftManager), this);
		Bukkit.getPluginManager().registerEvents(new BlockManager(), this);
		Bukkit.getPluginManager().registerEvents(this, this);

		recipes = new RecipesGUI(this);
		editItem = new EditGUI(Main.getInstance(), null);

		registerCommands();
		getLogger().log(Level.INFO, "Loaded " + giveRecipe.values().size() + " recipes.");
	}

	public static Main getInstance() {
		return instance;
	}

	public void clear() {
		reloadConfig();
		saveConfig();

		recipeBook.clear();
		vanillaRecipes.clear();
		saveInventory.clear();
		giveRecipe.clear();
		recipe.clear();
		addRecipe.clear();
		disabledrecipe.clear();
		recipes = null;
		editItem = null;
	}

	@Override
	public void onDisable() {

		if (cooldownManager != null) {
			// Check if there are any cooldowns
			if (cooldownManager.getCooldowns().isEmpty()) {
				cooldownConfig.set("Cooldowns.", null); // Remove cooldowns if none exist
				saveCustomYml(cooldownConfig, cooldownYml);
			} else {
				// Iterate over each player's cooldowns
				for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldownManager.getCooldowns().entrySet()) {
					UUID playerUUID = playerEntry.getKey();
					Map<String, Long> playerCooldowns = playerEntry.getValue();

					// Iterate over each recipe's cooldown for the player
					for (Map.Entry<String, Long> recipeEntry : playerCooldowns.entrySet()) {
						String recipeID = recipeEntry.getKey();
						Long cooldownTime = recipeEntry.getValue();

						// Save the cooldown data under the player's UUID and recipe ID
						cooldownConfig.set("Cooldowns." + playerUUID.toString() + "." + recipeID, cooldownTime);
					}
				}

				// Save the updated cooldown data to the YML file
				saveCustomYml(cooldownConfig, cooldownYml);
			}
		}

		clear(); // Clear any additional data or cleanup
	}

	public void reload() {
		clear();
		saveCustomYml(customConfig, customYml);
		saveAllCustomYml();

		debug = getConfig().getBoolean("Debug");

		plugin.addRecipes();
		removeRecipes();

		recipes = new RecipesGUI(this);
		editItem = new EditGUI(Main.getInstance(), null);
	}

	void addCooldowns() {
		if (cooldownConfig == null)
			return;

		if (cooldownConfig.isSet("Cooldowns")) {
			// Iterate through all players in the "Cooldowns" section
			for (String playerUUIDString : cooldownConfig.getConfigurationSection("Cooldowns").getKeys(false)) {
				UUID playerUUID = UUID.fromString(playerUUIDString);

				// Iterate through each recipe ID for this player
				for (String recipeID : cooldownConfig.getConfigurationSection("Cooldowns." + playerUUIDString)
						.getKeys(false)) {
					// Get the cooldown time for the recipe
					long cooldownTime = cooldownConfig.getLong("Cooldowns." + playerUUIDString + "." + recipeID);

					if (recipe != null) {
						// Set the cooldown for the specific player and recipe
						cooldownManager.setCooldown(playerUUID, recipeID, cooldownTime);
					}
				}
			}
		}
	}

	void removeRecipes() {
		if (customConfig == null)
			return;

		if (customConfig.isSet("override-recipes"))
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

		if (customConfig.isSet("disable-all-vanilla") && customConfig.getBoolean("disable-all-vanilla") == true)
			Bukkit.clearRecipes();
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

	String getCooldownMessage(long totalSeconds) {
		long days = totalSeconds / 86400; // Calculate days
		long hours = (totalSeconds % 86400) / 3600; // Calculate hours
		long minutes = (totalSeconds % 3600) / 60; // Calculate minutes
		long seconds = totalSeconds % 60; // Calculate remaining seconds

		// Configurable message template
		String messageTemplate = ChatColor.translateAlternateColorCodes('&',
				customConfig.getString("crafting-limit.chat-message.message"));

		// Replace placeholders with actual values
		String message = ChatColor.translateAlternateColorCodes('&',
				messageTemplate.replace("%days%", String.valueOf(days)).replace("%hours%", String.valueOf(hours))
						.replace("%minutes%", String.valueOf(minutes)).replace("%seconds%", String.valueOf(seconds)));

		return message;
	}

	public void sendMessages(Player p, String s, long seconds) {

		String send = null;

		if (s.equalsIgnoreCase("none")) {
			send = "recipe-disabled-message.";
		} else if (s.equalsIgnoreCase("crafting-limit")) {
			send = "crafting-limit.";
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

			if (send.equals("crafting-limit."))
				message = getCooldownMessage(seconds);

			p.sendMessage(message);
		}

		if (customConfig.getBoolean(send + "close-inventory") == true)
			p.closeInventory();
	}

	public void sendnoPerms(Player p) {

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
			p.sendMessage(message);
		}

		if (getConfig().getBoolean("chat-message.close-inventory") == true)
			p.closeInventory();
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
		int version_minor = -1;
		int version_major = -1;

		// Split by "-" to get the version part before the hyphen
		String[] parts = version.split("-");
		if (parts.length > 0) {
			String[] version_parts = parts[0].split("\\."); // Split by dot to separate major and minor versions
			try {
				version_major = Integer.parseInt(version_parts[0]);
				version_minor = Integer.parseInt(version_parts[1]);
			} catch (NumberFormatException e) {
				Main.getInstance().getLogger().log(Level.WARNING, "Error parsing server version numbers: " + version);
			}
		}

		return new int[] { version_major, version_minor };
	}

	public boolean serverVersionAtLeast(int major, int minor) {
		int[] server_version = getServerVersionParts();

		if (server_version[0] >= major && server_version[1] >= minor) {
			return true;
		}
		return false;
	}

	public void debug(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);

	}
}