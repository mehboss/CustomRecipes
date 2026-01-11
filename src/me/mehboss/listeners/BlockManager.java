package me.mehboss.listeners;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;

public class BlockManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().getRecipeUtil();

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (e.getItemInHand() == null)
			return;

		Recipe recipe = null;
		String key = null;

		ItemStack item = e.getItemInHand();
		ReadWriteNBT nbt = NBT.itemStackToNBT(item);

		if (item.getType() != Material.AIR && item.hasItemMeta() && nbt.hasTag("CUSTOM_ITEM_IDENTIFER")) {
			key = nbt.getString("CUSTOM_ITEM_IDENTIFIER");
		}

		if (key != null && recipeUtil.getRecipeFromKey(key) != null)
			recipe = recipeUtil.getRecipeFromKey(key);

		if (recipe == null)
			if (recipeUtil.getRecipeFromResult(item) != null) {
				recipe = recipeUtil.getRecipeFromResult(item);
			}

		if (recipe == null || recipe.isPlaceable())
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
