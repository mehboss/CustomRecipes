package me.mehboss.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.changeme.nbtapi.NBT;
import me.mehboss.gui.framework.GuiButton.GuiLoreButton;
import me.mehboss.gui.framework.chat.ChatEditManager;
import me.mehboss.recipe.Main;
import me.mehboss.utils.libs.CompatibilityUtil;

public class CommandEditItem {
	@SuppressWarnings("deprecation")
	public static boolean Run(CRCommand command) {

		Player p = (Player) command.sender;
		ItemStack inhand = p.getItemInHand();

		if (inhand == null)
			return true;

		ItemMeta it = inhand.getItemMeta();

		if (command.args.length >= 4 && command.args.length <= 5 && command.args[1].equalsIgnoreCase("enchant")) {

			if (Enchantment.getByName(command.args[3].toUpperCase()) == null || p.getItemInHand() == null) {
				p.sendMessage(ChatColor.RED + "Invalid enchantment!");
				return false;
			}

			if (command.args[2].equalsIgnoreCase("remove")) {
				p.getItemInHand().removeEnchantment(Enchantment.getByName(command.args[3].toUpperCase()));
				p.sendMessage(
						ChatColor.GREEN + "Successfully removed item enchantment " + command.args[3].toUpperCase());

			} else if (command.args[2].equalsIgnoreCase("add")) {
				p.getItemInHand().addUnsafeEnchantment(Enchantment.getByName(command.args[3].toUpperCase()),
						Integer.parseInt(command.args[4]));
				p.sendMessage(ChatColor.GREEN + "Successfully added item enchantment " + command.args[3].toUpperCase());
			}

			return false;
		}

		if (command.args.length == 3 && command.args[1].equalsIgnoreCase("hideenchants")) {

			ItemStack item = p.getItemInHand();
			ItemMeta itemm = item.getItemMeta();

			if (command.args[2].equalsIgnoreCase("false")) {
				itemm.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
				p.getItemInHand().setItemMeta(itemm);

			} else if (command.args[2].equalsIgnoreCase("true")) {
				itemm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				p.getItemInHand().setItemMeta(itemm);

			} else {
				p.sendMessage(ChatColor.RED + "Invalid arguments!");
				return false;
			}

			p.sendMessage(ChatColor.GREEN + "Successfully updated item flag!");
			return false;
		}

		if (command.args.length == 3 && command.args[1].equalsIgnoreCase("key")) {
			ItemStack item = p.getItemInHand();
			NBT.modify(item, (nbt) -> {
				nbt.setString("CUSTOM_ITEM_IDENTIFIER", command.args[2]);
			});
			p.setItemInHand(item);

			p.sendMessage(ChatColor.GREEN + "Successfully updated item identifier!");
			return false;
		}

		if (command.args.length == 3 && command.args[1].equalsIgnoreCase("give")) {
			String key = command.args[2];
			ItemStack recipe = Bukkit.getRecipe(NamespacedKey.fromString(key)) == null ? null
					: Bukkit.getRecipe(NamespacedKey.fromString(key)).getResult();

			if (recipe == null) {
				p.sendMessage(ChatColor.RED + "Could not find bukkit recipe to give! ex: minecraft:stick");
				return false;
			}

			p.getInventory().addItem(recipe);
			p.sendMessage(ChatColor.GREEN + "Successfully retrieved recipe result for " + key + "!");
			return false;
		}

		if (command.args.length == 4 && command.args[1].equalsIgnoreCase("undiscover")) {
			NamespacedKey key = NamespacedKey.fromString(command.args[3]);
			Player player = Bukkit.getPlayer(command.args[2]);

			if (key == null) {
				p.sendMessage(ChatColor.RED + "Invalid NamespacedKey to undiscover!");
				return false;
			}

			if (player == null) {
				p.sendMessage(ChatColor.RED + "Player not found!");
				return false;
			}
			player.undiscoverRecipe(key);
			p.sendMessage(ChatColor.GREEN + "Successfully undiscovered recipe for " + command.args[2]);
			return false;
		}

		if (command.args.length == 4 && command.args[1].equalsIgnoreCase("discover")) {
			NamespacedKey key = NamespacedKey.fromString(command.args[3]);
			Player player = Bukkit.getPlayer(command.args[2]);

			if (key == null) {
				p.sendMessage(ChatColor.RED + "Invalid NamespacedKey to discover!");
				return false;
			}

			if (player == null) {
				p.sendMessage(ChatColor.RED + "Player not found!");
				return false;
			}
			player.discoverRecipe(key);
			p.sendMessage(ChatColor.GREEN + "Successfully discovered recipe for " + command.args[2]);
			return false;
		}

		if (command.args.length >= 3 && command.args[1].equalsIgnoreCase("name")) {

			StringBuilder sb = new StringBuilder();

			for (int i = 2; i < command.args.length; i++) {
				if (i > 2)
					sb.append(" ");
				sb.append(command.args[i]);
			}

			it.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.valueOf(sb)));
			inhand.setItemMeta(it);
			return true;
		}

		if (command.args.length >= 3 && command.args[1].equalsIgnoreCase("itemname")) {
			if (!CompatibilityUtil.supportsItemName())
				return false;
			
			StringBuilder sb = new StringBuilder();

			for (int i = 2; i < command.args.length; i++) {
				if (i > 2)
					sb.append(" ");
				sb.append(command.args[i]);
			}

			it.setItemName(ChatColor.translateAlternateColorCodes('&', String.valueOf(sb)));
			inhand.setItemMeta(it);
			return true;
		}
		
		if (command.args.length >= 3 && command.args[1].equalsIgnoreCase("modeldata")) {
			if (!CompatibilityUtil.supportsCustomModelData()) {
				p.sendMessage(ChatColor.RED + "Unsupported server version!");
				return false;
			}

			Integer model = -1;
			try {
				model = Integer.parseInt(command.args[2]);
			} catch (NumberFormatException e) {
				p.sendMessage(ChatColor.RED + "Custom model data must be an int! " + command.args[2]);
				return false;
			}

			p.sendMessage(ChatColor.GREEN + "Successfully updated item model data!");
			it.setCustomModelData(model);
			inhand.setItemMeta(it);
			return true;
		}

		if (command.args.length >= 3 && command.args[1].equalsIgnoreCase("itemmodel")) {
			if (!CompatibilityUtil.supportsItemModel()) {
				p.sendMessage(ChatColor.RED + "Unsupported server version!");
				return false;
			}

			try {
				String[] split = command.args[2].split(":");
				NamespacedKey model = new NamespacedKey(split[0], split[1]);
				it.setItemModel(model);
			} catch (Exception e) {
				p.sendMessage(ChatColor.RED + "An error occurred while updating the item model!");
				return false;
			}

			p.sendMessage(ChatColor.GREEN + "Successfully updated item model!");
			inhand.setItemMeta(it);
			return true;
		}

		if (command.args.length == 2 && command.args[1].equalsIgnoreCase("lore")) {
			if (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR)
				return false;

			ItemStack item = p.getItemInHand();
			GuiLoreButton button = new GuiLoreButton(1, item) {
				@Override
				public void onLoreChange(Player player, List<String> newLore) {
					ItemMeta meta = item.getItemMeta();
					if (meta != null) {
						meta.setLore(newLore);
						item.setItemMeta(meta);
					}
				}
			};
			ChatEditManager.get().beginLoreEdit(p, null, button);
			return true;
		}

		if (command.args.length == 2 && command.args[1].equalsIgnoreCase("print")) {
			p.sendMessage(p.getItemInHand().toString());
			return true;
		}

		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&aCUSTOM-RECIPES &fv" + Main.getInstance().getDescription().getVersion()));
		p.sendMessage(" ");
		p.sendMessage(ChatColor.RED + "/cr edititem give <bukkit recipe>");
		p.sendMessage(ChatColor.RED + "/cr edititem print");
		p.sendMessage(ChatColor.RED + "/cr edititem name [name]");
		p.sendMessage(ChatColor.RED + "/cr edititem itemname [name]");
		p.sendMessage(ChatColor.RED + "/cr edititem modeldata [int]");
		p.sendMessage(ChatColor.RED + "/cr edititem itemmodel namespace:string");
		p.sendMessage(ChatColor.RED + "/cr edititem enchant add/remove [enchantment] [level]");
		p.sendMessage(ChatColor.RED + "/cr edititem hideenchants [true/false]");
		p.sendMessage(ChatColor.RED + "/cr edititem lore");
		p.sendMessage(ChatColor.RED + "/cr edititem key [key])");
		p.sendMessage(ChatColor.RED + "/cr edititem undiscover [player] [NamespacedKey]");
		p.sendMessage(ChatColor.RED + "/cr edititem discover [player] [NamespacedKey]");
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));
		return true;
	}
}
