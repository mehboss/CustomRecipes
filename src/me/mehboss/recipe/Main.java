package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import me.mehboss.anvil.AnvilManager;
import me.mehboss.anvil.AnvilManager_1_8;
import me.mehboss.anvil.GrindstoneManager;
import me.mehboss.anvil.SmithingManager;
import me.mehboss.brewing.BrewEvent;
import me.mehboss.commands.CommandRecipes;
import me.mehboss.commands.TabCompletion;
import me.mehboss.cooking.CookingManager;
import me.mehboss.crafting.AmountManager;
import me.mehboss.crafting.CraftManager;
import me.mehboss.crafting.CrafterManager;
import me.mehboss.crafting.ShapedChecks;
import me.mehboss.crafting.ShapelessChecks;
import me.mehboss.gui.RecipeTypeGUI;
import me.mehboss.gui.framework.GuiListener;
import me.mehboss.gui.framework.RecipeGUI;
import me.mehboss.gui.BookGUI;
import me.mehboss.listeners.AutoDiscover;
import me.mehboss.listeners.BlockManager;
import me.mehboss.listeners.EffectsManager;
import me.mehboss.utils.Metrics;
import me.mehboss.utils.Placeholders;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.UpdateChecker;
import me.mehboss.utils.libs.CooldownManager;
import me.mehboss.utils.libs.ItemManager;
import me.mehboss.utils.libs.ItemFactory;
import me.mehboss.utils.libs.MetaChecks;
import me.mehboss.utils.libs.CooldownManager.Cooldown;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {

	public RecipeUtil recipeUtil;
	public RecipeBuilder recipeBuilder;
	public ExactChoice exactChoice;

	public AmountManager amountManager;
	public CraftManager craftManager;

	public ShapedChecks shapedChecks;
	public ShapelessChecks shapelessChecks;

	public ItemFactory itemFactory;
	public MetaChecks metaChecks;

	public BookGUI recipes;
	public RecipeTypeGUI typeGUI;
	public CooldownManager cooldownManager;

	private AutoDiscover autoDiscover;

	public AutoDiscover getAutoDiscover() {
		return autoDiscover;
	}

	private Blacklist handleBlacklist;

	public Blacklist getBlacklistHandler() {
		return handleBlacklist;
	}

	public RecipeGUI editItem;

	public Map<UUID, Long> debounceMap = new HashMap<>();
	public ArrayList<UUID> inInventory = new ArrayList<UUID>();
	public ArrayList<UUID> recipeBook = new ArrayList<UUID>();
	public ArrayList<String> disabledrecipe = new ArrayList<String>();

	// add three more shapelessname, amount, and ID specifically for config.

	public FileConfiguration customConfig = null;
	File customYml = new File(getDataFolder() + "/blacklisted.yml");

	FileConfiguration cooldownConfig = null;
	File cooldownYml = new File(getDataFolder() + "/cooldowns.yml");

	File cursedYml = new File(getDataFolder() + "/recipes/CursedPick.yml");
	File swordYml = new File(getDataFolder() + "/recipes/CursedSword.yml");
	File luckyYml = new File(getDataFolder() + "/items/LuckyPickaxe.yml");
	File bagYml = new File(getDataFolder() + "/recipes/HavenBag.yml");
	File sandYml = new File(getDataFolder() + "/recipes/WheatSand.yml");

	public Boolean debug = false;
	public Boolean crafterdebug = false;

	public Boolean hasAE = false;
	public Boolean hasEE = false;
	public Boolean hasHavenBags = false;
	public Boolean hasEEnchants = false;

	Boolean uptodate = true;
	Boolean isFirstLoad = true;
	String newupdate = null;

	private static Main instance;

	public static Main getInstance() {
		return instance;
	}

	public RecipeUtil getRecipeUtil() {
		return recipeUtil;
	}

	public boolean hasCustomPlugin(String plugin) {
		switch (plugin) {
		case "itemsadder":
			return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
		case "mythicmobs":
			return Bukkit.getPluginManager().getPlugin("MythicMobs") != null && serverVersionAtLeast(1, 16);
		case "executableitems":
			return Bukkit.getPluginManager().getPlugin("ExecutableItems") != null;
		case "nexo":
			return Bukkit.getPluginManager().getPlugin("Nexo") != null;
		case "oraxen":
			return Bukkit.getPluginManager().getPlugin("Oraxen") != null;
		case "mmoitems":
			return Bukkit.getPluginManager().getPlugin("MMOItems") != null;
		}
		return false;
	}

	void registerCommands() {
		PluginCommand crecipeCommand = getCommand("crecipe");
		crecipeCommand.setExecutor(new CommandListener());

		if (getConfig().getBoolean("Recipes-Alias", true)) {
			PluginCommand recipesCommand = getCommand("recipes");
			recipesCommand.setExecutor(new CommandRecipes());
		}

		if (serverVersionAtLeast(1, 15)) {
			TabCompletion tabCompleter = new TabCompletion();
			crecipeCommand.setTabCompleter(tabCompleter);
		}
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

		if (isFirstLoad) {
			if (!cursedYml.exists()) {
				saveResource("recipes/CursedPick.yml", false);
			}
			if (!swordYml.exists()) {
				saveResource("recipes/CursedSword.yml", false);
			}
			if (!bagYml.exists()) {
				saveResource("recipes/HavenBag.yml", false);
			}
			if (!sandYml.exists()) {
				saveResource("recipes/WheatSand.yml", false);
			}
			if (!luckyYml.exists()) {
				saveResource("items/LuckyPickaxe.yml", false);
			}
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
		File recipesFolder = new File(getDataFolder(), "recipes");
		if (!recipesFolder.exists() || !recipesFolder.isDirectory()) {
			return;
		}

		// Get all files in the "recipes" folder
		File[] recipeFiles = recipesFolder.listFiles();
		for (File recipeFile : recipeFiles) {
			if (recipeFile.isFile()) {
				String fileName = recipeFile.getName();
				String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

				if (fileExtension.equalsIgnoreCase("yml")) {
					FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
					saveCustomYml(recipeConfig, recipeFile);
				}
			}
		}
	}

	@Override
	public void onEnable() {

		instance = this;
		autoDiscover = new AutoDiscover();
		handleBlacklist = new Blacklist();
		itemFactory = new ItemFactory();
		cooldownManager = new CooldownManager();
		recipeUtil = new RecipeUtil();
		recipeBuilder = new RecipeBuilder();
		metaChecks = new MetaChecks();
		shapedChecks = new ShapedChecks();
		shapelessChecks = new ShapelessChecks();
		craftManager = new CraftManager();
		amountManager = new AmountManager(craftManager);

		if (serverVersionAtLeast(1, 13, 2))
			exactChoice = new ExactChoice();

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

		isFirstLoad = getConfig().getBoolean("firstLoad");
		debug = getConfig().getBoolean("Debug");
		crafterdebug = getConfig().getBoolean("Crafter-Debug");

		saveCustomYml(customConfig, customYml);
		saveCustomYml(cooldownConfig, cooldownYml);

		customConfig = YamlConfiguration.loadConfiguration(customYml);
		cooldownConfig = YamlConfiguration.loadConfiguration(cooldownYml);

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();

		if (getConfig().getBoolean("firstLoad")) {
			getConfig().set("firstLoad", false);
		}

		saveAllCustomYml();
		saveConfig();

		addCooldowns();

		getBlacklistHandler().removeCustomRecipes();
		getBlacklistHandler().collectBlacklistedRecipes();
		getBlacklistHandler().disableRecipes();

		// Make task run later, for itemsadder plugin
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				ItemManager.loadAll();
				recipeBuilder.addRecipes(null);
				registerCommands();
				getLogger().log(Level.INFO, "Loaded " + recipeUtil.getRecipeNames().size() + " recipe(s).");
			}
		}, 40L);

		editItem = new RecipeGUI();
		recipes = new BookGUI(this);
		typeGUI = new RecipeTypeGUI();

		Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
		Bukkit.getPluginManager().registerEvents(recipes, this);
		Bukkit.getPluginManager().registerEvents(typeGUI, this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(craftManager, this);
		Bukkit.getPluginManager().registerEvents(amountManager, this);
		Bukkit.getPluginManager().registerEvents(new BlockManager(), this);
		Bukkit.getPluginManager().registerEvents(new CookingManager(), this);
		Bukkit.getPluginManager().registerEvents(new BrewEvent(), this);
		Bukkit.getPluginManager().registerEvents(this, this);

		if (serverVersionAtLeast(1, 21)) {
			Bukkit.getPluginManager().registerEvents(new CrafterManager(), this);
		}
		
		if (serverVersionAtLeast(1, 14)) {
			Bukkit.getPluginManager().registerEvents(new GrindstoneManager(), this);
			Bukkit.getPluginManager().registerEvents(new SmithingManager(), this);
		}

		if (serverVersionAtLeast(1, 13)) {
			Bukkit.getPluginManager().registerEvents(autoDiscover, this);
		}
		
		if (serverVersionAtLeast(1, 9)) {
			Bukkit.getPluginManager().registerEvents(new AnvilManager(), this);
		} else {
			Bukkit.getPluginManager().registerEvents(new AnvilManager_1_8(), this);
		}

		registerUpdateChecker();
		registerBstats();
	}

	public void clear() {
		reloadConfig();
		saveConfig();

		// Reset debug flags from config
		debug = getConfig().getBoolean("Debug");
		crafterdebug = getConfig().getBoolean("Crafter-Debug");

		// Clear internal state
		if (recipeBook != null)
			recipeBook.clear();
		if (disabledrecipe != null)
			disabledrecipe.clear();

		// Nullify GUI references
		recipes = null;
		typeGUI = null;
		editItem = null;

		// Reset managers
		recipeUtil = null;
		recipeBuilder = null;
	}

	public void reload() {

		clear();
		customConfig = YamlConfiguration.loadConfiguration(customYml);
		saveCustomYml(customConfig, customYml);
		saveAllCustomYml();

		// Reset managers
		recipeUtil = new RecipeUtil();
		recipeBuilder = new RecipeBuilder();
		editItem = new RecipeGUI();
		recipes = new BookGUI(this);
		typeGUI = new RecipeTypeGUI();

		// Remove old recipes
		getBlacklistHandler().removeCustomRecipes();
		getBlacklistHandler().collectBlacklistedRecipes();
		getBlacklistHandler().disableRecipes();

		// Re-add recipes immediately
		ItemManager.reload();
		recipeBuilder.addRecipes(null);
		getLogger().log(Level.INFO, "Reloaded " + recipeUtil.getRecipeNames().size() + " recipe(s).");

		getAutoDiscover().handleAutoDiscover();
	}

	@Override
	public void onDisable() {

		if (cooldownManager != null) {

			Map<UUID, Map<String, Cooldown>> allCooldowns = cooldownManager.getCooldowns();

			// Clear the entire section first to avoid leftovers
			cooldownConfig.set("Cooldowns", null);
			for (Map.Entry<UUID, Map<String, Cooldown>> playerEntry : allCooldowns.entrySet()) {

				UUID playerUUID = playerEntry.getKey();
				Map<String, Cooldown> playerCooldowns = playerEntry.getValue();

				for (Map.Entry<String, Cooldown> recipeEntry : playerCooldowns.entrySet()) {

					String recipeID = recipeEntry.getKey();
					Cooldown cooldown = recipeEntry.getValue();

					// Skip expired cooldowns AND ensure old entries are removed
					if (cooldown.isExpired()) {
						cooldownConfig.set("Cooldowns." + playerUUID + "." + recipeID, null);
						continue;
					}

					// Save remaining time
					cooldownConfig.set("Cooldowns." + playerUUID + "." + recipeID, cooldown.getTimeLeft());
				}
			}
			saveCustomYml(cooldownConfig, cooldownYml);
		}
		clear();
	}

	void addCooldowns() {
		if (cooldownConfig == null)
			return;
		if (!cooldownConfig.isConfigurationSection("Cooldowns"))
			return;

		// Loop through each player UUID section
		for (String playerUUIDString : cooldownConfig.getConfigurationSection("Cooldowns").getKeys(false)) {
			UUID playerUUID;

			// Validate UUID
			try {
				playerUUID = UUID.fromString(playerUUIDString);
			} catch (IllegalArgumentException ex) {
				continue; // Skip invalid UUID entries
			}

			String basePath = "Cooldowns." + playerUUIDString;

			// Loop through each recipe ID inside this player's section
			for (String recipeID : cooldownConfig.getConfigurationSection(basePath).getKeys(false)) {
				long remainingSeconds = cooldownConfig.getLong(basePath + "." + recipeID);
				if (remainingSeconds <= 0)
					continue;

				// Create new cooldown with the remaining time
				Cooldown cooldown = new Cooldown(recipeID, remainingSeconds);
				cooldownManager.addCooldown(playerUUID, cooldown);
			}
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

	public void sendMessage(Player player, String basePath, long seconds) {

		// ACTION BAR
		if (customConfig.getBoolean(basePath + ".actionbar-message.enabled")) {
			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						customConfig.getString(basePath + ".actionbar-message.message", ""));

				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message", e);
			}
		}

		// CHAT MESSAGE
		if (customConfig.getBoolean(basePath + ".chat-message.enabled")) {
			String message;

			if (basePath.equalsIgnoreCase("crafting-limit")) {
				message = getCooldownMessage(seconds);
			} else {
				message = ChatColor.translateAlternateColorCodes('&',
						customConfig.getString(basePath + ".chat-message.message", ""));
			}

			player.sendMessage(message);
		}

		// CLOSE INVENTORY
		if (customConfig.getBoolean(basePath + ".close-inventory")) {
			player.closeInventory();
		}
	}

	@EventHandler
	public void update(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		// Update check
		if (getConfig().getBoolean("Update-Check") && p.hasPermission("crecipe.reload")
				&& getDescription().getVersion().compareTo(newupdate) < 0) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&cCustom-Recipes: &fAn update has been found. Please download version&c " + newupdate
							+ ", &fyou are on version&c " + getDescription().getVersion() + "!"));
		}
	}

	private int[] getServerVersionParts() {
		String version = Bukkit.getServer().getBukkitVersion();
		int major = -1, minor = -1, patch = 0;

		String[] parts = version.split("-");
		if (parts.length > 0) {
			String[] nums = parts[0].split("\\.");
			try {
				major = Integer.parseInt(nums[0]);
				minor = Integer.parseInt(nums[1]);
				if (nums.length > 2) {
					patch = Integer.parseInt(nums[2]);
				}
			} catch (NumberFormatException e) {
				Main.getInstance().getLogger().log(Level.WARNING, "Error parsing server version numbers: " + version);
			}
		}

		return new int[] { major, minor, patch };
	}

	public boolean serverVersionAtLeast(int major, int minor) {
		int[] server_version = getServerVersionParts();

		if (server_version[0] >= major && server_version[1] >= minor) {
			return true;
		}
		return false;
	}

	public boolean serverVersionAtLeast(int major, int minor, int patch) {
		int[] v = getServerVersionParts();

		if (v[0] != major)
			return v[0] > major;

		if (v[1] != minor)
			return v[1] > minor;

		return v[2] >= patch;
	}

	public boolean serverVersionLessThan(int major, int minor) {
		int[] server_version = getServerVersionParts();

		if (server_version[0] == major && server_version[1] < minor) {
			return true;
		}
		return false;
	}
}