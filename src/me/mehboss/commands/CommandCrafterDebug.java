package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import me.mehboss.recipe.Main;

public class CommandCrafterDebug {
		static FileConfiguration getConfig() {
			return Main.getInstance().getConfig();
		}

		public static boolean Run(CRCommand command) {

			CommandSender p = command.sender;
			if (Main.getInstance().isCrafterDebug()) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&c[CustomRecipes] &fCrafterDebug mode has been turned &cOFF."));
				getConfig().set("Crafter-Debug", false);
				Main.getInstance().setCrafterDebug(false);
				Main.getInstance().saveConfig();
				Main.getInstance().reloadConfig();
				return true;
			}

			if (!(Main.getInstance().isCrafterDebug())) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&c[CustomRecipes] &fCrafterDebug mode has been turned &aON.&f Check console for INFO."));
				getConfig().set("Crafter-Debug", true);
				Main.getInstance().setCrafterDebug(true);
				Main.getInstance().saveConfig();
				Main.getInstance().reloadConfig();
				return true;
			}

			return true;
		}
	}
