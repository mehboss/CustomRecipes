package me.mehboss.recipe;
/*
 * Mozilla Public License v2.0
 * 
 * Author: Mehboss
 * Copyright (c) 2023 Mehboss
 * Spigot: https://www.spigotmc.org/resources/authors/mehboss.139036/
 *
 * DO NOT REMOVE THIS SECTION IF YOU WISH TO UTILIZE ANY PORTIONS OF THIS CODE
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.BrewingRecipeData;
import me.mehboss.utils.data.CookingRecipeData;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.data.SmithingRecipeData;
import me.mehboss.utils.data.SmithingRecipeData.SmithingRecipeType;
import me.mehboss.utils.data.WorkstationRecipeData;
import me.mehboss.utils.libs.ItemFactory;
import me.mehboss.utils.libs.RecipeConditions;

public class RecipeBuilder {

	List<String> delayedRecipes = new ArrayList<>();
	boolean allFinished = false;
	FileConfiguration recipeConfig = null;

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ItemFactory getItemFactory() {
		return Main.getInstance().itemFactory;
	}

	FileConfiguration getConfig() {
		return recipeConfig;
	}

	void handleCommand(String item, Recipe recipe) {
		String path = item + ".Commands.Run-Commands";
		String grantItem = item + ".Commands.Give-Item";

		// Check if the list exists and is not empty
		if (!getConfig().isSet(path) || getConfig().getStringList(path).isEmpty())
			return;

		if (getConfig().isSet(grantItem))
			recipe.setGrantItem(getConfig().getBoolean(grantItem));

		List<String> commands = getConfig().getStringList(path);
		recipe.setCommands(commands);
		logDebug("Successfully set commands: " + commands, item);
	}

	void handleIdentifier(String item, Recipe recipe) {
		boolean isTagged = getConfig().getBoolean(item + ".Custom-Tagged")
				|| getConfig().getBoolean(item + ".custom-tagged");
		recipe.setTagged(isTagged);
	}

	void handleRecipeFlags(String item, Recipe recipe) {
		if (getConfig().isBoolean(item + ".Enabled"))
			recipe.setActive(getConfig().getBoolean(item + ".Enabled"));

		if (getConfig().isString(item + ".Permission"))
			recipe.setPerm(getConfig().getString(item + ".Permission"));

		if (getConfig().isBoolean(item + ".Uses-ID"))
			recipe.setUsesID(getConfig().getBoolean(item + ".Uses-ID"));

		if (getConfig().isInt(item + ".Cooldown") && getConfig().getInt(item + ".Cooldown") != -1)
			recipe.setCooldown(getConfig().getInt(item + ".Cooldown"));

		if (getConfig().isBoolean(item + ".Placeable"))
			recipe.setPlaceable(getConfig().getBoolean(item + ".Placeable"));

		if (getConfig().isSet(item + ".Disabled-Worlds")) {
			for (String world : getConfig().getStringList(item + ".Disabled-Worlds"))
				recipe.addDisabledWorld(world);
		}

		recipe.setIgnoreData(getConfig().getBoolean(item + ".Flags.Ignore-Data"));
		recipe.setIgnoreNames(getConfig().getBoolean(item + ".Flags.Ignore-Name"));
		recipe.setIgnoreModelData(getConfig().getBoolean(item + ".Flags.Ignore-Model-Data"));
		recipe.setIgnoreItemModel(getConfig().getBoolean(item + ".Flags.Ignore-Item-Model"));
		recipe.setIgnoreLore(getConfig().getBoolean(item + ".Flags.Ignore-Lore"));
		recipe.setExactChoice(getConfig().getBoolean(item + ".Exact-Choice"));
		recipe.setDiscoverable(getConfig().getBoolean(item + ".Auto-Discover-Recipe"));

		if (getConfig().isSet(item + ".Book-Category")) {
			try {
				recipe.setBookCategory(getConfig().getString(item + ".Book-Category").toUpperCase());
			} catch (NoClassDefFoundError e) {
			}
		}
	}

	void handleCraftingData(Recipe recipe, String configPath) {
		CraftingRecipeData workbench = (CraftingRecipeData) recipe;
		if (getConfig().getBoolean(configPath + ".Use-Conditions")) {
			ConfigurationSection rSec = getConfig().getConfigurationSection(configPath);
			workbench.setConditionSet(RecipeConditions.parseConditionSet(rSec));
		}

		if (getConfig().isSet(configPath + ".ItemsLeftover"))
			for (String leftOver : getConfig().getStringList(configPath + ".ItemsLeftover")) {
				if (leftOver.equalsIgnoreCase("none"))
					return;
				workbench.addLeftoverItem(leftOver);
			}

		if (Main.getInstance().serverVersionAtLeast(1, 13))
			if (getConfig().isSet(configPath + ".Group")
					&& !getConfig().getString(configPath + ".Group").equalsIgnoreCase("none")) {
				try {
					workbench.setGroup(getConfig().getString(configPath + ".Group"));
				} catch (NoClassDefFoundError e) {
				}
			}
	}

	void handleFurnaceData(Recipe recipe, String configPath) {
		CookingRecipeData furnace = (CookingRecipeData) recipe;
		if (getConfig().isInt(configPath + ".Cook-Time"))
			furnace.setCookTime(getConfig().getInt(configPath + ".Cook-Time"));
		if (getConfig().isDouble(configPath + ".Experience")) {
			double experience = getConfig().getDouble(configPath + ".Experience");
			furnace.setExperience((float) experience);
		}
	}

	void handleAnvilData(Recipe recipe, String configPath) {
		WorkstationRecipeData anvil = (WorkstationRecipeData) recipe;
		if (getConfig().isInt(configPath + ".Repair-Cost"))
			anvil.setRepairCost(getConfig().getInt(configPath + ".Repair-Cost"));
	}

	void handleGrindstoneData(Recipe recipe, String configPath) {
		WorkstationRecipeData grindstone = (WorkstationRecipeData) recipe;
		if (getConfig().isDouble(configPath + ".Experience")) {
			double experience = getConfig().getDouble(configPath + ".Experience");
			grindstone.setExperience((float) experience);
		}
	}

	void handleStonecutterData(Recipe recipe, String configPath) {
		WorkstationRecipeData stonecutter = (WorkstationRecipeData) recipe;
		if (getConfig().isSet(configPath + ".Group")
				&& !getConfig().getString(configPath + ".Group").equalsIgnoreCase("none"))
			stonecutter.setGroup(getConfig().getString(configPath + ".Group"));
	}

	void handleBrewingData(Recipe recipe, String configPath) {
		BrewingRecipeData brew = (BrewingRecipeData) recipe;
		String newPath = (configPath + ".Required-Item");

		if (getConfig().getBoolean(configPath + ".Requires-Items")
				&& getConfig().getConfigurationSection(configPath + ".Required-Item") != null) {
			ItemStack potionItem = getItemFactory().handlePotions(newPath);
			brew.setRequiredBottleType(potionItem.getType());
			brew.setRequiresBottles(true);
		}

		brew.setBrewPerfect(getConfig().getBoolean(configPath + ".Exact-Choice", true));

		if (getConfig().isSet(configPath + ".FuelSet")) {
			brew.setBrewFuelSet(getConfig().getInt(configPath + ".FuelSet"));
		}

		if (getConfig().isSet(configPath + ".FuelCharge")) {
			brew.setBrewFuelCharge(getConfig().getInt(configPath + ".FuelCharge"));
		}
	}

	void handleSmithingData(Recipe recipe, String configPath) {
		SmithingRecipeData smithing = (SmithingRecipeData) recipe;

		SmithingRecipeType type = SmithingRecipeType.fromString(getConfig().getString(configPath + ".Type"));
		smithing.setSmithingType(type);
		boolean copyTrim = getConfig().getBoolean(configPath + ".Copy-Trim");
		smithing.setCopyTrim(copyTrim);
		boolean copyEnchants = getConfig().getBoolean(configPath + ".Copy-Enchants");
		smithing.setCopyEnchants(copyEnchants);
	}

	void checkIdentifiers() {
		for (Recipe recipe : new ArrayList<>(getRecipeUtil().getAllRecipes().values())) {
			for (Ingredient ingredient : recipe.getIngredients()) {
				if (!ingredient.hasIdentifier())
					continue;

				// recipe name hyphen-argument is strictly for debugging, and is needed for
				// attaching a recipe during getResultFromKey()
				ItemStack matchedItem = getRecipeUtil()
						.getResultFromKey(ingredient.getIdentifier() + ":" + recipe.getName());
				if (matchedItem == null) {
					logError("Please double check the IDs of the ingredients matches that of a custom item or recipe.",
							recipe.getName());
					logError("Skipping recipe..", recipe.getName());
					getRecipeUtil().removeRecipe(recipe.getName());
					break;
				} else {
					if (ingredient.getMaterial() != matchedItem.getType()) {
						ingredient.setMaterial(matchedItem.getType());
					}
				}
			}
		}
	}

	ItemStack buildItem(Ingredient ingredient) {
		ItemStack item = null;

		if (ingredient.hasIdentifier()) {
			item = Main.getInstance().getRecipeUtil().getResultFromKey(ingredient.getIdentifier());
		} else if (ingredient.hasItem()) {
			ingredient.getItem();
		}

		if (item == null) {
			item = new ItemStack(ingredient.getMaterial());
			ItemMeta meta = item.getItemMeta();

			if (ingredient.hasDisplayName())
				meta.setDisplayName(ingredient.getDisplayName());

			if (ingredient.hasCustomModelData())
				meta.setCustomModelData(ingredient.getCustomModelData());

			item.setItemMeta(meta);
		}
		return item;
	}

	void setBrewingItems(BrewingRecipeData recipe) {
		recipe.setBrewIngredient(buildItem(recipe.getSlot(1)));
		recipe.setBrewFuel(buildItem(recipe.getSlot(2)));
	}

	void setFurnaceSource(CookingRecipeData recipe) {
		recipe.setFurnaceSource(buildItem(recipe.getSlot(1)));
	}

	void setSmithingItems(SmithingRecipeData recipe) {
		recipe.setTemplate(buildItem(recipe.getSlot(1)));
		recipe.setBase(buildItem(recipe.getSlot(2)));
		recipe.setAddition(buildItem(recipe.getSlot(3)));
		recipe.setTrimPattern(buildItem(recipe.getSlot(4)));
	}

	Optional<ItemStack> handleResult(String resultPath, Recipe recipe, ArrayList<String> keys) {
		// Checks for a custom item and attempts to set it
		String rawItem = getConfig().getString(resultPath + ".Item") == null
				? getConfig().getString(resultPath + ".material")
				: getConfig().getString(resultPath + ".Item");

		if (rawItem == null) {
			if (recipe.requiresResult()) {
				logError("Error loading recipe..", recipe.getName());
				logError("The 'Item' section is missing or improperly formatted! Skipping..", recipe.getName());
			}
			return Optional.empty();
		}

		// Attach recipe name ONLY for debug purposes
		ItemStack i = getRecipeUtil().getResultFromKey(rawItem + ":" + recipe.getName());
		if (i == null) {
			if (keys.contains(rawItem)) {
				// found custom item, but recipe isn't active yet so it must be added last.
				delayedRecipes.add(recipe.getName());
				return Optional.empty();
			}

			ItemStack stackFromConfig = getConfig().getItemStack(resultPath + ".Item");
			if (stackFromConfig != null) {
				// handle itemstack checks
				i = stackFromConfig;
			} else {
				// handle material checks
				Optional<ItemStack> built = getItemFactory().buildItem(resultPath, getConfig());
				if (!built.isPresent())
					return Optional.empty();

				i = built.get();
			}
		} else {
			// handle custom item stacks
			recipe.setCustomItem(rawItem);
		}

		i = getItemFactory().handleDurability(i, resultPath);

		int amount = getConfig().getInt(resultPath + ".Amount", i.getAmount());
		i.setAmount(amount);
		return Optional.of(i);
	}

	public ArrayList<String> validateKeys(File[] recipeFiles) {
		ArrayList<String> keys = new ArrayList<String>();
		for (File recipeFile : recipeFiles) {
			FileConfiguration recipe = YamlConfiguration.loadConfiguration(recipeFile);
			String item = recipeFile.getName().replace(".yml", "");

			if (!recipe.isConfigurationSection(item))
				continue;
			if (recipe.isString(item + ".Identifier"))
				keys.add(recipe.getString(item + ".Identifier"));
		}
		return keys;
	}

	public void addRecipes(String name) {
		File recipeFolder = new File(Main.getInstance().getDataFolder(), "recipes");
		if (!recipeFolder.exists()) {
			recipeFolder.mkdirs();
		}

		File[] recipeFiles = recipeFolder.listFiles();
		if (recipeFiles == null) {
			logError("Could not add recipes because none were found to load!", "");
			return;
		}

		if (name != null) {
			if (!name.endsWith(".yml")) {
				name = name + ".yml";
			}

			File single = new File(recipeFolder, name);
			if (single.exists() && single.isFile()) {
				recipeFiles = new File[] { single };
			} else {
				logError("Recipe file " + name + " not found!", name);
				return;
			}
		}

		ArrayList<String> keys = validateKeys(recipeFiles);
		recipeLoop: for (File recipeFile : recipeFiles) {
			recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
			String item = recipeFile.getName().replace(".yml", "");

			if (!(recipeConfig.isConfigurationSection(item))) {
				logError("Could not find configuration section " + item
						+ " in the recipe file that must match its filename: " + item
						+ ".yml - (CaSeSeNsItIvE) - Skipping this recipe", item);
				continue;
			}

			Recipe recipe;

			List<String> gridRows = getConfig().getStringList(item + ".ItemCrafting");
			String converter = getConfig().isString(item + ".Converter")
					? getConfig().getString(item + ".Converter").toLowerCase()
					: "converterNotDefined";
			int amountRequirement = 9;

			logDebug("Attempting to add recipe..", item);

			if (!getConfig().isConfigurationSection(item + ".Ingredients")) {
				logError("Error loading recipe..", item);
				logError("Could not locate the ingredients section. Please double check formatting. Skipping recipe..",
						item);
				continue;
			}

			RecipeType type = RecipeType.fromString(converter);
			switch (type) {
			case STONECUTTER:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleStonecutterData(recipe, item);
				amountRequirement = 1;
				break;
			case FURNACE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case BLASTFURNACE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case SMOKER:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 2;
				break;
			case CAMPFIRE:
				recipe = new CookingRecipeData(item);
				recipe.setType(type);
				handleFurnaceData(recipe, item);
				amountRequirement = 1;
				break;
			case ANVIL:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleAnvilData(recipe, item);
				amountRequirement = 2;
				break;
			case GRINDSTONE:
				recipe = new WorkstationRecipeData(item);
				recipe.setType(type);
				handleGrindstoneData(recipe, item);
				amountRequirement = 2;
				break;
			case BREWING_STAND:
				recipe = new BrewingRecipeData(item);
				recipe.setType(type);
				handleBrewingData(recipe, item);
				amountRequirement = 2;
				break;
			case SMITHING:
				recipe = new SmithingRecipeData(item);
				recipe.setType(type);
				handleSmithingData(recipe, item);
				amountRequirement = 4;
				break;
			default:
				recipe = new CraftingRecipeData(item);
				recipe.setType(getConfig().getBoolean(item + ".Shapeless") ? RecipeType.SHAPELESS : RecipeType.SHAPED);
				handleCraftingData(recipe, item);
				break;
			}

			// Checks version req. of recipe types
			if (!isVersionSupported(recipe.getType(), item))
				continue;

			if (recipe.getType() != RecipeType.SHAPED && recipe.getType() != RecipeType.SHAPELESS) {
				if (name == null) {
					delayedRecipes.add(recipe.getName());
					continue;
				}
			}

			String resultPath = getConfig().isConfigurationSection(item + ".Result") ? item + ".Result" : item;
			String identifier = getConfig().getString(item + ".Identifier");
			recipe.setKey(identifier);

			Optional<ItemStack> result = handleResult(resultPath, recipe, keys);
			if (result.isPresent()) {
				recipe.setResult(result.get());
			} else if (recipe.getType() != RecipeType.SMITHING) {
				continue;
			}

			handleIdentifier(resultPath, recipe);
			handleRecipeFlags(item, recipe);
			handleCommand(item, recipe);

			ArrayList<String> slotsAbbreviations = new ArrayList<String>();
			String row1 = gridRows.get(0);
			String row2 = gridRows.get(1);
			String row3 = gridRows.get(2);

			slotsAbbreviations.add(row1.split("")[0]);
			slotsAbbreviations.add(row1.split("")[1]);
			slotsAbbreviations.add(row1.split("")[2]);

			slotsAbbreviations.add(row2.split("")[0]);
			slotsAbbreviations.add(row2.split("")[1]);
			slotsAbbreviations.add(row2.split("")[2]);

			slotsAbbreviations.add(row3.split("")[0]);
			slotsAbbreviations.add(row3.split("")[1]);
			slotsAbbreviations.add(row3.split("")[2]);

			recipe.setRow(1, row1);
			recipe.setRow(2, row2);
			recipe.setRow(3, row3);

			int slot = 0;
			int count = 0;

			for (String abbreviation : slotsAbbreviations) {
				// Iterate through the specified 9x9 grid and get it from the Ingredients
				// section..
				slot++;

				RecipeUtil.Ingredient recipeIngredient;

				// adds the abbreviation for AIR automatically to the recipe
				if (abbreviation.equalsIgnoreCase("X")) {
					recipeIngredient = new RecipeUtil.Ingredient("X", Material.AIR);
					recipeIngredient.setSlot(slot);
					recipe.addIngredient(recipeIngredient);
					continue;
				}

				count++;
				if (count > amountRequirement) {
					logError("Error loading recipe..", recipe.getName());
					logError(
							"Found " + count + " slots but converter is " + converter + " so use only "
									+ amountRequirement + " slot(s) (X for others) for 'ItemCrafting'",
							recipe.getName());
					continue recipeLoop;
				}

				// Try to deserialize using XItemstack (Deserializer)
				Optional<ItemStack> deserializedItem = getItemFactory().deserializeItemFromPath(recipeConfig,
						item + ".Ingredients." + abbreviation);
				if (deserializedItem.isPresent()) {
					ItemStack stack = deserializedItem.get();
					recipeIngredient = new RecipeUtil.Ingredient(abbreviation, stack.getType());
					recipeIngredient.setItem(stack);
					logDebug("Loading ingredient '" + abbreviation + "' from ItemStack..", recipe.getName());
				} else {
					recipeIngredient = getItemFactory().deserializeItemFromConfig(recipeConfig, recipe, item,
							abbreviation);
					if (recipeIngredient == null) {
						continue recipeLoop;
					}
					logDebug("Loading ingredient '" + abbreviation + "' from configuration settings..",
							recipe.getName());
				}

				recipeIngredient.setSlot(slot);
				recipe.addIngredient(recipeIngredient);

				if (count == amountRequirement)
					break;
			}

			int amount = recipe.hasResult() ? recipe.getResult().getAmount() : 0;
			logDebug("Recipe Type: " + recipe.getType(), recipe.getName());
			logDebug("Successfully added " + item + " with the amount output of " + amount, recipe.getName());

			getRecipeUtil().createRecipe(recipe);
			if (delayedRecipes.isEmpty() && name != null) {
				getRecipeUtil().registerRecipe(recipe);
				return;
			}
		}

		if (!delayedRecipes.isEmpty() && name == null) {
			for (String recipe : delayedRecipes)
				addRecipes(recipe);
			delayedRecipes.clear();
		}

		if (delayedRecipes.isEmpty()) {
			getRecipeUtil().reloadRecipes();
		}
	}

	public void addRecipesFromAPI(Recipe specificRecipe) {
		RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
		HashMap<String, Recipe> recipeList = recipeUtil.getAllRecipes() == null ? null
				: new HashMap<>(recipeUtil.getAllRecipes());

		if (recipeList == null || recipeList.isEmpty()) {
			logError("No recipes were found to load..", "");
			return;
		}
		if (specificRecipe != null) {
			recipeList.clear();
			recipeList.put(specificRecipe.getName(), specificRecipe);
		}

		for (Recipe recipe : recipeList.values()) {
			try {
				ShapedRecipe shapedRecipe = null;
				ShapelessRecipe shapelessRecipe = null;
				FurnaceRecipe furnaceRecipe = null;
				StonecuttingRecipe sCutterRecipe = null;
				BlastingRecipe blastRecipe = null;
				SmokingRecipe smokerRecipe = null;
				CampfireRecipe campfireRecipe = null;
				SmithingRecipe smithingRecipe = null;

				// Create recipes based on type
				switch (recipe.getType()) {
				case SHAPELESS:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						shapelessRecipe = Main.getInstance().exactChoice
								.createShapelessRecipe((CraftingRecipeData) recipe);
						break;
					}

					shapelessRecipe = createShapelessRecipe((CraftingRecipeData) recipe);
					break;

				case SHAPED:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						shapedRecipe = Main.getInstance().exactChoice.createShapedRecipe((CraftingRecipeData) recipe);
						break;
					}

					shapedRecipe = createShapedRecipe((CraftingRecipeData) recipe);
					break;

				case FURNACE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						furnaceRecipe = Main.getInstance().exactChoice.createFurnaceRecipe((CookingRecipeData) recipe);
						break;
					}

					furnaceRecipe = createFurnaceRecipe((CookingRecipeData) recipe);
					break;

				case BLASTFURNACE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						blastRecipe = Main.getInstance().exactChoice
								.createBlastFurnaceRecipe((CookingRecipeData) recipe);
					}
					break;

				case SMOKER:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						smokerRecipe = Main.getInstance().exactChoice.createSmokerRecipe((CookingRecipeData) recipe);
					}
					break;

				case STONECUTTER:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						sCutterRecipe = Main.getInstance().exactChoice
								.createStonecuttingRecipe((WorkstationRecipeData) recipe);
					}
					break;

				case CAMPFIRE:
					if (Main.getInstance().serverVersionAtLeast(1, 13, 2)) {
						campfireRecipe = Main.getInstance().exactChoice
								.createCampfireRecipe((CookingRecipeData) recipe);
					}
					break;

				// Manual brewing events, since no official BrewingRecipe exists.
				case BREWING_STAND:
					setBrewingItems((BrewingRecipeData) recipe);
					break;
				default:
					break;

				case SMITHING:
					if (Main.getInstance().serverVersionAtLeast(1, 20)) {
						smithingRecipe = Main.getInstance().exactChoice
								.createSmithingRecipe((SmithingRecipeData) recipe);
					}
				}

				// Register recipes with the server
				if (shapedRecipe != null)
					Bukkit.getServer().addRecipe(shapedRecipe);
				if (shapelessRecipe != null)
					Bukkit.getServer().addRecipe(shapelessRecipe);
				if (furnaceRecipe != null)
					Bukkit.getServer().addRecipe(furnaceRecipe);
				if (blastRecipe != null)
					Bukkit.getServer().addRecipe(blastRecipe);
				if (sCutterRecipe != null)
					Bukkit.getServer().addRecipe(sCutterRecipe);
				if (smokerRecipe != null)
					Bukkit.getServer().addRecipe(smokerRecipe);
				if (campfireRecipe != null)
					Bukkit.getServer().addRecipe(campfireRecipe);
				if (smithingRecipe != null)
					Bukkit.getServer().addRecipe(smithingRecipe);

			} catch (Exception e) {
				Main.getInstance().getLogger().log(Level.SEVERE, "Error loading recipe: " + e.getMessage(), e);
			}
		}

		checkIdentifiers();
	}

	@SuppressWarnings("deprecation")
	private ShapelessRecipe createShapelessRecipe(CraftingRecipeData recipe) {
		ShapelessRecipe shapelessRecipe;

		if (Main.getInstance().serverVersionAtLeast(1, 12)) {
			shapelessRecipe = new ShapelessRecipe(createNamespacedKey(recipe), recipe.getResult());
		} else {
			shapelessRecipe = new ShapelessRecipe(recipe.getResult());
		}

		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			// Added material data for legacy minecraft versions
			if (ingredient.hasMaterialData()) {
				shapelessRecipe.addIngredient(ingredient.getMaterialData());
			} else {
				shapelessRecipe.addIngredient(ingredient.getMaterial());
			}
		}

		return shapelessRecipe;
	}

	@SuppressWarnings("deprecation")
	private ShapedRecipe createShapedRecipe(CraftingRecipeData recipe) {
		ShapedRecipe shapedRecipe;

		ArrayList<String> ingredients = new ArrayList<String>();

		if (Main.getInstance().serverVersionAtLeast(1, 12)) {
			shapedRecipe = new ShapedRecipe(createNamespacedKey(recipe), recipe.getResult());
		} else {
			shapedRecipe = new ShapedRecipe(recipe.getResult());
		}

		shapedRecipe.shape(recipe.getRow(1), recipe.getRow(2), recipe.getRow(3));
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty() || ingredient.getMaterial() == Material.AIR
					|| ingredients.contains(ingredient.getAbbreviation()))
				continue;

			ingredients.add(ingredient.getAbbreviation());

			// Added material data for legacy minecraft versions
			if (ingredient.hasMaterialData()) {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), ingredient.getMaterialData());
			} else {
				shapedRecipe.setIngredient(ingredient.getAbbreviation().charAt(0), ingredient.getMaterial());
			}
		}

		return shapedRecipe;
	}

	@SuppressWarnings("deprecation")
	private FurnaceRecipe createFurnaceRecipe(CookingRecipeData recipe) {

		FurnaceRecipe furnaceRecipe;

		if (Main.getInstance().serverVersionAtLeast(1, 13)) {
			furnaceRecipe = new FurnaceRecipe(createNamespacedKey(recipe), recipe.getResult(),
					recipe.getSlot(1).getMaterial(), recipe.getExperience(), recipe.getCookTime());
		} else {
			furnaceRecipe = new FurnaceRecipe(recipe.getResult(), recipe.getSlot(1).getMaterial());
		}

		setFurnaceSource(recipe);
		return furnaceRecipe;
	}

	NamespacedKey createNamespacedKey(Recipe recipe) {
		return new NamespacedKey(Main.getInstance(), recipe.getKey());
	}

	boolean isVersionSupported(RecipeType type, String item) {
		List<RecipeType> v16_2_Types = Arrays.asList(RecipeType.SMITHING);
		List<RecipeType> v16_Types = Arrays.asList(RecipeType.GRINDSTONE);
		List<RecipeType> legacyTypes = Arrays.asList(RecipeType.ANVIL, RecipeType.SHAPED, RecipeType.SHAPELESS,
				RecipeType.FURNACE);

		if (!Main.getInstance().serverVersionAtLeast(1, 16, 2) && v16_2_Types.contains(type)) {
			logError("Error loading recipe..", item);
			logError(">= 1.16.2 is required for " + type.toString() + " recipes!", item);
			return false;
		}

		if (Main.getInstance().serverVersionLessThan(1, 16) && v16_Types.contains(type)) {
			logError("Error loading recipe..", item);
			logError(">= 1.16 is required for " + type.toString() + " recipes!", item);
			return false;
		}

		if (Main.getInstance().serverVersionLessThan(1, 14) && !legacyTypes.contains(type)) {
			logError("Error loading recipe..", item);
			logError(">= 1.14 is required for " + type.toString() + " recipes!", item);
			return false;
		}

		return true;
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	private void logError(String st, String recipe) {
		Logger.getLogger("Minecraft").log(Level.WARNING,
				"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "] " + st);
	}

	private void logDebug(String st, String recipe) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][" + recipe.replaceAll(".Result", "") + "] " + st);
	}
}
