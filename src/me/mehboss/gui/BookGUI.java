package me.mehboss.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

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

import com.cryptomorin.xseries.XMaterial;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;

public class BookGUI implements Listener {

	public static Inventory inv;
	public static String[] r = null;
	public static String rname = null;
	public static ArrayList<ShapedRecipe> rshape = new ArrayList<ShapedRecipe>();

	ItemStack i = null;
	ArrayList<ItemStack> recipe = new ArrayList<ItemStack>();
	int slots;

	public FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public BookGUI(Plugin p) {
		inv = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
				getConfig().getString("gui.Displayname").replace("%page%", "1")));
	}

	private List<RecipeUtil.Recipe> buildRecipesFor(Player p, RecipeType type) {
		HashMap<String, RecipeUtil.Recipe> recipesMap = (type != null)
				? Main.getInstance().recipeUtil.getRecipesFromType(type)
				: Main.getInstance().recipeUtil.getAllRecipes();

		if (recipesMap == null || recipesMap.isEmpty())
			return new ArrayList<>();

		return recipesMap.values().stream().filter(r -> r.isActive() && (!r.hasPerm() || p.hasPermission(r.getPerm())))
				.collect(Collectors.toList());
	}

	private ItemStack createTypeHeader(RecipeType type) {
		XMaterial icon;
		String label;
		if (type == null) {
			icon = XMaterial.BOOK;
			label = ChatColor.GOLD + "Recipes";
		} else {
			switch (type) {
			case SHAPED:
				icon = XMaterial.CRAFTING_TABLE;
				label = ChatColor.GOLD + "Shaped Recipes";
				break;
			case SHAPELESS:
				icon = XMaterial.CRAFTING_TABLE;
				label = ChatColor.GOLD + "Shapeless Recipes";
				break;
			case FURNACE:
				icon = XMaterial.FURNACE;
				label = ChatColor.GOLD + "Furnace Recipes";
				break;
			case BLASTFURNACE:
				icon = XMaterial.BLAST_FURNACE;
				label = ChatColor.GOLD + "Blast Furnace Recipes";
				break;
			case SMOKER:
				icon = XMaterial.SMOKER;
				label = ChatColor.GOLD + "Smoker Recipes";
				break;
			case CAMPFIRE:
				icon = XMaterial.CAMPFIRE;
				label = ChatColor.GOLD + "Campfire Recipes";
				break;
			case BREWING_STAND:
				icon = XMaterial.BREWING_STAND;
				label = ChatColor.GOLD + "Brewing Recipes";
				break;
			case STONECUTTER:
				icon = XMaterial.STONECUTTER;
				label = ChatColor.GOLD + "Stonecutter Recipes";
				break;
			case ANVIL:
				icon = XMaterial.ANVIL;
				label = ChatColor.GOLD + "Anvil Recipes";
				break;
			case GRINDSTONE:
				icon = XMaterial.GRINDSTONE;
				label = ChatColor.GOLD + "Grindstone Recipes";
				break;
			default:
				icon = XMaterial.BOOK;
				label = ChatColor.GOLD + "Recipes";
				break;
			}
		}
		return createItem("typeHeader", icon, label);
	}

	/** Use the *display name* of slot 4 to infer the type. */
	private RecipeType parseTypeFromHeader(ItemStack header) {
		if (header == null)
			return null;
		ItemMeta m = header.getItemMeta();
		if (m == null || !m.hasDisplayName())
			return null;

		String name = ChatColor.stripColor(m.getDisplayName()).toUpperCase().split(" ")[0];
		return RecipeType.fromString(name);
	}

	public void showCreationMenu(Inventory inv, ItemStack item, Player p, String recipeName, String perm,
			Boolean creating, Boolean viewing, RecipeType type) {
		Inventory cInv = null;

		if (!(Main.getInstance().recipeBook.contains(p.getUniqueId()))) {
			cInv = Bukkit.getServer().createInventory(null, 54,
					ChatColor.translateAlternateColorCodes('&', "&cEDITING: " + recipeName));
		} else {
			cInv = Bukkit.getServer().createInventory(null, 54,
					ChatColor.translateAlternateColorCodes('&', "&cVIEWING: " + recipeName));
		}

		RecipeGUI.getInstance().setItems(creating, viewing, cInv, recipeName, perm, item, p, type);
		p.openInventory(cInv);

		if (inv != null)
			Main.getInstance().saveInventory.put(p.getUniqueId(), inv);
	}

	@EventHandler
	void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null)
			return;

		String title = CompatibilityUtil.getTitle(e);

		if (e.getClickedInventory().getType() == InventoryType.CHEST) {

			if (title == null)
				return;

			if (title.contains(ChatColor.translateAlternateColorCodes('&',
					getConfig().getString("gui.Displayname").replace(" %page%", "")))) {

				Player p = (Player) e.getWhoClicked();
				e.setCancelled(true);

				// derive type & recipes from header (slot 4)
				ItemStack headerInOld = e.getInventory().getItem(4);
				RecipeType type = parseTypeFromHeader(headerInOld);

				if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
					return;

				// --- Create button
				if (e.getCurrentItem().getType() != XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()
						&& (e.getRawSlot() == 3 || e.getRawSlot() == 5)) {
					showCreationMenu(e.getInventory(), null, p, "", null, true, false, type);
					return;
				}

				// --- Main menu button
				if (e.getRawSlot() == 49) {
					if (Main.getInstance().saveInventory.containsKey(p.getUniqueId())) {
						p.openInventory(Main.getInstance().saveInventory.get(p.getUniqueId()));
						Main.getInstance().saveInventory.remove(p.getUniqueId());
					} else {
						Main.getInstance().typeGUI.open(p);
					}
					return;
				}

				// --- Paging (Prev 48 / Next 50) ---
				if (e.getRawSlot() == 48 || e.getRawSlot() == 50) {
					String original = CompatibilityUtil.getTitle(e);
					String[] cp = original.split("Page ");
					int currentpage = Integer.valueOf(cp[1]);
					int newpage = currentpage;

					List<RecipeUtil.Recipe> recipes = buildRecipesFor(p, type);

					final int pageSize = 14;
					int maxPage = Math.max(1, (int) Math.ceil(recipes.size() / (double) pageSize));

					if (e.getRawSlot() == 48) { // prev
						if (currentpage <= 1)
							return;
						newpage--;
					} else { // next
						if (currentpage >= maxPage)
							return;
						newpage++;
					}

					String newname = ChatColor.translateAlternateColorCodes('&',
							original.replace(String.valueOf(currentpage), String.valueOf(newpage)));

					Inventory newp = Bukkit.getServer().createInventory(null, 54, newname);
					ItemStack header = (type == null) ? createTypeHeader(null) : createTypeHeader(type);

					items(p, newp, newpage - 1, recipes, header);
					p.openInventory(newp);
					return;
				}

				if (e.getRawSlot() < 19 || e.getRawSlot() > 34 || e.getRawSlot() == 26 || e.getRawSlot() == 27)
					return;

				String recipeName = null;

				for (Recipe recipe : Main.getInstance().recipeUtil.getRecipesFromType(type).values()) {
					if (recipe.getResult().isSimilar(e.getCurrentItem())) {
						recipeName = recipe.getName();
						break;
					}
				}

				logDebug("[RecipeBooklet][" + p.getName() + "] Triggered open recipe matrix.. " + recipeName);

				if (recipeName != null) {
					Boolean viewing = false;
					if (Main.getInstance().recipeBook.contains(p.getUniqueId()))
						viewing = true;

					logDebug("[RecipeBooklet][" + p.getName() + "] Opening recipe matrix.. " + recipeName);
					Main.getInstance().saveInventory.put(p.getUniqueId(), e.getInventory());
					showCreationMenu(e.getInventory(), e.getCurrentItem(), p, recipeName, null, false, viewing, null);
				}
			}
			return;
		}
	}

	void items(Player p, Inventory inv, int page) {
		items(p, inv, page, new ArrayList<>(), createTypeHeader(null));
	}

	void items(Player p, Inventory inv, int page, List<RecipeUtil.Recipe> recipes, ItemStack header) {
		int[] slotPositions = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
		this.slots = slotPositions.length;

		if (recipes == null)
			recipes = new ArrayList<>();

		int startSlot = page * slotPositions.length;

		for (int slot = 0; slot < slotPositions.length; slot++) {
			int index = startSlot + slot;
			if (index >= recipes.size())
				break;

			try {
				RecipeUtil.Recipe rObj = recipes.get(index);
				if (rObj == null)
					continue;

				ItemStack result = rObj.getResult();
				if (result == null || result.getType() == Material.AIR)
					continue;

				ItemStack item = result.clone();
				ItemMeta itemM = item.getItemMeta();
				if (itemM != null)
					item.setItemMeta(itemM);

				inv.setItem(slotPositions[slot], item);
			} catch (Exception e) {
				Main.getInstance().getLogger().log(Level.SEVERE, "Can not parse itemstack for recipe "
						+ recipes.get(index).getName().toLowerCase() + ". Skipping for now.", e);
			}
		}
		ItemStack stained = createItem("stainedG", XMaterial.BLACK_STAINED_GLASS_PANE, " ");
		ItemStack redstained = createItem("stainedG", XMaterial.RED_STAINED_GLASS_PANE,
				ChatColor.RED + "Previous Page");
		ItemStack orangestained = createItem("stainedG", XMaterial.ORANGE_STAINED_GLASS_PANE,
				ChatColor.GREEN + "Next Page");
		ItemStack greenstained = createItem("stainedG", XMaterial.GREEN_STAINED_GLASS_PANE,
				ChatColor.YELLOW + "Main Menu");
		ItemStack limestained = null;

		if (!Main.getInstance().recipeBook.contains(p.getUniqueId()))
			limestained = createItem("stainedG", XMaterial.LIME_STAINED_GLASS_PANE,
					ChatColor.DARK_GREEN + "Create Recipe");

		defaults(inv, redstained, orangestained, greenstained, limestained, stained, header);
	}

	void defaults(Inventory i, ItemStack prev, ItemStack next, ItemStack back, ItemStack create, ItemStack filler,
			ItemStack header) {
		int[] set = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41,
				42, 43, 44, 45, 46, 47, 51, 52, 53 };

		for (int ii : set)
			i.setItem(ii, filler);

		if (header == null) {
			ItemStack bl = new ItemStack(XMaterial.WRITABLE_BOOK.parseItem());
			ItemMeta bm = bl.getItemMeta();
			if (bm != null) {
				bm.setDisplayName(ChatColor.DARK_AQUA + "Recipes");
				bl.setItemMeta(bm);
			}
			header = bl;
		}
		i.setItem(4, header);
		i.setItem(48, prev);
		i.setItem(49, back);
		i.setItem(50, next);

		if (create != null) {
			i.setItem(3, create);
			i.setItem(5, create);
		}
	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");
		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	ItemStack createItem(String id, XMaterial material, String name, String... lore) {
		ItemStack base = material.parseItem();
		ItemStack item = (base != null) ? base : new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
			if (lore != null)
				meta.setLore(Arrays.asList(lore));
			item.setItemMeta(meta);
		}
		return item;
	}

	public void openType(Player p, RecipeType type, Inventory inv) {
		List<RecipeUtil.Recipe> recipes = buildRecipesFor(p, type);
		if (recipes.isEmpty()) {
			showCreationMenu(inv, null, p, "", null, true, false, type);
			return;
		}

		String title = ChatColor.translateAlternateColorCodes('&',
				Main.getInstance().getConfig().getString("gui.Displayname").replace("%page%", "1"));

		Inventory newInv = Bukkit.getServer().createInventory(null, 54,
				title.replace("%page%", "1").replace(" %page%", ""));
		ItemStack header = createTypeHeader(type);

		items(p, newInv, 0, recipes, header);
		p.openInventory(newInv);
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}
}