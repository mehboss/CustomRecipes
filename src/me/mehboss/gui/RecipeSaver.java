package me.mehboss.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Recipe;

public class RecipeSaver {

	// Save the recipe map to the configuration file
	public void saveRecipe(Inventory inv, Player p, String recipeName) {

		File recipesFolder = new File(Main.getInstance().getDataFolder(), "recipes");

		// Ensure the 'recipes' folder exists
		if (!recipesFolder.exists()) {
			recipesFolder.mkdirs();
		}

		// Create the specific recipe YAML file (e.g., CursedPick.yml)
		File recipeFile = new File(recipesFolder, recipeName + ".yml");
		FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

		// Set the recipe data under a unique recipe name
		Map<String, Object> recipeData = convertToRecipeConfig(inv);
		recipeConfig.set(recipeName, recipeData);

		try {
			recipeConfig.save(recipeFile); // This step is required to persist changes!
			p.sendMessage("Recipe saved successfully to " + recipeFile.getName() + "!");
		} catch (IOException e) {
			e.printStackTrace();
			p.sendMessage("Failed to save the recipe to " + recipeFile.getName() + ".");
		}

		Main.getInstance().reload();
		p.closeInventory();

	}

	public Map<String, Object> convertToRecipeConfig(Inventory inventory) {
		Map<String, Object> recipeConfig = new HashMap<>();

		// General settings
		recipeConfig.put("Enabled", true);
		recipeConfig.put("Shapeless", false);
		recipeConfig.put("Cooldown", 60);
		// Get result item from slot 23
		ItemStack resultItem = inventory.getItem(23);
		if (resultItem != null && resultItem.getType() != Material.AIR) {
			recipeConfig.put("Identifier", inventory.getItem(7).getItemMeta().getLore().get(0));
		}

		recipeConfig.put("Placeable", true);
		recipeConfig.put("Ignore-Data", false);
		recipeConfig.put("Ignore-Model-Data", false);
		recipeConfig.put("Custom-Tagged", true);
		recipeConfig.put("Converter", "none");
		
		recipeConfig.put("Permission", "crecipe.recipe." + recipeConfig.get("Identifier"));
		recipeConfig.put("Custom-Tags", new ArrayList<>());
		recipeConfig.put("Item-Flags", new ArrayList<>());
		recipeConfig.put("Attribute", new ArrayList<>());
		recipeConfig.put("Custom-Model-Data", "none");
		recipeConfig.put("Disabled-Worlds", new ArrayList<>());

		if (resultItem != null && resultItem.getType() != Material.AIR) {
			recipeConfig.put("Item", resultItem.clone());
		}
		
		// Ingredients and crafting pattern
		Map<String, Object> ingredients = extractSerializedIngredients(inventory);
		Map<ItemStack, String> letters = setItemLetters(inventory);
		
		List<String> itemCrafting = generateItemCrafting(letters, inventory);
		recipeConfig.put("ItemCrafting", itemCrafting);
		recipeConfig.put("Ingredients", ingredients);

		return recipeConfig;
	}

	private Map<String, Object> extractSerializedIngredients(Inventory inventory) {
		int[] ingredientSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
		Map<String, Object> ingredients = new HashMap<>();
		Set<String> usedLetters = new HashSet<>();

		for (int i = 0; i < ingredientSlots.length; i++) {
			ItemStack item = inventory.getItem(ingredientSlots[i]);
			if (item != null && item.getType() != Material.AIR) {
				String letter = getUniqueLetter(usedLetters);
				usedLetters.add(letter);

				// Check if the item is a result of a recipe
				Recipe recipe = Main.getInstance().getRecipeUtil().getRecipeFromResult(item);
				if (recipe != null) {
					// Store only the identifier instead of the full ItemStack
					Map<String, Object> ref = new HashMap<>();
					ref.put("Identifier", recipe.getKey());
					ingredients.put(letter, ref);
				} else {
					ingredients.put(letter, item.clone());
				}
			}
		}

		return ingredients;
	}

	private Map<ItemStack, String> setItemLetters(Inventory inventory) {
		int[] ingredientSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
		Map<ItemStack, String> itemToLetterMap = new HashMap<>();
		Set<String> usedLetters = new HashSet<>();

		for (int i = 0; i < ingredientSlots.length; i++) {
			ItemStack current = inventory.getItem(ingredientSlots[i]);

			if (current != null && current.getType() != Material.AIR) {
				// Check if we've already seen an item similar to this one
				ItemStack matchingItem = null;
				for (ItemStack key : itemToLetterMap.keySet()) {
					if (key.isSimilar(current)) {
						matchingItem = key;
						break;
					}
				}

				if (matchingItem != null) {
					// Use the same letter
					itemToLetterMap.put(current, itemToLetterMap.get(matchingItem));
				} else {
					// New letter
					String letter = getUniqueLetter(usedLetters);
					usedLetters.add(letter);
					itemToLetterMap.put(current, letter);
				}
			}
		}
		return itemToLetterMap;
	}

	private String getUniqueLetter(Set<String> usedLetters) {
		String letter;
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			letter = String.valueOf(ch);
			if (!usedLetters.contains(letter)) {
				return letter;
			}
		}
		return "X"; // Default fallback if no letters are available
	}

	public List<String> generateItemCrafting(Map<ItemStack, String> ingredients, Inventory inv) {
		List<String> itemCrafting = new ArrayList<>(Arrays.asList("XXX", "XXX", "XXX"));
		int[] ingredientSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 }; // Mapping from inventory slots to crafting grid
																		// positions

		// Update the crafting pattern with the ingredient letters
		for (int i = 0; i < ingredientSlots.length; i++) {
			int slot = ingredientSlots[i];
			String letter = ingredients.get(inv.getItem(slot));
			
			if (letter == null)
				continue;

			// Determine the position in the 3x3 crafting grid
			int row = i / 3; // This determines which row (0, 1, or 2) we're on
			int col = i % 3; // This gives the correct column (0, 1, or 2) based on the remainder

			// Update the corresponding row in the crafting pattern
			String currentRow = itemCrafting.get(row);
			currentRow = currentRow.substring(0, col) + letter + currentRow.substring(col + 1);
			itemCrafting.set(row, currentRow);
		}

		return itemCrafting;
	}

	private String formatLore(String lore) {
		if (lore.isEmpty()) {
			return "[]";
		}
		return Arrays.toString(lore.split("\n"));
	}

	public String getItemName(ItemStack itemStack) {
		return (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
				? itemStack.getItemMeta().getDisplayName()
				: "none";
	}

	public int getItemAmount(ItemStack itemStack) {
		return (itemStack != null) ? itemStack.getAmount() : 1;
	}

	public String getItemLore(ItemStack itemStack) {
		return (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore())
				? String.join("\n", itemStack.getItemMeta().getLore())
				: "";
	}

	public String getCustomIdentifier(ItemStack itemStack) {
		if (itemStack != null && NBTEditor.contains(itemStack, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			return NBTEditor.getString(itemStack, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");
		}
		return "none";
	}
}