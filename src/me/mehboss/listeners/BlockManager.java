package me.mehboss.listeners;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;

public class BlockManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().getRecipeUtil();

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (e.getItemInHand() == null)
			return;

		String configName = null;
		String foundID = null;

		boolean found = false;
		ItemStack item = e.getItemInHand();

		if (NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			foundID = NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");
			found = true;
		}

		if (foundID != null && recipeUtil.getRecipeFromKey(foundID) != null)
			configName = recipeUtil.getRecipeFromKey(foundID).getName();

		if (foundID == null && !found)
			if (recipeUtil.getRecipeFromResult(item) != null) {
				configName = recipeUtil.getRecipeFromResult(item).getName();
				found = true;
			}

		FileConfiguration recipeConfig = getConfig(configName);

		if (configName == null || !found || !recipeConfig.isSet(configName + ".Placeable")
				|| recipeConfig.getBoolean(configName + ".Placeable") == true)
			return;

		e.setCancelled(true);
		String message = Main.getInstance().getConfig().isSet("Messages.No-Perm-Place")
				? Main.getInstance().getConfig().getString("Messages.No-Perm-Place")
				: null;

		if (message != null && !message.equalsIgnoreCase("none"))
			e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));

	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
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
