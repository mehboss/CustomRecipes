package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandGUI {
	public static boolean Run(CRCommand command) {

		Player p = (Player) command.sender;
		p.sendMessage(ChatColor.RED + "The GUI is under development and will be coming soon!");
		/*
		 * Player player = (Player) sender;
		 * 
		 * if (!sender.hasPermission("crecipe.gui")) { sender.sendMessage(
		 * ChatColor.translateAlternateColorCodes('&',
		 * debug().getString("Messages.Invalid-Perms"))); return false; }
		 * 
		 * if (Main.getInstance().recipeBook.contains(player.getUniqueId()))
		 * Main.getInstance().recipeBook.remove(player.getUniqueId());
		 * 
		 * Main.getInstance().recipes.show(player); String OpenMessage =
		 * ChatColor.translateAlternateColorCodes('&',
		 * debug().getString("gui.Open-Message")); player.sendMessage(OpenMessage);
		 * player.playSound(player.getLocation(),
		 * XSound.matchXSound(debug().getString("gui.Open-Sound")).get().parseSound(),
		 * 1, 1); return true;
		 */
		return true;
	}
}
