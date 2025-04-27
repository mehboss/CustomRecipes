package me.mehboss.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.gui.EditGUI;
import me.mehboss.recipe.Main;

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
					p.getItemInHand().addUnsafeEnchantment(Enchantment.getByName(args[2].toUpperCase()),
							Integer.parseInt(args[3]));
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

			if (args.length == 3 && args[0].equalsIgnoreCase("undiscover")) {
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				Player player = Bukkit.getPlayer(args[1]);

				if (key == null) {
					p.sendMessage(ChatColor.RED + "Invalid NamespacedKey to undiscover!");
					return false;
				}

				if (player == null) {
					p.sendMessage(ChatColor.RED + "Player not found!");
					return false;
				}
				player.undiscoverRecipe(key);
				p.sendMessage(ChatColor.GREEN + "Successfully undiscovered recipe for " + args[1]);
				return false;
			}

			if (args.length == 3 && args[0].equalsIgnoreCase("discover")) {
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				Player player = Bukkit.getPlayer(args[1]);

				if (key == null) {
					p.sendMessage(ChatColor.RED + "Invalid NamespacedKey to discover!");
					return false;
				}

				if (player == null) {
					p.sendMessage(ChatColor.RED + "Player not found!");
					return false;
				}
				player.discoverRecipe(key);
				p.sendMessage(ChatColor.GREEN + "Successfully discovered recipe for " + args[1]);
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

			if (args.length >= 2 && args[0].equalsIgnoreCase("modeldata")) {

				Integer modelData = -1;

				try {
					modelData = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					p.sendMessage(ChatColor.RED + "Custom model data must be an int! " + args[1]);
					return false;
				}

				p.sendMessage(ChatColor.GREEN + "Successfully updated item model data!");
				it.setCustomModelData(modelData);
				inhand.setItemMeta(it);
				return false;
			}

			if (args.length == 1 && args[0].equalsIgnoreCase("lore")) {
				EditGUI.getInstance().sendLoremsg(p);
				EditGUI.getInstance().editmeta.put(p.getUniqueId(), "Lore");
				return false;
			}

			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&aCUSTOM-RECIPES &fv" + Main.getInstance().getDescription().getVersion()));
			p.sendMessage(" ");
			p.sendMessage(ChatColor.RED + "/edititem name [name]");
			p.sendMessage(ChatColor.RED + "/edititem modeldata [int]");
			p.sendMessage(ChatColor.RED + "/edititem enchant add/remove [enchantment] [level]");
			p.sendMessage(ChatColor.RED + "/edititem hideenchants [true/false]");
			p.sendMessage(ChatColor.RED + "/edititem lore");
			p.sendMessage(ChatColor.RED + "/edititem key [key])");
			p.sendMessage(ChatColor.RED + "/edititem undiscover [player] [NamespacedKey]");
			p.sendMessage(ChatColor.RED + "/edititem discover [player] [NamespacedKey]");
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));

		}
		return false;
	}
}
