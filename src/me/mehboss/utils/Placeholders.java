package me.mehboss.utils;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.mehboss.recipe.Main;
import me.mehboss.recipe.RecipeAPI;
import me.mehboss.recipe.RecipeAPI.Ingredient;

public class Placeholders extends PlaceholderExpansion {

	Plugin plugin = Main.getInstance();

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().toString();
	}

	@Override
	public String getIdentifier() {
		return "customrecipes";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	/**
	 * This is the method called when a placeholder with our identifier is found and
	 * needs a value. <br>
	 * We specify the value identifier in this method. <br>
	 * Since version 2.9.1 can you use OfflinePlayers in your requests.
	 *
	 * @param player     A {@link org.bukkit.Player Player}.
	 * @param identifier A String containing the identifier/value.
	 *
	 * @return possibly-null String of the requested identifier.
	 */
	@Override
	public String onPlaceholderRequest(Player player, String identifier) {

		if (player == null) {
			return "";
		}

		// %someplugin_placeholder1%
		if (identifier.contains("_ingredient_")) {
			String[] split = identifier.split("_");
			String recipe = split[0];
			int ingredient = Integer.parseInt(split[2]) - 1;

			if (!Main.getInstance().configName.containsValue(recipe))
				return null;

			RecipeAPI api = Main.getInstance().api;
			ArrayList<Ingredient> ingredients = api.getIngredients(recipe);

			return ingredients.get(ingredient).getMaterial().toString();
		}

		if (identifier.contains("_ingredient_name_")) {
			String[] split = identifier.split("_");
			String recipe = split[0];
			int ingredient = Integer.parseInt(split[3]) - 1;

			if (!Main.getInstance().configName.containsValue(recipe))
				return null;

			RecipeAPI api = Main.getInstance().api;
			ArrayList<Ingredient> ingredients = api.getIngredients(recipe);

			return ingredients.get(ingredient).getDisplayName();
		}

		if (identifier.contains("_ingredient_amount_")) {
			String[] split = identifier.split("_");
			String recipe = split[0];
			int ingredient = Integer.parseInt(split[3]) - 1;

			if (!Main.getInstance().configName.containsValue(recipe))
				return null;

			RecipeAPI api = Main.getInstance().api;
			ArrayList<Ingredient> ingredients = api.getIngredients(recipe);

			return String.valueOf(ingredients.get(ingredient).getAmount());
		}

		// %someplugin_placeholder2%
		if (identifier.contains("_ingredient_identifier_")) {
			String[] split = identifier.split("_");
			String recipe = split[0];
			int ingredient = Integer.parseInt(split[3]) - 1;

			if (!Main.getInstance().configName.containsValue(recipe))
				return null;

			RecipeAPI api = Main.getInstance().api;
			ArrayList<Ingredient> ingredients = api.getIngredients(recipe);

			return String.valueOf(ingredients.get(ingredient).getIdentifier());
		}

		// We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
		// was provided
		return null;
	}
}
