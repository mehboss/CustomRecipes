package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class TabCompletion implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			// Tab complete for /customrecipes debug
			// Tab complete for /customrecipes list
			// Tab complete for /customrecipes give
			// Tab complete for /customrecipes reload
			// Tab complete for /customrecipes gui
			List<String> subCommands = new ArrayList<>();

			if (sender.hasPermission("crecipe.debug"))
				subCommands.add("debug");
			if (sender.hasPermission("crecipe.list"))
				subCommands.add("list");
			if (sender.hasPermission("crecipe.give"))
				subCommands.add("give");
			if (sender.hasPermission("crecipe.reload"))
				subCommands.add("reload");
			if (sender.hasPermission("crecipe.gui"))
				subCommands.add("gui");
			if (sender.hasPermission("crecipe.book"))
				subCommands.add("book");

			StringUtil.copyPartialMatches(args[0], subCommands, completions);
		} else if (args.length > 1 && !args[0].equals("give") && sender.hasPermission("crecipe.give")) {
			return completions;

		} else if (args.length == 2 && sender.hasPermission("crecipe.give")) {
			// Tab complete for /customrecipes give <player>

			List<String> playerNames = getOnlinePlayerNames();
			StringUtil.copyPartialMatches(args[1], playerNames, completions);

		} else if (args.length == 3 && sender.hasPermission("crecipe.give")) {
			// Tab complete for /customrecipes give <player> <recipelist>
			Collection<String> recipeNames = getRecipes();

			StringUtil.copyPartialMatches(args[2], recipeNames, completions);
		}

		return completions;
	}

	private List<String> getOnlinePlayerNames() {
		List<String> playerNames = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			playerNames.add(player.getName());
		}
		return playerNames;
	}

	private Collection<String> getRecipes() {
		return Main.getInstance().itemNames.keySet();
	}
}
