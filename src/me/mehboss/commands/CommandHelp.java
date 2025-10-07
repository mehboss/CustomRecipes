package me.mehboss.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandHelp {

	public static boolean Run(CRCommand command) {

		CommandSender p = command.sender;
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m-------------------------------------"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCUSTOM-RECIPES &fv"
				+ Bukkit.getPluginManager().getPlugin("CustomRecipes").getDescription().getVersion()));
		p.sendMessage(" ");
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe &8-&f Displays this help page &e(crecipe.help)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe list &8-&f Gives a list of all current recipes &e(crecipe.list)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe give <player> <recipename> &8-&f Gives a player a custom recipe item &e(crecipe.give)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe gui &8-&f Opens the user-friendly menu for adding/editing &e(crecipe.gui)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe book &8-&f Opens a custom made recipe booklet &e(crecipe.book)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe reload &8-&f Reloads the configs and resets all recipes &e(crecipe.reload)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe debug &8-&f Enables debug mode for crafting &e(crecipe.debug)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe crafterdebug &8-&f Enables debug mode for the crafter &e(crecipe.crafterdebug)"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&c/crecipe edititem &8-&f Convenient recipe & item handler"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/crecipe create <type> <id> [optional perm]"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/crecipe remove <id>"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/crecipe show <id>"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m-------------------------------------"));
		return true;
	}
}
