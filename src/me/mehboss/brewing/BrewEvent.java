package me.mehboss.brewing;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.mehboss.recipe.Main;
import me.mehboss.utils.data.BrewingRecipeData;

/**
 * Handles custom brewing logic: - Allows non-standard items in brewing stands -
 * Prevents item disappearance with shift-clicks - Automatically starts brewing
 * when valid custom recipes are present - Supports both empty-slot and
 * bottle-required brewing
 */
public class BrewEvent implements Listener {

	@EventHandler
	void onCustomPotionClick(InventoryClickEvent event) {
		Inventory inv = event.getClickedInventory();
		if (inv == null || inv.getType() != InventoryType.BREWING)
			return;
		if (!(inv instanceof BrewerInventory))
			return;

		BrewerInventory brewer = (BrewerInventory) inv;
		Player player = (Player) event.getWhoClicked();
		int slot = event.getRawSlot();

	    if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
	        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
	            if (brewer.getHolder().getBrewingTime() > 0)
	                return;

	            BrewingRecipeData recipe = BrewingRecipe.getRecipe(brewer);
	            if (recipe == null)
	                return;

	            if (!canBrewIntoResultSlots(brewer, recipe))
	                return;

	            BrewingRecipe.startBrewing(brewer, recipe);
	        }, 2L);
	        return;
	    }

	    // --- Regular left/right click handling ---
	    if (!(event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT))
	        return;

		event.setCancelled(true); // override vanilla behavior safely

		ItemStack clicked = event.getCurrentItem();
		ItemStack cursor = event.getCursor();

		if (clicked == null)
			clicked = new ItemStack(Material.AIR);
		if (cursor == null)
			cursor = new ItemStack(Material.AIR);

		boolean compare = clicked.isSimilar(cursor);
		ClickType clickType = event.getClick();
		int firstAmount = clicked.getAmount();
		int secondAmount = cursor.getAmount();
		int stackMax = clicked.getMaxStackSize();

		// --- Manual placement logic ---
		if (clickType == ClickType.LEFT) {
			if (clicked.getType() == Material.AIR) {
				inv.setItem(slot, cursor.clone());
				player.setItemOnCursor(null);
			} else if (compare) {
				int canAdd = stackMax - firstAmount;
				if (secondAmount <= canAdd) {
					clicked.setAmount(firstAmount + secondAmount);
					player.setItemOnCursor(null);
				} else {
					cursor.setAmount(secondAmount - canAdd);
					clicked.setAmount(stackMax);
					player.setItemOnCursor(cursor);
				}
			} else {
				inv.setItem(slot, cursor.clone());
				player.setItemOnCursor(clicked.clone());
			}
		} else if (clickType == ClickType.RIGHT) {
			if (clicked.getType() == Material.AIR) {
				ItemStack one = cursor.clone();
				one.setAmount(1);
				inv.setItem(slot, one);
				cursor.setAmount(secondAmount - 1);
				if (cursor.getAmount() <= 0)
					player.setItemOnCursor(null);
				else
					player.setItemOnCursor(cursor);
			} else if (compare) {
				if (firstAmount + 1 <= stackMax) {
					clicked.setAmount(firstAmount + 1);
					cursor.setAmount(secondAmount - 1);
					if (cursor.getAmount() <= 0)
						player.setItemOnCursor(null);
				}
			} else {
				inv.setItem(slot, cursor.clone());
				player.setItemOnCursor(clicked.clone());
			}
		}

		// --- Run brew check after inventory updates (next tick) ---
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {

			// If stand is already brewing, don't restart it
			if (brewer.getHolder().getBrewingTime() > 0)
				return;

			// Lookup recipe (now that slots are updated)
			BrewingRecipeData recipe = BrewingRecipe.getRecipe(brewer);
			if (recipe == null)
				return;

			// Validate bottle/output slots before brewing
			if (!canBrewIntoResultSlots(brewer, recipe))
				return;

			BrewingRecipe.startBrewing(brewer, recipe);
		}, 2L);
	}

	/**
	 * Validates that there’s at least one valid bottle slot or empty slot depending
	 * on whether the recipe requires bottles.
	 */
	private boolean canBrewIntoResultSlots(BrewerInventory inventory, BrewingRecipeData recipe) {
		boolean requiresBottles = recipe.requiresBottles();
		Material requiredBottle = recipe.getRequiredBottleType();

		for (int i = 0; i < 3; i++) {
			ItemStack item = inventory.getItem(i);
			if (!requiresBottles) {
				if (item == null || item.getType() == Material.AIR)
					return true;
			} else {
				if (item != null && item.getType() == requiredBottle)
					return true;
			}
		}
		return false;
	}
}