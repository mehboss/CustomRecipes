package me.mehboss.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.libs.ItemManager;

public class TabCompletion implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			// Main subcommands
			List<String> subCommands = new ArrayList<>();

			if (sender.hasPermission("crecipe.debug")) {
				subCommands.add("debug");
				subCommands.add("crafterdebug");
			}
			if (sender.hasPermission("crecipe.list"))
				subCommands.add("list");
			if (sender.hasPermission("crecipe.items"))
				subCommands.add("items");
			if (sender.hasPermission("crecipe.give"))
				subCommands.add("give");
			if (sender.hasPermission("crecipe.reload"))
				subCommands.add("reload");
			if (sender.hasPermission("crecipe.gui"))
				subCommands.add("gui");
			if (sender.hasPermission("crecipe.book"))
				subCommands.add("book");
			if (sender.hasPermission("crecipe.help"))
				subCommands.add("help");
			if (sender.hasPermission("crecipe.edititem"))
				subCommands.add("edititem");
			if (sender.hasPermission("crecipe.show"))
				subCommands.add("show");
			if (sender.hasPermission("crecipe.remove"))
				subCommands.add("remove");
			if (sender.hasPermission("crecipe.create"))
				subCommands.add("create");

			StringUtil.copyPartialMatches(args[0], subCommands, completions);

		} else if (args[0].equalsIgnoreCase("give") && args.length == 2 && sender.hasPermission("crecipe.give")) {
			// /customrecipes give <player>
			StringUtil.copyPartialMatches(args[1], getOnlinePlayerNames(), completions);

		} else if (args[0].equalsIgnoreCase("give") && args.length == 3 && sender.hasPermission("crecipe.give")) {
			// /customrecipes give <player> <recipe>
			StringUtil.copyPartialMatches(args[2], getAllKeys(), completions);

		} else if (args[0].equalsIgnoreCase("edititem") && sender.hasPermission("crecipe.edititem")) {
			// /customrecipes edititem ...

			if (args.length == 2) {
				// Second arg = subcommand
				List<String> editItemSubs = new ArrayList<>();
				editItemSubs.add("name");
				editItemSubs.add("modeldata");
				editItemSubs.add("enchant");
				editItemSubs.add("hideenchants");
				editItemSubs.add("lore");
				editItemSubs.add("key");
				editItemSubs.add("undiscover");
				editItemSubs.add("discover");
				editItemSubs.add("print");

				StringUtil.copyPartialMatches(args[1], editItemSubs, completions);

			} else if (args.length == 3 && args[1].equalsIgnoreCase("enchant")) {
				List<String> candidates = new ArrayList<>();
				candidates.add("add");
				candidates.add("remove");
				StringUtil.copyPartialMatches(args[2], candidates, completions);

			} else if (args.length == 4 && args[1].equalsIgnoreCase("enchant")) {
				// suggest enchantments
				List<String> enchants = new ArrayList<>();
				for (Enchantment ench : Enchantment.values()) {
					if (ench != null && ench.getKey() != null) {
						enchants.add(ench.getKey().getKey());
					}
				}
				StringUtil.copyPartialMatches(args[3], enchants, completions);

			} else if (args.length == 3 && args[1].equalsIgnoreCase("hideenchants")) {
				completions.add("true");
				completions.add("false");
				StringUtil.copyPartialMatches(args[2], completions, completions);

			} else if ((args[1].equalsIgnoreCase("discover") || args[1].equalsIgnoreCase("undiscover"))) {
				if (args.length == 3) {
					// suggest players
					StringUtil.copyPartialMatches(args[2], getOnlinePlayerNames(), completions);
				} else if (args.length == 4) {
					// suggest NamespacedKeys (recipes)
					List<String> keys = new ArrayList<>();
					for (String key : Main.getInstance().recipeUtil.getAllRecipes().keySet()) {
						keys.add(key);
					}
					StringUtil.copyPartialMatches(args[3], keys, completions);
				}
			}
		} else if (args[0].equalsIgnoreCase("create") && sender.hasPermission("crecipe.create")) {
			// /crecipe create <type> <id> [permission]
			if (args.length == 2) {
				// Suggest types (includes your three new ones)
				StringUtil.copyPartialMatches(args[1], getRecipeTypes(), completions);
			}
			// Let the user type a brand-new id freely.
			else if (args.length == 4) {
				// Optional permission: suggest a sensible prefix
				List<String> perms = new ArrayList<>();
				perms.add("crecipe.recipe.");
				StringUtil.copyPartialMatches(args[3], perms, completions);
			}
		} else if (args[0].equalsIgnoreCase("remove") && sender.hasPermission("crecipe.remove")) {
			if (args.length == 2) {
				StringUtil.copyPartialMatches(args[1], getRecipes(), completions);
			}
		} else if (args[0].equalsIgnoreCase("show") && sender.hasPermission("crecipe.show")) {
			if (args.length == 2) {
				StringUtil.copyPartialMatches(args[1], getRecipes(), completions);
			}
		}

		return completions;
	}

	private List<String> getRecipeTypes() {
		List<String> types = new ArrayList<>();
		for (RecipeType type : RecipeType.values()) {
			types.add(type.toString());
		}
		return types;
	}

	private List<String> getOnlinePlayerNames() {
		List<String> playerNames = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			playerNames.add(player.getName());
		}
		return playerNames;
	}

	private Collection<String> getAllKeys() {
		Set<String> combined = new HashSet<>();
		combined.addAll(ItemManager.getAllItems());
		combined.addAll(Main.getInstance().recipeUtil.getAllKeys());
		return combined;
	}

	private Collection<String> getRecipes() {
		return Main.getInstance().recipeUtil.getAllKeys();
	}
}
