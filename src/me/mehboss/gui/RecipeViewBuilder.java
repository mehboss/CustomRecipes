package me.mehboss.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.gui.framework.GuiButton;
import me.mehboss.gui.framework.GuiButton.GuiStringButton;
import me.mehboss.gui.framework.GuiButton.GuiLoreButton;
import me.mehboss.gui.framework.GuiButton.GuiToggleButton;
import me.mehboss.gui.framework.GuiView;
import me.mehboss.gui.framework.RecipeItemFactory;
import me.mehboss.gui.framework.RecipeLayout;
import me.mehboss.recipe.Main;
import me.mehboss.utils.RecipeUtil.Ingredient;
import me.mehboss.utils.RecipeUtil.Recipe;
import me.mehboss.utils.RecipeUtil.Recipe.RecipeType;
import me.mehboss.utils.data.BrewingRecipeData;
import me.mehboss.utils.data.CookingRecipeData;
import me.mehboss.utils.data.CraftingRecipeData;
import me.mehboss.utils.data.SmithingRecipeData;
import me.mehboss.utils.data.SmithingRecipeData.SmithingRecipeType;
import me.mehboss.utils.data.WorkstationRecipeData;
import me.mehboss.utils.libs.CompatibilityUtil;

/**
 * Builds the GUI for viewing and editing recipes using the modular GUI
 * framework.
 */
public class RecipeViewBuilder {

	public GuiView buildViewing(Recipe recipe) {
		return build(recipe, false, null);
	}

	public GuiView buildEditing(Recipe recipe, Player p) {
		return build(recipe, true, p);
	}

	private GuiView build(Recipe recipe, boolean editing, Player p) {

		RecipeType type = recipe.getType();
		RecipeLayout layout = RecipeLayout.forType(type);

		GuiView view = new GuiView(54,
				(editing ? getParsedValue("Editing", "&aEDITING: ") : getParsedValue("Viewing", "&cVIEWING: "))
						+ safeString(recipe.getName()));

		/* Background */
		fillBackground(view, layout);

		/* Ingredients */
		setupIngredientSlots(view, recipe, layout, p);
		setupBrewingItem(view, recipe, layout, p);

		/* Result */
		setupResultSlot(view, recipe, layout, p);
		view.setEditing(editing);

		if (!editing) {
			setupViewingControls(view);
		} else {
			setupEditingControls(view, recipe, layout, p);
		}

		return view;
	}

	private void fillBackground(GuiView view, RecipeLayout layout) {

		ItemStack pane = RecipeItemFactory.button(XMaterial.BLACK_STAINED_GLASS_PANE, " ");

		boolean[] blocked = new boolean[54];
		for (int i = 0; i < 54; i++)
			blocked[i] = true;

		blocked[layout.getResultSlot()] = false;

		for (int slot : layout.getIngredientSlots())
			blocked[slot] = false;

		if (layout.usesFuel())
			blocked[layout.getFuelSlot()] = false;

		for (int slot = 0; slot < 54; slot++) {
			if (blocked[slot]) {
				view.addButton(new GuiButton(slot, pane) {
					@Override
					public void onClick(Player p, GuiView v, InventoryClickEvent e) {
					}
				});
			}
		}
	}

	private void setupIngredientSlots(GuiView view, Recipe recipe, RecipeLayout layout, Player p) {

		List<Ingredient> ingredients = recipe.getIngredients();

		// New recipe → keep ingredient slots empty (AIR)
		if (ingredients == null || ingredients.isEmpty())
			return;

		int[] slots = layout.getIngredientSlots();
		int index = 0;

		for (Ingredient ingredient : ingredients) {

			if (index >= slots.length)
				break;

			ItemStack icon = ingredientItem(ingredient, p);
			if (icon == null)
				icon = null; // leave AIR

			final int slot = slots[index];
			view.getInventory().setItem(slot, icon);

			index++;
		}
	}

	private void setupBrewingItem(GuiView view, Recipe recipe, RecipeLayout layout, Player p) {

		if (recipe.getType() != RecipeType.BREWING_STAND)
			return;

		BrewingRecipeData brewing = (BrewingRecipeData) recipe;
		ItemStack requiredItem = brewing.requiresBottles() ? new ItemStack(brewing.getRequiredBottleType()) : null;
		int slot = layout.getFuelSlot();

		// New recipe → keep ingredient slots empty (AIR)
		if (requiredItem == null)
			return;

		view.getInventory().setItem(slot, requiredItem);
	}

	private ItemStack ingredientItem(Ingredient ing, Player p) {

		if (ing == null || ing.isEmpty())
			return null;

		ItemStack base = null;

		if (ing.hasIdentifier()) {
			Recipe r = Main.getInstance().recipeUtil.getRecipeFromKey(ing.getIdentifier());
			if (r != null)
				base = r.getResult();
			else
				base = Main.getInstance().recipeUtil.getResultFromKey(ing.getIdentifier());
		} else if (ing.hasItem()) {
			base = ing.getItem();
		} else {
			base = XMaterial.matchXMaterial(ing.getMaterial().toString()).get().parseItem();

			if (ing.hasMaterialData())
				base = ing.getMaterialData().toItemStack();

			if (base == null)
				return null;

			ItemMeta meta = base.getItemMeta();
			if (ing.hasDisplayName()) {
				meta.setDisplayName(ing.getDisplayName());
			}

			if (CompatibilityUtil.supportsItemModel() && meta.hasItemModel())
				meta.setItemModel(org.bukkit.NamespacedKey.fromString(ing.getItemModel()));
			if (CompatibilityUtil.supportsCustomModelData() && meta.hasCustomModelData())
				meta.setCustomModelData(ing.getCustomModelData());

			if (ing.hasLore())
				meta.setLore(ing.getLore());

			base.setItemMeta(meta);
		}

		if (base == null)
			return null;

		base.setAmount(ing.getAmount());
		return base;
	}

	private void setupResultSlot(GuiView view, Recipe recipe, RecipeLayout layout, Player p) {

		// New recipe → leave result slot empty
		if (recipe.getResult() == null)
			return;

		ItemStack result = recipe.getResult().clone();
		view.getInventory().setItem(layout.getResultSlot(), result);
	}

	private void setupViewingControls(GuiView view) {
		ItemStack back = RecipeItemFactory.button(XMaterial.RED_STAINED_GLASS_PANE, getValue("Buttons.Back", "&cBack"));

		for (int slot : new int[] { 48, 49, 50 }) {
			view.addButton(new GuiButton(slot, back) {
				@Override
				public void onClick(Player p, GuiView v, InventoryClickEvent e) {
					p.closeInventory();
				}
			});
		}
	}

	private void setupEditingControls(GuiView view, Recipe recipe, RecipeLayout layout, Player p) {

		BrewingRecipeData brewing = recipe instanceof BrewingRecipeData ? (BrewingRecipeData) recipe : null;
		CookingRecipeData cooking = recipe instanceof CookingRecipeData ? (CookingRecipeData) recipe : null;
		WorkstationRecipeData workstation = recipe instanceof WorkstationRecipeData ? (WorkstationRecipeData) recipe
				: null;
		CraftingRecipeData crafting = recipe instanceof CraftingRecipeData ? (CraftingRecipeData) recipe : null;

		/*
		 * Identifier (slot 9)
		 */
		view.addButton(new GuiStringButton(9, "Identifier", RecipeItemFactory.button(XMaterial.PAPER,
				getValue("Recipe.Identifier", "&fIdentifier"), safeString(recipe.getKey()))) {

			@Override
			public void onStringChange(Player player, String newValue) {
				updateSingleLineLore(this, newValue);
			}
		});

		/*
		 * Lore Editor (slot 44)
		 */
		ItemStack resultItem = recipe.getResult();
		List<String> lore = (resultItem != null && resultItem.hasItemMeta() && resultItem.getItemMeta().hasLore())
				? resultItem.getItemMeta().getLore()
				: new ArrayList<>();

		view.addButton(new GuiLoreButton(44,
				RecipeItemFactory.button(XMaterial.BOOK, getValue("Recipe.Lore", "&fLore"), lore)) {

			@Override
			public void onLoreChange(Player player, List<String> newLore) {

				// Update icon
				ItemStack icon = getIcon();
				ItemMeta meta = icon.getItemMeta();
				meta.setLore(newLore);
				icon.setItemMeta(meta);
				setIcon(icon);

				// Update result slot if exists
				ItemStack result = view.getInventory().getItem(layout.getResultSlot());
				if (result != null) {
					ItemMeta m = result.getItemMeta();
					m.setLore(newLore);
					result.setItemMeta(m);
				}
			}
		});

		/*
		 * Effects (slot 7)
		 */
		view.addButton(new GuiStringButton(8, "Effects",
				RecipeItemFactory.button(XMaterial.GHAST_TEAR, getValue("Recipe.Effects", "&fEffects"),
						recipe.getEffects() != null ? recipe.getEffects() : Arrays.asList("None"))) {

			@Override
			public void onStringChange(Player player, String newValue) {
				updateSingleLineLore(this, newValue);
			}
		});

		/*
		 * Permission (slot 26)
		 */
		view.addButton(new GuiStringButton(26, "Permission", RecipeItemFactory.button(XMaterial.ENDER_PEARL,
				getValue("Recipe.Permission", "&fPermission"), safeString(recipe.getPerm()))) {

			@Override
			public void onStringChange(Player p2, String nv) {
				updateSingleLineLore(this, nv);
			}
		});

		/*
		 * Name (slot 0)
		 */
		view.addButton(new GuiStringButton(0, "Name", RecipeItemFactory.button(XMaterial.WRITABLE_BOOK,
				getValue("Recipe.Recipe-Name", "&fRecipe Name"), safeString(recipe.getName()))) {

			@Override
			public void onStringChange(Player p2, String nv) {
				updateSingleLineLore(this, nv);
			}
		});

		/*
		 * Group (slot 51)
		 */
		if (recipe.getType() == RecipeType.STONECUTTER || crafting != null) {
			String group = crafting != null ? safeString(crafting.getGroup())
					: safeString(workstation != null ? workstation.getGroup() : "");

			view.addButton(new GuiStringButton(51, "Group",
					RecipeItemFactory.button(XMaterial.OAK_SIGN, getValue("Recipe.Group", "&fGroup"), group)) {

				@Override
				public void onStringChange(Player p2, String nv) {
					updateSingleLineLore(this, nv);
				}
			});
		}

		/*
		 * Amount (slot 35)
		 */
		int amount = (resultItem != null ? resultItem.getAmount() : 1);

		view.addButton(new GuiStringButton(35, "Amount", RecipeItemFactory.button(XMaterial.FEATHER,
				getValue("Recipe.Amount", "&fAmount"), String.valueOf(amount))) {

			@Override
			public void onStringChange(Player p2, String nv) {
				try {
					int amt = Integer.parseInt(nv);

					ItemStack icon = getIcon();
					ItemMeta meta = icon.getItemMeta();
					meta.setLore(Arrays.asList(String.valueOf(amt)));
					icon.setItemMeta(meta);
					setIcon(icon);

					ItemStack result = view.getInventory().getItem(layout.getResultSlot());
					if (result != null)
						result.setAmount(amt);

				} catch (Exception ex) {
					p2.sendMessage(ChatColor.RED + "Amount must be a number!");
				}
			}
		});

		/*
		 * Placeable toggle (slot 8)
		 */
		view.addButton(new GuiToggleButton(17, recipe.isPlaceable(), "Placeable",
				RecipeItemFactory.button(XMaterial.DIRT, getValue("Recipe.Placeable", "&fPlaceable"))) {
			@Override
			public void onToggle(Player p2, boolean val) {
			}
		});

		/*
		 * Shapeless toggle (slot 52)
		 */
		if (recipe.getType() == RecipeType.SHAPED || recipe.getType() == RecipeType.SHAPELESS) {

			boolean isShapeless = recipe.getType() == RecipeType.SHAPELESS;

			view.addButton(new GuiToggleButton(52, isShapeless, "Shapeless",
					RecipeItemFactory.button(XMaterial.CRAFTING_TABLE, getValue("Recipe.Shapeless", "&fShapeless"))) {

				@Override
				public void onToggle(Player p2, boolean val) {
				}
			});
		}

		/*
         * Smithing toggle (slot 52)
         */
        if (recipe instanceof SmithingRecipeData) {
            SmithingRecipeData smithing = (SmithingRecipeData) recipe;
            boolean isTrim = smithing.isTrim();

            view.addButton(new GuiToggleButton(52, isTrim, "Trim",
                    RecipeItemFactory.button(XMaterial.SMITHING_TABLE, getValue("Smithing.Trim","&fTrim"))) {
                
                @Override
                public void onToggle(Player p2, boolean val) {
                    
                }
            });
        }

		/*
		 * Exact Choice toggle (slot 18)
		 */
		view.addButton(new GuiToggleButton(18, recipe.isExactChoice(), "Exact-Choice",
				RecipeItemFactory.button(XMaterial.LEVER, getValue("Recipe.Exact-Choice", "&fExact-Choice"))) {

			@Override
			public void onToggle(Player p2, boolean val) {
			}
		});

		/*
		 * Custom Tagged toggle (slot 27)
		 */
		view.addButton(new GuiToggleButton(27, recipe.isCustomTagged(), "Custom-Tagged",
				RecipeItemFactory.button(XMaterial.BOOKSHELF, getValue("Recipe.Custom-Tagged", "&fCustom-Tagged"))) {

			@Override
			public void onToggle(Player p2, boolean val) {
			}
		});

		/*
		 * Uses ID toggle (slot 7)
		 */
		view.addButton(new GuiToggleButton(7, recipe.usesID(), "Uses-ID",
				RecipeItemFactory.button(XMaterial.WHEAT, getValue("Recipe.Uses-ID", "&fUses-ID"))) {

			@Override
			public void onToggle(Player p2, boolean val) {
			}
		});

		/*
		 * Enabled toggle (slot 53)
		 */
		view.addButton(new GuiToggleButton(53, recipe.isActive(), "Enabled",
				RecipeItemFactory.button(recipe.isActive() ? XMaterial.SLIME_BALL : XMaterial.SNOWBALL,
						recipe.isActive() ? getValue("Buttons.Toggle-True", "&atrue")
								: getValue("Buttons.Toggle-False", "&cfalse"))) {

			@Override
			public void onToggle(Player p2, boolean val) {
			}
		});

		/*
		 * Cook Time (slot 14)
		 */
		if (cooking != null) {
			view.addButton(new GuiStringButton(43, "Cook-Time", RecipeItemFactory.button(XMaterial.CLOCK,
					getValue("Cooking.Cook-Time", "&fCook Time"), String.valueOf(cooking.getCookTime()))) {

				@Override
				public void onStringChange(Player player, String nv) {
					applyIntLore(this, nv, "Cook Time must be a number!");
				}
			});
		}

		/*
		 * Experience (slot 52)
		 */
		if (cooking != null || recipe.getType() == RecipeType.GRINDSTONE) {
			float experience = cooking == null ? workstation.getExperience() : cooking.getExperience();

			view.addButton(new GuiStringButton(52, "Experience", RecipeItemFactory.button(XMaterial.EXPERIENCE_BOTTLE,
					getValue("Workstation.Experience", "&fExperience"), String.valueOf(experience))) {

				@Override
				public void onStringChange(Player player, String nv) {
					try {
						double d = Double.parseDouble(nv);
						ItemStack i = getIcon();
						ItemMeta m = i.getItemMeta();
						m.setLore(Arrays.asList(ChatColor.GRAY + String.valueOf(d)));
						i.setItemMeta(m);
						setIcon(i);
					} catch (Exception ex) {
						player.sendMessage(ChatColor.RED + "Experience must be a decimal number!");
					}
				}
			});
		}

		/*
		 * Fuel Set (slot 16)
		 */
		if (recipe.getType() == RecipeType.BREWING_STAND) {
			view.addButton(new GuiStringButton(43, "Fuel-Set",
					RecipeItemFactory.button(XMaterial.COAL, getValue("Brewing.Fuel-Set", "&fFuel Set"),
							String.valueOf(brewing != null ? brewing.getBrewFuelSet() : 0))) {

				@Override
				public void onStringChange(Player player, String nv) {
					applyIntLore(this, nv, "Fuel Set must be a number!");
				}
			});
		}

		/*
		 * Repair Cost (slot 23)
		 */
		if (recipe.getType() == RecipeType.ANVIL || recipe.getType() == RecipeType.GRINDSTONE)
			view.addButton(new GuiStringButton(51, "Repair-Cost",
					RecipeItemFactory.button(XMaterial.ANVIL, getValue("Workstation.Repair-Cost", "&fRepair Cost"),
							String.valueOf(workstation != null ? workstation.getRepairCost() : 0))) {

				@Override
				public void onStringChange(Player player, String nv) {
					applyIntLore(this, nv, "Repair Cost must be a number!");
				}
			});

		/*
		 * Fuel Charge (slot 25)
		 */
		if (recipe.getType() == RecipeType.BREWING_STAND) {
			view.addButton(new GuiStringButton(52, "Fuel-Charge",
					RecipeItemFactory.button(XMaterial.FIRE_CHARGE, getValue("Brewing.Fuel-Charge", "&fFuel Charge"),
							String.valueOf(brewing != null ? brewing.getBrewFuelCharge() : 0))) {

				@Override
				public void onStringChange(Player player, String nv) {
					applyIntLore(this, nv, "Fuel Charge must be a number!");
				}
			});

			/*
			 * Required Bottles (slot 10)
			 */
			view.addButton(new GuiToggleButton(15, brewing != null && brewing.requiresBottles(), "Required-Items",
					RecipeItemFactory.button(XMaterial.BREWING_STAND,
							getValue("Brewing.Required-Items", "&fRequired Items"))) {

				@Override
				public void onToggle(Player p2, boolean val) {
				}
			});
		}

		/*
		 * Delete / Cancel / Save buttons (45,48,49,50)
		 */
		view.addButton(new GuiButton(45,
				RecipeItemFactory.button(XMaterial.BARRIER, getValue("Buttons.Delete", "&cDelete Recipe"))) {
			@Override
			public void onClick(Player p2, GuiView v, InventoryClickEvent e) {
			}
		});

		view.addButton(new GuiButton(48,
				RecipeItemFactory.button(XMaterial.RED_STAINED_GLASS_PANE, getValue("Buttons.Cancel", "&cCancel"))) {
			@Override
			public void onClick(Player p2, GuiView v, InventoryClickEvent e) {
			}
		});

		view.addButton(new GuiButton(49, RecipeItemFactory.button(XMaterial.WHITE_STAINED_GLASS_PANE,
				getValue("Buttons.Main-Menu", "&fMain Menu"))) {
			@Override
			public void onClick(Player p2, GuiView v, InventoryClickEvent e) {
			}
		});

		view.addButton(new GuiButton(50,
				RecipeItemFactory.button(XMaterial.GREEN_STAINED_GLASS_PANE, getValue("Buttons.Update", "&aUpdate"))) {
			@Override
			public void onClick(Player p2, GuiView v, InventoryClickEvent e) {
			}
		});
	}

	private void updateSingleLineLore(GuiButton btn, String text) {
		ItemStack icon = btn.getIcon();
		ItemMeta meta = icon.getItemMeta();
		meta.setLore(Arrays.asList(ChatColor.GRAY + text));
		icon.setItemMeta(meta);
		btn.setIcon(icon);
	}

	private void applyIntLore(GuiButton btn, String newValue, String errorMsg) {
		try {
			int val = Integer.parseInt(newValue.replace(" ", ""));
			ItemStack icon = btn.getIcon();
			ItemMeta meta = icon.getItemMeta();
			meta.setLore(Arrays.asList(ChatColor.GRAY + String.valueOf(val)));
			icon.setItemMeta(meta);
			btn.setIcon(icon);
		} catch (Exception ex) {
			// caller handles sending error message
		}
	}

	private String getValue(String path, String def) {
		String val = getConfig().getString("gui." + path);
		return (val == null || val.isEmpty()) ? def : val;
	}

	private String getParsedValue(String msg, String def) {
		return ChatColor.translateAlternateColorCodes('&', getValue(msg, def));
	}

	private String safeString(String s) {
		return (s == null || s.isEmpty()) ? getValue("Empty", "None") : s;
	}

	private FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
}