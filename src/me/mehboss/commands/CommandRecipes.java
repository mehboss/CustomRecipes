package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.mehboss.recipe.Main;

public class CommandRecipes implements CommandExecutor {

	FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		final CRCommand cmd = new CRCommand(sender, command, label, args);

		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player in order to use this command!");
			return true;
		}

		if (!sender.hasPermission("crecipe.book")) {
			sender.sendMessage(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Invalid-Perms")));
			return true;
		}

		return CommandBook.Run(cmd);
	}
}
