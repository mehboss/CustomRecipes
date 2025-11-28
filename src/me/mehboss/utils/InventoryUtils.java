package me.mehboss.utils;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Streams;

/**
 * Utility class for handling various inventory-related operations such as:
 * <ul>
 *     <li>Custom click interaction behavior</li>
 *     <li>Stack merging logic</li>
 *     <li>Space calculations for inventory storage</li>
 *     <li>Detecting empty inventory slots</li>
 * </ul>
 *
 * <p>This class is stateless and all methods are static.</p>
 */
public class InventoryUtils {

    /**
     * Wrapper method that reads the cursor and clicked item directly from the event.
     *
     * @param event The inventory click event.
     */
	public static void calculateClickedSlot(InventoryClickEvent event) {
		calculateClickedSlot(event, event.getCursor(), event.getCurrentItem());
	}

    /**
     * Manually computes the behavior of clicking inside an inventory, overriding
     * Bukkit's default logic. Supports custom stack behavior for both left and
     * right clicks.
     *
     * <p>Actions supported:</p>
     * <ul>
     *     <li>Left-click stack swapping</li>
     *     <li>Left-click stack merge</li>
     *     <li>Right-click single-item placement</li>
     *     <li>Right-click stack merge</li>
     * </ul>
     *
     * @param event       The click event being processed.
     * @param cursor      The item on the player's cursor.
     * @param currentItem The item currently in the clicked slot.
     */
	public static void calculateClickedSlot(final InventoryClickEvent event, final ItemStack cursor,
			final ItemStack currentItem) {
		if (cursor == null)
			return;
		if (event.getClick().isLeftClick()) {
			if (!isAirOrNull(currentItem)) {
				event.setCancelled(true);
				if (currentItem.isSimilar(cursor)) {
					int possibleAmount = currentItem.getMaxStackSize() - currentItem.getAmount();
					currentItem.setAmount(currentItem.getAmount() + (Math.min(cursor.getAmount(), possibleAmount)));
					cursor.setAmount(cursor.getAmount() - possibleAmount);
					event.setCurrentItem(currentItem);
					event.setCursor(cursor);
				} else {
					event.setCursor(currentItem);
					event.setCurrentItem(cursor);
				}
			} else if (!event.getAction().equals(InventoryAction.PICKUP_ALL)) {
				event.setCancelled(true);
				event.setCursor(new ItemStack(Material.AIR));
				event.setCurrentItem(cursor);
			}
		} else if (event.getClick().isRightClick()) {
			if (!isAirOrNull(currentItem)) {
				if (currentItem.isSimilar(cursor)) {
					if (currentItem.getAmount() < currentItem.getMaxStackSize() && cursor.getAmount() > 0) {
						event.setCancelled(true);
						currentItem.setAmount(currentItem.getAmount() + 1);
						cursor.setAmount(cursor.getAmount() - 1);
					}
				} else {
					event.setCancelled(true);
					event.setCursor(currentItem);
					event.setCurrentItem(cursor);
				}
			} else {
				event.setCancelled(true);
				ItemStack itemStack = cursor.clone();
				cursor.setAmount(cursor.getAmount() - 1);
				itemStack.setAmount(1);
				event.setCurrentItem(itemStack);
			}
		}
		if (event.getWhoClicked() instanceof Player) {
			((Player) event.getWhoClicked()).updateInventory();
		}
	}

    /**
     * Checks if an item is null or AIR.
     *
     * @param item The item to check.
     * @return True if null or AIR.
     */
	public static boolean isAirOrNull(ItemStack item) {
		return item == null || item.getType().equals(Material.AIR);
	}

    /**
     * Checks if an inventory has room for a given number of a specific item.
     *
     * @param inventory Inventory to check.
     * @param itemStack The item type being added.
     * @param amount    Number of stacks to fit.
     * @return True if enough space exists.
     */
	public static boolean hasInventorySpace(Inventory inventory, ItemStack itemStack, int amount) {
		return getInventorySpace(inventory, itemStack) >= itemStack.getAmount() * amount;
	}

    /**
     * Checks if an inventory has space for one stack of the given item.
     *
     * @param inventory Inventory to check.
     * @param itemStack Item being added.
     * @return True if enough space exists.
     */
	public static boolean hasInventorySpace(Inventory inventory, ItemStack itemStack) {
		return hasInventorySpace(inventory.getStorageContents(), itemStack);
	}

    /**
     * Checks if inventory contents have enough space for the given item.
     *
     * @param contents  Inventory contents.
     * @param itemStack Item being added.
     * @return True if space is available.
     */
	public static boolean hasInventorySpace(ItemStack[] contents, ItemStack itemStack) {
		return getInventorySpace(contents, itemStack) >= itemStack.getAmount();
	}

    /**
     * Checks if a player has room for the given item.
     *
     * @param p    Player whose inventory will be checked.
     * @param item Item the player wants to add.
     * @return True if enough space exists.
     */
	public static boolean hasInventorySpace(Player p, ItemStack item) {
		return getInventorySpace(p, item) >= item.getAmount();
	}

    /**
     * Computes available space for a specific item in a player's inventory.
     *
     * @param p    Player to check.
     * @param item Item type that may be added.
     * @return The number of items that can fit.
     */
	public static int getInventorySpace(Player p, ItemStack item) {
		return getInventorySpace(p.getInventory(), item);
	}

    /**
     * Computes available space in an inventory for one specific item type.
     *
     * @param inventory The inventory to scan.
     * @param item      The item type being checked.
     * @return The total number of items that can fit.
     */
	public static int getInventorySpace(Inventory inventory, ItemStack item) {
		return getInventorySpace(inventory.getStorageContents(), item);
	}

    /**
     * Computes total available capacity for an item based on empty slots and
     * partially filled stacks that match.
     *
     * @param contents Inventory contents.
     * @param item     Item type being evaluated.
     * @return Total capacity for the item.
     */
	public static int getInventorySpace(ItemStack[] contents, ItemStack item) {
		int free = 0;
		for (ItemStack i : contents) {
			if (isAirOrNull(i)) {
				free += item.getMaxStackSize();
			} else if (i.isSimilar(item)) {
				free += item.getMaxStackSize() - i.getAmount();
			}
		}
		return free;
	}

    /**
     * Checks whether a player has at least a specified number of empty slots.
     *
     * @param p     The player whose inventory to check.
     * @param count Minimum number of empty slots needed.
     * @return True if at least {@code count} empty slots exist.
     */
	public static boolean hasEmptySpaces(Player p, int count) {
		return Streams.stream(p.getInventory()).filter(Objects::isNull).count() >= count;
	}
}
