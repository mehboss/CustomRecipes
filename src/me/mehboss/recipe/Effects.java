package me.mehboss.recipe;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Effects implements Listener {

	private Main plugin;

	public Effects(Main plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void oneffect(EntityDamageByEntityEvent p) {
		Plugin config = Bukkit.getPluginManager().getPlugin("CustomRecipes");

		if (p.getDamager() instanceof Player) {

			Player pl = (Player) p.getDamager();
			ItemStack item = pl.getItemInHand();

			if (pl.getItemInHand() == null || item.getItemMeta() == null
					|| item.getItemMeta().getDisplayName() == null) {
				return;
			}

			String displayname = item.getItemMeta().getDisplayName().replaceAll(" ", "").replaceAll("&", "");

			if (p.getCause() == DamageCause.ENTITY_ATTACK && (!(p.getEntity().isDead())) && item.getItemMeta().hasLore()
					&& plugin.isItemforEffect.contains(displayname)
					&& plugin.getConfig().getStringList("Items." + displayname + ".Effects") != null) {

				for (String e : config.getConfig()
						.getStringList("Items." + displayname.replaceAll("(§([a-fk-o0-9]))", "") + ".Effects")) {

					if (e == null) {
						return;
					}

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
