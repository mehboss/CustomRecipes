package me.mehboss.recipe;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class addMenu implements Listener {

	// *** this menu is the GUI for adding a new recipe

	private Inventory inv;

	HashMap<String, String> recipeName = new HashMap<String, String>();

	public addMenu(Plugin p, String item) {
		inv = Bukkit.getServer().createInventory(null, 45,
				ChatColor.translateAlternateColorCodes('&', Main.getInstance().messagesConfig.getString("add.Title")));
		setItems(inv);
		Bukkit.getServer().getPluginManager().registerEvents(this, p);
	}

	public void show(Player p) {
		p.openInventory(Recipes.inv);
	}

	public void showadd(Player p) {
		p.openInventory(inv);
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (Main.getInstance().addRecipe.contains(e.getPlayer().getName())) {
			Main.getInstance().addRecipe.remove(e.getPlayer().getName());

			inv = Bukkit.getServer().createInventory(null, 45, ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().messagesConfig.getString("add.Title").replace("%recipe%", e.getMessage())));

			e.setCancelled(true);
			setItems(inv);
			showadd(e.getPlayer());
		}
	}

	public void setItems(Inventory i) {

		Material stained = XMaterial.WHITE_STAINED_GLASS_PANE.parseMaterial();

		for (int j = 0; j < 45; j++) {
			if (j != 11 && j != 12 && j != 13 && j != 20 && j != 21 && j != 22 && j != 29 && j != 30 && j != 31
					&& j != 24) {
				i.setItem(j, new ItemStack(stained));
			}
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		Player p = (Player) e.getWhoClicked();

		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.PLAYER
				|| recipeName.get(p.getName()) == null) {
			return;
		}

		String RecipeN = ChatColor.translateAlternateColorCodes('&', Main.getInstance().getConfig()
				.getString("add.Title").replaceAll("%recipe%", recipeName.get(p.getName()).toUpperCase()));

		if (e.getView().getTitle() != null && e.getView().getTitle().equals(RecipeN)
				&& recipeName.get(p.getName()) != null) {

			if (e.getRawSlot() != 11 && e.getRawSlot() != 12 && e.getRawSlot() != 13 && e.getRawSlot() != 20
					&& e.getRawSlot() != 21 && e.getRawSlot() != 22 && e.getRawSlot() != 29 && e.getRawSlot() != 30
					&& e.getRawSlot() != 31 && e.getRawSlot() != 24) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {

		if (e.getInventory() == null || e.getInventory().getType() != InventoryType.PLAYER
				|| recipeName.get(e.getPlayer().getName()) == null) {
			return;
		}

		if (recipeName.get(e.getPlayer().getName()) != null && e.getView().getTitle() != null) {

			String RecipeN = ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().messagesConfig.getString("add.Title").replaceAll("%recipe%",
							recipeName.get(e.getPlayer().getName()).toUpperCase()));

			if (e.getView().getTitle().equals(RecipeN)) {

				if (hasSomething(e.getInventory()) == true) {

					System.out.print("it does match!");
					System.out.print(recipeName.get(e.getPlayer().getName()));
					System.out.print(e.getView().getTitle());
					System.out.print(e.getPlayer().getName());
				}
			}
		}
	}

	public boolean hasSomething(Inventory i) {

		if (i.getItem(11) != null || i.getItem(12) != null || i.getItem(13) != null || i.getItem(20) != null
				|| i.getItem(21) != null || i.getItem(22) != null || i.getItem(29) != null || i.getItem(30) != null
				|| i.getItem(31) != null) {
			return true;
		}
		return false;
	}
}
