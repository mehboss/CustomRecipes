package me.mehboss.recipe;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

		if (p.getDamager() instanceof Player) {

			Player pl = (Player) p.getDamager();
			ItemStack item = pl.getItemInHand();
			String configName = null;

			if (pl.getItemInHand() == null)
				return;

			if (Main.getInstance().configName.containsKey(item))
				configName = Main.getInstance().configName.get(item);

			if (configName == null || getConfig(configName) == null)
				return;

			if (p.getCause() == DamageCause.ENTITY_ATTACK && (!(p.getEntity().isDead()))
					&& getConfig(configName).isSet(configName + ".Effects")) {

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

					l.addPotionEffect(effect);
				}
			}
		}
	}
}
