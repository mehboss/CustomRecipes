package me.mehboss.recipe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bananapuncher714.nbteditor.NBTEditor;

public class NBTCommands implements CommandExecutor {

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {

		if (sender instanceof Player) {

			Player p = (Player) sender;
			ItemStack inhand = null;

			if (p.getItemInHand() != null) {
				inhand = p.getItemInHand();
			}

			if (!p.hasPermission("crecipe.admin")) {
				p.sendMessage(ChatColor.RED + "You are missing the permission 'crecipe.admin'!");
				return false;
			}

			ItemMeta it = inhand.getItemMeta();

			if (args.length >= 3 && args.length <= 4 && args[0].equalsIgnoreCase("enchant")) {

				if (Enchantment.getByName(args[2].toUpperCase()) == null || p.getItemInHand() == null) {
					p.sendMessage(ChatColor.RED + "Invalid enchantment!");
					return false;
				}

				if (args[1].equalsIgnoreCase("remove")) {
					p.getItemInHand().removeEnchantment(Enchantment.getByName(args[2].toUpperCase()));
					p.sendMessage(ChatColor.GREEN + "Successfully removed item enchantment " + args[2].toUpperCase());

				} else if (args[1].equalsIgnoreCase("add")) {
					p.getItemInHand().addUnsafeEnchantment(Enchantment.getByName(args[2].toUpperCase()), Integer.parseInt(args[3]));
					p.sendMessage(ChatColor.GREEN + "Successfully added item enchantment " + args[2].toUpperCase());
				}

				return false;
			}

			if (args.length == 2 && args[0].equalsIgnoreCase("hideenchants")) {

				ItemStack item = p.getItemInHand();
				ItemMeta itemm = item.getItemMeta();

				if (args[1].equalsIgnoreCase("false")) {
					itemm.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
					p.getItemInHand().setItemMeta(itemm);

				} else if (args[1].equalsIgnoreCase("true")) {
					itemm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					p.getItemInHand().setItemMeta(itemm);

				} else {
					p.sendMessage(ChatColor.RED + "Invalid arguments!");
					return false;
				}

				p.sendMessage(ChatColor.GREEN + "Successfully updated item flag!");
				return false;
			}

			if (args.length == 2 && args[0].equalsIgnoreCase("key")) {
				ItemStack item = p.getItemInHand();
				item = NBTEditor.set(item, args[1], "CUSTOM_ITEM_IDENTIFIER");
				p.setItemInHand(item);

				p.sendMessage(ChatColor.GREEN + "Successfully updated item identifier!");
				return false;
			}

			if (args.length >= 2 && args[0].equalsIgnoreCase("name")) {

				StringBuilder sb = new StringBuilder();

				for (int i = 1; i < args.length; i++) {
					if (i > 1)
						sb.append(" ");
					sb.append(args[i]);
				}

				it.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.valueOf(sb)));
				inhand.setItemMeta(it);
				return false;
			}

			if (args.length == 1 && args[0].equalsIgnoreCase("lore")) {
				EditGUI.getInstance().sendLoremsg(p);
				EditGUI.getInstance().editmeta.put(p.getUniqueId(), "Lore");
				return false;
			}

			p.sendMessage(ChatColor.DARK_RED + "Invalid args!");
			p.sendMessage(ChatColor.RED + "/edititem name [name]");
			p.sendMessage(ChatColor.RED + "/edititem enchant add/remove [enchantment] [level]");
			p.sendMessage(ChatColor.RED + "/edititem hideenchants [true/false]");
			p.sendMessage(ChatColor.RED + "/edititem lore");
			p.sendMessage(ChatColor.RED + "/edititem key [key])");

		}
		return false;
	}
}
