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
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.anvil.AnvilManager;
import me.mehboss.anvil.GrindstoneManager;
import me.mehboss.commands.TabCompletion;
import me.mehboss.cooking.CookingManager;
import me.mehboss.crafting.AmountManager;
import me.mehboss.crafting.CooldownManager;
import me.mehboss.crafting.CraftManager;
import me.mehboss.crafting.CrafterManager;
import me.mehboss.gui.RecipeGUI;
import me.mehboss.gui.RecipeTypeGUI;
import me.mehboss.gui.BookGUI;
import me.mehboss.listeners.BlockManager;
import me.mehboss.listeners.EffectsManager;
import me.mehboss.utils.ItemBuilder;
import me.mehboss.utils.MetaChecks;
import me.mehboss.utils.Metrics;
import me.mehboss.utils.Placeholders;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	public AmountManager amountManager;
	public CraftManager craftManager;
	public RecipeManager recipeManager;
	public BookGUI recipes;
	public RecipeTypeGUI typeGUI;
	public CooldownManager cooldownManager;
	public RecipeUtil recipeUtil;
	public ExactChoice exactChoice;

	RecipeGUI editItem;

	public Map<UUID, Long> debounceMap = new HashMap<>();
	public ArrayList<UUID> inInventory = new ArrayList<UUID>();
	public ArrayList<UUID> recipeBook = new ArrayList<UUID>();
	public HashMap<UUID, Inventory> saveInventory = new HashMap<UUID, Inventory>();
	public ArrayList<String> disabledrecipe = new ArrayList<String>();

	// add three more shapelessname, amount, and ID specifically for config.

	File customYml = new File(getDataFolder() + "/blacklisted.yml");
	public FileConfiguration customConfig = null;

	File cooldownYml = new File(getDataFolder() + "/cooldowns.yml");
	FileConfiguration cooldownConfig = null;

	File cursedYml = new File(getDataFolder() + "/recipes/CursedPick.yml");
	File swordYml = new File(getDataFolder() + "/recipes/CursedSword.yml");
	File luckyYml = new File(getDataFolder() + "/items/LuckyPickaxe.yml");
	File bagYml = new File(getDataFolder() + "/recipes/HavenBag.yml");
	File sandYml = new File(getDataFolder() + "/recipes/WheatSand.yml");

	public Boolean hasAE = false;
	public Boolean hasEE = false;
	public Boolean hasHavenBags = false;
	Boolean hasEEnchants = false;
	Boolean uptodate = true;
	Boolean isFirstLoad = true;
	String newupdate = null;

	public Boolean debug = false;
	public Boolean crafterdebug = false;

	public RecipeUtil getRecipeUtil() {
		return recipeUtil;
	}

	public static Main getInstance() {
		return instance;
	}

	private static Main instance;
	public MetaChecks metaChecks;

	public boolean hasCustomPlugin(String plugin) {
		switch (plugin) {
		case "itemsadder":
			return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
		case "mythicmobs":
			return Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
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

		if (isFirstLoad && !cursedYml.exists()) {
			saveResource("recipes/CursedPick.yml", false);
		}

		if (isFirstLoad && !swordYml.exists()) {
			saveResource("recipes/CursedSword.yml", false);
		}

		if (isFirstLoad && !bagYml.exists()) {
			saveResource("recipes/HavenBag.yml", false);
		}

		if (isFirstLoad && !sandYml.exists()) {
			saveResource("recipes/WheatSand.yml", false);
		}

		if (isFirstLoad && !luckyYml.exists()) {
			saveResource("items/LuckyPickaxe.yml", false);
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
		recipeManager = new RecipeManager();
		metaChecks = new MetaChecks();
		craftManager = new CraftManager();
		amountManager = new AmountManager(craftManager);

		if (serverVersionAtLeast(1, 12))
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

		debug = getConfig().getBoolean("Debug");
		crafterdebug = getConfig().getBoolean("Crafter-Debug");

		saveAllCustomYml();
		saveConfig();
		removeRecipes();
		addCooldowns();

		disableRecipes();

		// Make task run later, for itemsadder plugin
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				ItemBuilder.loadAll();
				recipeManager.addRecipes(null);
				registerCommands();
				getLogger().log(Level.INFO, "Loaded " + recipeUtil.getRecipeNames().size() + " recipe(s).");
			}
		}, 40L);

		editItem = new RecipeGUI(this, null);
		recipes = new BookGUI(this);
		typeGUI = new RecipeTypeGUI();

		Bukkit.getPluginManager().registerEvents(editItem, this);
		Bukkit.getPluginManager().registerEvents(recipes, this);
		Bukkit.getPluginManager().registerEvents(typeGUI, this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(craftManager, this);
		Bukkit.getPluginManager().registerEvents(new CrafterManager(), this);
		Bukkit.getPluginManager().registerEvents(new AmountManager(craftManager), this);
		Bukkit.getPluginManager().registerEvents(new BlockManager(), this);
		Bukkit.getPluginManager().registerEvents(new AnvilManager(), this);
		Bukkit.getPluginManager().registerEvents(new CookingManager(), this);
		Bukkit.getPluginManager().registerEvents(new GrindstoneManager(), this);
		Bukkit.getPluginManager().registerEvents(this, this);

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
		if (saveInventory != null)
			saveInventory.clear();
		if (disabledrecipe != null)
			disabledrecipe.clear();

		// Nullify GUI references
		recipes = null;
		typeGUI = null;
		editItem = null;

		// Reset managers
		recipeUtil = null;
		recipeManager = null;
	}

	public void reload() {

		// Remove old recipes
		removeCustomRecipes();
		removeRecipes();
		clear();

		disableRecipes();

		// Reload configs
		reloadConfig();
		customConfig = YamlConfiguration.loadConfiguration(customYml);
		saveCustomYml(customConfig, customYml);
		saveAllCustomYml();

		// Reset managers
		recipeUtil = new RecipeUtil();
		recipeManager = new RecipeManager();
		recipes = new BookGUI(this);
		typeGUI = new RecipeTypeGUI();
		editItem = new RecipeGUI(this, null);

		// Re-add recipes immediately
		ItemBuilder.reload();
		recipeManager.addRecipes(null);
		getLogger().log(Level.INFO, "Reloaded " + recipeUtil.getRecipeNames().size() + " recipe(s).");

		handleAutoDiscover();
	}

	@Override
	public void onDisable() {

		if (cooldownManager != null) {
			// Check if there are any cooldowns
			if (cooldownManager.getCooldowns().isEmpty() && cooldownConfig.isConfigurationSection("Cooldowns")) {
				cooldownConfig.set("Cooldowns", null);
				saveCustomYml(cooldownConfig, cooldownYml);
			} else if (!cooldownManager.getCooldowns().isEmpty()) {
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

	void handleAutoDiscover() {
		if (!serverVersionAtLeast(1, 12))
			return;

		// Re-discover recipes for all online players
		for (Player p : Bukkit.getOnlinePlayers()) {
			for (RecipeUtil.Recipe recipe : recipeUtil.getAllRecipes().values()) {
				NamespacedKey key = NamespacedKey.fromString("customrecipes:" + recipe.getKey().toLowerCase());

				if (!recipe.isDiscoverable() || key == null || Bukkit.getRecipe(key) == null)
					continue;

				if (recipe.hasPerm() && !p.hasPermission(recipe.getPerm())) {
					if (p.hasDiscoveredRecipe(key)) {
						p.undiscoverRecipe(key);
					}
				} else {
					if (!p.hasDiscoveredRecipe(key)) {
						p.discoverRecipe(key);
					}
				}
			}
		}
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
					cooldownManager.setCooldown(playerUUID, recipeID, cooldownTime);
				}
			}
		}
	}

	void removeCustomRecipes() {
		if (!serverVersionAtLeast(1, 16) || recipeUtil.getAllRecipes().isEmpty())
			return;

		for (RecipeUtil.Recipe recipe : recipeUtil.getAllRecipes().values()) {
			String key = recipe.getKey();
			NamespacedKey npk = NamespacedKey.fromString("customrecipes:" + key.toLowerCase());

			if (npk == null)
				continue;

			Bukkit.removeRecipe(npk);
		}
	}

	void removeRecipes() {
		if (customConfig == null)
			return;

		if (!serverVersionAtLeast(1, 16)) {
			debug("[Blacklisted] You must be on 1.16 or higher to utilize the blacklisted feature!");
			return;
		}

		if (customConfig.isConfigurationSection("override-recipes")) {
			ConfigurationSection sec = customConfig.getConfigurationSection("override-recipes");

			for (String typeKey : sec.getKeys(false)) {
				List<String> targets = sec.getStringList(typeKey);
				if (targets == null || targets.isEmpty())
					continue;

				for (String entry : targets) {
					String s = entry == null ? "" : entry;
					if (s.isEmpty())
						continue;

					// --- First: try explicit namespaced key, e.g. "minecraft:stick"
					NamespacedKey nk = NamespacedKey.fromString(s.toLowerCase());
					if (nk != null) {
						Recipe rec = Bukkit.getRecipe(nk);
						if (rec == null) {
							if (debug)
								debug("[Blacklisted] Could not find recipe to remove for key " + nk);
							continue;
						}
						if (!(rec instanceof Keyed))
							continue; // sanity
						if (!matchesType(typeKey, rec)) {
							if (debug)
								debug("[Blacklisted] Recipe key " + nk + " does not match type bucket " + typeKey);
							continue;
						}
						if (debug)
							debug("[Blacklisted] Removing recipe " + nk + " from the server..");
						try {
							Bukkit.removeRecipe(nk);
						} catch (Exception ex) {
							getLogger().warning("[Blacklisted] Could not remove recipe from the server.. " + nk + ": "
									+ ex.getMessage());
						}
						continue; // done with this entry
					}

					// --- Fallback: treat entry as an item name (your old behavior)
					Optional<XMaterial> xm = XMaterial.matchXMaterial(s);
					if (!xm.isPresent())
						continue;

					ItemStack result = xm.get().parseItem();
					if (result == null)
						continue;

					for (Recipe r : Bukkit.getRecipesFor(result)) {
						if (!(r instanceof Keyed))
							continue;
						if (!matchesType(typeKey, r))
							continue;

						NamespacedKey key = ((Keyed) r).getKey();
						if (debug)
							debug("[Blacklisted] Removing recipe " + key + " from the server..");
						try {
							Bukkit.removeRecipe(key);
						} catch (Exception ex) {
							getLogger().warning("[Blacklisted] Could not remove recipe from the server.. " + key + ": "
									+ ex.getMessage());
						}
					}
				}
			}
		}

		if (customConfig.getBoolean("disable-all-vanilla", false)) {
			Bukkit.clearRecipes();
		}
	}

	// --- Optional recipe classes; null if not present on this server ---
	private static final Class<?> C_BLASTING = classOrNull("org.bukkit.inventory.BlastingRecipe");
	private static final Class<?> C_SMOKING = classOrNull("org.bukkit.inventory.SmokingRecipe");
	private static final Class<?> C_CAMPFIRE = classOrNull("org.bukkit.inventory.CampfireRecipe");
	private static final Class<?> C_STONECUT = classOrNull("org.bukkit.inventory.StonecuttingRecipe");
	private static final Class<?> C_COOKING = classOrNull("org.bukkit.inventory.CookingRecipe"); // abstract base on
																									// newer versions

	private static Class<?> classOrNull(String fqn) {
		try {
			return Class.forName(fqn);
		} catch (Throwable t) {
			return null;
		}
	}

	private static boolean isInstance(Object obj, Class<?> cls) {
		return cls != null && cls.isInstance(obj);
	}

	/** Java-8 friendly type filter (no switch expressions). */
	private static boolean matchesType(String typeKey, Recipe r) {
		String t = (typeKey == null ? "" : typeKey.toLowerCase(java.util.Locale.ROOT));
		switch (t) {
		case "crafting":
			return (r instanceof ShapedRecipe) || (r instanceof ShapelessRecipe);

		case "furnace":
		case "smelting":
			return (r instanceof FurnaceRecipe) || isInstance(r, C_BLASTING) || isInstance(r, C_SMOKING)
					|| isInstance(r, C_CAMPFIRE) || isInstance(r, C_COOKING); // broad fallback if present

		case "stonecutter":
		case "stonecutting":
			return isInstance(r, C_STONECUT);

		default:
			return false;
		}
	}

	public void disableRecipes() {
		if (customConfig == null)
			return;

		if (customConfig.isConfigurationSection("vanilla-recipe"))
			for (String vanilla : customConfig.getConfigurationSection("vanilla-recipes").getKeys(false)) {
				disabledrecipe.add(vanilla);
			}

		if (customConfig.isConfigurationSection("custom-recipes"))
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

		Player p = e.getPlayer();

		if (getConfig().getBoolean("Update-Check") == true && e.getPlayer().hasPermission("crecipe.reload")
				&& getDescription().getVersion().compareTo(newupdate) < 0) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&cCustom-Recipes: &fAn update has been found. Please download version&c " + newupdate
							+ ", &fyou are on version&c " + getDescription().getVersion() + "!"));
		}

		if (serverVersionLessThan(1, 14))
			return;

		try {
			for (RecipeUtil.Recipe recipe : recipeUtil.getAllRecipes().values()) {
				NamespacedKey key = NamespacedKey.fromString("customrecipes:" + recipe.getKey().toLowerCase());
				Recipe result = Bukkit.getRecipe(key);

				if (key == null || (result == null))
					continue;

				if (!recipe.isDiscoverable())
					continue;

				if (recipe.getPerm() != null && !p.hasPermission(recipe.getPerm())) {
					if (p.hasDiscoveredRecipe(key))
						p.undiscoverRecipe(key);
				} else if (recipe.getPerm() == null || p.hasPermission(recipe.getPerm())) {
					if (!p.hasDiscoveredRecipe(key))
						p.discoverRecipe(key);
				}

			}
		} catch (NoClassDefFoundError error) {
			Main.getInstance().getLogger().log(Level.WARNING, "Couldn't discover recipe upon player join.");
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

	public boolean serverVersionLessThan(int major, int minor) {
		int[] server_version = getServerVersionParts();

		if (server_version[0] == major && server_version[1] < minor) {
			return true;
		}
		return false;
	}

	public void debug(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);

	}
}