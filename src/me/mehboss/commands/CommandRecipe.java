package me.mehboss.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CommandRecipe {

	static RecipeUtil getRecipeUtil() {
	    return Main.getInstance().recipeUtil;
	}

	// Keep the method header as requested
	public static boolean Run(CRCommand command) {
		CommandSender sender = command.sender;
		String[] args = command.args;

		// Expected: /cr create <type> <id> [permission]
		if (args.length < 3) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] Usage: /crecipe create <type> <id> [permission]");
			sender.sendMessage(ChatColor.GRAY
					+ "Valid <type> values: SHAPELESS, SHAPED, STONECUTTER, FURNACE, ANVIL, BLASTFURNACE, SMOKER, CAMPFIRE, GRINDSTONE, BREWING_STAND");
			return true;
		}

		// Parse <type>
		String typeRaw = args[1];
		RecipeType type;
		try {
			type = RecipeType.valueOf(typeRaw.toUpperCase());
		} catch (IllegalArgumentException ex) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] Invalid recipe type: " + typeRaw);
			sender.sendMessage(ChatColor.GRAY
					+ "Valid types: SHAPELESS, SHAPED, STONECUTTER, FURNACE, ANVIL, BLASTFURNACE, SMOKER, CAMPFIRE, GRINDSTONE, BREWING_STAND");
			return true;
		}

		// Parse <id>
		String id = args[2].toLowerCase();

		// Validate ID format
		if (!id.matches("^[a-z0-9._\\-/]+$")) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] Invalid ID: " + id);
			sender.sendMessage(ChatColor.GRAY
					+ "IDs may only contain: lowercase letters, numbers, periods (.), underscores (_), hyphens (-), and forward slashes (/).");
			return true;
		}

		// Check if recipe already exists
		Recipe existing = getRecipeUtil().getRecipeFromKey(id);
		if (existing != null) {
			sender.sendMessage(ChatColor.RED + "[CustomRecipes] A recipe with the ID '" + id + "' already exists.");
			return true;
		}

		// Optional permission
		String permission = (args.length >= 4) ? args[3] : "none";

		sender.sendMessage(
				ChatColor.translateAlternateColorCodes('&', "&c[CustomRecipes] &fPreparing to create recipe:&r "));
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
				"&fType= &a" + type.toString() + " &fID= &a" + id + " &fPermission= &a" + permission));
		Main.getInstance().recipes.showCreationMenu(null, null, (Player) sender, id, permission, true, false, type);
		return true;
	}
}