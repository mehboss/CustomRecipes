package me.mehboss.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;

public class CompatibilityUtil {
	/**
	* In API versions 1.20.6 and earlier, InventoryView is a class.
	* In versions 1.21 and later, it is an interface.
	* This method uses reflection to get the top Inventory object from the
	* InventoryView associated with an InventoryEvent, to avoid runtime errors.
	* @param event The generic InventoryEvent with an InventoryView to inspect.
	* @return The top Inventory object from the event's InventoryView.
	*/
	public static Inventory getTopInventory(InventoryEvent event) {
	    try {
	        Object view = event.getView();
	        Method getTopInventory = view.getClass().getMethod("getTopInventory");
	        getTopInventory.setAccessible(true);
	        return (Inventory) getTopInventory.invoke(view);
	    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
	        throw new RuntimeException(e);
	    }
	}
	
    public static String getTitle(InventoryEvent event) {
        try {
            Object view = event.getView(); // InventoryView
            Class<?> inventoryViewClass = view.getClass().getSuperclass(); // Go one level up to avoid anonymous class
            Method getTitle = inventoryViewClass.getMethod("getTitle");
            return (String) getTitle.invoke(view);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get inventory title", e);
        }
    }
}