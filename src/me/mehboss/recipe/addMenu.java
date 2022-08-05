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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class addMenu implements Listener {

	// *** this menu is the GUI for adding a new recipe

	private Inventory inv;

	HashMap<String, String> recipeName = new HashMap<String, String>();
	Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");

	public addMenu(Plugin p, String item) {
		inv = Bukkit.getServer().createInventory(null, 45,
				ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("ADD.Menu-Title")));
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
		if (Main.addRecipe.contains(e.getPlayer().getName())) {

			Main.addRecipe.remove(e.getPlayer().getName());
			recipeName.put(e.getPlayer().getName(), e.getMessage());

			e.setCancelled(true);

			inv = Bukkit.getServer().createInventory(null, 45,
					ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("ADD.Menu-Title")
							.replaceAll("%recipe%", recipeName.get(e.getPlayer().getName()).toUpperCase())));
			setItems(inv);
			showadd(e.getPlayer());
		}
	}

	public void setItems(Inventory i) {

		ItemStack stained = null;
		try {
			// Minecraft 1.9
			stained = new ItemStack(Material.matchMaterial("WHITE_STAINED_GLASS_PANE"));
		} catch (Exception e) {
			// Minecraft 1.8.8
			stained = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
		}

		for (int j = 0; j < 45; j++) {
			if (j != 11 && j != 12 && j != 13 && j != 20 && j != 21 && j != 22 && j != 29 && j != 30 && j != 31
					&& j != 24) {
				i.setItem(j, stained);
			}
		}
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		Player p = (Player) e.getWhoClicked();

		if (e.getInventory() != null && e.getInventory().getName() != null) {
			if (recipeName.get(p.getName()) != null) {

				String RecipeN = ChatColor.translateAlternateColorCodes('&', config.getConfig()
						.getString("ADD.Menu-Title").replaceAll("%recipe%", recipeName.get(p.getName()).toUpperCase()));

				if (e.getClickedInventory().getName().equals(RecipeN)) {
					if (e.getRawSlot() != 11 && e.getRawSlot() != 12 && e.getRawSlot() != 13 && e.getRawSlot() != 20
							&& e.getRawSlot() != 21 && e.getRawSlot() != 22 && e.getRawSlot() != 29
							&& e.getRawSlot() != 30 && e.getRawSlot() != 31 && e.getRawSlot() != 24) {
						e.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (e.getInventory() != null && e.getInventory().getName() != null
				&& recipeName.get(e.getPlayer().getName()) != null) {

			String RecipeN = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("ADD.Menu-Title")
					.replaceAll("%recipe%", recipeName.get(e.getPlayer().getName()).toUpperCase()));

			if (e.getInventory().getName().equals(RecipeN)) {

				if (hasSomething(e.getInventory()) == true) {

					System.out.print("it does match!");
					System.out.print(recipeName.get(e.getPlayer().getName()));
					System.out.print(e.getInventory().getName());
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
