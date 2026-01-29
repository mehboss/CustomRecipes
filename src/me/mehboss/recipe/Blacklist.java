package me.mehboss.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.cryptomorin.xseries.XMaterial;

import me.mehboss.utils.RecipeUtil;

public class Blacklist {
	private final Set<NamespacedKey> blacklistedKeys = new HashSet<>();
	private final Map<String, Set<ItemStack>> blacklistedLegacyResults = new HashMap<>();
	private boolean disableAllVanilla = false;

	public boolean hasDisabledVanilla() {
		return disableAllVanilla;
	}

	public Map<String, Set<ItemStack>> getLegacyBlacklisted() {
		return blacklistedLegacyResults;
	}

	public Set<NamespacedKey> getModernBlacklisted() {
		return blacklistedKeys;
	}

	public void collectBlacklistedRecipes() {
		blacklistedKeys.clear();
		blacklistedLegacyResults.clear();
		disableAllVanilla = false;

		if (instance().customConfig == null)
			return;

		boolean modern = instance().serverVersionAtLeast(1, 16);

		if (instance().customConfig.isConfigurationSection("override-recipes")) {
			ConfigurationSection sec = instance().customConfig.getConfigurationSection("override-recipes");

			if (modern) {
				removeRecipesModern(sec);
			} else {
				removeRecipesLegacy(sec);
			}
		}

		disableAllVanilla = instance().customConfig.getBoolean("disable-all-vanilla", false);
	}

	// --- Modern recipe removal (1.16+) ---
	private void removeRecipesModern(ConfigurationSection sec) {
		logDebug("[Blacklisted] Collecting modern blacklisted recipes");

		for (String typeKey : sec.getKeys(false)) {
			List<String> targets = sec.getStringList(typeKey);
			if (targets == null || targets.isEmpty())
				continue;

			for (String entry : targets) {
				if (entry == null || entry.isEmpty())
					continue;

				NamespacedKey nk = entry.contains(":") ? NamespacedKey.fromString(entry.toLowerCase())
						: NamespacedKey.minecraft(entry.toLowerCase());

				if (nk != null) {
					blacklistedKeys.add(nk);
					logDebug("[Blacklisted] Added modern key " + nk + " for type " + typeKey);
				}
			}
		}
	}

	// --- Legacy recipe removal (1.8–1.15) ---
	private void removeRecipesLegacy(ConfigurationSection sec) {
		logDebug("[Blacklisted] Collecting legacy blacklisted recipes");

		for (String typeKey : sec.getKeys(false)) {
			List<String> targets = sec.getStringList(typeKey);
			if (targets == null || targets.isEmpty())
				continue;

			for (String entry : targets) {
				if (entry == null || entry.isEmpty())
					continue;

				XMaterial.matchXMaterial(entry).ifPresent(xm -> {
					ItemStack item = xm.parseItem();
					if (item != null) {
						blacklistedLegacyResults.computeIfAbsent(typeKey.toLowerCase(), k -> new HashSet<>()).add(item);
						logDebug("[Blacklisted] Added legacy item " + item.getType() + " for type " + typeKey);
					}
				});
			}
		}
	}

	void removeCustomRecipes() {
		if (!instance().serverVersionAtLeast(1, 16) || getRecipeUtil().getAllRecipes().isEmpty())
			return;

		for (RecipeUtil.Recipe recipe : getRecipeUtil().getAllRecipes().values()) {
			String key = recipe.getKey();
			org.bukkit.NamespacedKey npk = org.bukkit.NamespacedKey.fromString("customrecipes:" + key.toLowerCase());

			if (npk == null)
				continue;

			Bukkit.removeRecipe(npk);
		}
	}

	public void disableRecipes() {
		if (getBlacklistConfig() == null)
			return;

		getDisabledRecipes().clear();

		if (getBlacklistConfig().isConfigurationSection("vanilla-recipes")) {
			for (String vanilla : getBlacklistConfig().getConfigurationSection("vanilla-recipes").getKeys(false)) {
				getDisabledRecipes().add(vanilla);
			}
		}

		if (getBlacklistConfig().isConfigurationSection("custom-recipes")) {
			for (String custom : getBlacklistConfig().getConfigurationSection("custom-recipes").getKeys(false)) {
				getDisabledRecipes().add(custom);
			}
		}
	}

	// --- Optional recipe classes; null if not present on this server ---
	private static final Class<?> C_BLASTING = classOrNull("org.bukkit.inventory.BlastingRecipe");
	private static final Class<?> C_SMOKING = classOrNull("org.bukkit.inventory.SmokingRecipe");
	private static final Class<?> C_CAMPFIRE = classOrNull("org.bukkit.inventory.CampfireRecipe");
	private static final Class<?> C_STONECUT = classOrNull("org.bukkit.inventory.StonecuttingRecipe");
	private static final Class<?> C_COOKING = classOrNull("org.bukkit.inventory.CookingRecipe");

	private static Class<?> classOrNull(String fqn) {
		try {
			return Class.forName(fqn);
		} catch (Throwable t) {
			return null;
		}
	}

	private boolean isInstance(Object obj, Class<?> cls) {
		return cls != null && cls.isInstance(obj);
	}

	public boolean matchesType(String typeKey, Recipe r) {
		String t = (typeKey == null ? "" : typeKey.toLowerCase(java.util.Locale.ROOT));
		switch (t) {
		case "crafting":
			return (r instanceof ShapedRecipe) || (r instanceof ShapelessRecipe);

		case "furnace":
		case "smelting":
			return (r instanceof FurnaceRecipe) || isInstance(r, C_BLASTING) || isInstance(r, C_SMOKING)
					|| isInstance(r, C_CAMPFIRE) || isInstance(r, C_COOKING); // broad fallback if present

		case "stonecutter":
		case "stonecutting":
			return isInstance(r, C_STONECUT);

		default:
			return false;
		}
	}

	Main instance() {
		return Main.getInstance();
	}

	FileConfiguration getBlacklistConfig() {
		return Main.getInstance().customConfig;
	}

	RecipeUtil getRecipeUtil() {
		return Main.getInstance().recipeUtil;
	}

	ArrayList<String> getDisabledRecipes() {
		return Main.getInstance().disabledrecipe;
	}

	public void logError(String st) {
		Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}

	public void logDebug(String st) {
		if (instance().debug)
			Logger.getLogger("Minecraft").log(Level.WARNING, "[DEBUG][" + Main.getInstance().getName() + "]" + st);
	}
}
