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

			logDebug("[handleShiftClicks] ItemsToRemove: " + itemsToRemove + " - ItemsToAdd: " + itemsToAdd);
			logDebug("[handleShiftClicks] ItemAmount: " + availableItems + "|| RequiredAmount: " + requiredAmount);
			logDebug("[handleShiftClicks] Identifier: " + ingredient.getIdentifier() + " - ID? "
					+ ingredient.hasIdentifier());
			logDebug("[handleShiftClicks] Material: " + ingredient.getMaterial().toString());
			logDebug("[handleShiftClicks] Displayname: " + ingredient.getDisplayName());

			if (availableItems < requiredAmount)
				return;

			String id = ingredient.hasIdentifier() ? ingredient.getIdentifier() : item.getType().toString();
			if (recipe.isLeftover(id)) {
				if (item.getType().toString().contains("_BUCKET"))
					item.setType(XMaterial.BUCKET.parseMaterial());
				return;
			}

			if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if ((item.getAmount() + 1) - requiredAmount == 0)
					inv.setItem(slot, null);
				else
					item.setAmount((item.getAmount() + 1) - requiredAmount);
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

		return ((ingredient.hasIdentifier() && NBTEditor.contains(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")
				&& ingredient.getIdentifier()
						.equals(NBTEditor.getString(item, NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")))
				|| (customItem != null && item.isSimilar(customItem))
				|| (customItem != null
						&& craftManager.checkCustomItems(customItem, item, ingredient.getIdentifier(), true))
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

		// added debounce mechanism, which will automatically remove after 75ms to
		// greatly improve server performance on large amounts of crafts (such as
		// concrete power)
		Main.getInstance().debounceMap.put(id, System.currentTimeMillis());
		if (getRecipeUtil().getRecipeFromResult(inv.getResult()) == null)
			return;

		logDebug("[handleShiftClicks] Fired amount checking mechanics..");

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		String findName = getRecipeUtil().getRecipeFromResult(inv.getResult()) != null
				? getRecipeUtil().getRecipeFromResult(inv.getResult()).getName()
				: null;

		final ItemStack result = inv.getResult();
		boolean isShapeless = e.getRecipe() instanceof ShapelessRecipe ? true : false;
		HashMap<String, Recipe> types = isShapeless ? getRecipeUtil().getRecipesFromType(RecipeType.SHAPELESS)
				: getRecipeUtil().getRecipesFromType(RecipeType.SHAPED);

		logDebug("[handleShiftClicks] Initial recipe found recipe '" + findName + "' to handle..");

		if (NBTEditor.contains(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER")) {
			String foundID = NBTEditor.getString(inv.getResult(), NBTEditor.CUSTOM_DATA, "CUSTOM_ITEM_IDENTIFIER");

			if (getRecipeUtil().getRecipeFromKey(foundID) != null) {
				findName = getRecipeUtil().getRecipeFromKey(foundID).getName();
			}
		}

		// safe guard check to prevent matches to other recipes types
		if (types != null && !types.isEmpty())
			for (Recipe recipe : types.values()) {
				ItemStack item = recipe.getResult();
				if (hasAllIngredients(inv, recipe.getName(), recipe.getIngredients(), id)
						&& (result.equals(item) || result.isSimilar(item))) {
					findName = recipe.getName();
					break;
				}
			}

		logDebug("[handleShiftClicks] Actual found recipe '" + findName + "' to handle..");

		if (findName == null)
			return;

		logDebug("[handleShiftClicks] Paired it to a custom recipe. Running crafting amount calculations..");

		final String recipeName = findName;
		if (e.isCancelled()) {
			logDebug("Couldn't complete craftItemEvent for recipe " + recipeName
					+ ", the event was unexpectedly cancelled.");
			logDebug("Please seek support or open a ticket https://github.com/mehboss/CustomRecipes/issues");
			return;
		}

		Recipe recipe = getRecipeUtil().getRecipe(recipeName);
		ArrayList<String> handledIngredients = new ArrayList<String>();

		// =========================
		// PASS 1: compute itemsToAdd
		// bottleneck across ingredients based on required amounts
		// =========================
		int itemsToAdd = Integer.MAX_VALUE;
		int itemsToRemove = 0;

		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			final int requiredAmount = Math.max(1, ingredient.getAmount());
			int totalAvailable = 0;

			if (recipe.getType() == RecipeType.SHAPELESS) {
				// Sum across ALL crafting grid slots (1..9) that match THIS ingredient
				for (int i = 1; i < 10; i++) {
					ItemStack slot = inv.getItem(i);
					if (slot == null || slot.getType() == Material.AIR)
						continue;

					if (matchesIngredient(slot, recipeName, ingredient, ingredient.getMaterial(),
							ingredient.getDisplayName(), ingredient.hasIdentifier())) {
						totalAvailable += slot.getAmount();
					}
				}
			} else {
				// SHAPED / fixed-slot types: only the ingredient's slot counts
				int fixed = ingredient.getSlot();
				ItemStack slot = inv.getItem(fixed);
				if (slot != null && slot.getType() != Material.AIR && matchesIngredient(slot, recipeName, ingredient,
						ingredient.getMaterial(), ingredient.getDisplayName(), ingredient.hasIdentifier())) {
					totalAvailable = slot.getAmount();
				}
			}

			int possibleForThisIngredient = totalAvailable / requiredAmount; // floor
			itemsToAdd = Math.min(itemsToAdd, possibleForThisIngredient);
		}

		// If nothing craftable, bail before removal logic
		if (itemsToAdd <= 0 || itemsToAdd == Integer.MAX_VALUE) {
			logDebug("[handleShiftClicks][" + findName
					+ "] An issue has been detected whilest calculating amount deductions. Please reach out for support to report this.");
			e.setCancelled(true);
			return;
		}

		// =========================
		// PASS 2: your existing removal logic (UNCHANGED)
		// Keep itemsToRemove and handlesItemRemoval calls as you have them.
		// =========================
		for (RecipeUtil.Ingredient ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty())
				continue;

			final Material material = ingredient.getMaterial();
			final String displayName = ingredient.getDisplayName();
			final int requiredAmount = Math.max(1, ingredient.getAmount());
			final boolean hasIdentifier = ingredient.hasIdentifier();

			// === Your existing per-type logic ===
			if (recipe.getType() == RecipeType.SHAPELESS) {
				logDebug("[handleShiftClicks] Found shapeless recipe to handle..");

				for (int i = 1; i < 10; i++) {
					ItemStack item = inv.getItem(i);
					int slot = i;

					if (item == null || item.getType() == Material.AIR)
						continue;

					if (!matchesIngredient(item, recipeName, ingredient, material, displayName, hasIdentifier))
						continue;

					// If your original removal didn’t require an explicit matchesIngredient()
					// here, leave it as-is. (You earlier said only Pass 1 needs matching.)
					if (!handledIngredients.contains(ingredient.getAbbreviation())) {
						handlesItemRemoval(e, inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd,
								requiredAmount);
					}
				}

				if (!handledIngredients.contains(ingredient.getAbbreviation()))
					handledIngredients.add(ingredient.getAbbreviation());

			} else {
				logDebug("[handleShiftClicks] Found other recipe type to handle..");

				ItemStack item = inv.getItem(ingredient.getSlot());
				int slot = ingredient.getSlot();

				if (item == null || item.getType() == Material.AIR)
					continue;

				// Same note: keep your original behavior.
				handlesItemRemoval(e, inv, recipe, item, ingredient, slot, itemsToRemove, itemsToAdd, requiredAmount);
			}
		}

		// Add the result items to the player's inventory
		Player player = (Player) e.getWhoClicked();
		Main.getInstance().cooldownManager.setCooldown(player.getUniqueId(), recipe.getKey(),
				System.currentTimeMillis());

		if (recipe.hasCommands()) {
			logDebug("[ExecuteCMD] Found commands to run for recipe " + recipeName);

			for (String command : recipe.getCommand()) {
				String parsedCommand = command.replace("%crafter%", player.getName());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
			}

			if (!recipe.isGrantItem()) {
				logDebug("[ExecuteCMD] Cancelling craft of " + recipeName
						+ " because a command to perform was found instead.");
				Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
					@Override
					public void run() {
						e.getWhoClicked().setItemOnCursor(null);
					}
				}, 2L);

				return;
			}
		}
		// delayed task to prevent debug spam
		Main.getInstance().inInventory.add(id);
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
			Main.getInstance().inInventory.remove(id);
		}, 2L);

		if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			logDebug("[handleShiftClicks] Didn't detect shift click from inventory.. Ignoring..");

		} else {
			e.setCancelled(true);

			if (recipe.hasCommands() && !recipe.isGrantItem())
				return;

			inv.setResult(new ItemStack(Material.AIR));

			for (int i = 0; i < itemsToAdd; i++) {
				if (player.getInventory().firstEmpty() == -1) {
					player.getLocation().getWorld().dropItem(player.getLocation(), result);
					continue;
				}
				player.getInventory().addItem(result);
			}

			logDebug("[handleShiftClicks] Shift click detected. Adding " + itemsToAdd + " to inventory.");
			logDebug("[handleShiftClicks] Added " + itemsToAdd + " items and removed items from table.");
		}
	}
}
