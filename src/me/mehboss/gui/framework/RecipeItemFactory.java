package me.mehboss.gui.framework;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

/**
 * A helper class for clean, consistent ItemStack creation.
 */
public class RecipeItemFactory {

	public static ItemStack button(ItemStack itemStack, String name, String... lore) {
		ItemStack item = new ItemStack(itemStack);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(color(name));

		if (lore != null && lore.length > 0) {
			meta.setLore(colorList(Arrays.asList(lore)));
		}

		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack button(XMaterial mat, String name, String... lore) {
		return button(mat.parseItem(), name, lore);
	}

	public static ItemStack button(ItemStack mat, String name, List<String> lore) {
		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(color(name));
		if (lore != null) {
			meta.setLore(colorList(lore));
		}

		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack button(XMaterial mat, String name, List<String> lore) {
		return button(mat.parseItem(), name, lore);
	}

	private static String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	private static List<String> colorList(List<String> list) {
		return list.stream().map(RecipeItemFactory::color).collect(Collectors.toList());
	}
}