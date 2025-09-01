package me.mehboss.utils;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Streams;

public class InventoryUtils {

	public static void calculateClickedSlot(InventoryClickEvent event) {
		calculateClickedSlot(event, event.getCursor(), event.getCurrentItem());
	}

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

	public static boolean isAirOrNull(ItemStack item) {
		return item == null || item.getType().equals(Material.AIR);
	}

	public static boolean hasInventorySpace(Inventory inventory, ItemStack itemStack, int amount) {
		return getInventorySpace(inventory, itemStack) >= itemStack.getAmount() * amount;
	}

	public static boolean hasInventorySpace(Inventory inventory, ItemStack itemStack) {
		return hasInventorySpace(inventory.getStorageContents(), itemStack);
	}

	public static boolean hasInventorySpace(ItemStack[] contents, ItemStack itemStack) {
		return getInventorySpace(contents, itemStack) >= itemStack.getAmount();
	}

	public static boolean hasInventorySpace(Player p, ItemStack item) {
		return getInventorySpace(p, item) >= item.getAmount();
	}

	public static int getInventorySpace(Player p, ItemStack item) {
		return getInventorySpace(p.getInventory(), item);
	}

	public static int getInventorySpace(Inventory inventory, ItemStack item) {
		return getInventorySpace(inventory.getStorageContents(), item);
	}

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

	public static boolean hasEmptySpaces(Player p, int count) {
		return Streams.stream(p.getInventory()).filter(Objects::isNull).count() >= count;
	}
}
