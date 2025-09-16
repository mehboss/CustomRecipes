package me.mehboss.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.json.JSONObject;

public class CompatibilityUtil {
	/**
	* In API versions 1.20.6 and earlier, InventoryView is a class.
	* In versions 1.21 and later, it is an interface.
	* This method uses reflection to get the top Inventory object from the
	* InventoryView associated with an InventoryEvent, to avoid runtime errors.
	* @param event The generic InventoryEvent with an InventoryView to inspect.
	* @return The top Inventory object from the event's InventoryView.
	*/
	
    /**
     * Gets the InventoryView from an InventoryEvent using reflection to avoid
     * class/interface compatibility issues across versions (e.g. 1.20 vs 1.21+).
     *
     * @param event The inventory event
     * @return The InventoryView associated with the event
     */
    public static Object getInventoryView(InventoryEvent event) {
        try {
            Method getViewMethod = InventoryEvent.class.getMethod("getView");
            return getViewMethod.invoke(event); // returns Object (not InventoryView!)
        } catch (Exception e) {
            throw new RuntimeException("Failed to get InventoryView reflectively", e);
        }
    }

    public static Player getPlayerFromView(Object view) {
        try {
            Method getPlayerMethod = view.getClass().getMethod("getPlayer");
            Object result = getPlayerMethod.invoke(view);
            if (result instanceof Player) {
                return (Player) result;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Player from InventoryView reflectively", e);
        }
    }
    
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
    
    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void invokeMethod(Method method, Object instance, Object... args) {
        try {
            method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setFieldValue(Object instance, String fieldName, Object value) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    public static String extractUrlFromBase64(String base64Texture) {
        // Decode the Base64 string back into the original JSON string
        byte[] decodedBytes = Base64.getDecoder().decode(base64Texture);
        String json = new String(decodedBytes);

        // Parse the JSON string to extract the texture URL
        JSONObject jsonObj = new JSONObject(json);
        String textureUrl = jsonObj
                .getJSONObject("textures")
                .getJSONObject("SKIN")
                .getString("url");

        return textureUrl;
    }
}