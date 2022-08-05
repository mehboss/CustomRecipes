package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Manager implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {

		if (cmd.getName().equalsIgnoreCase("edititem")) {

			if (sender instanceof Player) {

				Player p = (Player) sender;
				ItemStack inhand = null;

				if (p.getItemInHand() != null) {
					inhand = p.getItemInHand();
				}

				if (p.hasPermission("crecipe.admin")) {

					ItemMeta it = inhand.getItemMeta();

					if (args.length == 0 || args.length == 1) {
						p.sendMessage("Invalid args!");
						p.sendMessage("/edititem name [name]");
						p.sendMessage("/edititem durability [1-100]");
						p.sendMessage("/edititem enchant [enchantment] [level]");
						p.sendMessage("/edititem hidenenchants [true/false]");
						p.sendMessage("/edititem lore [lore]");
						return false;
					}

					if (args[0].equalsIgnoreCase("name")) {

						StringBuilder sb = new StringBuilder();

						for (int i = 1; i < args.length; i++) {
							if (i > 1)
								sb.append(" ");
							sb.append(args[i]);
						}

						it.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.valueOf(sb)));
						inhand.setItemMeta(it);
					}

					if (args[0].equalsIgnoreCase("lore")) {

						List<String> loreList = new ArrayList<String>();
						StringBuilder sb = new StringBuilder();

						for (int i = 1; i < args.length; i++) {
							if (i > 1)
								sb.append(" ");
							sb.append(args[i]);
							loreList.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(sb)));
						}

						it.setLore(loreList);
						inhand.setItemMeta(it);
					}
				} else {
					p.sendMessage("No permissions!");
				}
			}
		}
		return false;
	}
}
