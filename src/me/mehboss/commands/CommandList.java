package me.mehboss.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.mehboss.recipe.Main;

public class CommandList {
	public static boolean Run(CRCommand command) {
		CommandSender p = command.sender;
		int page = 1;

		if (command.args.length == 2) {
			try {
				page = Integer.parseInt(command.args[1]);
			} catch (NumberFormatException e) {
				p.sendMessage(ChatColor.RED + "Invalid page number.");
				return true;
			}
		}

		List<String> recipes = new ArrayList<>(Main.getInstance().getRecipeUtil().getRecipeNames());

		// Sort the recipes alphabetically
		Collections.sort(recipes, String.CASE_INSENSITIVE_ORDER);

		int totalRecipes = recipes.size();
		int pageSize = 6;
		int totalPages = (int) Math.ceil((double) totalRecipes / pageSize);

		if (totalPages < 1) {
			p.sendMessage(ChatColor.RED + "Could not find any active recipes.");
			return false;
		}

		if (page < 1 || page > totalPages) {
			p.sendMessage(ChatColor.RED + "Invalid page number.");
			return false;
		}

		int startIndex = (page - 1) * pageSize;
		int endIndex = Math.min(startIndex + pageSize, totalRecipes);

		p.sendMessage(
				ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------------------"));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&cCurrent Recipes (Page " + page + "/" + totalPages + "):"));

		for (int i = startIndex; i < endIndex; i++) {
			String recipe = recipes.get(i);
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &f" + recipe));
		}

		p.sendMessage(
				ChatColor.translateAlternateColorCodes('&', "&8-------------------------------------------------"));
		return true;

	}
}
