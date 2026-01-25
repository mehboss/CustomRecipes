package me.mehboss.utils.libs;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.cryptomorin.xseries.XMaterial;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import me.mehboss.recipe.Main;
import net.kyori.adventure.text.Component;

/**
 * Utility class providing cross-version compatibility helpers that rely on
 * reflection to handle differences between Minecraft / Bukkit versions.
 * <p>
 * This includes:
 * <ul>
 * <li>Accessing InventoryView methods safely across versions</li>
 * <li>Retrieving players, titles, and top inventories without relying on
 * version-specific classes</li>
 * <li>Reflection helpers for invoking private methods and fields</li>
 * <li>Base64 decoding for skin texture analysis</li>
 * <li>Item name getters/setters supporting 1.20.5+ name changes</li>
 * </ul>
 */
public class CompatibilityUtil {

	/**
	 * Retrieves the InventoryView from an {@link InventoryEvent} using reflection.
	 * <p>
	 * Different server versions may change how InventoryView is exposed, so this
	 * reflective access ensures compatibility.
	 *
	 * @param event The inventory event.
	 * @return The InventoryView object (returned as {@code Object} for
	 *         compatibility).
	 */
	public static Object getInventoryView(InventoryEvent event) {
		try {
			Method getViewMethod = InventoryEvent.class.getMethod("getView");
			return getViewMethod.invoke(event); // returns Object (not InventoryView!)
		} catch (Exception e) {
			throw new RuntimeException("Failed to get InventoryView reflectively", e);
		}
	}

	/**
	 * Extracts the {@link Player} from an InventoryView instance using reflection.
	 *
	 * @param view The InventoryView object.
	 * @return The player who opened the inventory, or {@code null} if unavailable.
	 */
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

	/**
	 * Retrieves the top inventory from an {@link InventoryEvent} using reflection.
	 *
	 * @param event The inventory event.
	 * @return The top inventory (e.g. chest, crafting grid).
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

	/**
	 * Retrieves an inventory's title reflectively.
	 * <p>
	 * Some Bukkit versions moved {@code getTitle()} into subclasses, so this method
	 * uses superclass traversal for compatibility.
	 *
	 * @param event The inventory event.
	 * @return Title of the inventory window.
	 */
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

	/**
	 * Extracts the skin texture URL from a Base64-encoded JSON texture payload.
	 *
	 * @param base64Texture A Base64-encoded Mojang skin texture string.
	 * @return The texture URL inside the JSON.
	 */
	public static String extractUrlFromBase64(String base64Texture) {
		byte[] decodedBytes = Base64.getDecoder().decode(base64Texture);
		String json = new String(decodedBytes);

		JsonObject obj = Json.parse(json).asObject();

		return obj.get("textures").asObject().get("SKIN").asObject().getString("url", null);
	}

	public static boolean supportsItemName() {
		return Main.getInstance().serverVersionAtLeast(1, 20, 5);
	}

	public static boolean supportsCustomModelData() {
		return Main.getInstance().serverVersionAtLeast(1, 14);
	}

	public static boolean supportsItemModel() {
		return Main.getInstance().serverVersionAtLeast(1, 21, 4);
	}

	public static boolean supportsMaterialData() {
		return Main.getInstance().serverVersionLessThan(1, 13);
	}
	
	public static boolean isPaperServer() {
		String serverName = Bukkit.getServer().getName();
		if (!serverName.contains("Paper"))
			return false;
		return true;
	}

	/**
	 * Checks if an item has a display name. Supports Bukkit name API changes
	 * introduced in Minecraft 1.20.5.
	 *
	 * @param item ItemMeta to inspect.
	 * @return True if the item has a custom name.
	 */
	public static boolean hasDisplayname(ItemMeta item, boolean hasItemName) {
		if (supportsItemName() && hasItemName)
			return item.hasItemName();

		return item.hasDisplayName();
	}

	/**
	 * Retrieves the display name from an item.
	 *
	 * @param item ItemMeta from which to read the name.
	 * @return The custom display name.
	 */
	public static String getDisplayname(ItemMeta item, boolean hasItemName) {
		if (supportsItemName() && hasItemName)
			return item.getItemName();

		return item.getDisplayName();
	}

	/**
	 * Sets an item's display name using version-aware APIs.
	 * <p>
	 * In 1.20.5+:
	 * <ul>
	 * <li>New naming system applies to all items except potion variants.</li>
	 * </ul>
	 *
	 * @param item The item whose meta should be modified.
	 * @param name The new display name (supports color codes).
	 * @return The updated ItemMeta with the new name applied.
	 */
	public static ItemMeta setDisplayname(ItemStack item, String name, boolean isLegacyNames) {
		XMaterial[] itemTypes = { XMaterial.POTION, XMaterial.LINGERING_POTION, XMaterial.SPLASH_POTION };
		XMaterial itemMaterial = XMaterial.matchXMaterial(item.getType());
		ItemMeta itemM = item.getItemMeta();

		if (!isLegacyNames && Main.getInstance().serverVersionAtLeast(1, 20, 5)
				&& !Arrays.asList(itemTypes).contains(itemMaterial)) {
			itemM.setItemName(ChatColor.translateAlternateColorCodes('&', name));
			return itemM;
		}

		itemM.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		return itemM;
	}
	
	public static void setDisplayName(ItemMeta meta, Component name, String string) {
		try {
			Method method = meta.getClass().getMethod("displayName", Component.class);
			method.invoke(meta, name);
		} catch (Exception ignored) {
			String translated = string;
			meta.setDisplayName(translated);
		}
	}
	
	public static void setItemName(ItemMeta meta, Component name, String string) {
		try {
			Method itemNameMethod = meta.getClass().getMethod("itemName", Component.class);
			itemNameMethod.setAccessible(true);
			itemNameMethod.invoke(meta, name);
		} catch (Exception ignored) {
			String translated = string;
			meta.setItemName(translated);
		}
	}
}