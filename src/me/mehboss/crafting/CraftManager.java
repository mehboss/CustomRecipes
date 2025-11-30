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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import com.cryptomorin.xseries.XMaterial;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.crafting.ShapedChecks.AlignedResult;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.CooldownManager;
import me.mehboss.utils.RecipeConditions.ConditionSet;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.CraftingRecipeData;

public class CraftManager implements Listener {

	FileConfiguration customConfig() {
		return Main.getInstance().customConfig;
	}

	ArrayList<String> disabledrecipe() {
		return Main.getInstance().disabledrecipe;
	}

	Logger getLogger() {
		return Main.getInstance().getLogger();
	}

	CooldownManager getCooldownManager() {
		return Main.getInstance().cooldownManager;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().shapedChecks;
	}

	ShapelessChecks shapelessChecks() {
		return Main.getInstance().shapelessChecks;
	}

	boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	void logDebug(String st, String recipeName, UUID id) {
		if (Main.getInstance().debug && (id == null || (!Main.getInstance().inInventory.contains(id))))
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}

	void logDebug(String st, String recipeName) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING,
					"[DEBUG][" + Main.getInstance().getName() + "][Crafting][" + recipeName + "]" + st);
	}

	void sendMessages(Player p, String s, long seconds) {
		Main.getInstance().sendMessages(p, s, seconds);
	}

	void sendNoPermsMessage(Player p, String recipe) {
		logDebug("[sendNoPermsMessage] Player " + p.getName()
				+ " does not have required recipe crafting permissions for recipe", recipe, p.getUniqueId());
		Main.getInstance().sendnoPerms(p);
	}

	public boolean matchedRecipe(CraftingInventory inv, UUID id) {
		if (Main.getInstance().serverVersionAtLeast(1, 13)) {
			if (inv.isEmpty()) {
				logDebug("[matchedRecipe] Could not find a recipe to match with!", "", id);
				return false;
			}
		} else {
			boolean isEmpty = true;
			for (ItemStack item : inv.getContents())
				if (item != null && item.getType() != Material.AIR) {
					isEmpty = false;
					break;
				}
			if (isEmpty) {
				logDebug("[matchedRecipe] Could not find a recipe to match with!", "", id);
				return false;
			}
		}
		return true;
	}

	boolean validateItem(ItemStack item, RecipeUtil.Ingredient ingredient, String recipeName, int slot, boolean debug,
			boolean returnType) {

		if (item == null)
			return returnType;

		ItemStack recipeResult = null;
		Recipe foundRecipe = getRecipeUtil().getRecipeFromResult(item);
		if (foundRecipe != null) {
			recipeResult = foundRecipe.getResult();
		}

		ItemStack exactMatch = null;
		boolean matchesIdentifier = false;
		boolean matchesTags = tagsMatch(ingredient, item);

		// since getResultFromKey is so resource heavy, try to check tags first for
		// custom items.
		if (!matchesTags) {
			exactMatch = ingredient.hasIdentifier() ? getRecipeUtil().getResultFromKey(ingredient.getIdentifier())
					: recipeResult;
			matchesIdentifier = ingredient.hasIdentifier() && item.isSimilar(exactMatch);
		}

		if (ingredient.hasIdentifier() && !matchesTags && exactMatch == null)
			return returnType;

		boolean matchesMaterial = item.getType() == ingredient.getMaterial();
		boolean matchesMaterialChoice = ingredient.hasMaterialChoices()
				&& ingredient.getMaterialChoices().contains(item.getType());
		boolean matchesSingleMaterialWithName = matchesMaterial && hasMatchingDisplayName(recipeName, item,
				ingredient.getDisplayName(), ingredient.getIdentifier(), ingredient.hasIdentifier(), false);

		if (matchesIdentifier || matchesTags || matchesMaterialChoice || matchesSingleMaterialWithName) {

			if (debug)
				logDebug("[amountsMatch] Checking slot " + slot + " for required amounts.. ", recipeName);

			if (ingredient.isEmpty())
				return true;

			if (item.getAmount() < ingredient.getAmount()) {

				if (debug) {
					logDebug("[amountsMatch] Item amount: " + item.getAmount(), recipeName);
					logDebug("[amountsMatch] Required amount: " + ingredient.getAmount(), recipeName);
				}
				return false;
			}

			if (debug)
				logDebug("[amountsMatch] Item amount and ingredient amounts matched (#" + ingredient.getAmount() + ")",
						recipeName);

			return true;
		}

		return returnType;
	}

	boolean amountsMatch(AlignedResult alignedGrid, Inventory inv, String recipeName,
			List<RecipeUtil.Ingredient> recipeIngredients, boolean debug, UUID id) {

		logDebug("[amountsMatch] Checking recipe amounts..", recipeName, id);

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		RecipeType type = getRecipeUtil().getRecipe(recipeName).getType();
		ItemStack[] matrix = inv.getContents();

		boolean[] usedSlots = new boolean[matrix.length];
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {

			if (type == RecipeType.SHAPED) {

				int slot = ingredient.getSlot();
				ItemStack invSlot = alignedGrid.getMatrix[slot - 1];

				if (ingredient.isEmpty()) {
					continue;
				}

				if (!validateItem(invSlot, ingredient, recipeName, slot, debug, false))
					return false;

				continue;

			} else {
				if (ingredient.isEmpty())
					continue;
				int slot = isCraftingInventory ? 0 : -1;

				for (ItemStack item : matrix) {
					if (isCraftingInventory && slot == 0) {
						slot++;
						continue;
					}

					if (usedSlots[slot])
						continue;
					if (item == null || item.getType() == Material.AIR)
						continue;
					if (validateItem(item, ingredient, recipeName, slot, debug, false)) {
						usedSlots[slot] = true;
						break;
					}
					slot++;
				}
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	boolean isBlacklisted(ItemStack result, Player p) {
		if (customConfig().getBoolean("blacklist-recipes")) {
			for (String item : disabledrecipe()) {

				String[] split = item.split(":");
				String id = split[0];
				ItemStack i = null;

				UUID pID = p.getUniqueId();

				if (result == null)
					return false;

				if (!customConfig().isConfigurationSection("vanilla-recipes." + split[0]))
					continue;

				if (!XMaterial.matchXMaterial(split[0]).isPresent()) {
					getLogger().log(Level.SEVERE, "We are having trouble matching the material '" + split[0]
							+ "' to a minecraft item. This can cause issues with the plugin. Please double check you have inputted the correct material "
							+ "ID into the blacklisted config file and try again. If this problem persists please contact Mehboss on Spigot!");
					continue;
				}
				i = XMaterial.matchXMaterial(split[0]).get().parseItem();

				if (split.length == 2)
					i.setDurability(Short.valueOf(split[1]));

				logDebug("[isBlacklisted] Found " + disabledrecipe().size() + " disabled recipes.", "", pID);
				String getPerm = customConfig().getString("vanilla-recipes." + item + ".permission");

				if (i == null)
					continue;

				if ((NBTEditor.contains(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id)
						&& NBTEditor.getString(i, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER", id).equals(id)
						&& getRecipeUtil().getRecipe(id) == null) || result.isSimilar(i)) {

					if (getPerm != null && !getPerm.equalsIgnoreCase("none") && p.hasPermission("crecipe." + getPerm)) {
						logDebug("[isBlacklisted] Player " + p.getName() + " does have required permission '" + getPerm
								+ "' for item " + item, "", pID);
						return false;
					}

					logDebug("[isBlacklisted] Player " + p.getName() + " does not have required permission '" + getPerm
							+ "' for item " + item + " or this recipe has been globally blacklisted!", "", pID);

					sendMessages(p, getPerm, 0);
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean debug) {
		Recipe recipe = getRecipeUtil().getRecipe(recipeName);
		if (recipe.getIgnoreData() || recipe.getIgnoreNames())
			return true;

		ItemMeta itemM = item.getItemMeta();
		String recipeIdentifier = NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				? NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				: "none";

		if (hasIdentifier && identifier.equals(recipeIdentifier))
			return true;

		if (debug)
			logDebug("[hasMatchingDisplayName] Checking displayname..", recipeName);

		if (displayName == null || displayName.equals("false") || displayName.equals("none")) {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found that ingredient does not have a displayname to check",
						recipeName);
			return !item.hasItemMeta() || !itemM.hasDisplayName();

		} else {
			if (debug)
				logDebug("[hasMatchingDisplayName] Found displayname " + displayName
						+ " and the match check came back: " + Boolean.valueOf(item.hasItemMeta()
								&& item.getItemMeta().hasDisplayName() && itemM.getDisplayName().equals(displayName)),
						recipeName);
			return item.hasItemMeta() && itemM.hasDisplayName() && itemM.getDisplayName().equals(displayName);
		}
	}

	// Checks all inv materials for recipe (shaped or shapeless)
	public boolean hasAllIngredients(Inventory inv, String recipeName, List<RecipeUtil.Ingredient> recipeIngredients,
			UUID id) {
		ArrayList<String> invMaterials = new ArrayList<>();
		ArrayList<String> ingMaterials = new ArrayList<>();

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		int slot = 0;
		
		if (inv.getType() == InventoryType.CRAFTING) {
			// Since ingredients is always 9, add 5 to the 2x2 slot for the inventory check.
			Collections.addAll(invMaterials, "null", "null", "null", "null", "null");
		}

		for (ItemStack invSlot : inv.getContents()) {

			if (invMaterials.size() == 9)
				break;

			if (isCraftingInventory && slot == 0) {
				slot++;
				continue;
			}
			if (invSlot == null || invSlot.getType() == Material.AIR) {
				invMaterials.add("null");
				continue;
			}

			invMaterials.add(invSlot.getType().toString());
		}

		ArrayList<String> leftOvers = new ArrayList<>();
		for (RecipeUtil.Ingredient ingredient : recipeIngredients) {
			if (!ingredient.isEmpty()) {

				if (getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier()) != null) {
					ingMaterials.add(getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier()).getResult().getType()
							.toString());
				} else if (getRecipeUtil().getResultFromKey(ingredient.getIdentifier()) != null) {
					ingMaterials.add(getRecipeUtil().getResultFromKey(ingredient.getIdentifier()).getType().toString());
				} else if (ingredient.hasMaterialChoices()) {
					for (Material choice : ingredient.getMaterialChoices()) {
						if (invMaterials.contains(choice.toString())) {
							ingMaterials.add(choice.toString());
							invMaterials.remove(choice.toString());
							leftOvers.add(choice.toString());
							break;
						}
					}
				} else {
					String mat = ingredient.getMaterial().toString();
					ingMaterials.add(mat);
					invMaterials.remove(mat);
					leftOvers.add(mat);
				}

				continue;
			}
			ingMaterials.add("null");
		}

		for (String material : leftOvers) {
			invMaterials.add(material);
		}

		if (inv.getType() == InventoryType.WORKBENCH || inv.getType() == InventoryType.CRAFTER) {

			if (invMaterials.size() != 9 || ingMaterials.size() != 9) {
				logDebug("[hasAllIngredients] Ingredients size is " + ingMaterials.size() + ", Inventory size is "
						+ invMaterials.size(), recipeName, id);
				return false;
			}

			Map<String, Integer> invCount = new HashMap<>();
			Map<String, Integer> ingCount = new HashMap<>();

			for (String s : invMaterials)
				invCount.put(s, invCount.getOrDefault(s, 0) + 1);

			for (String s : ingMaterials)
				ingCount.put(s, ingCount.getOrDefault(s, 0) + 1);

			for (Map.Entry<String, Integer> e : ingCount.entrySet()) {
				if (invCount.getOrDefault(e.getKey(), 0) < e.getValue()) {
					logDebug("[hasAllIngredients] Missing required: " + e.getKey(), recipeName, id);
					return false;
				}
			}
		}

		// handles 2x2 slot
		if (inv.getType() == InventoryType.CRAFTING) {
			Map<String, Integer> invCount = new HashMap<>();
			Map<String, Integer> ingCount = new HashMap<>();

			for (String s : invMaterials)
				invCount.put(s, invCount.getOrDefault(s, 0) + 1);

			for (String s : ingMaterials)
				ingCount.put(s, ingCount.getOrDefault(s, 0) + 1);

			for (Map.Entry<String, Integer> e : ingCount.entrySet()) {
				if (invCount.getOrDefault(e.getKey(), 0) < e.getValue()) {
					logDebug("[hasAllIngredients] Recipe ingredient requirements not met..", recipeName, id);
					logDebug("IngCount: " + ingCount.keySet(), "");
					logDebug("IngCount: " + ingCount.values(), "");
					logDebug("InvCount: " + invCount.keySet(), "");
					logDebug("InvCount: " + invCount.values(), "");
					return false;
				}
			}
		}

		return true;
	}

	public boolean hasVanillaIngredients(Inventory inv, ItemStack result) {

		if (result != null) {
			if (result.hasItemMeta() && (result.getItemMeta().hasDisplayName() || result.getItemMeta().hasLore()
					|| NBTEditor.contains(result, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
				return false;

			// vanilla ingredients, but matched to a custom recipe result, so still check
			// amount requirements.
			if (getRecipeUtil().getRecipeFromResult(result) != null)
				return false;
		}

		boolean isCraftingInventory = inv.getType() == InventoryType.WORKBENCH
				|| inv.getType() == InventoryType.CRAFTING;
		int slot = 0;
		for (ItemStack item : inv.getContents()) {

			if (isCraftingInventory && slot == 0) {
				slot++;
				continue;
			}

			if (item == null || item.getType() == Material.AIR)
				continue;
			if (item.hasItemMeta() && (CompatibilityUtil.hasDisplayname(item.getItemMeta()) || item.getItemMeta().hasLore()))
				return false;
		}
		logDebug("[hasVanillaIngredients] Skipping checks.. vanilla recipe detected.", "");
		return true;
	}

	boolean tagsMatch(RecipeUtil.Ingredient ingredient, ItemStack item) {
		String invID = getRecipeUtil().getKeyFromResult(item);
		String ingID = ingredient.hasIdentifier() ? ingredient.getIdentifier().toLowerCase() : "none";

		logDebug("[tagsMatch] invID === " + invID, "");
		logDebug("[tagsMatch] ingID === " + ingID, "");
		if (invID == null && ingID.equals("none"))
			return true;
		if (invID != null && invID.toLowerCase().equals(ingID))
			return true;

		return false;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void handleCrafting(PrepareItemCraftEvent e) {

		CraftingInventory inv = e.getInventory();

		Object view = CompatibilityUtil.getInventoryView(e);
		Player p = CompatibilityUtil.getPlayerFromView(view);

		if (!(p instanceof Player) || p == null)
			return;

		if ((inv.getType() != InventoryType.WORKBENCH && inv.getType() != InventoryType.CRAFTING)
				|| !(matchedRecipe(inv, p.getUniqueId())))
			return;

		long now = System.currentTimeMillis();
		UUID id = p.getUniqueId();

		// avoids redundant checks to increase server performance
		if (Main.getInstance().debounceMap.containsKey(id)) {
			if (now - Main.getInstance().debounceMap.get(id) < 75) {
				return;
			}
			Main.getInstance().debounceMap.remove(id);
		}

		if (isBlacklisted(inv.getResult(), p)) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		if (hasVanillaIngredients(inv, inv.getResult()))
			return;

		if (inv.getType() == InventoryType.CRAFTING) {
			if (!(inv.getRecipe() instanceof ShapelessRecipe)) {
				logDebug("[handleCrafting] Skipping 2x2 checks.. recipe isn't shapeless..", "");
				
				if (getRecipeUtil().getRecipeFromResult(inv.getResult()) != null)
					inv.setResult(new ItemStack(Material.AIR));
				return;
			}
		}

		logDebug("[handleCrafting] Fired craft event, beginning checks..", "", p.getUniqueId());
		handleCraftingChecks(inv, p);
	}

	public void handleCraftingChecks(CraftingInventory inv, Player p) {
		CraftingRecipeData recipe = null;
		Boolean passedCheck = true;
		Boolean found = false;

		UUID id = p.getUniqueId();
		AlignedResult alignedGrid = null;

		for (Recipe data : getRecipeUtil().getAllRecipesSortedByResult(inv.getResult())) {
			if (data.getType() != RecipeType.SHAPELESS && data.getType() != RecipeType.SHAPED)
				continue;

			recipe = (CraftingRecipeData) data;
			List<RecipeUtil.Ingredient> recipeIngredients = recipe.getIngredients();
			if (!hasAllIngredients(inv, recipe.getName(), recipeIngredients, id)) {
				logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not match..", recipe.getName(),
						id);
				passedCheck = false;
				continue;
			}

			logDebug("[handleCrafting] Inventory contained all of the ingredients. Continuing checks.. RecipeType: "
					+ recipe.getType(), recipe.getName(), id);

			passedCheck = true;
			found = true;

			// -------------------------
			// SHAPELESS
			// -------------------------
			if (recipe.getType() == RecipeType.SHAPELESS) {

				if (!shapelessChecks().handleShapelessRecipe(inv, recipe, recipeIngredients, id)) {
					passedCheck = false;
					continue;
				}
			}

			// -------------------------
			// SHAPED
			// -------------------------
			else {

				alignedGrid = shapedChecks().getAlignedGrid(inv, recipeIngredients);
				if (alignedGrid == null) {
					logDebug("[handleCrafting] Skipping to the next recipe! Ingredients did not have exact match..",
							recipe.getName(), id);
					passedCheck = false;
					continue;
				}

				logDebug("[handleCrafting] Ingredients matched. Continuing checks..", recipe.getName(), id);

				// strict checks (NBT / CMD / name)
				if (!recipe.getIgnoreData())
					if (!shapedChecks().handleShapedRecipe(inv.getType(), alignedGrid, recipe, recipeIngredients)) {
						passedCheck = false;
						continue;
					}
			}

			// -------------------------
			// AMOUNT CHECK
			// -------------------------
			if (!amountsMatch(alignedGrid, inv, recipe.getName(), recipeIngredients, true, id)) {
				logDebug(
						"[handleCrafting] Skipping recipe.. The amount check indicates that the requirements have not been met..",
						recipe.getName(), id);
				passedCheck = false;
				continue;
			}

			if (passedCheck && found)
				break;
		}

		if (!found)
			return;

		logDebug("[handleCrafting] Final crafting results: (passedChecks: " + passedCheck + ")(foundRecipe: " + found
				+ ")", recipe.getName(), id);

		if ((!passedCheck) || (passedCheck && !found)) {
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		// -------------------------
		// DISABLED CHECKS
		// -------------------------
		if (!recipe.isActive()) {
			inv.setResult(new ItemStack(Material.AIR));
			logDebug(" Attempt to craft disabled recipe detected..", recipe.getName(), id);
			return;
		}

		// PERMISSIONS
		if (recipe.hasPerm() && !p.hasPermission(recipe.getPerm())) {
			inv.setResult(new ItemStack(Material.AIR));
			sendNoPermsMessage(p, recipe.getName());
			return;
		}

		// WORLD DISABLED
		if (!(recipe.getDisabledWorlds().isEmpty())) {
			for (String string : recipe.getDisabledWorlds()) {
				if (p.getWorld().getName().equalsIgnoreCase(string)) {
					inv.setResult(new ItemStack(Material.AIR));
					sendMessages(p, "none", 0);
					return;
				}
			}
		}

		// COOLDOWN
		if ((recipe.hasPerm() && !(p.hasPermission(recipe.getPerm() + ".bypass")))
				&& getCooldownManager().hasCooldown(p.getUniqueId(), recipe.getKey())) {

			Long timeLeft = Main.getInstance().cooldownManager.getTimeLeft(p.getUniqueId(), recipe.getKey());
			sendMessages(p, "crafting-limit", timeLeft);
			inv.setResult(new ItemStack(Material.AIR));
			return;
		}

		// CONDITIONS
		ConditionSet cs = recipe.getConditionSet();
		if (cs != null && !cs.isEmpty()) {
			List<String> reasons = new ArrayList<String>();
			if (!cs.test(p.getLocation(), p, reasons)) {

				if (!reasons.isEmpty()) {
					p.sendMessage(ChatColor.RED + "You cannot craft this recipe until conditions are met: "
							+ ChatColor.GRAY + String.join(", ", reasons));
				}

				Boolean closeInventory = Main.getInstance().customConfig.getBoolean("conditions-failed.close-inventory",
						true);

				if (closeInventory)
					p.closeInventory();

				logDebug(" Preventing craft due to failing required recipe conditions!", recipe.getName(), id);
				inv.setResult(new ItemStack(Material.AIR));
				return;
			}
		}

		// -------------------------
		// SUCCESSFUL CRAFT
		// -------------------------
		if (passedCheck && found) {
			ItemStack item = new ItemStack(recipe.getResult());

			// PlaceholderAPI support
			if (!recipe.isCustomItem()) {

				List<String> withPlaceholders = null;

				if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
					withPlaceholders = item.hasItemMeta() && item.getItemMeta().hasLore()
							? PlaceholderAPI.setPlaceholders(p, item.getItemMeta().getLore())
							: null;

				if (withPlaceholders != null) {
					ItemMeta itemMeta = item.getItemMeta();
					itemMeta.setLore(withPlaceholders);
					item.setItemMeta(itemMeta);
				}
			}

			inv.setResult(item);
		}
	}
}
