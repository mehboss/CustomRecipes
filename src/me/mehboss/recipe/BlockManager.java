package me.mehboss.recipe;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockManager implements Listener {

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (e.getItemInHand() == null)
			return;

		ItemStack item = e.getItemInHand();

		if (!configName().containsKey(item))
			return;

		String configName = configName().get(item);
		FileConfiguration recipeConfig = getConfig(configName);

		if (!recipeConfig.isSet(configName + ".Placeable")
				|| recipeConfig.getBoolean(configName + ".Placeable") == true)
			return;

		e.setCancelled(true);

	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	HashMap<ItemStack, String> configName() {
		return Main.getInstance().configName;
	}

	FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	void sendMessage(Player p) {
		if (!getConfig().isSet("Messages.No-Perm-Place")
				|| getConfig().getString("Messages.No-Perm-Place").equalsIgnoreCase("none"))
			return;

		String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.No-Perm-Place"));
		p.sendMessage(message);
	}
}
