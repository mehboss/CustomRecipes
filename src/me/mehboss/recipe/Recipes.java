package me.mehboss.recipe;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class Recipes implements Listener {

	private addMenu addR;

	// *** this menu is the main GUI for recipes. it shows what recipes exist and
	// gives options to add a new recipe

	public Recipes(addMenu plugin) {
		this.addR = plugin;
	}

	public editMenu editItem;
	public static Inventory inv;
	public static Inventory inv2;

	public static String[] r = null;
	public static String rname = null;

	public static ArrayList<ShapedRecipe> rshape = new ArrayList<ShapedRecipe>();
	
	ItemStack i = null;
	ArrayList<ItemStack> recipe = new ArrayList<ItemStack>();

	public void showedit(Player p) {
		p.openInventory(editMenu.inv);
	}

	public Recipes(Plugin p, String item) {
		Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");
		inv = Bukkit.getServer().createInventory(null, 54,
				ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("GUI.Displayname")));
		inv2 = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
				config.getConfig().getString("GUI.Displayname") + " Page 2"));
		items();
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		Plugin config = Bukkit.getServer().getPluginManager().getPlugin("CustomRecipes");
		if (e.getClickedInventory().getName() != null) {
			if (e.getClickedInventory().getName().equals(
					ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("GUI.Displayname")))
					|| e.getClickedInventory().getName().equals(ChatColor.translateAlternateColorCodes('&',
							config.getConfig().getString("GUI.Displayname") + " Page 2"))) {

				Player p = (Player) e.getWhoClicked();

				e.setCancelled(true);

				if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
					return;
				}

				if (e.getRawSlot() == 48) {
					e.getWhoClicked().closeInventory();
					e.getWhoClicked().openInventory(inv);
					return;
				}

				if (e.getRawSlot() == 50) {
					e.getWhoClicked().closeInventory();
					e.getWhoClicked().openInventory(inv2);
					return;
				}

				for (ShapedRecipe item : Main.recipe) {

					if (e.getCurrentItem().getItemMeta().equals(item.getResult().getItemMeta())) {
						r = item.getShape();
						rname = e.getCurrentItem().getItemMeta().getDisplayName();
						
						for (String i : item.getShape()) {
							
						}
						
						editItem = new editMenu(config, null);
						
						p.closeInventory();
						showedit(p);
						break;
					}
				}

				if (e.getRawSlot() == 49) {
					p.closeInventory();
					Main.addRecipe.add(p.getName());
					p.sendMessage("Type the name of the recipe here!");

				}

				return;
			}
		}
	}

	private void items() {

		int i = 0;
		ItemStack stained = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);

		ItemStack greenstained = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13);
		ItemMeta gs = greenstained.getItemMeta();
		gs.setDisplayName(ChatColor.GREEN + "Next Page");
		greenstained.setItemMeta(gs);

		ItemStack redstained = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
		ItemMeta rs = redstained.getItemMeta();
		rs.setDisplayName(ChatColor.RED + "Previous Page");
		redstained.setItemMeta(rs);

		ItemStack whitestained = new ItemStack(Material.STAINED_GLASS_PANE, 1);
		ItemMeta ws = whitestained.getItemMeta();
		ws.setDisplayName(ChatColor.WHITE + "Add Recipe");
		whitestained.setItemMeta(ws);

		if (!(Main.recipe.isEmpty()) && Main.recipe != null) {
			for (ShapedRecipe item : Main.recipe) {
				i++;

				if (i > 45 && i < 91) {
					inv2.addItem(new ItemStack(item.getResult()));
				}

				if (i < 46) {
					inv.addItem(new ItemStack(item.getResult()));
				}
			}
		}
		defaults(inv, redstained, whitestained, greenstained, stained);
		defaults(inv2, redstained, whitestained, greenstained, stained);
	}

	public void defaults(Inventory i, ItemStack r, ItemStack w, ItemStack g, ItemStack d) {
		i.setItem(45, d);
		i.setItem(46, d);
		i.setItem(47, d);

		i.setItem(48, r);
		i.setItem(49, w);
		i.setItem(50, g);

		i.setItem(51, d);
		i.setItem(52, d);
		i.setItem(53, d);
	}

	public void show(Player p) {
		p.openInventory(inv);
	}
}
