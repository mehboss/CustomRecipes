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
import me.mehboss.commands.CommandItems;
import me.mehboss.commands.CommandList;
import me.mehboss.commands.CommandCreate;
import me.mehboss.commands.CommandReload;
import me.mehboss.commands.CommandRemove;
import me.mehboss.commands.CommandShow;

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

				if (!sender.hasPermission("crecipe." + args[0].toLowerCase())) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							getConfig().getString("Messages.Invalid-Perms")));
					return true;
				}

				if (args[0].equalsIgnoreCase("remove")) {
					return CommandRemove.Run(cmd);
				}
				
				if (args[0].equalsIgnoreCase("help")) {
					return CommandHelp.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("list")) {
					return CommandList.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("items")) {
					return CommandItems.Run(cmd);
				}
				
				if (args[0].equalsIgnoreCase("give")) {
					return CommandGive.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("reload")) {
					return CommandReload.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("debug")) {
					return CommandDebug.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("crafterdebug")) {
					return CommandCrafterDebug.Run(cmd);
				}

				// anything after this must require sender to be a player
				if (!(sender instanceof Player)) {
					sender.sendMessage("You must be a player in order to use this command!");
					return true;
				}

				if (args[0].equalsIgnoreCase("create")) {
					return CommandCreate.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("show")) {
					return CommandShow.Run(cmd);
				}
				
				if (args[0].equalsIgnoreCase("edititem")) {
					return CommandEditItem.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("book")) {
					return CommandBook.Run(cmd);
				}

				if (args[0].equalsIgnoreCase("gui")) {
					return CommandGUI.Run(cmd);
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