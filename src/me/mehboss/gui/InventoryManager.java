package me.mehboss.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.cryptomorin.xseries.XMaterial;

import io.github.bananapuncher714.nbteditor.NBTEditor;

public class InventoryManager implements Listener {

	private final Map<String, Inventory> inventories = new HashMap<>();
	private final Map<String, List<Inventory>> pagedInventories = new HashMap<>();

	public InventoryManager(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	// ------------------------- INVENTORY METHODS ----------------------------

	public void createInventory(String id, int size, String title) {
		inventories.put(id, Bukkit.createInventory(null, size, title));
	}

	public void createPagedInventory(String id, int pageSize, String title) {
		List<Inventory> pages = new ArrayList<>();
		pages.add(Bukkit.createInventory(null, pageSize, title + " - Page 1"));
		pagedInventories.put(id, pages);
	}

	public Inventory getInventory(String id, int page) {
		return pagedInventories.containsKey(id) ? pagedInventories.get(id).get(page) : inventories.get(id);
	}

	public void addItem(String id, ItemStack item, int page) {
		if (pagedInventories.containsKey(id)) {
			List<Inventory> pages = pagedInventories.get(id);
			if (page < pages.size()) {
				pages.get(page).addItem(item);
			} else {
				Bukkit.getLogger().warning("Page " + page + " doesn't exist!");
			}
		} else {
			inventories.get(id).addItem(item);
		}
	}

	public void removeItem(String id, ItemStack item, int page) {
		if (pagedInventories.containsKey(id)) {
			List<Inventory> pages = pagedInventories.get(id);
			pages.get(page).remove(item);
		} else {
			inventories.get(id).remove(item);
		}
	}

	public void addFillerItem(String id, ItemStack item, int... slots) {
		Inventory inventory = inventories.get(id);
		if (inventory != null) {
			for (int slot : slots) {
				inventory.setItem(slot, item);
			}
		}
	}

	// ------------------------- PAGE MANAGEMENT ------------------------------

	public void addPage(String id, int size, String title) {
		if (pagedInventories.containsKey(id)) {
			List<Inventory> pages = pagedInventories.get(id);
			pages.add(Bukkit.createInventory(null, size, title + " - Page " + (pages.size() + 1)));
		}
	}

	public int getPageCount(String id) {
		return pagedInventories.containsKey(id) ? pagedInventories.get(id).size() : 1;
	}

	public void openInventory(Player player, String id, int page) {
		Inventory inventory = getInventory(id, page);
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

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Inventory inventory = event.getInventory();
		if (inventories.containsValue(inventory) || containsPagedInventory(inventory)) {
			event.setCancelled(true); // Cancel click events
			Player player = (Player) event.getWhoClicked();
			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem != null && clickedItem.getType() != Material.AIR) {
				player.sendMessage("Clicked on: " + clickedItem.getItemMeta().getDisplayName());
			}
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		Inventory inventory = event.getInventory();
		if (inventories.containsValue(inventory) || containsPagedInventory(inventory)) {
			event.setCancelled(true); // Cancel drag events
		}
	}

	public boolean containsPagedInventory(Inventory inventory) {
		return pagedInventories.values().stream().anyMatch(pages -> pages.contains(inventory));
	}
}