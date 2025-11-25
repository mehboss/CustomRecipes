package me.mehboss.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.cryptomorin.xseries.XMaterial;

import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.commands.CommandRemove;
import me.mehboss.recipe.Main;
import me.mehboss.utils.CompatibilityUtil;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;

public class RecipeGUI implements Listener {

	// *** this menu is the GUI for adding a new recipe
	public HashMap<UUID, String> editmeta = new HashMap<>();
	HashMap<UUID, Inventory> inventoryinstance = new HashMap<>();
	HashMap<UUID, String> getnewid = new HashMap<>();

	List<String> lore = new ArrayList<>();

	private static RecipeGUI instance;

	Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");
	RecipeSaver saveChanges;

	public RecipeGUI(Plugin p, String item) {
		instance = this;
		saveChanges = new RecipeSaver();
	}

	public static RecipeGUI getInstance() {
		return instance;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");
		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	private static final class RecipeLayout {
		final int[] ingredientSlots; // where ingredients go (order matters for SHAPED)
		final Integer fuelSlot; // null when no fuel slot is used
		final int resultSlot; // output slot

		RecipeLayout(int[] ingredientSlots, Integer fuelSlot, int resultSlot) {
			this.ingredientSlots = ingredientSlots;
			this.fuelSlot = fuelSlot;
			this.resultSlot = resultSlot;
		}

		boolean usesFuel() {
			return fuelSlot != null;
		}
	}

	private static final HashMap<RecipeType, RecipeLayout> LAYOUTS = new HashMap<>();
	static {
		int[] CRAFTING_GRID = { 11, 12, 13, 20, 21, 22, 29, 30, 31 };

		// Crafting
		LAYOUTS.put(RecipeType.SHAPED, new RecipeLayout(CRAFTING_GRID, null, 24));
		LAYOUTS.put(RecipeType.SHAPELESS, new RecipeLayout(CRAFTING_GRID, null, 24));

		// Single-input machines (no fuel)
		LAYOUTS.put(RecipeType.STONECUTTER, new RecipeLayout(new int[] { 20 }, null, 24));
		LAYOUTS.put(RecipeType.CAMPFIRE, new RecipeLayout(new int[] { 20 }, null, 24));

		// Furnace-like (input at 11, fuel 29, result 24)
		LAYOUTS.put(RecipeType.FURNACE, new RecipeLayout(new int[] { 11 }, 29, 24));
		LAYOUTS.put(RecipeType.BLASTFURNACE, new RecipeLayout(new int[] { 11 }, 29, 24));
		LAYOUTS.put(RecipeType.SMOKER, new RecipeLayout(new int[] { 11 }, 29, 24));

		// Two-input utilities with a center gap (20 [gap 21] 22)
		LAYOUTS.put(RecipeType.ANVIL, new RecipeLayout(new int[] { 20, 22 }, null, 24));
		LAYOUTS.put(RecipeType.GRINDSTONE, new RecipeLayout(new int[] { 20, 22 }, null, 24));

		// 22 is input, 24 is required items, 20 is fuel, 40 is result
		LAYOUTS.put(RecipeType.BREWING_STAND, new RecipeLayout(new int[] { 22, 24 }, 20, 40));
	}

	public void setItems(Boolean creating, Boolean viewing, Inventory i, String invname, String perm, ItemStack item,
			OfflinePlayer p, RecipeType rType) {

		String configname = invname;

		// Get recipe and decide layout from its type
		Recipe r = Main.getInstance().getRecipeUtil().getRecipe(configname);
		RecipeType type = (r != null && r.getType() != null) ? r.getType() : rType;
		RecipeLayout layout = LAYOUTS.getOrDefault(type, LAYOUTS.get(RecipeType.SHAPED));

		List<String> loreList = new ArrayList<>();

		final int[] controlSlots = { 0, 7, 8, 9, 26, 35, 44, 45, 48, 49, 50, 52, 53 };

		boolean[] paneAt = new boolean[54];
		for (int s = 0; s < 54; s++)
			paneAt[s] = true;
		paneAt[layout.resultSlot] = false;
		for (int s : layout.ingredientSlots)
			paneAt[s] = false;
		if (layout.usesFuel())
			paneAt[layout.fuelSlot] = false;

		// gap between two-input utilities (center slot 21)
		boolean isTwoInputUtility = (type == RecipeType.ANVIL || type == RecipeType.GRINDSTONE);
		if (isTwoInputUtility)
			paneAt[21] = true;

		// In EDITING mode, keep control slots empty (they'll be populated by widgets);
		// in VIEWING mode, we want them to be panes except the Back buttons.
		if (!viewing) {
			for (int s : controlSlots)
				paneAt[s] = false;
		}

		ItemStack stained = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
		ItemMeta stainedm = stained.getItemMeta();
		stainedm.setDisplayName(" ");
		stained.setItemMeta(stainedm);
		for (int s = 0; s < 54; s++)
			if (paneAt[s])
				i.setItem(s, stained);

		// not creating new recipe, fill in slots
		if (!creating) {
			int ingIndex = 0;
			Recipe recipe = Main.getInstance().getRecipeUtil().getRecipe(configname);

			for (Ingredient ingredient : recipe.getIngredients()) {
				if (recipe.getType() != RecipeType.SHAPED && (ingredient == null || ingredient.isEmpty())) {
					continue; // skip empties, don't advance ingIndex
				}

				if (ingIndex >= layout.ingredientSlots.length)
					break;

				boolean foundIdentifier = itemIngredients(ingredient, configname, ingIndex + 1, p) != null;
				ItemStack mat = foundIdentifier ? itemIngredients(ingredient, configname, ingIndex + 1, p)
						: XMaterial.matchXMaterial(ingredient.getMaterial().toString()).get().parseItem();

				// for legacy versions for short values
				if (!foundIdentifier && ingredient.hasMaterialData())
					mat = ingredient.getMaterialData().toItemStack();

				mat.setAmount(ingredient.getAmount());

				if (!foundIdentifier) {
					ItemMeta matm = mat.getItemMeta();
					if (ingredient.hasDisplayName()) {
						if (Main.getInstance().serverVersionAtLeast(1, 20, 5))
							matm.setItemName(ingredient.getDisplayName());
						else
							matm.setDisplayName(ingredient.getDisplayName());
					}
					mat.setItemMeta(matm);
				}

				i.setItem(layout.ingredientSlots[ingIndex], mat);
				ingIndex++;
			}

			ItemStack result = new ItemStack(Main.getInstance().recipeUtil.getRecipe(configname).getResult());
			ItemMeta resultM = result.getItemMeta();

			if (result.hasItemMeta() && resultM.hasLore() && hasPlaceholder()) {
				resultM.setLore(PlaceholderAPI.setPlaceholders(p, resultM.getLore()));
				result.setItemMeta(resultM);
			}

			i.setItem(layout.resultSlot, result);
		}

		// VIEWING mode: turn all control slots into panes, then add Back buttons
		if (viewing) {
			for (int s : controlSlots) {
				if (s == 48 || s == 49 || s == 50)
					continue; // back buttons set below
				i.setItem(s, stained);
			}

			// Three "Back" buttons centered bottom
			ItemStack back = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
			ItemMeta backm = back.getItemMeta();
			backm.setDisplayName(ChatColor.DARK_RED + "Back");
			back.setItemMeta(backm);
			i.setItem(48, back);
			i.setItem(49, back);
			i.setItem(50, back);

			return;
		}

		/* ====== below here are the editor widgets (only when not viewing) ====== */

		ItemStack identifier = XMaterial.PAPER.parseItem();
		ItemMeta identifierm = identifier.getItemMeta();
		identifierm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fIdentifier:&7 "));
		loreList.add(ChatColor.GRAY + getConfig(configname).getString(configname + ".Identifier"));

		if (creating && !invname.isEmpty() && !invname.equals("")) {
			loreList.clear();
			loreList.add(ChatColor.GRAY + invname);
		}

		identifierm.setLore(loreList);
		identifier.setItemMeta(identifierm);
		loreList.clear();

		ItemStack lore = XMaterial.BOOK.parseItem();
		ItemMeta lorem = lore.getItemMeta();
		lorem.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fLore"));

		if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
			lorem.setLore(item.getItemMeta().getLore());
			if (hasPlaceholder())
				lorem.setLore(PlaceholderAPI.setPlaceholders(p, item.getItemMeta().getLore()));
		}
		lore.setItemMeta(lorem);

		ItemStack effects = XMaterial.GHAST_TEAR.parseItem();
		ItemMeta effectsm = effects.getItemMeta();
		effectsm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fEffects"));
		if (getConfig(configname).getString(configname + ".Effects") != null) {
			for (String effectslore : getConfig(configname).getStringList(configname + ".Effects")) {
				loreList.add(ChatColor.GRAY + effectslore);
			}
			effectsm.setLore(loreList);
		}
		effects.setItemMeta(effectsm);
		loreList.clear();

		ItemStack converter = XMaterial.FURNACE.parseItem();
		ItemMeta converterm = effects.getItemMeta(); // keep original behavior
		converterm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fConverter"));
		loreList.add(ChatColor.GRAY + type.toString().toUpperCase());
		converterm.setLore(loreList);
		converter.setItemMeta(converterm);
		loreList.clear();

		ItemStack permission = XMaterial.ENDER_PEARL.parseItem();
		ItemMeta permissionm = permission.getItemMeta();
		permissionm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fPermission: &7"));

		if (getConfig(configname).isSet(configname + ".Permission")
				&& !getConfig(configname).getString(configname + ".Permission").equals("none"))
			loreList.add(ChatColor.GRAY + getConfig(configname).getString(configname + ".Permission"));

		if (perm != null)
			loreList.add(ChatColor.GRAY + perm);
		if (loreList.isEmpty())
			loreList.add(ChatColor.GRAY + "none");

		permissionm.setLore(loreList);
		permission.setItemMeta(permissionm);
		loreList.clear();

		ItemStack hideenchants = XMaterial.DIRT.parseItem();
		ItemMeta hideenchantsm = hideenchants.getItemMeta();
		Boolean hc = getConfig(configname).getBoolean(configname + ".Placeable");
		String ht = "&cfalse";
		if (hc == true)
			ht = "&atrue";
		hideenchantsm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fPlaceable: &f" + ht));
		hideenchants.setItemMeta(hideenchantsm);

		// Shapeless toggle only for crafting types
		boolean showShapeless = (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS);
		boolean showGroup = showShapeless || (type == RecipeType.STONECUTTER);

		ItemStack shapeless = XMaterial.CRAFTING_TABLE.parseItem();
		if (showShapeless) {
			shapeless = XMaterial.CRAFTING_TABLE.parseItem();
			ItemMeta shapelessm = shapeless.getItemMeta();
			Boolean isShapeless = creating ? rType.equals(RecipeType.SHAPELESS) : true;
			Boolean sc = creating ? isShapeless : getConfig(configname).getBoolean(configname + ".Shapeless");
			String toggle = sc ? "&atrue" : "&cfalse";
			shapelessm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fShapeless: &f" + toggle));
			shapeless.setItemMeta(shapelessm);
		}

		// Exact choice
		ItemStack ec = XMaterial.LEVER.parseItem();
		ItemMeta ecm = ec.getItemMeta();
		Boolean exact = getConfig(configname).getBoolean(configname + ".Exact-Choice", true);
		String toggle = exact ? "&atrue" : "&cfalse";
		ecm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fExact-Choice: &f" + toggle));
		ec.setItemMeta(ecm);

		ItemStack name = XMaterial.WRITABLE_BOOK.parseItem();
		ItemMeta namem = name.getItemMeta();
		namem.setDisplayName(ChatColor.WHITE + "Recipe Name: ");
		if (getConfig(configname).isConfigurationSection(configname))
			loreList.add(ChatColor.GRAY + configname);

		if (creating && !invname.isEmpty() && !invname.equals("")) {
			loreList.clear();
			loreList.add(ChatColor.GRAY + invname);
		}

		namem.setLore(loreList);
		name.setItemMeta(namem);
		loreList.clear();

		ItemStack group = XMaterial.OAK_SIGN.parseItem();
		ItemMeta groupm = permission.getItemMeta();
		groupm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fGroup: &7"));

		if (getConfig(configname).isSet(configname + ".Group")
				&& !getConfig(configname).getString(configname + ".Group").equals("none"))
			loreList.add(ChatColor.GRAY + getConfig(configname).getString(configname + ".Group"));

		if (loreList.isEmpty())
			loreList.add(ChatColor.GRAY + "none");

		groupm.setLore(loreList);
		group.setItemMeta(groupm);
		loreList.clear();

		ItemStack amount = XMaterial.FEATHER.parseItem();
		ItemMeta amountm = amount.getItemMeta();
		amountm.setDisplayName(ChatColor.translateAlternateColorCodes('&',
				"&fAmount: &7" + getConfig(configname).getString(configname + ".Amount")));
		amount.setItemMeta(amountm);

		i.setItem(9, identifier);
		i.setItem(8, hideenchants);
		i.setItem(7, effects);
		i.setItem(17, converter);
		i.setItem(26, permission);
		i.setItem(0, name);
		i.setItem(35, amount);
		i.setItem(44, lore);
		i.setItem(18, ec);

		// auto set to stain and change in a bit
		i.setItem(52, stained);
		i.setItem(51, stained);

		// Slot 52 – shapeless & group only shown for crafting; otherwise fill with pane
		if (showShapeless)
			i.setItem(52, shapeless);
		if (showGroup)
			i.setItem(51, group);

		ItemStack enabled = XMaterial.SLIME_BALL.parseItem();
		ItemMeta enabledm = enabled.getItemMeta();
		enabledm.setDisplayName(ChatColor.GREEN + "Enabled");
		enabled.setItemMeta(enabledm);

		Boolean toggled = getConfig(configname).isBoolean(configname + ".Enabled")
				? getConfig(configname).getBoolean(configname + ".Enabled")
				: null;
		if (toggled != null && !toggled) {
			enabled = XMaterial.SNOWBALL.parseItem();
			enabledm.setDisplayName(ChatColor.RED + "Disabled");
			enabled.setItemMeta(enabledm);
		}
		i.setItem(53, enabled);

		ItemStack cancel = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
		ItemMeta cancelm = cancel.getItemMeta();
		cancelm.setDisplayName(ChatColor.DARK_RED + "Cancel");
		cancel.setItemMeta(cancelm);
		i.setItem(48, cancel);

		ItemStack mainmenu = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
		ItemMeta mainmenum = mainmenu.getItemMeta();
		mainmenum.setDisplayName("Main Menu");
		mainmenu.setItemMeta(mainmenum);
		i.setItem(49, mainmenu);

		ItemStack update = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
		ItemMeta updatem = update.getItemMeta();
		updatem.setDisplayName(ChatColor.DARK_GREEN + "Update");
		update.setItemMeta(updatem);
		i.setItem(50, update);

		ItemStack delete = XMaterial.BARRIER.parseItem();
		ItemMeta deletem = delete.getItemMeta();
		deletem.setDisplayName(ChatColor.DARK_RED + "Delete Recipe");
		delete.setItemMeta(deletem);
		i.setItem(45, delete);
	}

	String chatColor(String st) {
		if (st == null)
			return null;
		if (st.equalsIgnoreCase("false"))
			return st;
		return ChatColor.translateAlternateColorCodes('&', st);
	}

	ItemStack itemIngredients(Ingredient ingredient, String recipename, int slot, OfflinePlayer p) {
		if (ingredient.hasIdentifier()) {
			Boolean isCustomItem = getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier()) == null ? true : false;
			ItemStack item = isCustomItem ? getRecipeUtil().getResultFromKey(ingredient.getIdentifier())
					: getRecipeUtil().getRecipeFromKey(ingredient.getIdentifier()).getResult();

			if (item == null) {
				logError("Failed to set item ingredients! Could not find itemstack for ingredient slot "
						+ ingredient.getSlot() + ". isCustomItem: " + isCustomItem, recipename);
				return null;
			}

			ItemStack finalItem = new ItemStack(item);
			ItemMeta itemM = finalItem.getItemMeta();
			if (!isCustomItem && finalItem.hasItemMeta() && itemM.hasLore() && hasPlaceholder()) {
				itemM.setLore(PlaceholderAPI.setPlaceholders(p, itemM.getLore()));
				finalItem.setItemMeta(itemM);
			}
			return finalItem;
		}
		return null;
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		if (!(e.getWhoClicked() instanceof Player) || e.getClickedInventory() == null
				|| e.getClickedInventory().getType() == InventoryType.PLAYER)
			return;

		Player p = (Player) e.getWhoClicked();
		String inventoryTitle = CompatibilityUtil.getTitle(e);

		if (e.getInventory() != null && inventoryTitle != null && inventoryTitle.contains("VIEWING: ")) {
			e.setCancelled(true);

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getRawSlot() == 24)
				return;

			if (getRecipeUtil().getRecipeFromResult(e.getCurrentItem()) != null) {
				String name = getRecipeUtil().getRecipeFromResult(e.getCurrentItem()).getName();
				Inventory edit = Bukkit.getServer().createInventory(null, 54,
						ChatColor.translateAlternateColorCodes('&', "&cVIEWING: " + name));
				setItems(false, true, edit, name, null, e.getCurrentItem(), p, null);
				Main.getInstance().saveInventory.put(p.getUniqueId(), e.getInventory());
				p.openInventory(edit);
			}

			if (e.getRawSlot() == 48 || e.getRawSlot() == 49 || e.getRawSlot() == 50) {
				if (Main.getInstance().saveInventory.containsKey(p.getUniqueId())) {
					p.openInventory(Main.getInstance().saveInventory.get(p.getUniqueId()));
					Main.getInstance().saveInventory.remove(p.getUniqueId());
				} else {
					Main.getInstance().typeGUI.open(p);
				}
			}
			return;
		}

		if (e.getInventory() != null && inventoryTitle != null && inventoryTitle.contains("EDITING: ")) {

			String[] split = inventoryTitle.split("EDITING: ");
			String recipeName = split.length == 2 ? split[1] : null;

			// Type-aware click allowance
			Recipe r = Main.getInstance().getRecipeUtil().getRecipe(recipeName);

			ItemMeta converterItem = e.getInventory().getItem(17).getItemMeta();
			String converterLore = converterItem.hasLore() ? converterItem.getLore().get(0) : "SHAPED";

			RecipeType creationType = RecipeType.valueOf(ChatColor.stripColor(converterLore));
			RecipeType type = (r != null && r.getType() != null) ? r.getType() : creationType;
			boolean showShapeless = (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS);
			boolean isCooking = (type == RecipeType.FURNACE || type == RecipeType.BLASTFURNACE
					|| type == RecipeType.SMOKER || type == RecipeType.CAMPFIRE);
			boolean isBrewing = (type == RecipeType.BREWING_STAND);

			// Control buttons (always handled below)
			HashSet<Integer> controls = new HashSet<>();
			int[] craftingSlots = { 11, 12, 13, 20, 21, 22, 29, 30, 31, 24 };
			int[] cookingSlots = { 11, 29, 24 };
			int[] brewingSlots = { 20, 22, 24, 40 };
			int[] otherSlots = { 20, 22, 24 };

			int[] controlSlots = craftingSlots;

			if (!showShapeless) {
				controlSlots = otherSlots;
				if (isCooking)
					controlSlots = cookingSlots;
				if (isBrewing)
					controlSlots = brewingSlots;
			}

			for (int s : controlSlots)
				controls.add(s);

			if (!controls.contains(e.getRawSlot())) {
				e.setCancelled(true);
			}

			if (e.getRawSlot() == 7)
				return; // effects button
			if (e.getRawSlot() == 17)
				return; // enchants button

			if (recipeName == null || recipeName.isEmpty() || recipeName.equals(" ") || recipeName.equals("null")) {
				ItemMeta nameItem = e.getInventory().getItem(0).getItemMeta();
				String newTitle = nameItem.hasLore() ? nameItem.getLore().get(0) : null;
				if (recipeName == null || !recipeName.equals(newTitle))
					e.getView().setTitle(ChatColor.GREEN + "EDITING: " + newTitle);
				inventoryinstance.put(p.getUniqueId(), p.getOpenInventory().getTopInventory());
			}
			if (e.getRawSlot() == 35) {
				// amount button
				handleStringMessage(p, "Amount", e);
			}

			ItemStack toggle = e.getCurrentItem();
			if (toggle == null || toggle.getItemMeta() == null)
				return;
			ItemMeta togglem = toggle.getItemMeta();

			if (e.getRawSlot() == 44) {
				// item lore
				ItemStack item = e.getInventory().getItem(44);
				if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
					lore = item.getItemMeta().getLore();
				}
				inventoryinstance.put(p.getUniqueId(), p.getOpenInventory().getTopInventory());
				editmeta.put(p.getUniqueId(), "Lore");
				p.closeInventory();
				sendLoremsg(p);
			}

			if (e.getRawSlot() == 48) {
				// cancel button
				p.sendMessage(ChatColor.RED + "You have successfully cancelled any changes to the recipe '" + recipeName
						+ "'");
				p.closeInventory();
			}

			if (e.getRawSlot() == 49) {
				// main menu button
				Main.getInstance().typeGUI.open(p);
			}

			if (e.getRawSlot() == 50) {
				// update button
				saveChanges.saveRecipe(e.getClickedInventory(), p, recipeName);
			}

			if (e.getRawSlot() == 53) {
				// recipe toggle enable/disable
				if (togglem.getDisplayName().contains("Enabled")) {
					togglem.setDisplayName(ChatColor.RED + "Disabled");
					toggle.setItemMeta(togglem);
					p.getOpenInventory().getTopInventory().setItem(53, toggle);
					e.getCurrentItem().setType(XMaterial.SNOWBALL.parseMaterial());
					return;
				}
				if (togglem.getDisplayName().contains("Disabled")) {
					togglem.setDisplayName(ChatColor.GREEN + "Enabled");
					toggle.setItemMeta(togglem);
					p.getOpenInventory().getTopInventory().setItem(53, toggle);
					e.getCurrentItem().setType(XMaterial.SLIME_BALL.parseMaterial());
					return;
				}
			}

			if (e.getRawSlot() == 8) {
				toggleBooleanDisplay(p, e, toggle, 8, "Placeable: ");
				return;
			}

			if (e.getRawSlot() == 51)
				handleStringMessage(p, "Group", e);

			if (e.getRawSlot() == 52 && showShapeless) {
				toggleBooleanDisplay(p, e, toggle, 52, "Shapeless: ");
				return;
			}

			if (e.getRawSlot() == 18) {
				toggleBooleanDisplay(p, e, toggle, 18, "Exact-Choice: ");
				return;
			}

			if (e.getRawSlot() == 9)
				handleStringMessage(p, "Identifier", e);
			if (e.getRawSlot() == 26)
				handleStringMessage(p, "Permission", e);
			if (e.getRawSlot() == 0)
				handleStringMessage(p, "Name", e);

			if (e.getRawSlot() == 45) {
				if (recipeName == null || recipeName.isEmpty() || recipeName.equals(" ") || recipeName.equals("null"))
					return;

				if (ChatColor.stripColor(e.getClickedInventory().getItem(45).getItemMeta().getDisplayName())
						.equals("Confirm Recipe Deletion")) {

					File recipeFile = new File(Main.getInstance().getDataFolder(), "recipes/" + recipeName + ".yml");
					if (recipeFile.exists()) {
						if (recipeFile.delete()) {
							CommandRemove.removeRecipe(recipeName);
							p.sendMessage(ChatColor.GREEN + "Recipe '" + recipeName + "' was successfully deleted.");
						} else {
							p.sendMessage(ChatColor.RED + "Failed to delete recipe '" + recipeName + "'.");
						}
					}

					Main.getInstance().typeGUI.open(p);
					return;
				}

				ItemStack confirm = e.getClickedInventory().getItem(45);
				ItemMeta confirmm = e.getClickedInventory().getItem(45).getItemMeta();
				confirmm.setDisplayName(ChatColor.DARK_RED + "Confirm Recipe Deletion");
				confirm.setItemMeta(confirmm);
			}
		}
	}

	void toggleBooleanDisplay(Player p, InventoryClickEvent e, ItemStack toggle, int slot, String labelPrefix) {

		ItemMeta meta = toggle.getItemMeta();
		String name = ChatColor.stripColor(meta.getDisplayName());

		if (name.contains("true")) {
			meta.setDisplayName(labelPrefix + ChatColor.RED + "false");
			toggle.setItemMeta(meta);
			p.getOpenInventory().getTopInventory().setItem(slot, toggle);
		} else if (name.contains("false")) {
			meta.setDisplayName(labelPrefix + ChatColor.GREEN + "true");
			toggle.setItemMeta(meta);
			p.getOpenInventory().getTopInventory().setItem(slot, toggle);
		}
	}

	public void sendLoremsg(Player p) {

		int num = 1;

		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
		p.sendMessage("Please type your new recipe lore");
		p.sendMessage("Current Lore: ");
		p.sendMessage(" ");

		if (lore != null) {
			for (String line : lore) {
				p.sendMessage(num + ": " + line);
				num++;
			}
		} else {
			p.sendMessage("NO LORE");
		}

		p.sendMessage(" ");
		p.sendMessage("Type 'LINE#: string' to edit/add a line to your lore.");
		p.sendMessage("Type 'LINE#: remove' to remove a line.");
		p.sendMessage("Type ONLY the line number for an empty space.");
		p.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to go back.");
		p.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "when finished.");
		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
	}

	boolean hasPlaceholder() {
		return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
	}

	public void handleLore(int slot, String string, Player p, AsyncPlayerChatEvent e) {

		Inventory inv = inventoryinstance.get(p.getUniqueId());
		ItemStack item = inv.getItem(44);
		ItemStack result = inv.getItem(24); // result slot updated

		if (!editmeta.containsKey(p.getUniqueId()))
			return;

		if (e.getMessage().equalsIgnoreCase("done")) {

			if (!lore.isEmpty() && result != null && item.hasItemMeta()) {
				ItemMeta itemm = item.getItemMeta();
				itemm.setLore(lore);

				if (hasPlaceholder())
					itemm.setLore(PlaceholderAPI.setPlaceholders(p, lore));

				item.setItemMeta(itemm);

				ItemMeta resultm = result.getItemMeta();
				resultm.setLore(lore);

				if (hasPlaceholder())
					resultm.setLore(PlaceholderAPI.setPlaceholders(p, lore));

				result.setItemMeta(resultm);
			}

			lore.clear();

			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			editmeta.remove(p.getUniqueId());
			inventoryinstance.remove(p.getUniqueId());
			return;
		}

		if (e.getMessage().equalsIgnoreCase("cancel")) {
			lore.clear();

			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			editmeta.remove(p.getUniqueId());
			inventoryinstance.remove(p.getUniqueId());
			return;
		}

		String[] spl = e.getMessage().split(": ");

		if (spl == null || !isInt(spl[0]) || Integer.parseInt(spl[0]) <= 0) {
			p.sendMessage(ChatColor.RED + "Wrong format! You must first define the line number!");
			return;
		}

		if (lore.isEmpty() || lore.size() < Integer.parseInt(spl[0])) {

			if (spl.length < 2) {
				lore.add(" ");
				sendLoremsg(p);
				return;
			}

			lore.add(ChatColor.translateAlternateColorCodes('&', spl[1]));
			sendLoremsg(p);
			return;
		}

		if (spl.length < 2) {
			lore.set(Integer.parseInt(spl[0]) - 1, " ");
			sendLoremsg(p);

		} else if (spl[1].equals("remove")) {
			lore.remove(Integer.parseInt(spl[0]) - 1);
			sendLoremsg(p);

		} else {
			lore.set(Integer.parseInt(spl[0]) - 1, ChatColor.translateAlternateColorCodes('&', spl[1]));
			sendLoremsg(p);
		}
	}

	@EventHandler
	public void onClose(PlayerQuitEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();

		if (Main.getInstance().recipeBook.contains(uuid))
			Main.getInstance().recipeBook.remove(uuid);

		if (Main.getInstance().saveInventory.containsKey(uuid))
			Main.getInstance().saveInventory.remove(uuid);

		if (editmeta.containsKey(uuid))
			editmeta.remove(uuid);

		if (inventoryinstance.containsKey(uuid))
			inventoryinstance.remove(uuid);

		if (getnewid.containsKey(uuid))
			getnewid.remove(uuid);
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {

		Player p = e.getPlayer();

		if (!editmeta.containsKey(p.getUniqueId()) || editmeta.get(p.getUniqueId()) == null)
			return;

		e.setCancelled(true);

		if (editmeta.get(p.getUniqueId()).equals("Lore")) {
			handleLore(44, "Lore", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("identifier")) {
			handleString(9, "Identifier", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("permission")) {
			handleString(26, "Permission", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("name")) {
			handleString(0, "Name", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("amount")) {
			handleString(35, "Amount", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("group")) {
			handleString(51, "Group", p, e);
			return;
		}
	}

	public void handleStringMessage(Player p, String s, InventoryClickEvent e) {
		inventoryinstance.put(p.getUniqueId(), p.getOpenInventory().getTopInventory());
		editmeta.put(p.getUniqueId(), s.toLowerCase());
		p.closeInventory();

		List<String> getid = null;
		String identifier = null;
		if (e.getCurrentItem().getItemMeta().hasLore()) {
			getid = e.getCurrentItem().getItemMeta().getLore();
			identifier = getid.get(0);
		} else {
			String[] split = e.getCurrentItem().getItemMeta().getDisplayName().split(": ");
			String name = split.length > 1 ? split[1] : "null";
			identifier = name;
		}

		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
		p.sendMessage("Please type your new recipe " + s.toLowerCase());
		p.sendMessage("Current " + s + ": " + ChatColor.translateAlternateColorCodes('&', identifier));
		p.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to go back.");
		p.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "when finished.");
		p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
	}

	public boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void handleString(int slot, String string, Player p, AsyncPlayerChatEvent e) {

		Inventory inv = inventoryinstance.get(p.getUniqueId());
		ItemStack id = inv.getItem(slot);
		ItemMeta idm = id.getItemMeta();

		if (!e.getMessage().equalsIgnoreCase("done") && !e.getMessage().equalsIgnoreCase("cancel")) {
			p.sendMessage(" ");
			p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");
			p.sendMessage(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', e.getMessage()) + ChatColor.WHITE
					+ " has been selected as the new " + string.toLowerCase() + ".");
			p.sendMessage("Type " + ChatColor.GREEN + "DONE " + ChatColor.WHITE + "to confirm your selection!");
			p.sendMessage("Type " + ChatColor.RED + "CANCEL " + ChatColor.WHITE + "to cancel your selection!");
			p.sendMessage(ChatColor.GRAY + "Or retype selection for a different " + string.toLowerCase());
			p.sendMessage(ChatColor.DARK_GRAY + "---------------------------------------------");

			getnewid.put(p.getUniqueId(), e.getMessage());
		}

		if (getnewid.containsKey(p.getUniqueId()) && e.getMessage().equalsIgnoreCase("done")) {

			String newidf = getnewid.get(p.getUniqueId());
			List<String> loreList = new ArrayList<>();

			if (!string.equals("Amount")) {
				loreList.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', newidf));
				idm.setLore(loreList);
				id.setItemMeta(idm);
				inv.setItem(slot, id);
			}

			if (string.equals("Amount")) {
				if (!isInt(newidf)) {
					p.sendMessage(" ");
					p.sendMessage(ChatColor.RED + "Value must be in number format! Please try again.");
					return;
				}
				ItemStack item = inv.getItem(35);
				ItemMeta itemm = item.getItemMeta();
				itemm.setDisplayName(ChatColor.WHITE + "Amount: " + ChatColor.GRAY + newidf);
				item.setItemMeta(itemm);
				inv.setItem(35, item);

				if (inv.getItem(24) != null)
					inv.getItem(24).setAmount(Integer.valueOf(newidf)); // result slot updated
			}

			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			inventoryinstance.remove(p.getUniqueId());
			editmeta.remove(p.getUniqueId());
			getnewid.remove(p.getUniqueId());
			return;
		}

		if (!getnewid.containsKey(p.getUniqueId()) || e.getMessage().equalsIgnoreCase("cancel")) {
			editmeta.remove(p.getUniqueId());
			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			inventoryinstance.remove(p.getUniqueId());
		}
	}

	// Legacy helper left as-is for compatibility with other code paths
	public boolean hasSomething(Inventory i) {
		// updated crafting grid + result slot
		if ((i.getItem(11) != null || i.getItem(12) != null || i.getItem(13) != null || i.getItem(20) != null
				|| i.getItem(21) != null || i.getItem(22) != null || i.getItem(29) != null || i.getItem(30) != null
				|| i.getItem(31) != null) && i.getItem(24) != null) {
			return true;
		}
		return false;
	}

	private void logError(String st, String recipe) {
		Logger.getLogger("Minecraft").log(Level.WARNING,
				"[DEBUG][" + Main.getInstance().getName() + "][" + recipe + "] " + st);
	}
}