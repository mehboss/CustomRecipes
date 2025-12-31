package me.mehboss.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.ItemBuilder;

public class CommandGive {

	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public static boolean Run(CRCommand command) {

		Recipe recipe = Main.getInstance().getRecipeUtil().getRecipeFromKey(command.args[2]);
		CommandSender p = command.sender;
		int amount = 1;

		if (command.args.length > 4 || command.args.length < 3) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Invalid-Args")));
			return true;
		}

		Player target = Bukkit.getPlayer(command.args[1]);

		if (target == null) {
			p.sendMessage(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Player-Not-Found")));
			return true;
		}

		if (recipe == null && ItemBuilder.get(command.args[2]) == null) {
			p.sendMessage(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Recipe-Not-Found")));
			return true;
		}

		if (target.getInventory().firstEmpty() == -1) {
			p.sendMessage(
					ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Inventory-Full")));
			return true;
		}

		if (command.args.length == 4 && isInt(command.args[3])) {
			amount = Integer.parseInt(command.args[3]);
		}

		ItemStack item = null;

		// recipe id
		if (recipe != null)
			item = new ItemStack(recipe.getResult());
		// custom item id
		else
			item = new ItemStack(ItemBuilder.get(command.args[2]));

		item.setAmount(amount);

		target.getInventory().addItem(item);

		p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.Give-Recipe")
				.replaceAll("%itemname%", command.args[2]).replaceAll("%player%", target.getName())));
		return true;
	}

	public static boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
