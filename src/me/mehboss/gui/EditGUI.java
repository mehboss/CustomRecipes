package me.mehboss.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

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
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.cryptomorin.xseries.XMaterial;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil;
import me.mehboss.utils.RecipeUtil.Ingredient;

public class EditGUI implements Listener {

	// *** this menu is the GUI for adding a new recipe
	public HashMap<UUID, String> editmeta = new HashMap<UUID, String>();
	HashMap<UUID, Inventory> inventoryinstance = new HashMap<UUID, Inventory>();
	HashMap<UUID, String> getr = new HashMap<UUID, String>();
	HashMap<UUID, String> getnewid = new HashMap<UUID, String>();

	List<String> lore = new ArrayList<>();

	RecipeUtil recipeUtil = Main.getInstance().getRecipeUtil();
	private static EditGUI instance;

	Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");
	RecipeSaver saveChanges;

	public EditGUI(Plugin p, String item) {
		instance = this;
		saveChanges = new RecipeSaver();
	}

	public static EditGUI getInstance() {
		return instance;
	}

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	public void setItems(Boolean viewing, Inventory i, String invname, ItemStack item, OfflinePlayer p) {

		String configname = invname;

		List<String> loreList = new ArrayList<String>();

		int[] paneslot = { 0, 1, 2, 3, 4, 5, 6, 9, 13, 14, 15, 18, 22, 24, 27, 31, 32, 33, 34, 36, 37, 38, 39, 41, 42,
				43, 45, 46, 47, 51, 52 };
		ItemStack stained = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
		ItemMeta stainedm = stained.getItemMeta();
		stainedm.setDisplayName(" ");
		stained.setItemMeta(stainedm);

		for (int slot : paneslot) {
			i.setItem(slot, stained);
		}

		int slot = 1;
		for (Ingredient ingredient : Main.getInstance().getRecipeUtil().getRecipe(configname).getIngredients()) {

			if (ingredient.isEmpty() || !XMaterial.matchXMaterial(ingredient.getMaterial().toString()).isPresent()) {
				slot++;
				continue;
			}

			boolean foundIdentifier = itemIngredients(ingredient, configname, slot, p) != null ? true : false;
			ItemStack mat = foundIdentifier ? itemIngredients(ingredient, configname, slot, p)
					: XMaterial.matchXMaterial(ingredient.getMaterial().toString()).get().parseItem();

			mat.setAmount(getAmounts(ingredient, slot));

			if (!foundIdentifier) {
				ItemMeta matm = mat.getItemMeta();

				if (getNames(ingredient, slot) != null)
					matm.setDisplayName(getNames(ingredient, slot));

				mat.setItemMeta(matm);
			}

			if (slot == 1)
				i.setItem(10, mat);
			if (slot == 2)
				i.setItem(11, mat);
			if (slot == 3)
				i.setItem(12, mat);
			if (slot == 4)
				i.setItem(19, mat);
			if (slot == 5)
				i.setItem(20, mat);
			if (slot == 6)
				i.setItem(21, mat);
			if (slot == 7)
				i.setItem(28, mat);
			if (slot == 8)
				i.setItem(29, mat);
			if (slot == 9)
				i.setItem(30, mat);

			slot++;

		}

		ItemStack result = new ItemStack(Main.getInstance().recipeUtil.getRecipe(configname).getResult());
		ItemMeta resultM = result.getItemMeta();

		if (result.hasItemMeta() && resultM.hasLore() && hasPlaceholder()) {
			resultM.setLore(PlaceholderAPI.setPlaceholders(p, resultM.getLore()));
			result.setItemMeta(resultM);
		}

		i.setItem(23, result);

		if (viewing) {
			int[] emptySlots = { 7, 8, 16, 17, 25, 26, 35, 40, 44, 53 };

			ItemStack back = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
			ItemMeta backm = back.getItemMeta();
			backm.setDisplayName(ChatColor.DARK_RED + "Back");
			back.setItemMeta(backm);

			i.setItem(48, back);
			i.setItem(49, back);
			i.setItem(50, back);

			for (int slots : emptySlots)
				i.setItem(slots, stained);

			return;
		}

		ItemStack identifier = XMaterial.PAPER.parseItem();
		ItemMeta identifierm = identifier.getItemMeta();
		identifierm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fIdentifier:&7 "));
		loreList.add(ChatColor.GRAY + getConfig(configname).getString(configname + ".Identifier"));
		identifierm.setLore(loreList);
		identifier.setItemMeta(identifierm);
		loreList.clear();

		ItemStack lore = XMaterial.BOOK.parseItem();
		ItemMeta lorem = lore.getItemMeta();
		lorem.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fLore"));

		if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
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

		ItemStack enchants = XMaterial.EXPERIENCE_BOTTLE.parseItem();
		ItemMeta enchantsm = effects.getItemMeta();
		enchantsm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fEnchantments"));
		if (getConfig(configname).getStringList(configname + ".Enchantments") != null) {
			for (String enchantslore : getConfig(configname).getStringList(configname + ".Enchantments")) {
				loreList.add(ChatColor.GRAY + enchantslore);
			}
			enchantsm.setLore(loreList);
		}
		enchants.setItemMeta(enchantsm);
		loreList.clear();

		ItemStack permission = XMaterial.ENDER_PEARL.parseItem();
		ItemMeta permissionm = permission.getItemMeta();
		permissionm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fPermission: &7"));

		if (getConfig(configname).isSet(configname + ".Permission")
				&& !getConfig(configname).getString(configname + ".Permission").equals("none"))
			loreList.add(ChatColor.GRAY + getConfig(configname).getString(configname + ".Permission"));
		permissionm.setLore(loreList);
		permission.setItemMeta(permissionm);
		loreList.clear();

		ItemStack hideenchants = XMaterial.ENCHANTING_TABLE.parseItem();
		ItemMeta hideenchantsm = hideenchants.getItemMeta();
		Boolean hc = getConfig(configname).getBoolean(configname + ".Hide-Enchants");
		String ht = "&cfalse";

		if (hc == true)
			ht = "&atrue";

		hideenchantsm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fHide Enchants: &f" + ht));
		hideenchants.setItemMeta(hideenchantsm);

		ItemStack shapeless = XMaterial.CRAFTING_TABLE.parseItem();
		ItemMeta shapelessm = shapeless.getItemMeta();
		Boolean sc = getConfig(configname).getBoolean(configname + ".Shapeless");
		String toggle = "&cfalse";

		if (sc == true)
			toggle = "&atrue";

		shapelessm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fShapeless: &f" + toggle));
		shapeless.setItemMeta(shapelessm);

		ItemStack name = XMaterial.WRITABLE_BOOK.parseItem();
		ItemMeta namem = name.getItemMeta();
		namem.setDisplayName(ChatColor.WHITE + "Recipe Name: ");

		if (getConfig(configname).isSet(configname + ".Name")
				&& !getConfig(configname).getString(configname + ".Name").equals("none"))
			loreList.add(
					ChatColor.translateAlternateColorCodes('&', getConfig(configname).getString(configname + ".Name")));
		namem.setLore(loreList);
		name.setItemMeta(namem);
		loreList.clear();

		ItemStack amount = XMaterial.FEATHER.parseItem();
		ItemMeta amountm = amount.getItemMeta();
		amountm.setDisplayName(ChatColor.translateAlternateColorCodes('&',
				"&fAmount: &7" + getConfig(configname).getString(configname + ".Amount")));
		amount.setItemMeta(amountm);

		i.setItem(7, identifier);
		i.setItem(8, hideenchants);
		i.setItem(16, effects);
		i.setItem(17, enchants);
		i.setItem(25, permission);
		i.setItem(26, name);
		i.setItem(35, amount);
		i.setItem(44, lore);
		i.setItem(53, shapeless);

		ItemStack enabled = XMaterial.SLIME_BALL.parseItem();
		ItemMeta enabledm = enabled.getItemMeta();
		enabledm.setDisplayName(ChatColor.GREEN + "Enabled");
		enabled.setItemMeta(enabledm);
		if (getConfig(configname).getBoolean(configname + ".Enabled") == false) {
			enabled = XMaterial.SNOWBALL.parseItem();
			enabledm.setDisplayName(ChatColor.RED + "Disabled");
			enabled.setItemMeta(enabledm);
		}
		i.setItem(40, enabled);

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

	public int getAmounts(Ingredient ingredient, int slot) {
		if (!ingredient.isEmpty())
			return ingredient.getAmount();
		return 0;
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
			Boolean isCustomItem = recipeUtil.getRecipeFromKey(ingredient.getIdentifier()) == null ? true : false;
			ItemStack itemsAdder = isCustomItem ? Main.getInstance().plugin.handleItemAdderCheck(null, recipename,
					ingredient.getIdentifier(), false) : null;
			ItemStack mythicItem = isCustomItem ? Main.getInstance().plugin.handleMythicItemCheck(null, recipename,
					ingredient.getIdentifier(), false) : null;
			ItemStack item = null;

			if (isCustomItem) {
				item = (itemsAdder != null) ? itemsAdder : (mythicItem != null) ? mythicItem : null;
			} else {
				item = recipeUtil.getRecipeFromKey(ingredient.getIdentifier()).getResult();
			}

			if (item == null)
				return null;

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

	public String getNames(Ingredient ingredient, int slot) {
		if (!ingredient.isEmpty() && ingredient.hasDisplayName())
			return ingredient.getDisplayName();
		return null;
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		if (!(e.getWhoClicked() instanceof Player) || e.getClickedInventory() == null
				|| e.getClickedInventory().getType() == InventoryType.PLAYER)
			return;

		Player p = (Player) e.getWhoClicked();
		InventoryView view = e.getView();
		String inventoryTitle = view.getTitle();

		if (e.getInventory() != null && inventoryTitle != null && inventoryTitle.contains("VIEWING: ")) {
			e.setCancelled(true);

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getRawSlot() == 23)
				return;

			if (recipeUtil.getRecipeFromResult(e.getCurrentItem()) != null) {

				String name = recipeUtil.getRecipeFromResult(e.getCurrentItem()).getName();

				Inventory edit = Bukkit.getServer().createInventory(null, 54,
						ChatColor.translateAlternateColorCodes('&', "&cVIEWING: " + name));

				setItems(true, edit, name, e.getCurrentItem(), p);

				Main.getInstance().saveInventory.put(p.getUniqueId(), e.getInventory());
				p.openInventory(edit);
			}

			if (e.getRawSlot() == 48 || e.getRawSlot() == 49 || e.getRawSlot() == 50) {

				if (Main.getInstance().saveInventory.containsKey(p.getUniqueId())) {
					p.openInventory(Main.getInstance().saveInventory.get(p.getUniqueId()));
					Main.getInstance().saveInventory.remove(p.getUniqueId());
				} else {
					Main.getInstance().recipes.show(p);
				}
			}

			return;
		}

		if (e.getInventory() != null && inventoryTitle != null && inventoryTitle.contains("EDITING: ")) {

			String[] split = inventoryTitle.split("EDITING: ");
			String recipe = split[1];

			if (e.getRawSlot() != 10 && e.getRawSlot() != 11 && e.getRawSlot() != 12 && e.getRawSlot() != 19
					&& e.getRawSlot() != 20 && e.getRawSlot() != 21 && e.getRawSlot() != 28 && e.getRawSlot() != 29
					&& e.getRawSlot() != 30 && e.getRawSlot() != 23) {

				e.setCancelled(true);
			}

			if (e.getRawSlot() == 16)
				return;
			// effects button

			if (e.getRawSlot() == 17)
				return;
			// enchants button

			if (e.getRawSlot() == 35) {
				// amount button
				handleStringMessage(p, "Amount", e);
			}

			ItemStack toggle = e.getCurrentItem();

			if (toggle == null || toggle.getItemMeta() == null)
				return;

			ItemMeta togglem = toggle.getItemMeta();

			if (e.getRawSlot() == 8) {
				// hide enchants?

				if (togglem.getDisplayName().contains("true")) {
					togglem.setDisplayName("Hide Enchants: " + ChatColor.RED + "false");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(8, toggle);
					return;
				}

				if (togglem.getDisplayName().contains("false")) {
					togglem.setDisplayName("Hide Enchants: " + ChatColor.GREEN + "true");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(8, toggle);
					return;
				}
			}

			if (e.getRawSlot() == 40) {

				// recipe toggle enable/disable

				if (togglem.getDisplayName().contains("Enabled")) {
					togglem.setDisplayName(ChatColor.RED + "Disabled");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(40, toggle);
					e.getCurrentItem().setType(XMaterial.SNOWBALL.parseMaterial());
					return;
				}

				if (togglem.getDisplayName().contains("Disabled")) {
					togglem.setDisplayName(ChatColor.GREEN + "Enabled");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(40, toggle);
					e.getCurrentItem().setType(XMaterial.SLIME_BALL.parseMaterial());
					return;
				}
			}

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
				p.sendMessage(
						ChatColor.RED + "You have successfully cancelled any changes to the recipe '" + recipe + "'");
				p.closeInventory();
			}

			if (e.getRawSlot() == 49) {
				// main menu button
				Main.getInstance().recipes.show(p);
			}

			if (e.getRawSlot() == 50) {
				// update button
				saveChanges.saveRecipe(e.getClickedInventory(), p, recipe);
			}

			if (e.getRawSlot() == 53) {
				// shapeless?

				if (togglem.getDisplayName().contains("true")) {
					togglem.setDisplayName("Shapeless: " + ChatColor.RED + "false");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(53, toggle);
					return;
				}

				if (togglem.getDisplayName().contains("false")) {
					togglem.setDisplayName("Shapeless: " + ChatColor.GREEN + "true");
					toggle.setItemMeta(togglem);

					p.getOpenInventory().getTopInventory().setItem(53, toggle);
					return;
				}
			}

			if (e.getRawSlot() == 7) {
				// change identifier
				handleStringMessage(p, "Identifier", e);
			}

			if (e.getRawSlot() == 25) {
				// change permission
				handleStringMessage(p, "Permission", e);
			}

			if (e.getRawSlot() == 26) {
				// change recipe name
				handleStringMessage(p, "Name", e);
			}

			if (e.getRawSlot() == 45) {

				if (ChatColor.stripColor(e.getClickedInventory().getItem(45).getItemMeta().getDisplayName())
						.equals("Confirm Recipe Deletion")) {

					Main.getInstance().getConfig().set("Items." + recipe, null);
					Main.getInstance().saveConfig();
					Main.getInstance().reload();

					Main.getInstance().recipes.show(p);
					return;
				}

				ItemStack confirm = e.getClickedInventory().getItem(45);
				ItemMeta confirmm = e.getClickedInventory().getItem(45).getItemMeta();
				confirmm.setDisplayName(ChatColor.DARK_RED + "Confirm Recipe Deletion");
				confirm.setItemMeta(confirmm);
			}
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
		ItemStack result = inv.getItem(23);

		if (!editmeta.containsKey(p.getUniqueId()))
			return;

		if (e.getMessage().equalsIgnoreCase("done")) {

			if (!lore.isEmpty() && item.hasItemMeta()) {
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

		if (getr.containsKey(uuid))
			getr.remove(uuid);

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
			handleString(7, "Identifier", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("permission")) {
			handleString(25, "Permission", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("name")) {
			handleString(26, "Name", p, e);
			return;
		}

		if (editmeta.get(p.getUniqueId()).equals("amount")) {
			handleString(35, "Amount", p, e);
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
			String amount = split[1];
			identifier = amount;
		}

		if (identifier == null)
			Main.getInstance().getLogger().log(Level.SEVERE,
					"Something went wrong while using the GUI! This was unexpected.. Please PM Mehboss on Spigot for further information regarding this issue. STRING: "
							+ s);

		String[] split = e.getView().getTitle().split("EDITING: ");
		String recipe = split[1];

		getr.put(p.getUniqueId(), recipe);
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
			List<String> loreList = new ArrayList<String>();

			if (!string.equals("Amount")) {
				loreList.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', newidf));
				idm.setLore(loreList);
				id.setItemMeta(idm);

				inv.setItem(slot, id);
			}

			if (string.equals("Name")) {
				ItemStack item = inv.getItem(23);
				ItemMeta itemm = item.getItemMeta();

				itemm.setDisplayName(ChatColor.translateAlternateColorCodes('&', newidf));
				item.setItemMeta(itemm);
				inv.setItem(23, item);
				// slot 23
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
				inv.getItem(23).setAmount(Integer.valueOf(newidf));
				item.setItemMeta(itemm);
				inv.setItem(35, item);
				// slot 35
			}

			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			inventoryinstance.remove(p.getUniqueId());
			getr.remove(p.getUniqueId());
			editmeta.remove(p.getUniqueId());
			getnewid.remove(p.getUniqueId());
			return;
		}

		if (!getnewid.containsKey(p.getUniqueId()) || e.getMessage().equalsIgnoreCase("cancel")) {
			editmeta.remove(p.getUniqueId());
			getr.remove(p.getUniqueId());

			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomRecipes"), () -> {
				p.openInventory(inv);
			});

			inventoryinstance.remove(p.getUniqueId());

		}
	}

	public boolean hasSomething(Inventory i) {

		if ((i.getItem(10) != null || i.getItem(11) != null || i.getItem(12) != null || i.getItem(19) != null
				|| i.getItem(20) != null || i.getItem(21) != null || i.getItem(28) != null || i.getItem(29) != null
				|| i.getItem(30) != null) && i.getItem(23) != null) {
			return true;
		}
		return false;
	}
}