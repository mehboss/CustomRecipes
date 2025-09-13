package me.mehboss.recipe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.mehboss.commands.CRCommand;
import me.mehboss.commands.CommandBook;
import me.mehboss.commands.CommandCrafterDebug;
import me.mehboss.commands.CommandDebug;
import me.mehboss.commands.CommandEditItem;
import me.mehboss.commands.CommandGUI;
import me.mehboss.commands.CommandGive;
import me.mehboss.commands.CommandHelp;
import me.mehboss.commands.CommandList;
import me.mehboss.commands.CommandReload;

public class CommandListener implements CommandExecutor {

	String Name = "§c[CustomRecipes]§r";

	FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		final CRCommand cmd = new CRCommand(sender, command, label, args);

		if (args.length == 0) {
			sender.sendMessage(Name + " CustomRecipes by Mehboss.");
			return false;

		} else if (args.length >= 1) {
			try {

				if (!sender.hasPermission("crecipe." + args[0])) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							getConfig().getString("Messages.Invalid-Perms")));
					return true;
				}

				if (args[0].equalsIgnoreCase("help") && sender.hasPermission("crecipe.help")) {
					return CommandHelp.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("list") && sender.hasPermission("crecipe.list")) {
					return CommandList.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("give") && sender.hasPermission("crecipe.give")) {
					return CommandGive.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("book") && sender.hasPermission("crecipe.book")) {
					if (sender instanceof Player) {
						return CommandBook.Run(cmd);
					} else {
						sender.sendMessage("You must be a player in order to use this command!");
						return true;
					}
				}

				if (args[0].equalsIgnoreCase("gui") && sender.hasPermission("crecipe.gui")) {
					if (sender instanceof Player) {
						return CommandGUI.Run(cmd);
					} else {
						sender.sendMessage("You must be a player in order to use this command!");
						return true;
					}
				}

				if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("crecipe.reload")) {
					return CommandReload.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("crecipe.debug")) {
					return CommandDebug.Run(cmd);
				}
				
				if (args[0].equalsIgnoreCase("crafterdebug") && sender.hasPermission("crecipe.debug")) {
					return CommandCrafterDebug.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("edititem") && sender.hasPermission("crecipe.edititem")) {
					if (sender instanceof Player) {
						return CommandEditItem.Run(cmd);
					} else {
						sender.sendMessage("You must be a player in order to use this command!");
						return true;
					}
				}
			} catch (Exception e) {
				sender.sendMessage(ChatColor.RED + "Invalid command");
				e.printStackTrace();
			}
		}
		sender.sendMessage(ChatColor.RED + "Unknown command.");
		return false;
	}
}