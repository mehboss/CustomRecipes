package me.mehboss.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import me.mehboss.recipe.Main;

public class RecipeBuilder implements CommandExecutor {

	private Main plugin;

	public RecipeBuilder(Main plugin) {
		this.plugin = plugin;
	}

	Boolean underDev = true;

	FileConfiguration debug() {
		return Main.getInstance().getConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {

		CommandSender p = sender;

		if (args.length == 0) {
		}
		return false;
	}
}
