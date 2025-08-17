package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import me.mehboss.recipe.Main;

public class CommandReload {
	
	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
	
	public static boolean Run(CRCommand command) {
		
		CommandSender p = command.sender;
		p.sendMessage(
				ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Reloading")));

		Main.getInstance().reload();
		p.sendMessage(
				ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Reload").replaceAll(
						"%recipes%", String.valueOf(Main.getInstance().recipeUtil.getRecipeNames().size()))));

		return true;
	}
}
