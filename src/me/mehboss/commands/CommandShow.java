package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;

public class CommandShow {

	static RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	public static boolean Run(CRCommand command) {
		CommandSender sender = command.sender;
		String[] args = command.args;

		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] Usage: /crecipe show <id>");
			return true;
		}

		String id = args[1].toLowerCase();
		// Check if recipe already exists
		Recipe existing = getRecipeUtil().getRecipeFromKey(id);
		if (existing == null) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] A recipe with the ID '" + id + "' could not be found.");
			return true;
		}

		Player p = (Player) sender;
		sender.sendMessage(ChatColor.RED + "[CustomRecipes] Showing recipe with ID '" + id + "'..");
		Main.getInstance().recipes.showCreationMenu(p, existing, false, false);
		return true;
	}
}