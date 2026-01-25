package me.mehboss.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.libs.ItemManager;

public class CommandGive {

	static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public static boolean Run(CRCommand command) {

		CommandSender p = command.sender;
		int amount = 1;

		if (command.args.length > 4 || command.args.length < 3) {
			p.sendMessage(getMessage("Messages.Invalid-Args",
					"&cInvalid args! (use: /crecipe give <player> <recipename> [amount])"));
			return true;
		}

		Player target = Bukkit.getPlayer(command.args[1]);

		if (target == null) {
			p.sendMessage(getMessage("Messages.Player-Not-Found", "&cERROR: player not found!"));
			return true;
		}

		Recipe recipe = Main.getInstance().getRecipeUtil().getRecipeFromKey(command.args[2]);
		ItemStack item = recipe == null ? ItemManager.get(command.args[2]) : recipe.getResult();

		if (recipe == null && item == null) {
			p.sendMessage(getMessage("Messages.Recipe-Not-Found", "&cERROR: recipe not found!"));
			return true;
		}

		if (target.getInventory().firstEmpty() == -1) {
			p.sendMessage(getMessage("Messages.Inventory-Full", "&cERROR: target''s inventory is full!"));
			return true;
		}

		if (recipe != null && !recipe.hasResult()) {
			p.sendMessage(getMessage("Messages.No-Result", "&cThis recipe type does not require a result!"));
			return true;
		}

		// amounts
		if (command.args.length == 4 && isInt(command.args[3]))
			amount = Integer.parseInt(command.args[3]);

		item.setAmount(amount);
		target.getInventory().addItem(item);

		p.sendMessage(getMessage("Messages.Give-Recipe", "&aSuccessfully given player item!")
				.replaceAll("%itemname%", command.args[2]).replaceAll("%player%", target.getName()));
		return true;
	}

	private static String getMessage(String string, String fallback) {
		return ChatColor.translateAlternateColorCodes('&', getConfig().getString(string, fallback));
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
