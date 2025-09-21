package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XSound;

import me.mehboss.recipe.Main;

public class CommandGUI {
	public static boolean Run(CRCommand command) {

		Player p = (Player) command.sender;

		if (!p.hasPermission("crecipe.gui")) {
			p.sendMessage(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Invalid-Perms")));
			return false;
		}
		
		if (Main.getInstance().recipeBook.contains(p.getUniqueId()))
			Main.getInstance().recipeBook.remove(p.getUniqueId());

		Main.getInstance().typeGUI.open(p);
		String OpenMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui.Open-Message"));
		p.sendMessage(OpenMessage);
		p.playSound(p.getLocation(),
				XSound.matchXSound(getConfig().getString("gui.Open-Sound")).get().parseSound(), 1, 1);
		
		return true;
	}
	
	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
}
