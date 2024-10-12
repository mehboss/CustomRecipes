package me.mehboss.recipe;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.bananapuncher714.nbteditor.NBTEditor;

public class EffectsManager implements Listener {

	FileConfiguration getConfig(String recipeName) {
		File dataFolder = Main.getInstance().getDataFolder();
		File recipesFolder = new File(dataFolder, "recipes");
		File recipeFile = new File(recipesFolder, recipeName + ".yml");

		if (!recipeFile.exists())
			return null;

		return YamlConfiguration.loadConfiguration(recipeFile);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void oneffect(EntityDamageByEntityEvent p) {

		if (p.getDamager() instanceof Player || (versionHasTrident() && p.getDamager() instanceof Trident)) {

			Player pl = null;

			if (p.getDamager() instanceof Player)
				pl = (Player) p.getDamager();

			if (versionHasTrident() && p.getDamager() instanceof Trident) {
				Projectile trident = (Projectile) p.getDamager();
				pl = trident.getShooter() instanceof Player ? (Player) trident.getShooter() : null;
			}

			if (pl == null || pl.getItemInHand() == null)
				return;

			ItemStack item = pl.getItemInHand();
			ItemStack foundItem = null;
			String identifier = null;
			String configName = null;

			if (NBTEditor.contains(item, "CUSTOM_ITEM_IDENTIFIER"))
				identifier = NBTEditor.getString(item, "CUSTOM_ITEM_IDENTIFIER");

			if (identifier != null && identifier().containsKey(identifier))
				foundItem = identifier().get(identifier);

			if (foundItem != null && configName().containsKey(foundItem))
				configName = configName().get(foundItem);

			if (configName == null || foundItem == null || identifier == null || getConfig(configName) == null)
				return;

			if ((p.getCause() == DamageCause.PROJECTILE || p.getCause() == DamageCause.ENTITY_ATTACK)
					&& (!(p.getEntity().isDead())) && getConfig(configName).isSet(configName + ".Effects")) {

				for (String e : getConfig(configName).getStringList(configName + ".Effects")) {

					String[] effectSplit = e.split(":");
					String eff = effectSplit[0].toUpperCase();

					if (e == null || PotionEffectType.getByName(eff) == null)
						continue;

					String dur = effectSplit[1];
					String amp = effectSplit[2];

					int duration = Integer.parseInt(dur) * 20;
					int amplifier = Integer.parseInt(amp);

					PotionEffect effect = new PotionEffect(PotionEffectType.getByName(eff), duration, amplifier);
					LivingEntity l = (LivingEntity) p.getEntity();

					if (versionHasBlocking() && l instanceof Player && ((Player) l).isBlocking())
						return;

					l.addPotionEffect(effect);
				}
			}
		}
	}

	boolean versionHasBlocking() {
		return Main.getInstance().serverVersionAtLeast(1, 10);
	}

	public boolean versionHasTrident() {
		return Main.getInstance().serverVersionAtLeast(1, 13);
	}

	HashMap<String, ItemStack> identifier() {
		return Main.getInstance().identifier;
	}

	HashMap<ItemStack, String> configName() {
		return Main.getInstance().configName;
	}
}
