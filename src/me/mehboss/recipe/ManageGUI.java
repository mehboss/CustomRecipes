package me.mehboss.recipe;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class ManageGUI implements Listener {

	// *** this menu is the main GUI for recipes. it shows what recipes exist and
	// gives options to add a new recipe

	public static Inventory inv;

	public static String[] r = null;
	public static String rname = null;

	public static ArrayList<ShapedRecipe> rshape = new ArrayList<ShapedRecipe>();

	ItemStack i = null;
	ArrayList<ItemStack> recipe = new ArrayList<ItemStack>();

	public FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public ManageGUI(Plugin p, String item) {
		inv = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
				getConfig().getString("gui.Displayname").replace("%page%", "1")));

		items(inv, 0);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null) {
			return;
		}

		if (e.getClickedInventory().getType() == InventoryType.CHEST) {

			if (e.getClickedInventory() == null || e.getView().getTitle() == null) {
				return;
			}

			if (e.getView().getTitle().contains(ChatColor.translateAlternateColorCodes('&',
					getConfig().getString("gui.Displayname").replaceAll(" %page%", "")))) {

				Player p = (Player) e.getWhoClicked();

				e.setCancelled(true);

				if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
					return;
				}

				if (e.getRawSlot() == 48 || e.getRawSlot() == 50) {
					String original = e.getView().getTitle();
					String[] cp = e.getView().getTitle().split("Page ");
					int currentpage = Integer.valueOf(cp[1]);
					int newpage = currentpage;

					if (e.getRawSlot() == 48 && String.valueOf(currentpage).equals("1")) {
						return;
					}

					if (e.getRawSlot() == 48) {
						newpage--;
					}

					if (e.getRawSlot() == 50) {
						newpage++;
					}

					if (e.getInventory().firstEmpty() != -1) {
						return;
					}

					String newname = ChatColor.translateAlternateColorCodes('&',
							original.replaceAll(String.valueOf(currentpage), String.valueOf(newpage)));

					Inventory newp = Bukkit.getServer().createInventory(null, 54, newname);
					items(newp, newpage - 1);

					p.closeInventory();
					p.openInventory(newp);
					return;
				}

				if (e.getRawSlot() == 49) {
					return;
				}

				if (Main.getInstance().configName.containsKey(e.getCurrentItem())) {
					String name = Main.getInstance().configName.get(e.getCurrentItem());
					Inventory edit = Bukkit.getServer().createInventory(null, 54,
							ChatColor.translateAlternateColorCodes('&',
									"&cEDITING: " + name));
					EditGUI.getInstance().setItems(edit, name, e.getCurrentItem());
					p.closeInventory();
					p.openInventory(edit);
				}
			}

			return;
		}
	}

	private void items(Inventory inv, int page) {

		int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };

		for (int slot = page * slots.length; slot < (page + 1) * slots.length; slot++) {
			if (slot >= Main.getInstance().menui.size()) {
				break;
			}

			inv.setItem(slots[slot - page * slots.length], Main.getInstance().menui.get(slot));
		}

		// page 1 get 1-14
		// page 2 get 15 - 29
		// page 3 get 30 - 44
		// etc etc

		ItemStack stained = new ItemStack(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
		ItemMeta s = stained.getItemMeta();
		s.setDisplayName(" ");
		stained.setItemMeta(s);

		ItemStack greenstained = new ItemStack(XMaterial.GREEN_STAINED_GLASS_PANE.parseItem());
		ItemMeta gs = greenstained.getItemMeta();
		gs.setDisplayName(ChatColor.LIGHT_PURPLE + "Add Recipe");
		greenstained.setItemMeta(gs);

		ItemStack redstained = new ItemStack(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
		ItemMeta rs = redstained.getItemMeta();
		rs.setDisplayName(ChatColor.RED + "Previous Page");
		redstained.setItemMeta(rs);

		ItemStack orangestained = new ItemStack(XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem());
		ItemMeta ws = orangestained.getItemMeta();
		ws.setDisplayName(ChatColor.GREEN + "Next Page");
		orangestained.setItemMeta(ws);

		defaults(inv, redstained, orangestained, greenstained, stained);
	}

	public void defaults(Inventory i, ItemStack r, ItemStack o, ItemStack g, ItemStack d) {
		int[] set = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41,
				42, 43, 44, 45, 46, 47, 51, 52, 53 };

		for (int ii : set)
			i.setItem(ii, d);

		ItemStack bl = new ItemStack(XMaterial.WRITABLE_BOOK.parseItem());
		ItemMeta bm = bl.getItemMeta();
		bm.setDisplayName(ChatColor.DARK_AQUA + "Blacklisted Recipes");
		bl.setItemMeta(bm);

		i.setItem(4, bl);
		i.setItem(48, r);
		i.setItem(49, g);
		i.setItem(50, o);
	}

	public void show(Player p) {
		p.openInventory(inv);
	}
}
