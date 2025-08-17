package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import me.mehboss.recipe.Main;

public class CommandDebug {

	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public static boolean Run(CRCommand command) {

		CommandSender p = command.sender;
		if (Main.getInstance().debug) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c[CustomRecipes] &fDebug mode has been turned &cOFF."));
			getConfig().set("Debug", false);
			Main.getInstance().debug = false;
			Main.getInstance().saveConfig();
			Main.getInstance().reloadConfig();
			return true;
		}

		if (!(Main.getInstance().debug)) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c[CustomRecipes] &fDebug mode has been turned &aON.&f Check console for INFO."));
			getConfig().set("Debug", true);
			Main.getInstance().debug = true;
			Main.getInstance().saveConfig();
			Main.getInstance().reloadConfig();
			return true;
		}

		return true;
	}
}
