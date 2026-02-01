package me.mehboss.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.BrewingRecipeData;
import me.mehboss.utils.data.CookingRecipeData;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.data.SmithingRecipeData;
import me.mehboss.utils.data.WorkstationRecipeData;
import me.mehboss.utils.libs.CompatibilityUtil;

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

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import me.mehboss.gui.framework.RecipeGUI;
import me.mehboss.gui.framework.GuiView;
import me.mehboss.gui.framework.GuiView.GuiRegistry;
import me.mehboss.gui.framework.GuiView.PagedGuiView;
import me.mehboss.recipe.Main;

public class BookGUI implements Listener {

	public static Inventory inv;
	public static String[] r = null;
	public static String rname = null;
	public static ArrayList<ShapedRecipe> rshape = new ArrayList<ShapedRecipe>();

	ItemStack i = null;
	ArrayList<ItemStack> recipe = new ArrayList<ItemStack>();
	int slots;

	private FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}

	public BookGUI(Plugin p) {
		inv = Bukkit.getServer().createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
				getConfig().getString("gui.Displayname").replace("%page%", "1")));
	}

	public List<RecipeUtil.Recipe> buildRecipesFor(Player p, RecipeType type, Boolean isViewing) {
		HashMap<String, RecipeUtil.Recipe> recipesMap = (type != null)
				? Main.getInstance().recipeUtil.getRecipesFromType(type)
				: Main.getInstance().recipeUtil.getAllRecipes();

		if (recipesMap == null || recipesMap.isEmpty())
			return new ArrayList<>();

		List<Recipe> filtered = new ArrayList<>();

		for (Recipe r : recipesMap.values()) {
			if ((r.hasPerm() && !p.hasPermission(r.getPerm())))
				continue;
			if (isViewing && !r.isActive())
				continue;
			filtered.add(r);
		}

		return filtered;
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
			case SMITHING:
				icon = XMaterial.SMITHING_TABLE;
				label = ChatColor.GOLD + "Smithing Recipes";
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

	public void showCreationMenu(Player player, Recipe rawRecipe, boolean creating, boolean viewing) {

		RecipeGUI gui = Main.getInstance().editItem;
		Recipe recipe = rawRecipe;

		if (creating) {
			RecipeType type = rawRecipe.getType();
			String id = rawRecipe.getName();
			switch (type) {

			case SHAPED:
			case SHAPELESS:
				recipe = new CraftingRecipeData(id);
				break;

			case FURNACE:
			case BLASTFURNACE:
			case SMOKER:
			case CAMPFIRE:
				recipe = new CookingRecipeData(id);
				break;

			case STONECUTTER:
			case GRINDSTONE:
			case ANVIL:
				recipe = new WorkstationRecipeData(id);
				break;

			case BREWING_STAND:
				recipe = new BrewingRecipeData(id);
				break;

			case SMITHING:
				recipe = new SmithingRecipeData(id);
				break;

			default:
				throw new IllegalArgumentException("Unsupported recipe type: " + type);
			}
			recipe.setType(rawRecipe.getType());
			recipe.setPerm(rawRecipe.getPerm());
			recipe.setKey(rawRecipe.getKey());
		}

		if (viewing) {
			gui.openViewing(player, recipe);
		} else {
			gui.openEditing(player, recipe);
		}
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

				boolean isViewing = Main.getInstance().recipeBook.contains(p.getUniqueId());

				ItemStack stained = createItem("stainedG", XMaterial.BLACK_STAINED_GLASS_PANE, " ");
				if (!e.getCurrentItem().equals(stained) && (e.getRawSlot() == 3 || e.getRawSlot() == 5)) {
					Recipe recipe = new Recipe("New Recipe");
					recipe.setType(type);
					showCreationMenu(p, recipe, true, false);
					return;
				}

				if (e.getRawSlot() == 49) {
					Main.getInstance().typeGUI.open(p);
					return;
				}

				if (e.getRawSlot() == 48 || e.getRawSlot() == 50) {

					GuiView base = GuiRegistry.get(p.getUniqueId());
					if (!(base instanceof PagedGuiView))
						return;

					PagedGuiView view = (PagedGuiView) base;

					int currentpage = view.getPage();
					int newpage = currentpage;

					List<RecipeUtil.Recipe> recipes = buildRecipesFor(p, type, isViewing);

					final int pageSize = 14;
					int maxPage = Math.max(1, (int) Math.ceil(recipes.size() / (double) pageSize));

					// determine new page
					if (e.getRawSlot() == 48) {
						if (currentpage <= 1)
							return;
						newpage--;
					} else {
						if (currentpage >= maxPage)
							return;
						newpage++;
					}

					// build new title from config
					String newTitle = ChatColor.translateAlternateColorCodes('&',
							getConfig().getString("gui.Displayname").replace("%page%", String.valueOf(newpage)));

					// create NEW paged view
					PagedGuiView newView = new PagedGuiView(54, newTitle, newpage, type);

					Inventory inv = newView.getInventory();
					ItemStack header = createTypeHeader(type);
					items(p, inv, newpage - 1, recipes, header);

					newView.open(p);
					GuiRegistry.register(p.getUniqueId(), newView);
					return;
				}

				if (e.getRawSlot() < 19 || e.getRawSlot() > 34 || e.getRawSlot() == 26 || e.getRawSlot() == 27)
					return;

				Recipe recipe = null;
				ReadableNBT nbt = NBT.readNbt(e.getCurrentItem());
				if (nbt.hasTag("CUSTOM_ITEM_IDENTIFIER")) {
					String key = nbt.getString("CUSTOM_ITEM_IDENTIFIER");
					recipe = Main.getInstance().getRecipeUtil().getRecipeFromKey(key);
				}

				if (recipe != null) {
					showCreationMenu(p, recipe, false, isViewing);
				}
			}
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
				NBT.modify(item, nbt -> {
					if (!nbt.hasTag("CUSTOM_ITEM_IDENTIFIER")) {
						nbt.setString("CUSTOM_ITEM_IDENTIFIER", rObj.getKey());
					}
				});

				inv.setItem(slotPositions[slot], item);
			} catch (Exception e) {
				Main.getInstance().getLogger().log(Level.SEVERE, "Can not parse itemstack for recipe "
						+ recipes.get(index).getName().toLowerCase() + ". Skipping for now.", e);
			}
		}
		ItemStack stained = createItem("stainedG", XMaterial.BLACK_STAINED_GLASS_PANE, " ");
		ItemStack redstained = createItem("stainedG", XMaterial.RED_STAINED_GLASS_PANE,
				getParsedValue("Buttons.Previous", "&cPrevious Page"));
		ItemStack orangestained = createItem("stainedG", XMaterial.ORANGE_STAINED_GLASS_PANE,
				getParsedValue("Buttons.Next", "&aNext Page"));
		ItemStack greenstained = createItem("stainedG", XMaterial.GREEN_STAINED_GLASS_PANE,
				getParsedValue("Buttons.Main-Menu", "&eMain Menu"));
		ItemStack limestained = null;

		if (!Main.getInstance().recipeBook.contains(p.getUniqueId()))
			limestained = createItem("stainedG", XMaterial.LIME_STAINED_GLASS_PANE,
					getParsedValue("Buttons.Create", "&2Create Recipe"));

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

	public void openType(Player p, RecipeType type) {
		boolean isViewing = Main.getInstance().recipeBook.contains(p.getUniqueId());
		List<RecipeUtil.Recipe> recipes = buildRecipesFor(p, type, isViewing);

		if (recipes.isEmpty()) {
			if (!isViewing) {
				Recipe recipe = new Recipe("New Recipe");
				recipe.setType(type);
				showCreationMenu(p, recipe, true, false);
			}
			return;
		}

		boolean hasRoot = GuiRegistry.hasRootView(p.getUniqueId());
		if (hasRoot)
			GuiRegistry.clearRootView(p.getUniqueId());

		String title = ChatColor.translateAlternateColorCodes('&',
				Main.getInstance().getConfig().getString("gui.Displayname").replace("%page%", "1"));

		ItemStack header = createTypeHeader(type);

		PagedGuiView view = new PagedGuiView(54, title, 1, type);
		GuiRegistry.register(p.getUniqueId(), view);

		Inventory inv = view.getInventory();
		items(p, inv, 0, recipes, header);
		view.open(p);
	}

	void logDebug(String st) {
		if (Main.getInstance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "] " + st);
	}

	private String getParsedValue(String msg, String def) {
		return ChatColor.translateAlternateColorCodes('&', getValue(msg, def));
	}

	private String getValue(String path, String def) {
		String val = getConfig().getString("gui." + path);
		return (val == null || val.isEmpty()) ? def : val;
	}
}