package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveRecipe implements CommandExecutor {

	private Main plugin;

	public GiveRecipe(Main plugin) {
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
			if (!(p.hasPermission("crecipe.help"))) {
				sender.sendMessage(
						ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
				return true;
			}

			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&aCUSTOM-RECIPES &fv" + plugin.getDescription().getVersion()));
			p.sendMessage(" ");
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe &8-&f Displays this help page &e(crecipe.help)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe list &8-&f Gives a list of all current recipes &e(crecipe.list)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe give <player> <recipename> &8-&f Gives a player a custom recipe item &e(crecipe.give)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe gui &8-&f Opens the user-friendly menu for adding/editing &e(crecipe.gui)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe book &8-&f Opens a custom made recipe booklet &e(crecipe.book)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe reload &8-&f Reloads the configs and resets all recipes &e(crecipe.reload)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&c/crecipe debug &8-&f Enables debug mode for the author to troubleshoot &e(crecipe.debug)"));
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------"));
		}

		if (args.length > 0) {

			if (args.length == 1 && args[0].equalsIgnoreCase("book")) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("You must be a player in order to use this command!");
					return false;
				}

				if (!sender.hasPermission("crecipe.book")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return false;
				}

				Player player = (Player) sender;

				Main.getInstance().recipeBook.add(player.getUniqueId());
				Main.recipes.show(player);
				String OpenMessage = ChatColor.translateAlternateColorCodes('&', debug().getString("gui.Open-Message"));
				player.sendMessage(OpenMessage);

				try {
					player.playSound(player.getLocation(),
							Sound.valueOf(debug().getString("gui.Open-Sound").toUpperCase()), 1, 1);
				} catch (IllegalArgumentException e) {
				}
			}

			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("crecipe.reload")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return false;
				}

				sender.sendMessage(
						ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Reloading")));

				plugin.reload();
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Reload")));

				return false;
			}

			if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("You must be a player in order to use this command!");
					return false;
				}

				Player player = (Player) sender;

				if (!sender.hasPermission("crecipe.gui")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return false;
				}

				if (Main.getInstance().recipeBook.contains(player.getUniqueId()))
					Main.getInstance().recipeBook.remove(player.getUniqueId());
				
				player.sendMessage(ChatColor.RED + "GUI is currently undergoing maintenance. Come back soon!");
//				Main.recipes.show(p);
//				String OpenMessage = ChatColor.translateAlternateColorCodes('&', debug().getString("gui.Open-Message"));
//				p.sendMessage(OpenMessage);
//
//				try {
//					p.playSound(p.getLocation(), Sound.valueOf(debug().getString("gui.Open-Sound").toUpperCase()), 1,
//							1);
//				} catch (IllegalArgumentException e) {
//				}
				return true;
			}

			if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {

				if (!sender.hasPermission("crecipe.debug")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return false;
				}

				if (Main.getInstance().debug) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							"&c[CustomRecipes] &fDebug mode has been turned &cOFF."));
					debug().set("Debug", false);
					Main.getInstance().debug = false;
					Main.getInstance().saveConfig();
					Main.getInstance().reloadConfig();
					return true;
				}

				if (!(Main.getInstance().debug)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							"&c[CustomRecipes] &fDebug mode has been turned &aON.&f Check console for INFO."));
					debug().set("Debug", true);
					Main.getInstance().debug = true;
					Main.getInstance().saveConfig();
					Main.getInstance().reloadConfig();
					return true;
				}
			}

			if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
				if (!sender.hasPermission("crecipe.list")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return false;
				}
				int page = 1;
				if (args.length == 2) {
					try {
						page = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatColor.RED + "Invalid page number.");
						return false;
					}
				}

				List<String> recipes = new ArrayList<>(Main.getInstance().configName.values());

				// Sort the recipes alphabetically
				Collections.sort(recipes, String.CASE_INSENSITIVE_ORDER);

				int totalRecipes = recipes.size();
				int pageSize = 6;
				int totalPages = (int) Math.ceil((double) totalRecipes / pageSize);

				if (totalPages < 1) {
					sender.sendMessage(ChatColor.RED + "Could not find any active recipes.");
					return false;
				}

				if (page < 1 || page > totalPages) {
					sender.sendMessage(ChatColor.RED + "Invalid page number.");
					return false;
				}

				int startIndex = (page - 1) * pageSize;
				int endIndex = Math.min(startIndex + pageSize, totalRecipes);

				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&8-------------------------------------------------"));
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&cCurrent Recipes (Page " + page + "/" + totalPages + "):"));

				for (int i = startIndex; i < endIndex; i++) {
					String recipe = recipes.get(i);
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &f" + recipe));
				}

				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&8-------------------------------------------------"));
				return false;
			}

			if (args[0].equalsIgnoreCase("give")) {

				int amount = 1;

				if (!sender.hasPermission("crecipe.give")) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Perms")));
					return true;
				}

				if (args.length > 4 || args.length < 3) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Invalid-Args")));
					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);

				if (target == null) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							debug().getString("Messages.Player-Not-Found")));
					return true;
				}

				if (plugin.giveRecipe.get(args[2].toLowerCase()) == null) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							debug().getString("Messages.Recipe-Not-Found")));
					return true;
				}

				if (target.getInventory().firstEmpty() == -1) {
					sender.sendMessage(
							ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Inventory-Full")));
					return true;
				}

				if (args.length == 4 && isInt(args[3])) {
					amount = Integer.parseInt(args[3]);
				}

				ItemStack item = new ItemStack(plugin.giveRecipe.get(args[2].toLowerCase()));
				item.setAmount(amount);

				target.getInventory().addItem(item);

				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', debug().getString("Messages.Give-Recipe")
						.replaceAll("%itemname%", args[2]).replaceAll("%player%", target.getName())));
			}
		}
		return false;
	}

	public boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
