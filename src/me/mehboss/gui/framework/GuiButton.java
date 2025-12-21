package me.mehboss.gui.framework;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mehboss.gui.framework.chat.ChatEditManager;
import me.mehboss.recipe.Main;

/**
 * Base GUI button. Each button occupies a single slot and has its own click
 * behavior.
 */
public abstract class GuiButton {

	protected final int slot;
	protected ItemStack icon;

	public GuiButton(int slot, ItemStack icon) {
		this.slot = slot;
		this.icon = icon;
	}

	public int getSlot() {
		return slot;
	}

	public ItemStack getIcon() {
		return icon;
	}

	public void setIcon(ItemStack icon) {
		this.icon = icon;
	}

	/**
	 * Called when this button is clicked.
	 */
	public abstract void onClick(Player player, GuiView view, InventoryClickEvent e);

	/**
	 * A lore editing button that triggers multiline edit mode.
	 */
	public static abstract class GuiLoreButton extends GuiButton {

		public GuiLoreButton(int slot, ItemStack icon) {
			super(slot, icon);
		}

		@Override
		public void onClick(Player player, GuiView view, InventoryClickEvent e) {
			ChatEditManager.get().beginLoreEdit(player, view, this);
			player.closeInventory();
		}

		/**
		 * Fired after the lore editing session has been completed.
		 */
		public abstract void onLoreChange(Player player, List<String> newLore);
	}

	/**
	 * Button that triggers chat-based string editing.
	 */
	public static abstract class GuiStringButton extends GuiButton {

		private final String fieldName;

		public GuiStringButton(int slot, String fieldName, ItemStack icon) {
			super(slot, icon);
			this.fieldName = fieldName;
		}

		public String getFieldName() {
			return fieldName;
		}

		@Override
		public void onClick(Player player, GuiView view, InventoryClickEvent e) {
			ChatEditManager.get().beginStringEdit(player, view, this);
			player.closeInventory();
		}

		/**
		 * Fired after a new value is submitted.
		 */
		public abstract void onStringChange(Player player, String newValue);
	}

	/**
	 * A clickable boolean toggle button.
	 */
	public static abstract class GuiToggleButton extends GuiButton {

		protected boolean value;
		protected String label;

		public GuiToggleButton(int slot, boolean value, String label, ItemStack icon) {
			super(slot, icon);
			this.value = value;
			this.label = label;
			updateIcon();
		}

		public boolean getValue() {
			return value;
		}

		public String getFieldName() {
			return label;
		}

		public void toggle() {
			value = !value;
			updateIcon();
		}

		/**
		 * Updates the display name to reflect the boolean value.
		 */
		public void updateIcon() {
			if (icon == null)
				return;

			ItemMeta meta = icon.getItemMeta();
			meta.setDisplayName(getParsedValue("Recipe." + label, label) + ": "
					+ (value ? getParsedValue("Buttons.Toggle-True", "&atrue")
							: getParsedValue("Buttons.Toggle-False", "&cfalse")));
			icon.setItemMeta(meta);
		}

		@Override
		public void onClick(Player player, GuiView view, InventoryClickEvent e) {
			toggle();
			view.updateButton(this);
			onToggle(player, value);
		}

		/**
		 * Fired whenever the value is toggled.
		 */
		public abstract void onToggle(Player player, boolean newValue);
	}

	private static String getValue(String path, String def) {
		String val = getConfig().getString("gui." + path);
		return (val == null || val.isEmpty()) ? def : val;
	}

	private static String getParsedValue(String msg, String def) {
		return ChatColor.translateAlternateColorCodes('&', getValue(msg, def));
	}

	private static FileConfiguration getConfig() {
		return Main.getInstance().getConfig();
	}
}
