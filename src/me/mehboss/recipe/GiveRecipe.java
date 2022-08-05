package me.mehboss.recipe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveRecipe implements CommandExecutor {

	private Main plugin;

	public GiveRecipe(Main plugin) {
		this.plugin = plugin;
	}

	Boolean underDev = true;

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {

		if (cmd.getName().equalsIgnoreCase("crecipe")) {

			if (!(sender instanceof Player)) {
				sender.sendMessage("You must be a player in order to use this command!");
				return false;
			}

			Player p = (Player) sender;

			if (!(p.hasPermission("crecipe.admin"))) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						plugin.getConfig().getString("Messages.Invalid-Perms")));
			}

			if (args.length == 0) {

				Main.recipes.show(p);

				String OpenMessage = ChatColor.translateAlternateColorCodes('&',
						Bukkit.getPluginManager().getPlugin("CustomRecipes").getConfig().getString("GUI.Open-Message"));

				p.sendMessage(OpenMessage);
				p.playSound(p.getLocation(), Sound.valueOf(Bukkit.getPluginManager().getPlugin("CustomRecipes")
						.getConfig().getString("GUI.Open-Sound").toUpperCase()), 1, 1);

			}

			if (args.length > 0) {

				if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
					if (!sender.hasPermission("crecipe.reload")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								plugin.getConfig().getString("Messages.Invalid-Perms")));
						return false;
					}

					plugin.reload();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("Messages.Reload")));

					return false;
				}

				if (sender.hasPermission("crecipe.give")) {
					if (args.length != 3 || !(args[0].equalsIgnoreCase("give"))) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								plugin.getConfig().getString("Messages.Invalid-Args")));
						return false;
					}

					Player target = Bukkit.getPlayer(args[1]);

					if (target == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								plugin.getConfig().getString("Messages.Player-Not-Found")));
						return false;
					}

					if (plugin.giveRecipe.get(args[2].toLowerCase()) == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								plugin.getConfig().getString("Messages.Recipe-Not-Found")));
						return false;
					}

					if (target.getInventory().firstEmpty() == -1) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								plugin.getConfig().getString("Messages.Inventory-Full")));
						return false;
					}

					target.getInventory().addItem(plugin.giveRecipe.get(args[2].toLowerCase()));
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("Messages.Give-Recipe").replaceAll("%itemname%", args[2])
									.replaceAll("%player%", target.getName())));
				} else {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("Messages.Invalid-Perms")));
				}
			}
		}
		return false;
	}
}
