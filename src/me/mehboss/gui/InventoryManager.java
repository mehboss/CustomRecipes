package me.mehboss.gui;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

public class InventoryManager {

	private final Map<String, Inventory> inventories = new HashMap<>();

	// ------------------------- INVENTORY METHODS ----------------------------

	public void createInventory(String id, int size, String title) {
		inventories.put(id, Bukkit.createInventory(null, size, title));
	}

	public Inventory getInventory(String id) {
		return inventories.get(id);
	}

	public void addItem(String id, ItemStack item, int page) {
			inventories.get(id).addItem(item);
	}

	public void removeItem(String id, ItemStack item, int page) {
			inventories.get(id).remove(item);
	}

	public void addFillerItem(String id, ItemStack item, int... slots) {
		Inventory inventory = inventories.get(id);
		if (inventory != null) {
			for (int slot : slots) {
				inventory.setItem(slot, item);
			}
		}
	}

	public void openInventory(Player player, String id, int page) {
		Inventory inventory = getInventory(id);
		if (inventory != null) {
			player.openInventory(inventory);
		}
	}

	// --------------------- ITEM CREATION HELPER -----------------------------

	public ItemStack createItem(String id, XMaterial material, String name, String... lore) {
		ItemStack item = new ItemStack(material.parseItem());
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

			if (lore != null)
				meta.setLore(Arrays.asList(lore));

			item.setItemMeta(meta);
		}
		return item;
	}

	// ------------------- EVENT HANDLING METHODS -----------------------------
}