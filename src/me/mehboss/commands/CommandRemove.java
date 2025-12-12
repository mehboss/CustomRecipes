package me.mehboss.commands;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;

public class CommandRemove {

	static RecipeUtil getRecipeUtil() {
	    return Main.getInstance().recipeUtil;
	}

	public static boolean Run(CRCommand command) {
		CommandSender sender = command.sender;
		String[] args = command.args;

		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] Usage: /crecipe remove <id>");
			return true;
		}
		
		String id = args[1].toLowerCase();
		Recipe existing = getRecipeUtil().getRecipeFromKey(id);
		if (existing == null) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] A recipe with the ID '" + id + "' could not be found.");
			return true;
		}
		
		File recipeFile = new File(Main.getInstance().getDataFolder(), "recipes/" + existing.getName() + ".yml");
		if (recipeFile.exists()) {
			if (recipeFile.delete()) {
				removeRecipe(existing.getName());
				sender.sendMessage(ChatColor.GREEN + "[CustomRecipes] Recipe '" + id + "' was successfully deleted.");
			} else {
				sender.sendMessage(ChatColor.RED + "[CustomRecipes] Failed to delete recipe '" + id + "'.");
			}
		}
		return true;
	}
	
	public static void removeRecipe(String name) {
		getRecipeUtil().removeRecipe(name);
	}
	
}