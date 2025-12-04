package me.mehboss.gui.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Represents a GUI view (inventory window) with registered buttons.
 */
public class GuiView {

    private final Inventory inventory;
    private final Map<Integer, GuiButton> buttons = new HashMap<>();
    private boolean editing;

    public GuiView(int size, String title) {
        this.inventory = Bukkit.createInventory(null, size, title);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
    	GuiRegistry.register(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    public void addButton(GuiButton button) {
        buttons.put(button.getSlot(), button);
        inventory.setItem(button.getSlot(), button.getIcon());
    }

    public GuiButton getButton(int slot) {
        return buttons.get(slot);
    }

    /**
     * Replaces the button icon after a toggle or update.
     */
    public void updateButton(GuiButton button) {
        inventory.setItem(button.getSlot(), button.getIcon());
    }

    /**
     * Called by listener when player clicks in GUI.
     */
    public void handleClick(Player player, InventoryClickEvent e) {
        GuiButton button = buttons.get(e.getSlot());
        if (button != null) {
            button.onClick(player, this, e);
        }
    }
    
    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isEditing() {
        return editing;
    }
    
    /**
     * Keeps track of active GUI views.
     * Allows listener to route click events to the correct GUI controller.
     */
    public static class GuiRegistry {

        /** All active GUI views keyed by player UUID */
        private static final Map<UUID, GuiView> ACTIVE = new HashMap<>();

        /** Register a view as currently open for a player */
        public static void register(UUID uuid, GuiView view) {
            ACTIVE.put(uuid, view);
        }

        /** Unregister a view when a player closes */
        public static void unregister(UUID uuid) {
            ACTIVE.remove(uuid);
        }

        /** Get the view a player currently has open */
        public static GuiView get(UUID uuid) {
            return ACTIVE.get(uuid);
        }

        /**
         * Returns an immutable map of all active GUI views.
         * Key = player UUID  
         * Value = GuiView instance
         */
        public static Map<UUID, GuiView> getActiveViews() {
            return Collections.unmodifiableMap(ACTIVE);
        }
    }
}