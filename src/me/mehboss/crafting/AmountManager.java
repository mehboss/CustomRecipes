package me.mehboss.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class AmountManager implements Listener {

	private CraftManager craftManager;

	public AmountManager(CraftManager craftManager) {
		this.craftManager = craftManager;
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ShapedChecks shapedChecks() {
		return Main.getInstance().shapedChecks;
	}

	private boolean hasAllIngredients(CraftingInventory inv, String recipes, List<Ingredient> recipeIngredients,
			UUID id) {
		return craftManager.hasAllIngredients(inv, recipes, recipeIngredients, id);
	}

	private boolean hasMatchingDisplayName(String recipeName, ItemStack item, String displayName, String identifier,
			boolean hasIdentifier, boolean b) {
		return craftManager.hasMatchingDisplayName(recipeName, item, displayName, identifier, hasIdentifier, b);
	}

	// Helper method for the handleShiftClick method
	void handlesItemRemoval(CraftItemEvent e, CraftingInventory inv, Recipe recipe, ItemStack item,
			RecipeUtil.Ingredient ingredient, int slot, int itemsToRemove, int itemsToAdd, int requiredAmount) {

		String recipeName = recipe.getName();
		logDebug("[handleShiftClicks] Checking slot " + slot + " for the recipe " + recipeName);

		if (matchesIngredient(item, recipeName, ingredient, ingredient.getMaterial(), ingredient.getDisplayName(),
				ingredient.hasIdentifier())) {

			itemsToRemove = itemsToAdd * requiredAmount;

			int availableItems = item.getAmount();

			logDebug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove + " || ItemsToAdd: " + itemsToAdd);
			logDebug("[handleShiftClicks] ItemAmount: " + availableItems + " || RequiredAmount: " + requiredAmount);
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " || hasID: "
					+ ingredient.hasIdentifier());
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString() + " || Displayname: "
					+ ingredient.getDisplayName());

			if (availableItems < requiredAmount)
				return;

			String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
			if (recipe.isLeftover(id)) {
				logDebug("[isLeftover] Leaving item in the work bench because it has been listed to be leftover!");
				if (item.getType().toString().contains("_BUCKET"))
					item.setType(XMaterial.BUCKET.parseMaterial());

				item.setAmount(item.getAmount() + 1);
				return;
			}

			boolean isContainer = item.getType() == Material.DRAGON_BREATH || item.getType() == Material.POTION
					|| item.getType() == Material.LINGERING_POTION || item.getType().toString().contains("_BUCKET");

			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if ((item.getAmount() + 1) - requiredAmount == 0) {
					if (isContainer && !recipe.isLeftover(id)) {
						Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
							inv.setItem(slot, null);
						});
						return;
					}

					inv.setItem(slot, null);
				} else {
					item.setAmount((item.getAmount() + 1) - requiredAmount);
				}
			} else {
				if ((item.getAmount() - itemsToRemove) <= 0) {
					inv.setItem(slot, null);
					return;
				}

				item.setAmount(item.getAmount() - itemsToRemove);
			}
		}
	}

	// Helper method for the handleShiftClick method
	boolean matchesIngredient(ItemStack item, String recipeName, RecipeUtil.Ingredient ingredient, Material material,
			String displayName, boolean hasIdentifier) {
		Recipe exactMatch = getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier());
		ItemStack customItem = ingredient.hasIdentifier() ? getRecipeUtil().getResultFromKey(ingredient.getIdentifier())
				: null;

		return (customItem != null && item.isSimilar(customItem)) || (craftManager.tagsMatch(ingredient, item)
				|| (ingredient.hasIdentifier() && exactMatch != null && item.isSimilar(exactMatch.getResult()))
				|| (item.getType() == material && hasMatchingDisplayName(recipeName, item, displayName,
						ingredient.getIdentifier(), hasIdentifier, false)));
	}

	boolean isCraftingRecipe(RecipeType type) {
		if (type != RecipeType.SHAPELESS && type != RecipeType.SHAPED)
			return false;
		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	void handleShiftClicks(CraftItemEvent e) {
		CraftingInventory inv = e.getInventory();
		UUID id = e.getWhoClicked().getUniqueId();

		if (inv.getResult() == null || inv.getResult().getType() == Material.AIR)
			return;

		// debounce
		Main.getInstance().debounceMap.put(id, System.currentTimeMillis());

		if (getRecipeUtil().getRecipeFromResult(inv.getResult()) == null)
			return;

		logDebug("[handleShiftClicks] Fired");

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		String findName = getRecipeUtil().getRecipeFromResult(inv.getResult()).getName();
		final ItemStack result = inv.getResult();
		boolean isShapeless = e.getRecipe() instanceof ShapelessRecipe;

		HashMap<String, Recipe> types = isShapeless ? getRecipeUtil().getRecipesFromType(RecipeType.SHAPELESS)
				: getRecipeUtil().getRecipesFromType(RecipeType.SHAPED);

		if (NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = NBTEditor.getString(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");

			if (getRecipeUtil().getRecipeFromKey(foundID) != null)
				findName = getRecipeUtil().getRecipeFromKey(foundID).getName();
		}

		// safeguard: match correct custom recipe result
		if (types != null)
			for (Recipe r : types.values()) {
				ItemStack itm = r.getResult();
				if (hasAllIngredients(inv, r.getName(), r.getIngredients(), id)
						&& (result.equals(itm) || result.isSimilar(itm))) {
					findName = r.getName();
					break;
				}
			}

		logDebug("[handleShiftClicks] Final recipe = " + findName);

		Recipe recipe = getRecipeUtil().getRecipe(findName);
		if (recipe == null)
			return;

		RecipeType type = recipe.getType();
		ArrayList<String> handledIngredients = new ArrayList<>();

		// ============================================================
		// PASS 1 — ITEMS TO ADD
		// ============================================================
		int itemsToAdd = Integer.MAX_VALUE;
		int itemsToRemove = 0;

		ShapedChecks.AlignedResult aligned = null;

		if (type == RecipeType.SHAPED) {

			aligned = shapedChecks().getAlignedGrid(inv, recipe.getIngredients());
			if (aligned == null) {
				// cannot craft at all
				itemsToAdd = 0;
			} else {

				for (int i = 0; i < recipe.getIngredients().size(); i++) {

					Ingredient ing = recipe.getIngredients().get(i);
					if (ing.isEmpty())
						continue;

					ItemStack stack = aligned.getMatrix[i];
					if (stack == null || stack.getType() == Material.AIR) {
						itemsToAdd = 0;
						break;
					}

					int requiredAmount = Math.max(1, ing.getAmount());
					int possible = stack.getAmount() / requiredAmount;
					itemsToAdd = Math.min(itemsToAdd, possible);
				}
			}

		} else {
			// shapeless
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;
				int req = Math.max(1, ing.getAmount());

				for (int i = 1; i < 10; i++) {
					ItemStack slot = inv.getItem(i);
					if (slot == null || slot.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(slot, findName, ing, ing.getMaterial(), ing.getDisplayName(),
							ing.hasIdentifier()))
						continue;

					int possible = slot.getAmount() / req;
					itemsToAdd = Math.min(itemsToAdd, possible);
				}
			}
		}

		if (itemsToAdd <= 0 || itemsToAdd == Integer.MAX_VALUE) {
			logDebug("[handleShiftClicks][" + findName + "] No possible crafts.");
			e.setCancelled(true);
			return;
		}

		// ============================================================
		// PASS 2 — REMOVE ITEMS
		// ============================================================
		if (type == RecipeType.SHAPED) {

			for (int i = 0; i < recipe.getIngredients().size(); i++) {

				Ingredient ing = recipe.getIngredients().get(i);
				if (ing.isEmpty())
					continue;

				int realInvSlot = aligned.invSlotMap[i];
				ItemStack stack = inv.getItem(realInvSlot);
				if (stack == null || stack.getType() == Material.AIR)
					continue;

				int requiredAmount = Math.max(1, ing.getAmount());

				handlesItemRemoval(e, inv, recipe, stack, ing, realInvSlot, itemsToRemove, itemsToAdd, requiredAmount);
			}

		} else {
			// shapeless
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty())
					continue;

				final Material mat = ing.getMaterial();
				final String name = ing.getDisplayName();
				final boolean hasID = ing.hasIdentifier();
				int req = Math.max(1, ing.getAmount());

				for (int i = 1; i < 10; i++) {
					ItemStack stack = inv.getItem(i);
					if (stack == null || stack.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(stack, findName, ing, mat, name, hasID))
						continue;

					if (!handledIngredients.contains(ing.getAbbreviation())) {
						handlesItemRemoval(e, inv, recipe, stack, ing, i, itemsToRemove, itemsToAdd, req);
					}
				}

				handledIngredients.add(ing.getAbbreviation());
			}
		}

		// ============================================================
		// COMMANDS + OUTPUT
		// ============================================================
		Player player = (Player) e.getWhoClicked();
		Main.getInstance().cooldownManager.setCooldown(player.getUniqueId(), recipe.getKey(),
				System.currentTimeMillis());

		// debug suppression window
		Main.getInstance().inInventory.add(id);
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> Main.getInstance().inInventory.remove(id), 2L);

		if (recipe.hasCommands()) {

			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY)
				itemsToAdd = 1;

			for (int n = 0; n < itemsToAdd; n++)
				for (String cmd : recipe.getCommand())
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%crafter%", player.getName()));

			if (!recipe.isGrantItem()) {
				Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> e.getWhoClicked().setItemOnCursor(null),
						2L);
				return;
			}
		}

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			logDebug("[handleShiftClicks] Normal click, no mass-add");
			return;
		}

		// SHIFT CLICK OUTPUT
		e.setCancelled(true);

		if (recipe.hasCommands() && !recipe.isGrantItem())
			return;

		inv.setResult(new ItemStack(Material.AIR));

		for (int i = 0; i < itemsToAdd; i++) {
			if (player.getInventory().firstEmpty() == -1)
				player.getWorld().dropItem(player.getLocation(), result.clone());
			else
				player.getInventory().addItem(result.clone());
		}

		logDebug("[handleShiftClicks] Added x" + itemsToAdd);
	}
}
