package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;

public class ExactChoice {
	RecipeManager recipeManager = Main.getInstance().recipeManager;
	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;

	RecipeChoice.ExactChoice findExactChoice(Recipe recipe, Ingredient ingredient) {

		if (!Main.getInstance().serverVersionAtLeast(1, 12)) {
			return null;
		}

		Ingredient item = ingredient;

		// if item is null, the converter will never be shaped or shapeless.
		if (item == null) {
			for (Ingredient ingredients : recipe.getIngredients()) {
				if (ingredients.isEmpty())
					continue;
				item = recipe.getSlot(ingredients.getSlot());
			}
		}

		if (item.hasIdentifier() && recipeUtil.getRecipeFromKey(item.getIdentifier()) != null) {
			return new RecipeChoice.ExactChoice(recipeUtil.getRecipeFromKey(item.getIdentifier()).getResult());

		} else if (item.hasIdentifier() && recipeUtil.getResultFromKey(item.getIdentifier()) != null) {
			return new RecipeChoice.ExactChoice(recipeUtil.getResultFromKey(item.getIdentifier()));

		} else {
			ItemStack exactItem = new ItemStack(item.getMaterial());
			ItemMeta exactMeta = exactItem.getItemMeta();

			if (item.hasDisplayName())
				exactMeta.setDisplayName(item.getDisplayName());

			if (item.hasCustomModelData())
				exactMeta.setCustomModelData(item.getCustomModelData());

			exactItem.setItemMeta(exactMeta);
			return new RecipeChoice.ExactChoice(exactItem);
		}
	}

	ShapelessRecipe createShapelessRecipe(Recipe recipe) {
		ShapelessRecipe shapelessRecipe = new ShapelessRecipe(recipeManager.createNamespacedKey(recipe),
				recipe.getResult());

		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			// Uses exactChoice if version is 1.14 or higher
			if (recipe.isExactChoice() && !recipe.getIgnoreData()) {
				shapelessRecipe.addIngredient(findExactChoice(recipe, ingredient));

				try {
					shapelessRecipe.setCategory(CraftingBookCategory.valueOf(recipe.getBookCategory()));
				} catch (NoClassDefFoundError e) {
				}
			} else {
				shapelessRecipe.addIngredient(ingredient.getMaterial());
			}
		}

		if (recipe.isExactChoice())
			logDebug("Created " + recipe.getType() + " recipe using the ExactChoice method.", recipe.getName());

		return shapelessRecipe;
	}

	ShapedRecipe createShapedRecipe(Recipe recipe) {
		ShapedRecipe shapedRecipe = new ShapedRecipe(recipeManager.createNamespacedKey(recipe), recipe.getResult());
		ArrayList<String> ingredients = new ArrayList<String>();

		shapedRecipe.shape(recipe.getRow(1), recipe.getRow(2), recipe.getRow(3));
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty() || ingredient.getMaterial() == Material.AIR
					|| ingredients.contains(ingredient.getAbbreviation()))
				continue;

			ingredients.add(ingredient.getAbbreviation());

			// Uses exactChoice if version is 1.14 or higher
			// Ignores if IgnoreData or IgnoreModelData is true
			if (recipe.isExactChoice() && !recipe.getIgnoreData()) {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), findExactChoice(recipe, ingredient));

				try {
					shapedRecipe.setCategory(CraftingBookCategory.valueOf(recipe.getBookCategory()));
				} catch (NoClassDefFoundError e) {
				}
			} else {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), ingredient.getMaterial());
			}
		}

		if (recipe.isExactChoice())
			logDebug("Created " + recipe.getType() + " recipe using the ExactChoice method.", recipe.getName());

		return shapedRecipe;
	}

	FurnaceRecipe createFurnaceRecipe(Recipe recipe) {

		FurnaceRecipe furnaceRecipe;

		if (Main.getInstance().serverVersionAtLeast(1, 14)) {
			try {
				furnaceRecipe = new FurnaceRecipe(recipeManager.createNamespacedKey(recipe), recipe.getResult(),
						findExactChoice(recipe, null), recipe.getExperience(), recipe.getCookTime());
				furnaceRecipe.setCategory(CookingBookCategory.valueOf(recipe.getBookCategory()));
			} catch (NoClassDefFoundError e) {
				furnaceRecipe = null;
			}
		} else {
			furnaceRecipe = new FurnaceRecipe(recipeManager.createNamespacedKey(recipe), recipe.getResult(),
					recipe.getSlot(1).getMaterial(), recipe.getExperience(), recipe.getCookTime());
		}

		return furnaceRecipe;
	}

	BlastingRecipe createBlastFurnaceRecipe(Recipe recipe) {

		if (!Main.getInstance().serverVersionAtLeast(1, 14)) {
			logError("Error loading recipe. Your server version does not support BlastFurnace recipes!",
					recipe.getName());
			return null;
		}

		BlastingRecipe blastRecipe = new BlastingRecipe(recipeManager.createNamespacedKey(recipe), recipe.getResult(),
				findExactChoice(recipe, null), recipe.getExperience(), recipe.getCookTime());

		try {
			blastRecipe.setCategory(CookingBookCategory.valueOf(recipe.getBookCategory()));
		} catch (NoClassDefFoundError e) {
		}

		return blastRecipe;
	}

	SmokingRecipe createSmokerRecipe(Recipe recipe) {
		if (!Main.getInstance().serverVersionAtLeast(1, 14)) {
			logError("Error loading recipe. Your server version does not support Smoker recipes!", recipe.getName());
			return null;
		}

		SmokingRecipe smokingRecipe = new SmokingRecipe(recipeManager.createNamespacedKey(recipe), recipe.getResult(),
				findExactChoice(recipe, null), recipe.getExperience(), recipe.getCookTime());
		try {
			smokingRecipe.setCategory(CookingBookCategory.valueOf(recipe.getBookCategory()));
		} catch (NoClassDefFoundError e) {
		}

		return smokingRecipe;
	}

	StonecuttingRecipe createStonecuttingRecipe(Recipe recipe) {
		if (!Main.getInstance().serverVersionAtLeast(1, 14)) {
			logError("Error loading recipe. Your server version does not support Stonecutting recipes!",
					recipe.getName());
			return null;
		}

		StonecuttingRecipe sRecipe = new StonecuttingRecipe(new NamespacedKey(Main.getInstance(), recipe.getKey()), recipe.getResult(),
				findExactChoice(recipe, null));
		sRecipe.setGroup(recipe.getGroup());
		
		return sRecipe;
	}
	
	CampfireRecipe createCampfireRecipe(Recipe recipe) {
		if (!Main.getInstance().serverVersionAtLeast(1, 14)) {
			logError("Error loading recipe. Your server version does not support Campfire recipes!",
					recipe.getName());
			return null;
		}

		return new CampfireRecipe(new NamespacedKey(Main.getInstance(), recipe.getKey()), recipe.getResult(),
				findExactChoice(recipe, null), recipe.getExperience(), recipe.getCookTime());
	}

	private void logError(String st, String recipe) {
		Logger.getLogger("Minecraft").log(Level.WARNING,
				"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "][EC] " + st);
	}

	private void logDebug(String st, String recipe) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "][EC] " + st);
	}
}
