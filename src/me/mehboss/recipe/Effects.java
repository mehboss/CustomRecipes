package me.mehboss.recipe;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Effects implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler
	public void oneffect(EntityDamageByEntityEvent p) {

		if (p.getDamager() instanceof Player) {

			Player pl = (Player) p.getDamager();
			ItemStack item = pl.getItemInHand();
			String displayname = null;

			if (pl.getItemInHand() == null || item.getItemMeta() == null
					|| item.getItemMeta().getDisplayName() == null) {
				return;
			}

			if (Main.getInstance().giveRecipe.containsValue(pl.getItemInHand()))
				displayname = Main.getInstance().configName.get(pl.getItemInHand());

			if (displayname == null)
				return;

			if (p.getCause() == DamageCause.ENTITY_ATTACK && (!(p.getEntity().isDead()))
					&& Main.getInstance().getConfig().getStringList("Items." + displayname + ".Effects") != null) {

				for (String e : Main.getInstance().getConfig().getStringList("Items." + displayname + ".Effects")) {

					if (e == null)
						return;

					String[] effectSplit = e.split(":");

					String eff = effectSplit[0].toUpperCase();
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
