package me.mehboss.utils.libs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;

public class ItemManager {

	private static final Map<String, ItemStack> BY_IDENTIFIER = new HashMap<>();
	static FileConfiguration itemConfig = null;

	private static File itemsRoot() {
		return new File(Main.getInstance().getDataFolder(), "items");
	}

	/** Load all items from items folder */
	public static void loadAll() {
		BY_IDENTIFIER.clear();

		File root = itemsRoot();
		if (!root.exists())
			root.mkdirs();

		File[] files = root.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
		if (files == null)
			return;

		for (File ymlFile : files) {
			try {
				loadOne(ymlFile);
			} catch (Exception ex) {
				Main.getInstance().getLogger().log(Level.WARNING,
						"[CustomItemAPI] Failed loading item file '" + ymlFile.getName() + "': " + ex.getMessage(), ex);
			}
		}

		Main.getInstance().getLogger().info("Loaded " + BY_IDENTIFIER.size() + " custom item(s).");
	}

	/** Reload everything. */
	public static void reload() {
		loadAll();
	}

	/** Lists all items. */
	public static ArrayList<String> getAllItems() {
		return new ArrayList<>(BY_IDENTIFIER.keySet());
	}

	/** Get by Identifier. */
	public static ItemStack get(String identifier) {
		ItemStack s = BY_IDENTIFIER.get(identifier);
		return s == null ? null : s.clone();
	}

	/** Get by Result. */
	public static String get(ItemStack item) {
		for (String id : BY_IDENTIFIER.keySet()) {
			ItemStack custom = BY_IDENTIFIER.get(id);

			if (item.isSimilar(custom))
				return id;
		}
		return null;
	}

	/** Give one to a player by Identifier. */
	public static boolean give(Player p, String identifier) {
		ItemStack it = get(identifier);
		if (it == null)
			return false;
		p.getInventory().addItem(it);
		return true;
	}

	private static void loadOne(File ymlFile) {
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(ymlFile);

		// Prefer section = filename (without .yml), else first section in file
		String fileKey = ymlFile.getName().replace(".yml", "");
		String section = cfg.isConfigurationSection(fileKey) ? fileKey : firstTopLevelSection(cfg).orElse(null);

		if (section == null) {
			Main.getInstance().getLogger()
					.warning("[CustomItemAPI] No top-level section found in " + ymlFile.getName());
			return;
		}

		itemConfig = cfg;
		String resultPath = cfg.isConfigurationSection(section + ".Result") ? section + ".Result" : section;
		Optional<ItemStack> built = Main.getInstance().itemFactory.buildItem(resultPath, itemConfig);
		if (!built.isPresent()) {
			Main.getInstance().getLogger().warning("[CustomItemAPI] buildItem failed for section " + section);
			return;
		}

		ItemStack it = built.get();
		it = Main.getInstance().itemFactory.handleDurability(it, resultPath);
		int amount = cfg.isInt(resultPath + ".Amount") ? cfg.getInt(resultPath + ".Amount") : 1;
		it.setAmount(amount);

		// Identifier is required for lookup
		String id = cfg.getString(section + ".Identifier");
		if (id != null && !id.isEmpty()) {
			BY_IDENTIFIER.put(id, it.clone());
			if (Main.getInstance().debug) {
				Main.getInstance().getLogger().info("[CustomItemAPI] Loaded '" + section + "' (Identifier=" + id + ")");
			}
		} else if (Main.getInstance().debug) {
			Main.getInstance().getLogger()
					.info("[CustomItemAPI] Section '" + section + "' missing Identifier, skipping.");
		}
	}

	private static Optional<String> firstTopLevelSection(FileConfiguration cfg) {
		Set<String> keys = cfg.getKeys(false);
		for (String k : keys) {
			if (cfg.isConfigurationSection(k))
				return Optional.of(k);
		}
		return Optional.empty();
	}
}