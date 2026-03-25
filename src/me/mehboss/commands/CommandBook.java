package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XSound;

import me.mehboss.recipe.Main;

public class CommandBook {

	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public static boolean Run(CRCommand command) {
		Player p = (Player) command.sender;

		if (!Main.getInstance().getRecipeBook().contains(p.getUniqueId()))
			Main.getInstance().getRecipeBook().add(p.getUniqueId());

		Main.getInstance().getTypeGUI().open(p);
		String OpenMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui.Open-Message"));
		p.sendMessage(OpenMessage);
		p.playSound(p.getLocation(), XSound.matchXSound(getConfig().getString("gui.Open-Sound")).get().parseSound(), 1,
				1);
		return true;
	}
}
