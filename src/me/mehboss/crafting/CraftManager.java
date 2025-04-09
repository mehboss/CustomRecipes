package me.mehboss.crafting;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;
import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;

import dev.lone.itemsadder.api.CustomStack;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class CraftManager implements Listener {

	RecipeUtil recipeUtil = Main.getInstance().recipeUtil;
	ArrayList<Player> inInventory = new ArrayList<Player>();

	ArrayList<String> disabledrecipe() {
		return Main.getInstance().disabledrecipe;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	FileConfiguration customConfig() {
		return Main.getInstance().customConfig;
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	void sendMessages(Player p, String s, long seconds) {
		Main.getInstance().sendMessages(p, s, seconds);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe " + recipe);
		Main.getInstance().sendnoPerms(p);
	}

	public boolean matchedRecipe(CraftingInventory inv) {
		if (inv.getResult() == null || inv.getResult() == new ItemStack(Material.AIR)) {

			logDebug("[matchedRecipe] Could not find a recipe to match with!");

			return false;
		}
		return true;
	}

	HashMap<String, Integer> countItemsByMaterial(CraftingInventory inv) {
		HashMap<String, Integer> counts = new HashMap<>();

		for (ItemStack item : inv.getContents()) {
			if (item != null && item.getType() != Material.AIR && !(item.isSimilar(inv.getResult()))) {
				Material material = item.getType();
				String displayName = null; // Default to null if no display name is present

				if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					displayName = item.getItemMeta().getDisplayName();
				}

				// Generate a unique key for the material and display name combination
				String key = material.toString() + "-" + displayName;

				logDebug("[countItemsByMaterial] Key found is " + key);

				int amount = item.getAmount();
				counts.put(key, counts.getOrDefault(key, 0) + amount);
			}
		}

		// Combine counts for items with the same material and display name
		HashMap<String, Integer> combinedCounts = new HashMap<>();
		for (HashMap.Entry<String, Integer> entry : counts.entrySet()) {
			String key = entry.getKey();
			int amount = entry.getValue();
			combinedCounts.put(key, combinedCounts.getOrDefault(key, 0) + amount);
		}

		return combinedCounts;
	}

	boolean validateItem(ItemStack item, RecipeUtil.Ingredient ingredient, String recipeName, int slot, boolean debug,
			boolean returnType) {

		ItemStack recipe = recipeUtil.getRecipeFromResult(item) != null
				? recipeUtil.getRecipeFromResult(item).getResult()
				: null;
		ItemStack exactMatch = ingredient.hasIdentifier() ? recipeUtil.getResultFromKey(ingredient.getIdentifier())
				: recipe;

		// Attempt to match the ingredient to a recipe using the identifier
		// Attempt to match the ingredient to an itemsadder item
		// If all else fail, match the itemstack to the regular ingredient requirements
		if (item != null && ((ingredient.hasIdentifier() && exactMatch != null && item.isSimilar(exactMatch))
				|| (item.getType() == ingredient.getMaterial() && hasMatchingDisplayName(recipeName, item,
						ingredient.getDisplayName(), ingredient.getIdentifier(), ingredient.hasIdentifier(), false)))) {

			if (debug)
				logDebug("[amountsMatch] Checking slot " + slot + " for required amounts.. ");

			if (item.getAmount() < ingredient.getAmount()) {

				if (debug) {
					logDebug("[amountsMatch] Item amount: " + item.getAmount());
					logDebug("[amountsMatch] Required amount: " + ingredient.getAmount());
				}
				return false;
			}

			if (debug)
				logDebug("[amountsMatch] Item amount and ingredient amounts matched (#" + ingredient.getAmount() + ")");

			return true;
		}
		return returnType;
	}

	boolean amountsMatch(CraftingInventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			boolean debug) {
		
		logDebug("[AmountsMatch] Checking recipe " + recipeName);
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (ingredient.isEmpty())
				continue;

			int slot = ingredient.getSlot();
			ItemStack invSlot = inv.getMatrix()[slot - 1];

			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPED)
				if (!validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;

			if (recipeUtil.getRecipe(recipeName).getType() == RecipeType.SHAPELESS) {
				slot = 0;
				for (ItemStack item : inv.getMatrix()) {
					slot++;
					if (!validateItem(item, ingredient, recipeName, slot, debug, true))
						return false;
				}
			}
		}
		return true;
	}

	boolean hasCooldown(CraftingInventory inv, Player p, Recipe recipe) {
		if (recipe.hasCooldown())
			if (!(Main.getInstance().cooldownManager.cooldownExpired(p.getUniqueId(), recipe.getKey())))
				return true;

		return false;
	}

	@SuppressWarnings("deprecation")
	boolean isBlacklisted(CraftingInventory inv, Player p) {
		if (customConfig().getBoolean("blacklist-recipes") == true) {
			for (String item : disabledrecipe()) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				if (customConfig().isString("vanilla-recipes." + split[0]))
					return false;

				if (!XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
					return false;
				}
				i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				logDebug("[isBlacklisted] Found " + disabledrecipe().size() + " disabled recipes.");
				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if (i == null)
					return false;

				if ((NBTEditor.contains(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id)
						&& NBTEditor.getString(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id).equals(id)
						&& recipeUtil.getRecipe(id) == null) || inv.getResult().isSimilar(i)) {

					if (getPerm == null || getPerm.equalsIgnoreCase("none") || p.hasPermission("crecipe." + getPerm)) {

						logDebug("[isBlacklisted] Player " + p.getName() + " does have required permission " + getPerm
								+ " for item " + item);
						return false;
					}

					logDebug("[isBlacklisted] Player " + p.getName() + " does not have required permission " + getPerm
							+ " for item " + item);

					sendMessages(p, getPerm, 0);
					inv.setResult(new ItemStack(Material.AIR));
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean debug) {
		if (recipeUtil.getRecipe(recipeName).getIgnoreData() == true)
			return true;

		ItemMeta itemM = item.getItemMeta();
		String recipeIdentifier = NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				? NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				: "none";

		if (hasIdentifier && identifier.equals(recipeIdentifier))
			return true;

		if (debug)
			logDebug("[hasMatchingDisplayName] Checking displayname..");

		if (displayName == null || displayName.equals("false") || displayName.equals("none")) {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found that ingredient does not have a displayname to check");
			return !item.hasItemMeta() || !itemM.hasDisplayName();

		} else {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found displayname " + displayName
						+ " and the match check came back: " + Boolean.valueOf(item.hasItemMeta()
								&& item.getItemMeta().hasDisplayName() && itemM.getDisplayName().equals(displayName)));
			return item.hasItemMeta() && itemM.hasDisplayName() && itemM.getDisplayName().equals(displayName);
		}
	}

	// Checks if inventory slots and recipe materials match
	boolean hasShapedIngredients(CraftingInventory inv, String recipeName,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();

		for (ItemStack contents : inv.getMatrix()) {
			if (invMaterials.size() == 9)
				break;

			if (contents == null || contents.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(contents.getType().toString());
		}

		int slot = 0;

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

			if (ingredient.isEmpty() && !invMaterials.get(slot).equals("null")) {

				logDebug("[hasShapedIngredients] Required ingredient not found in slot " + slot + " for the recipe "
						+ recipeName);
				return false;
			}
			if (!ingredient.isEmpty() && !invMaterials.get(slot).equals(ingredient.getMaterial().toString())) {

				logDebug("[hasShapedIngredients] Recipe ingredient requirement for slot " + slot
						+ " does not match the ingredient for the recipe " + recipeName);

				return false;
			}

			slot++;
		}

		if (invMaterials.size() != 9) {

			logDebug(
					"[hasShapedIngredients] An internal error has occurred.. Please contact mehboss on spigot! Found size "
							+ invMaterials.size() + "but should be 9 for the recipe " + recipeName);

			return false;
		}
		return true;
	}

	// Method for specifically shapeless recipes only (checks all inv materials)
	public ArrayList<RecipeUtil.Ingredient> getIngredients(String recipeName,
			List<RecipeUtil.Ingredient> allIngredients) {
		ArrayList<RecipeUtil.Ingredient> newIngredients = new ArrayList<>();

		for (RecipeUtil.Ingredient ingredient : allIngredients) {
			// Ignore the slot field and only save material, amount, and slot
			if (!ingredient.isEmpty() && ingredient.getMaterial() != null) {
				if (!newIngredients.contains(ingredient)) {
					newIngredients.add(ingredient);
				}
			}
		}

		return newIngredients;
	}

	// Checks all inv materials for recipe (shaped or shapeless)
	public boolean hasAllIngredients(CraftingInventory inv, String recipeName,
			List<RecipeUtil.Ingredient> recipeIngredients) {
		ArrayList<String> invMaterials = new ArrayList<>();
		ArrayList<String> ingMaterials = new ArrayList<>();

		for (ItemStack invSlot : inv.getMatrix()) {

			if (invMaterials.size() == 9)
				break;

			if (invSlot == null || invSlot.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(invSlot.getType().toString());
		}

		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (!ingredient.isEmpty()) {

				if (!ingredient.hasIdentifier()) {
					ingMaterials.add(ingredient.getMaterial().toString());
				} else if (recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) != null) {
					ingMaterials.add(
							recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult().getType().toString());
				} else {
					String[] customItem = ingredient.getIdentifier().split(":");
					if (customItem.length <= 1)
						continue;

					if (customItem[0].equalsIgnoreCase("itemsadder") && Main.getInstance().hasItemsAdderPlugin()) {
						CustomStack stack = CustomStack.getInstance(customItem[1]);
						if (stack == null)
							continue;

						ingMaterials.add(stack.getItemStack().getType().toString());
					}

					if (customItem[0].equalsIgnoreCase("mythicmobs") && Main.getInstance().hasMythicMobsPlugin()) {
						ItemStack mythicItem = MythicBukkit.inst().getItemManager().getItemStack(customItem[1]);
						if (mythicItem == null)
							continue;

						ingMaterials.add(mythicItem.getType().toString());
					}

					if (customItem[0].equalsIgnoreCase("executableitems")
							&& Main.getInstance().hasExecutableItemsPlugin()) {
						Optional<ExecutableItemInterface> stack = ExecutableItemsAPI.getExecutableItemsManager()
								.getExecutableItem(customItem[1]);
						if (stack == null || !stack.isPresent())
							continue;

						ingMaterials.add(stack.get().buildItem(1, Optional.empty()).getType().toString());
					}
				}

				continue;
			}
			ingMaterials.add("null");
		}

		// handles crafting table
		if (inv.getType() == InventoryType.WORKBENCH
				&& (invMaterials.size() != 9 || ingMaterials.size() != 9 || !invMaterials.containsAll(ingMaterials))) {

			logDebug("[hasAllIngredients] Ingredients size is " + ingMaterials.size() + ", Inventory size is "
					+ invMaterials.size() + " for the recipe " + recipeName);

			return false;
		}

		// handles 4x4 slot
		if (inv.getType() == InventoryType.CRAFTING && !invMaterials.containsAll(ingMaterials)) {
			logDebug("[hasAllIngredients] Recipe ingredient requirements not met for the recipe " + recipeName);
			return false;
		}

		return true;
	}

	boolean hasVanillaIngredients(CraftingInventory inv) {
		if (inv.getResult() == null)
			return false;

		if (inv.getResult().hasItemMeta()
				&& (inv.getResult().getItemMeta().hasDisplayName() || inv.getResult().getItemMeta().hasLore()
						|| NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
			return false;
		for (ItemStack item : inv.getContents()) {
			if (item == null || item.getType() == Material.AIR)
				continue;
			if (item.hasItemMeta() && (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore()))
				return false;
		}
		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void handleCrafting(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Recipe finalRecipe = null;
		Boolean passedCheck = true;
		Boolean found = false;

		logDebug("[handleCrafting] Fired craft event!");
		if (!(e.getView().getPlayer() instanceof Player))
			return;

		Player p = (Player) e.getView().getPlayer();

		if ((inv.getType() != InventoryType.WORKBENCH && inv.getType() != InventoryType.CRAFTING)
				|| !(matchedRecipe(inv)) || isBlacklisted(inv, p))
			return;

		recipeLoop: for (String recipes : recipeUtil.getRecipeNames()) {

			Recipe recipe = recipeUtil.getRecipe(recipes);
			finalRecipe = recipe;

			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();

			if (!hasAllIngredients(inv, recipe.getName(), recipeIngredients)) {
				logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not match for recipe "
						+ recipe.getName());
				passedCheck = false;
				continue;
			}

			logDebug("[handleCrafting] Inventory contained all of the ingredients for the recipe " + recipe.getName()
					+ ". Continuing checks..");

			passedCheck = true;
			found = true;

			if (recipe.getType() == RecipeType.SHAPELESS) {
				// runs checks if recipe is shapeless

				if (recipeUtil.getRecipeFromResult(inv.getResult()) != null) {
					recipe = recipeUtil.getRecipeFromResult(inv.getResult());

					if (recipe.isExactChoice()) {
						logDebug("[handleCrafting] Found recipe " + recipe.getName() + " to handle..");
						logDebug("[handleCrafting] ExactChoice is enabled, but does not work for shapeless recipes!");
					}
				}

				ArrayList<String> slotNames = new ArrayList<>();
				ArrayList<String> recipeNames = new ArrayList<>();

				ArrayList<Integer> recipeMD = new ArrayList<>();
				ArrayList<Integer> inventoryMD = new ArrayList<>();

				Map<Material, Integer> recipeCount = new HashMap<>();
				Map<Material, Integer> inventoryCount = new HashMap<>();

				for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
					if (ingredient.isEmpty()) {
						recipeCount.put(Material.AIR, recipeCount.getOrDefault(Material.AIR, 0) + 1);
						recipeNames.add("null");
						continue;
					}

					if (ingredient.hasIdentifier() && recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null) {
						ItemStack exactMatch = recipeUtil.getResultFromKey(ingredient.getIdentifier());
						recipeCount.put(exactMatch.getType(), recipeCount.getOrDefault(exactMatch.getType(), 0) + 1);

						if (Main.getInstance().serverVersionAtLeast(1, 14) && exactMatch.hasItemMeta()
								&& exactMatch.getItemMeta().hasCustomModelData()) {
							recipeMD.add(exactMatch.getItemMeta().getCustomModelData());
						}

						if (exactMatch.getItemMeta().hasDisplayName()) {
							recipeNames.add(exactMatch.getItemMeta().getDisplayName());
						} else {
							recipeNames.add("false");
						}
						continue;
					}

					recipeCount.put(ingredient.getMaterial(),
							recipeCount.getOrDefault(ingredient.getMaterial(), 0) + 1);

					if (ingredient.hasDisplayName()) {
						recipeNames.add(ingredient.getDisplayName());
					} else {
						recipeNames.add("false");
					}
				}

				for (int slot = 1; slot < 10; slot++) {
					if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.AIR) {
						slotNames.add("null");
						inventoryCount.put(Material.AIR, inventoryCount.getOrDefault(Material.AIR, 0) + 1);
						continue;
					}
					
					if (Main.getInstance().serverVersionAtLeast(1, 14) && inv.getItem(slot).hasItemMeta()
							&& inv.getItem(slot).getItemMeta().hasCustomModelData()) {
						inventoryMD.add(inv.getItem(slot).getItemMeta().getCustomModelData());
					}

					inventoryCount.put(inv.getItem(slot).getType(),
							inventoryCount.getOrDefault(inv.getItem(slot).getType(), 0) + 1);

					if (!(inv.getItem(slot).getItemMeta().hasDisplayName())) {
						slotNames.add("false");
						continue;
					}

					if (recipeUtil.getRecipeFromResult(inv.getItem(slot)) != null && !(NBTEditor
							.contains(inv.getItem(slot), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"))) {
						continue;
					}

					slotNames.add(inv.getItem(slot).getItemMeta().getDisplayName());
				}

				boolean hasIngredients = true;
				for (Map.Entry<Material, Integer> entry : recipeCount.entrySet()) {
					Material material = entry.getKey();
					int requiredCount = entry.getValue();
					int invCount = inventoryCount.getOrDefault(material, 0);

					if (invCount < requiredCount) {
						hasIngredients = false;
						break;
					}
				}

				if (!hasIngredients) {
					logDebug("[handleCrafting] The recipe " + recipe.getName()
							+ " does not have all of the required ingredients! Skipping recipe..");
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] The recipe " + recipe.getName()
						+ " does have all of the required ingredients!");

				// ✔️ Compare model data counts exactly if not ignored
				if (!recipe.getIgnoreModelData()) {
					Map<Integer, Integer> recipeModelCount = new HashMap<>();
					Map<Integer, Integer> inventoryModelCount = new HashMap<>();

					for (int model : recipeMD) {
						recipeModelCount.put(model, recipeModelCount.getOrDefault(model, 0) + 1);
					}
					for (int model : inventoryMD) {
						inventoryModelCount.put(model, inventoryModelCount.getOrDefault(model, 0) + 1);
					}

					if (!recipeModelCount.equals(inventoryModelCount)) {
						logDebug("[handleCrafting] Model data mismatch: recipe vs inventory");
						logDebug("Recipe Model Data Map: " + recipeModelCount);
						logDebug("Inventory Model Data Map: " + inventoryModelCount);
						passedCheck = false;
						continue;
					}
				}

				// ✔️ Compare display name counts exactly if not ignored
				if (!recipe.getIgnoreData()) {
					Map<String, Integer> recipeNameCount = new HashMap<>();
					Map<String, Integer> slotNameCount = new HashMap<>();

					for (String name : recipeNames) {
						recipeNameCount.put(name, recipeNameCount.getOrDefault(name, 0) + 1);
					}
					for (String name : slotNames) {
						slotNameCount.put(name, slotNameCount.getOrDefault(name, 0) + 1);
					}

					if (!recipeNames.containsAll(slotNames) || !slotNames.containsAll(recipeNames)) {
						logDebug("[handleCrafting] Display name mismatch: recipe vs inventory");
						passedCheck = false;
						continue;
					}
					
					if (!recipeNameCount.equals(slotNameCount)) {
						logDebug("[handleCrafting] Display name count mismatch: recipe vs inventory");
						logDebug("Recipe Name Map: " + recipeNameCount);
						logDebug("Inventory Name Map: " + slotNameCount);
						passedCheck = false;
						continue;
					}
				}
			} else {

				if (recipeUtil.getRecipeFromResult(inv.getResult()) != null) {
					recipe = recipeUtil.getRecipeFromResult(inv.getResult());
					
					if (recipe.isExactChoice()) {
						logDebug("[handleCrafting] Found recipe " + recipe.getName() + " to handle..");
						logDebug("[handleCrafting] Manual checks and methods skipped because exactChoice is enabled!");

						if (!(amountsMatch(inv, recipe.getName(), recipe.getIngredients(), true))) {
							logDebug("[handleCrafting] Skipping recipe.. ");
							logDebug("The amount check indicates that the requirements have not been met for recipe "
									+ recipe.getName());
							inv.setResult(new ItemStack(Material.AIR));
							passedCheck = false;
						}
						break;
					}
				}

				if (!hasShapedIngredients(inv, recipe.getName(), recipeIngredients)) {
					logDebug(
							"[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match for recipe "
									+ recipe.getName());
					found = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched for recipe " + recipe.getName()
						+ ". Continuing system checks..");

				if (recipe.getIgnoreData() == true)
					continue;

				int i = 0;

				for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
					i++;

					if (ingredient.getMaterial() != null && !(inv.contains(ingredient.getMaterial()))) {

						logDebug("[handleCrafting] Initial ingredient check for recipe " + recipe.getName()
								+ " was false. Skipping recipe..");

						passedCheck = false;
						break;
					}

					if (inv.getItem(i) == null && !(ingredient.isEmpty())) {
						passedCheck = false;

						logDebug("[handleCrafting] Slot " + i + " did not have the required ingredient for recipe "
								+ recipe.getName() + ". Skipping recipe..");
						break;
					}

					if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
						ItemMeta meta = inv.getItem(i).getItemMeta();

						if (ingredient.hasIdentifier()
								&& recipeUtil.getResultFromKey(ingredient.getIdentifier()) != null
								&& inv.getItem(i).isSimilar(recipeUtil.getResultFromKey(ingredient.getIdentifier()))) {

							logDebug("[handleCrafting] Passed all required checks for the recipe ingredient in slot "
									+ i + " for recipe " + recipe.getName());
							continue;

						} else if (ingredient.hasIdentifier()) {

							logDebug(
									"[handleCrafting] Skipping recipe.. We should never reach this line of code.. please reach out for support.. Recipe: "
											+ recipe.getName());

							logDebug("[handleCrafting] Ingredient hasID: " + ingredient.hasIdentifier());

							logDebug("[handleCrafting] Invoke getRecipeFromKey:	"
									+ recipeUtil.getRecipeFromKey(ingredient.getIdentifier()));
							logDebug("[handleCrafting] invIngredient ID is " + NBTEditor.getString(inv.getItem(i),
									NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER"));
							logDebug("[handleCrafting] recipeIngredient ID is " + ingredient.getIdentifier());

							passedCheck = false;
							continue recipeLoop;
						}

						logDebug("[handleCrafting] The recipe " + recipe.getName() + " ingredient slot " + i
								+ " does not have an identifier.");

						// checks if displayname is null
						if ((!meta.hasDisplayName() && ingredient.hasDisplayName())
								|| (meta.hasDisplayName() && !ingredient.hasDisplayName())) {
							passedCheck = false;
							logDebug("[handleCrafting] Skipping recipe..");
							logDebug(
									"[handleCrafting] The recipe ingredient displayname and the inventory slot displayname do not match for recipe "
											+ recipe.getName());
							logDebug("[handleCrafting] The inventory slot in question: " + i
									+ ". The ingredient slot in question: " + ingredient.getSlot());
							logDebug("[handleCrafting] Does the ingredient have a displayname? "
									+ ingredient.hasDisplayName());
							logDebug(
									"[handleCrafting] Does the inventory have a displayname? " + meta.hasDisplayName());
							continue recipeLoop;
						}

						if (ingredient.hasDisplayName() && meta.hasDisplayName()
								&& !(ingredient.getDisplayName().equals(meta.getDisplayName()))) {
							passedCheck = false;
							logDebug("[handleCrafting] Skipping recipe..");
							logDebug(
									"[handleCrafting] The ingredient name for the recipe and inventory do not match for recipe "
											+ recipe.getName());
							logDebug("[handleCrafting] The inventory slot in question: " + i
									+ ". The ingredient slot in question: " + ingredient.getSlot());
							logDebug("[handleCrafting] The ingredient displayname: " + ingredient.getDisplayName());
							logDebug("[handleCrafting] The inventory displayname: " + meta.getDisplayName());
							continue recipeLoop;
						}

						logDebug("[handleCrafting] Inventory and recipe ingredient displayname matched for slot " + i);
					}
				}
			}

			if (!(amountsMatch(inv, recipe.getName(), recipeIngredients, true))) {
				logDebug(
						"[handleCrafting] Skipping recipe.. The amount check indicates that the requirements have not been met for recipe "
								+ recipe.getName());
				passedCheck = false;
				continue;
			}

			if (passedCheck && found) {
				break;
			}
		}

		logDebug("[handleCrafting] Final results for recipe " + finalRecipe.getName().toUpperCase() + " (passedChecks: "
				+ passedCheck + ")(foundRecipe: " + found + ")");

		if (hasVanillaIngredients(inv) || !found)
			return;

		if ((!passedCheck) || (passedCheck && !found)) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (!finalRecipe.isActive()) {
			inv.setResult(new ItemStack(Material.AIR));
			logDebug(" Attempt to craft " + finalRecipe.getName() + " was detected, but recipe is disabled!");
			return;
		}

		if (finalRecipe.getPerm() != null && !p.hasPermission(finalRecipe.getPerm())) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p, finalRecipe.getName());
			return;
		}

		if (!(finalRecipe.getDisabledWorlds().isEmpty())) {
			for (String string : finalRecipe.getDisabledWorlds()) {
				if (p.getWorld().getName().equalsIgnoreCase(string)) {
					inv.setResult(new ItemStack(Material.AIR));
					sendMessages(p, "none", 0);
					return;
				}
			}
		}

		if ((finalRecipe.getPerm() != null && !(p.hasPermission(finalRecipe.getPerm() + ".bypass")))
				&& hasCooldown(inv, p, finalRecipe)) {
			Long timeLeft = Main.getInstance().cooldownManager.getTimeLeft(p.getUniqueId(), finalRecipe.getKey());

			sendMessages(p, "crafting-limit", timeLeft);
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (passedCheck && found) {

			List<String> withPlaceholders = null;
			ItemStack item = new ItemStack(finalRecipe.getResult());

			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
				withPlaceholders = item.hasItemMeta() && item.getItemMeta().hasLore()
						? PlaceholderAPI.setPlaceholders(p, item.getItemMeta().getLore())
						: null;

			ItemMeta itemMeta = item.getItemMeta();

			if (withPlaceholders != null) {
				itemMeta.setLore(withPlaceholders);
				item.setItemMeta(itemMeta);
			}

			if (!finalRecipe.isExactChoice())
				inv.setResult(item);

			if (!inInventory.contains(p))
				inInventory.add(p);
		}
	}
}
