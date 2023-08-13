package me.mehboss.recipe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import me.clip.placeholderapi.PlaceholderAPI;

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
	}

	HashMap<Integer, String> itemMaps = new HashMap<Integer, String>();

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
						if (e.getInventory().firstEmpty() != -1) {
							return;
						}

						newpage++;
					}

					String newname = ChatColor.translateAlternateColorCodes('&',
							original.replaceAll(String.valueOf(currentpage), String.valueOf(newpage)));

					Inventory newp = Bukkit.getServer().createInventory(null, 54, newname);
					items(p, newp, newpage - 1);
					setDefaults(p, newp);

					p.openInventory(newp);
					return;
				}

				if (e.getRawSlot() == 49
						&& e.getCurrentItem().getItemMeta().getDisplayName().strip().equals("Add Recipe")) {
					return;
				}

				if (NBTEditor.contains(e.getCurrentItem(), "CUSTOM_ITEM_IDENTIFIER") || Main.getInstance().giveRecipe
						.containsKey(e.getCurrentItem().getItemMeta().getDisplayName().toLowerCase())) {

					Boolean viewing = false;
					String name = e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName()
							? e.getCurrentItem().getItemMeta().getDisplayName()
							: e.getCurrentItem().getType().toString();
							
					Inventory edit = null;

					if (!(Main.getInstance().recipeBook.contains(p.getUniqueId()))) {
						edit = Bukkit.getServer().createInventory(null, 54,
								ChatColor.translateAlternateColorCodes('&', "&cEDITING: " + name));
					} else {
						edit = Bukkit.getServer().createInventory(null, 54,
								ChatColor.translateAlternateColorCodes('&', "&cVIEWING: " + name));
						viewing = true;
					}

					Main.getInstance().saveInventory.put(p.getUniqueId(), e.getInventory());
					EditGUI.getInstance().setItems(viewing, edit, name, e.getCurrentItem(), p);

					p.openInventory(edit);
				}
			}

			return;
		}
	}

	int slots;

	private void items(Player p, Inventory inv, int page) {

		int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
		ArrayList<String> items = new ArrayList<>(Main.getInstance().itemNames.keySet());
		Collections.sort(items);

		int startSlot = page * slots.length;
		int currentSlot = 0;
		for (int slot = 0; slot < slots.length; slot++) {
			int index = startSlot + slot;
			if (index >= items.size()) {
				break;
			}

			ItemStack item = new ItemStack(Main.getInstance().giveRecipe.get(items.get(index).toLowerCase()));
			ItemMeta itemM = item.getItemMeta();

			if (itemM.hasLore() && hasPlaceholder())
				itemM.setLore(PlaceholderAPI.setPlaceholders(p, itemM.getLore()));

			itemM.setDisplayName(items.get(index));
			item.setItemMeta(itemM);

			String loc = items.get(index);
			if (p != null && !p.hasPermission("crecipe.gui")
					&& !p.hasPermission(getConfig(loc).getString(loc + ".Permission")))
				continue;

			inv.setItem(slots[currentSlot], item);
			currentSlot++;
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

	boolean hasPlaceholder() {
		return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
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

	public void setDefaults(Player p, Inventory inv) {
		if (Main.getInstance().recipeBook.contains(p.getUniqueId())) {
			ItemStack stained = new ItemStack(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
			ItemMeta s = stained.getItemMeta();
			s.setDisplayName(" ");
			stained.setItemMeta(s);

			inv.setItem(4, stained);
			inv.setItem(49, stained);
		}
	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	public void show(Player p) {
		inv.clear();
		items(p, inv, 0);
		setDefaults(p, inv);
		p.openInventory(inv);
	}
}
