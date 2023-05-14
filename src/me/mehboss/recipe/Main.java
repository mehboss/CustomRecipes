package me.mehboss.recipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	private ItemManager plugin;

	public Main(ItemManager plugin) {
		this.plugin = plugin;
	}
	ManageGUI recipes;
	AddGUI addItem;
	EditGUI editItem;

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

		plugin.addItems();

		Bukkit.getPluginManager().registerEvents(new ManageGUI(this, null), this);
		Bukkit.getPluginManager().registerEvents(new EffectsManager(), this);
		Bukkit.getPluginManager().registerEvents(new RecipeManager(), this);
		Bukkit.getPluginManager().registerEvents(this, this);
		
		recipes = new ManageGUI(this, null);
		editItem = new EditGUI(Main.getInstance(), null);

		getCommand("crecipe").setExecutor(new GiveRecipe(this));
		getCommand("edititem").setExecutor(new NBTCommands());
		addItem = new AddGUI(this, null);

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

		plugin.addItems();

		recipes = new ManageGUI(this, null);
		editItem = new EditGUI(Main.getInstance(), null);
		addItem = new AddGUI(this, null);
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

	public void sendmessage(Player p) {

		if (messagesConfig.getBoolean("action-bar.enabled") == true) {
			try {
				String message = ChatColor.translateAlternateColorCodes('&',
						messagesConfig.getString("action-bar.message"));

				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Error while sending action bar message");
			}
		}

		if (messagesConfig.getBoolean("chat-message.enabled") == true) {

			String message = ChatColor.translateAlternateColorCodes('&',
					messagesConfig.getString("chat-message.message"));

			p.closeInventory();
			p.sendMessage(message);
		}
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
}