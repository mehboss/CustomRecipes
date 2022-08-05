package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.function.Consumer;

public class Main extends JavaPlugin implements Listener {

	public static Recipes recipes;
	public addMenu addItem;

	ItemStack i = null;
	ItemStack ii = null;
	ShapedRecipe R = null;
	ItemStack Item = null;
	
	public static ArrayList<ShapedRecipe> recipe = new ArrayList<ShapedRecipe>();
	public static ArrayList<Material> ShapelessID = new ArrayList<Material>();
	public static ArrayList<ItemStack> recipee = new ArrayList<ItemStack>();
	public static ArrayList<String> addRecipe = new ArrayList<String>();
	public ArrayList<String> ShapelessName = new ArrayList<String>();
	public ArrayList<String> isItemforEffect = new ArrayList<String>();

	// add three more shapelessname, amount, and ID specifically for config.

	public static HashMap<String, String> menu = new HashMap<String, String>();
	public HashMap<Integer, Integer> ShapelessAmount = new HashMap<Integer, Integer>();
	public HashMap<String, String> matches = new HashMap<String, String>();
	public HashMap<String, ItemStack> giveRecipe = new HashMap<String, ItemStack>();
	public HashMap<String, String> recipeAmounts = new HashMap<String, String>();
	public HashMap<Integer, Integer> amountCheck = new HashMap<Integer, Integer>();
	public HashMap<ItemStack, Boolean> shapeless = new HashMap<ItemStack, Boolean>();

	public void onEnable() {

		System.out.print(
				"[CustomRecipes] Made by MehBoss on Spigot. For support please PM me and I will get back to you as soon as possible!");

		new UpdateChecker(this, 36925).getVersion(version -> {
			if (this.getDescription().getVersion().equals(version)) {
				System.out.print("[CustomRecipes] Checking for updates..");
				System.out.print(
						"[CustomRecipes] An update has been found! This could be bug fixes or additional features. Please update CustomRecipes at https://www.spigotmc.org/resources/authors/mehboss.139036/");
			} else {
				System.out.print("[CustomRecipes] Checking for updates..");
				System.out.print(
						"[CustomRecipes] We are all up to date with the latest version. Thank you for using custom recipes :)");
			}
		});

		Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");

		config.saveDefaultConfig();
		addItem = new addMenu(config, null);

		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new Effects(this), this);
		Bukkit.getPluginManager().registerEvents(new Recipes(this, null), this);
		getCommand("crecipe").setExecutor(new GiveRecipe(this));
		addItems();
	}
	
	public void clear() {
		reloadConfig();
		getServer().clearRecipes();

		matches.clear();
		recipe.clear();
		giveRecipe.clear();
		recipeAmounts.clear();
		amountCheck.clear();
		shapeless.clear();
		menu.clear();
		ShapelessName.clear();
		ShapelessAmount.clear();
		ShapelessID.clear();
		isItemforEffect.clear();
		addRecipe.clear();
		addItem = null;
		recipes = null;
	}
	public void onDisable() {
		clear();
	}
	public void reload() {
		clear();
		addItems();
	}

	public void addItems() {

		Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");

		for (String item : config.getConfig().getConfigurationSection("Items").getKeys(false)) {

			List<String> loreList = new ArrayList<String>();
			List<String> r = config.getConfig().getStringList("Items." + item + ".ItemCrafting");
			
			String type = config.getConfig().getString("Items." + item + ".Item").toUpperCase();
			String damage = config.getConfig().getString("Items." + item + ".Item-Damage");
			int amount = config.getConfig().getInt("Items." + item + ".Amount");
			
			ItemMeta m = i.getItemMeta();
			

			if (damage.equalsIgnoreCase("none")) {
				i = new ItemStack(Material.matchMaterial(type), amount);
			} else {
				i = new ItemStack(Material.matchMaterial(type), amount, Short.valueOf(damage));
			}

			if (config.getConfig().getString("Items." + item + ".Name") != null) {
				m.setDisplayName(ChatColor.translateAlternateColorCodes('&',
						config.getConfig().getString("Items." + item + ".Name")));
			}

			if (config.getConfig().getStringList("Items." + item + ".Lore") != null) {

				for (String Item1Lore : config.getConfig().getStringList("Items." + item + ".Lore")) {

					String crateLore = (Item1Lore.replaceAll("(&([a-fk-o0-9]))", "\u00A7$2"));

					loreList.add(crateLore);
				}
				m.setLore(loreList);
			}

			if (config.getConfig().getBoolean("Items." + item + ".Hide-Enchants") == true) {
				m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}

			i.setItemMeta(m);

			if (config.getConfig().getStringList("Items." + item + ".Effects") != null) {
				isItemforEffect.add(i.getItemMeta().getDisplayName().replaceAll(" ", ""));
			}

			if (config.getConfig().getStringList("Items." + item + ".Enchantments") != null) {

				for (String e : config.getConfig().getStringList("Items." + item + ".Enchantments")) {
					String[] breakdown = e.split(":");

					String enchantment = breakdown[0];

					int lvl = Integer.parseInt(breakdown[1]);
					i.addUnsafeEnchantment(Enchantment.getByName(enchantment), lvl);
				}
			}

			String line1 = r.get(0);
			String line2 = r.get(1);
			String line3 = r.get(2);

			giveRecipe.put(item.toLowerCase(), i);
			R = new ShapedRecipe(i);
			R.shape(line1, line2, line3);

			if (config.getConfig().getBoolean("Items." + item + "Shapeless") == true) {
				shapeless.put(R.getResult(), true);
			} else {
				shapeless.put(R.getResult(), false);
			}

			for (String I : config.getConfig().getStringList("Items." + item + ".Ingredients")) {
				String[] b = I.split(":");
				char lin1 = b[0].charAt(0);
				String lin2 = b[1];
				Material mi = Material.matchMaterial(lin2);

				R.setIngredient(lin1, mi);
			}
			
			Bukkit.getServer().addRecipe(R);
			recipe.add(R);
			recipes = new Recipes(this, null);
		}
	}

	@EventHandler
	public void check(PrepareItemCraftEvent e) {

		String letterSlot1 = "";
		String letterSlot2 = "";
		String letterSlot3 = "";
		String letterSlot4 = "";
		String letterSlot5 = "";
		String letterSlot6 = "";
		String letterSlot7 = "";
		String letterSlot8 = "";
		String letterSlot9 = "";

		CraftingInventory inv = e.getInventory();
		Boolean recipematches = false;
		Boolean recipeMatches = false;

		Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");

		if (inv.getType() == InventoryType.WORKBENCH) {

			if (inv.getResult() == null || inv.getResult().getType().equals(Material.AIR)) {

				// compare the two arrays and make sure they have required ingredients.
				// for each recipe in the config, check if shapeless, loop and add to arrays.
				// Inside loop execute checks, after check return since it may have failed
				for (int j = 1; j < 10; j++) {
					if (inv.getItem(j) != null) {

						ShapelessAmount.put(j, inv.getItem(j).getAmount());
						ShapelessID.add(inv.getItem(j).getType());
						// add amounts to corresponding slots needed for shapeless recipes
						// add materials needed to check later

						if (inv.getItem(j).getItemMeta() != null && inv.getItem(j).getItemMeta().hasDisplayName()) {
							ShapelessName.add(inv.getItem(j).getItemMeta().getDisplayName());
							// add item displaynames of slots to check later
						}

					}

				}

				return;
			}

			for (ShapedRecipe newrecipe : recipe) {
				if (inv.getResult().equals(newrecipe.getResult())) {

					recipee.add(newrecipe.getResult());

					recipematches = true;

					inv.setResult(new ItemStack(Material.AIR));
				}

				if (recipematches == true) {
					String invSlot1 = "";
					String invSlot2 = "";
					String invSlot3 = "";
					String invSlot4 = "";
					String invSlot5 = "";
					String invSlot6 = "";
					String invSlot7 = "";
					String invSlot8 = "";
					String invSlot9 = "";

					matches.put("X", "false");

					for (String name : config.getConfig().getConfigurationSection("Items").getKeys(false)) {
						for (String In : config.getConfig().getStringList("Items." + name + ".Ingredients")) {

							String[] newsplit = In.split(":");
							recipeAmounts.put(newsplit[0], newsplit[2]);

							if (newsplit.length == 3) {
								matches.put(newsplit[0], "false");
							} else if (newsplit.length > 3) {
								matches.put(newsplit[0], ChatColor.translateAlternateColorCodes('&', newsplit[3]));
							}
						}

						List<String> r = config.getConfig().getStringList("Items." + name + ".ItemCrafting");

						String row1 = r.get(0);
						String row2 = r.get(1);
						String row3 = r.get(2);

						// loops 3 times for all crafting rows

						for (int j = 1; j < 10; j++) {
							String[] newsplit1 = row1.split("");
							String[] newsplit2 = row2.split("");
							String[] newsplit3 = row3.split("");

							if (j == 1) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit1[0])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot1 = "false";

								} else {
									String slot1 = newsplit1[0];
									letterSlot1 = matches.get(slot1); // slot 1

								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot1 = "false";
								} else {
									invSlot1 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 2) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit1[1])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot2 = "false";
								} else {

									String slot2 = newsplit1[1];
									letterSlot2 = matches.get(slot2); // slot 2
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot2 = "false";
								} else {
									invSlot2 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 3) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit1[2])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot3 = "false";
								} else {

									String slot3 = newsplit1[2];
									letterSlot3 = matches.get(slot3); // slot 3
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot3 = "false";
								} else {
									invSlot3 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 4) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit2[0])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot4 = "false";
								} else {

									String slot4 = newsplit2[0];
									letterSlot4 = matches.get(slot4);
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot4 = "false";
								} else {
									invSlot4 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 5) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit2[1])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot5 = "false";
								} else {

									String slot5 = newsplit2[1];
									letterSlot5 = matches.get(slot5);
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot5 = "false";
								} else {
									invSlot5 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 6) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit2[2])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot6 = "false";
								} else {

									String slot6 = newsplit2[2];
									letterSlot6 = matches.get(slot6); // slot 6
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot6 = "false";
								} else {
									invSlot6 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 7) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit3[0])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot7 = "false";
								} else {

									String slot7 = newsplit3[0];
									letterSlot7 = matches.get(slot7); // slot 7

								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot7 = "false";
								} else {
									invSlot7 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 8) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit3[1])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot8 = "false";
								} else {

									String slot8 = newsplit3[1];
									letterSlot8 = matches.get(slot8); // slot 8
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot8 = "false";
								} else {
									invSlot8 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
							if (j == 9) {

								if (inv.getItem(j) != null) {
									amountCheck.put(j, Integer.valueOf(recipeAmounts.get(newsplit3[2])));
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null) {
									letterSlot9 = "false";
								} else {

									String slot9 = newsplit3[2];
									letterSlot9 = matches.get(slot9); // slot 9
								}

								if (inv.getItem(j) == null || inv.getItem(j).getType() == null
										|| !inv.getItem(j).getItemMeta().hasDisplayName()) {
									invSlot9 = "false";
								} else {
									invSlot9 = inv.getItem(j).getItemMeta().getDisplayName();
								}
							}
						}

						// name check
						// amount check

						boolean passedCheck = true;

						if ((invSlot1.equals(letterSlot1) && invSlot2.equals(letterSlot2)
								&& invSlot3.equals(letterSlot3) && invSlot4.equals(letterSlot4)
								&& invSlot5.equals(letterSlot5) && invSlot6.equals(letterSlot6)
								&& invSlot7.equals(letterSlot7) && invSlot8.equals(letterSlot8)
								&& invSlot9.equals(letterSlot9))) {

							for (int a = 1; a < 10; a++) {
								if (inv.getItem(a) != null) {
									if (inv.getItem(a).getAmount() != amountCheck.get(a)) {
										passedCheck = false;
									}
								}
							}

							if (passedCheck == true) {
								List<String> loreList = new ArrayList<String>();
								
								String type = config.getConfig().getString("Items." + name + ".Item").toUpperCase();
								String damage = config.getConfig().getString("Items." + name + ".Item-Damage");
								int amount = config.getConfig().getInt("Items." + name + ".Amount");
								ItemMeta m = ii.getItemMeta();

								if (damage.equalsIgnoreCase("none")) {
									ii = new ItemStack(Material.matchMaterial(type), amount);
								} else {
									ii = new ItemStack(Material.matchMaterial(type), amount, Short.valueOf(damage));
								}

								if (config.getConfig().getString("Items." + name + ".Name") != null) {
									m.setDisplayName(ChatColor.translateAlternateColorCodes('&',
											config.getConfig().getString("Items." + name + ".Name")));
								}

								if (config.getConfig().getStringList("Items." + name + ".Lore") != null) {

									for (String Item1Lore : config.getConfig()
											.getStringList("Items." + name + ".Lore")) {

										String crateLore = (Item1Lore.replaceAll("(&([a-fk-o0-9]))", "\u00A7$2"));

										loreList.add(crateLore);
									}
									m.setLore(loreList);
								}

								ii.setItemMeta(m);

								if (config.getConfig().getStringList("Items." + name + ".Enchantments") != null) {

									for (String ee : config.getConfig()
											.getStringList("Items." + name + ".Enchantments")) {
										String[] breakdown = ee.split(":");
										String enchantment = breakdown[0];
										int lvl = Integer.parseInt(breakdown[1]);
										
										ii.addUnsafeEnchantment(Enchantment.getByName(enchantment), lvl);
									}
								}

								Item = ii;
								recipeMatches = true;

								if (e.getView().getPlayer() instanceof Player) {
									if ((config.getConfig().getString("Items." + name + ".Permission") != null)
											&& (!e.getView().getPlayer().hasPermission(
													config.getConfig().getString("Items." + name + ".Permission")))) {
										recipeMatches = false;

									}
								}
								break;
							}
						}
					}
					if (recipeMatches == true) {
						inv.setResult(new ItemStack(ii));

					} else {
						inv.setResult(new ItemStack(Material.AIR));
					}
					
					matches.clear();
					Item = null;
					ii = null;
				}
			}
		}
	}
}