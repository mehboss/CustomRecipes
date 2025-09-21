package me.mehboss.gui;

import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import java.util.Arrays;

public class RecipeTypeGUI implements Listener {

	private static final String TITLE = ChatColor.DARK_GREEN + "Select Recipe Type";
	private final Inventory inv;

	public RecipeTypeGUI() {
		inv = Bukkit.createInventory(null, 54, TITLE);
		setupItems();
	}

	private void setupItems() {
		ItemStack filler = createItem(XMaterial.BLACK_STAINED_GLASS_PANE, " ");
		for (int i = 0; i < inv.getSize(); i++) {
			inv.setItem(i, filler);
		}

		// left crafting table (shaped)
		inv.setItem(30, createItem(XMaterial.CRAFTING_TABLE, "&eShaped Recipes"));

		// right crafting table (shapeless)
		inv.setItem(32, createItem(XMaterial.CRAFTING_TABLE, "&eShapeless Recipes"));

		// furnace group
		setIfSupported(20, XMaterial.FURNACE, "&eFurnace", 1, 8, RecipeType.FURNACE);
		setIfSupported(28, XMaterial.BLAST_FURNACE, "&eBlast Furnace", 1, 14, RecipeType.BLASTFURNACE);
		setIfSupported(10, XMaterial.SMOKER, "&eSmoker", 1, 14, RecipeType.SMOKER);

		// campfire
		setIfSupported(12, XMaterial.CAMPFIRE, "&eCampfire", 1, 14, RecipeType.CAMPFIRE);

		// brewing stand
		setIfSupported(14, XMaterial.BREWING_STAND, "&eBrewing Stand", 1, 14, RecipeType.BREWING_STAND);

		// stonecutter
		setIfSupported(16, XMaterial.STONECUTTER, "&eStonecutter", 1, 14, RecipeType.STONECUTTER);

		// anvil
		setIfSupported(24, XMaterial.ANVIL, "&eAnvil", 1, 14, RecipeType.ANVIL);

		// grindstone
		setIfSupported(34, XMaterial.GRINDSTONE, "&eGrindstone", 1, 14, RecipeType.GRINDSTONE);
	}

	private void setIfSupported(int slot, XMaterial mat, String name, int major, int minor, RecipeType type) {
		if (Main.getInstance().serverVersionAtLeast(major, minor) && mat != null) {
			inv.setItem(slot, createItem(mat, name));
		} else {
			inv.setItem(slot, createItem(XMaterial.BLACK_STAINED_GLASS_PANE, " "));
		}
	}

	private ItemStack createItem(XMaterial material, String name, String... lore) {
		ItemStack item = material.parseItem();
		if (item == null)
			item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
			if (lore != null && lore.length > 0) {
				meta.setLore(Arrays.asList(lore));
			}
			item.setItemMeta(meta);
		}
		return item;
	}

	public void open(Player player) {
		player.openInventory(inv);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST)
			return;
		if (!ChatColor.stripColor(CompatibilityUtil.getTitle(e)).equals(ChatColor.stripColor(TITLE)))
			return;

		e.setCancelled(true);
		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;

		Player p = (Player) e.getWhoClicked();
		String name = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
		if (name == null)
			return;

		try {
			RecipeType type = RecipeType.valueOf(name.split(" ")[0].toUpperCase());
			BookGUI.openType(p, type); // send to BookGUI to show recipes of this type
		} catch (Exception ignored) {
		}
	}

	BookGUI BookGUI = Main.getInstance().recipes;
}
