package me.mehboss.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.mehboss.recipe.Main;

public final class RecipeConditions {

	public static final class Block {
		// World gating
		public Set<String> allowWorlds = Collections.emptySet(); // lowercase
		public Set<String> denyWorlds = Collections.emptySet(); // lowercase

		// Time gating: one or more ranges like "13000-23000"
		public static final class TimeRange {
			public final int min; // inclusive
			public final int max; // inclusive

			public TimeRange(int min, int max) {
				this.min = min;
				this.max = max;
			}

			public boolean contains(int t) {
				// support wrap-around (e.g., 23000-2000)
				return (min <= max) ? (t >= min && t <= max) : (t >= min || t <= max);
			}

			public String asString() {
				return min + "-" + max;
			}
		}

		public List<TimeRange> timeRanges = Collections.emptyList();

		// Weather gating (allowed set). Valid values: "clear", "rain", "thunder"
		public Set<String> weathers = Collections.emptySet();

		// Biome gating (Biome enum names in lowercase, e.g. "plains", "nether_wastes")
		public Set<String> allowBiomes = Collections.emptySet();
		public Set<String> denyBiomes = Collections.emptySet();

		// Player advancement requirement(s). Any match passes. Ignored if player ==
		// null.
		public Set<String> advancements = Collections.emptySet();

		// Moon phases (0–7): 0 Full, 1 Waning Gibbous, 2 Last Quarter, 3 Waning
		// Crescent,
		// 4 New, 5 Waxing Crescent, 6 First Quarter, 7 Waxing Gibbous
		public Set<Integer> moonPhases = Collections.emptySet();

		public boolean isEmpty() {
			return (allowWorlds == null || allowWorlds.isEmpty()) && (denyWorlds == null || denyWorlds.isEmpty())
					&& (allowBiomes == null || allowBiomes.isEmpty()) && (denyBiomes == null || denyBiomes.isEmpty())
					&& (timeRanges == null || timeRanges.isEmpty()) && (weathers == null || weathers.isEmpty())
					&& (advancements == null || advancements.isEmpty()) && (moonPhases == null || moonPhases.isEmpty());
		}

		/** Fast-path test (no messages collected). */
		public boolean test(Location loc, @Nullable Player player) {
			return test(loc, player, null);
		}

		/**
		 * Tests this block against a location and (optionally) a player. If outReasons
		 * is non-null, collects ALL failure messages and still returns false. If
		 * outReasons is null, short-circuits on first failure for performance.
		 */
		public boolean test(Location loc, @Nullable Player player, @Nullable List<String> outReasons) {
			World world = loc.getWorld();
			long worldTime = world.getFullTime();
			boolean pass = true;

			// World allow/deny
			String wn = world.getName().toLowerCase();
			if (allowWorlds != null && !allowWorlds.isEmpty() && !allowWorlds.contains(wn)) {
				if (outReasons != null) {
					outReasons.add("Must be in world: " + joinSet(allowWorlds));
					pass = false;
				} else
					return false;
			}
			if (denyWorlds != null && !denyWorlds.isEmpty() && denyWorlds.contains(wn)) {
				if (outReasons != null) {
					outReasons.add("Not allowed in world: " + wn);
					pass = false;
				} else
					return false;
			}

			// Biome allow/deny
			String biome = world.getBiome(loc.getBlockX(), loc.getBlockZ()).name().toLowerCase();
			if (Main.getInstance().serverVersionAtLeast(1, 15))
				biome = world.getBiome(loc).name().toLowerCase();

			if (allowBiomes != null && !allowBiomes.isEmpty() && !allowBiomes.contains(biome)) {
				if (outReasons != null) {
					outReasons.add("Biome must be one of: " + joinSet(allowBiomes));
					pass = false;
				} else
					return false;
			}
			if (denyBiomes != null && !denyBiomes.isEmpty() && denyBiomes.contains(biome)) {
				if (outReasons != null) {
					outReasons.add("Biome not allowed: " + biome);
					pass = false;
				} else
					return false;
			}

			// Time: pass if ANY range contains current time
			if (timeRanges != null && !timeRanges.isEmpty()) {
				int t = (int) (worldTime % 24000);
				boolean ok = false;
				for (int i = 0; i < timeRanges.size(); i++) {
					TimeRange r = timeRanges.get(i);
					if (r != null && r.contains(t)) {
						ok = true;
						break;
					}
				}
				if (!ok) {
					if (outReasons != null) {
						outReasons.add("Time must be in: " + joinTimeRanges(timeRanges));
						pass = false;
					} else
						return false;
				}
			}

			// Weather: pass if current weather is in allowed set
			if (weathers != null && !weathers.isEmpty()) {
				boolean rain = world.hasStorm();
				boolean thun = world.isThundering();
				String now = thun ? "thunder" : (rain ? "rain" : "clear");
				if (!weathers.contains(now)) {
					if (outReasons != null) {
						outReasons.add("Weather must be: " + joinSet(weathers));
						pass = false;
					} else
						return false;
				}
			}

			// Moon phase
			if (moonPhases != null && !moonPhases.isEmpty()) {
				int phase = getMoonPhase(world); // 0–7
				if (!moonPhases.contains(Integer.valueOf(phase))) {
					if (outReasons != null) {
						outReasons.add("Moon phase must be one of: " + moonPhases.toString());
						pass = false;
					} else
						return false;
				}
			}

			// Advancements: pass if ANY advancement in the set is completed (ignored if
			// player == null)
			if (Main.getInstance().serverVersionAtLeast(1, 12))
				if (advancements != null && !advancements.isEmpty() && player != null) {
					boolean ok = false;
					for (String adv : advancements) {
						if (adv == null || adv.isEmpty())
							continue;
						NamespacedKey key = adv.indexOf(':') >= 0 ? NamespacedKey.fromString(adv)
								: NamespacedKey.minecraft(adv);
						if (key == null)
							continue;
						Advancement a = Bukkit.getAdvancement(key);
						if (a == null)
							continue;
						AdvancementProgress prog = player.getAdvancementProgress(a);
						if (prog.isDone()) {
							ok = true;
							break;
						}
					}
					if (!ok) {
						if (outReasons != null) {
							outReasons.add("Requires advancement: " + joinSet(advancements));
							pass = false;
						} else
							return false;
					}
				}

			return pass;
		}

		private static String joinSet(Set<String> set) {
			if (set == null || set.isEmpty())
				return "";
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String s : set) {
				if (!first)
					sb.append(", ");
				sb.append(s);
				first = false;
			}
			return sb.toString();
		}

		private static String joinTimeRanges(List<TimeRange> ranges) {
			if (ranges == null || ranges.isEmpty())
				return "";
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (int i = 0; i < ranges.size(); i++) {
				TimeRange r = ranges.get(i);
				if (r == null)
					continue;
				if (!first)
					sb.append(", ");
				sb.append(r.asString());
				first = false;
			}
			return sb.toString();
		}
	}

	public static int getMoonPhase(World world) {
		long day = world.getFullTime() / 24000L; // Get the in-game day
		return (int) (day % 8); // Calculate the moon phase (0-7)
	}

	/** The full condition set for a recipe. */
	public static final class ConditionSet {
		/** “conditions” or “conditions_all” block (AND). */
		public Block all;
		/**
		 * “conditions_any” is a list of OR blocks. If non-empty, at least one of these
		 * blocks must pass.
		 */
		public List<Block> any = Collections.emptyList();

		public boolean isEmpty() {
			boolean anyEmpty = (any == null || any.isEmpty());
			return (all == null || all.isEmpty()) && anyEmpty;
		}

		/** Fast-path test (no messages). */
		public boolean test(Location loc, @Nullable Player player) {
			return test(loc, player, null);
		}

		/**
		 * Overall logic: (all == null || all passes) AND (any is empty OR at least one
		 * any-block passes)
		 *
		 * If outReasons is non-null, messages are added only for failing parts.
		 */
		public boolean test(Location loc, @Nullable Player player, @Nullable List<String> outReasons) {
			boolean allPass = true;
			boolean anyPass = true; // default true if there's no ANY block

			// ---- ALL block ----
			if (all != null && !all.isEmpty()) {
				if (outReasons == null) {
					if (!all.test(loc, player, null))
						return false; // fast path
				} else {
					List<String> tmp = new ArrayList<String>();
					boolean pass = all.test(loc, player, tmp);
					if (!pass) {
						allPass = false;
						if (!tmp.isEmpty())
							outReasons.addAll(tmp);
					}
				}
			}

			// ---- ANY blocks (true OR across blocks) ----
			if (any != null && !any.isEmpty()) {
				anyPass = false;
				if (outReasons == null) {
					for (int i = 0; i < any.size(); i++) {
						Block b = any.get(i);
						if (b != null && b.test(loc, player, null)) {
							anyPass = true;
							break;
						}
					}
				} else {
					List<String> combined = new ArrayList<String>();
					for (int i = 0; i < any.size(); i++) {
						Block b = any.get(i);
						if (b == null)
							continue;
						List<String> tmp = new ArrayList<String>();
						if (b.test(loc, player, tmp)) {
							anyPass = true;
							break;
						} else if (!tmp.isEmpty()) {
							combined.addAll(tmp);
						}
					}
					if (!anyPass && !combined.isEmpty())
						outReasons.addAll(combined);
				}
			}

			return allPass && anyPass;
		}
	}

	/** Parse an entire condition set from the per-recipe section. */
	public static ConditionSet parseConditionSet(ConfigurationSection root) {
		ConditionSet set = new ConditionSet();
		if (root == null)
			return set;

		// ALL block (either "conditions" or "conditions_all")
		if (root.isConfigurationSection("Conditions")) {
			set.all = parseBlock(root.getConfigurationSection("Conditions"));
		}
		if (root.isConfigurationSection("Conditions_All")) {
			set.all = parseBlock(root.getConfigurationSection("Conditions_All"));
		}

		// ANY block: read as a single SECTION/MAP and split each key into its own
		// OR-block
		if (root.isConfigurationSection("Conditions_Any")) {
			ConfigurationSection anySec = root.getConfigurationSection("Conditions_Any");
			List<Block> list = splitAnySectionIntoOrBlocks(anySec);
			if (!list.isEmpty()) {
				set.any = list; // interpret each key as its own OR-block
			}
		}

		return set;
	}

	/**
	 * Parse a single AND block
	 * (world/world_deny/biome/biome_deny/time/weather/advancement/moon_phase).
	 */
	public static Block parseBlock(ConfigurationSection s) {
		Block b = new Block();
		if (s == null)
			return b;

		// world allow
		List<String> allow = s.getStringList("world");
		if (allow != null && !allow.isEmpty()) {
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < allow.size(); i++) {
				String w = allow.get(i);
				if (w != null)
					set.add(w.toLowerCase());
			}
			b.allowWorlds = set;
		}

		// world deny
		List<String> deny = s.getStringList("world_deny");
		if (deny != null && !deny.isEmpty()) {
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < deny.size(); i++) {
				String w = deny.get(i);
				if (w != null)
					set.add(w.toLowerCase());
			}
			b.denyWorlds = set;
		}

		// biome allow
		List<String> allowBiome = s.getStringList("biome");
		if (allowBiome != null && !allowBiome.isEmpty()) {
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < allowBiome.size(); i++) {
				String bn = allowBiome.get(i);
				if (bn != null)
					set.add(bn.toLowerCase());
			}
			b.allowBiomes = set;
		}

		// biome deny
		List<String> denyBiome = s.getStringList("biome_deny");
		if (denyBiome != null && !denyBiome.isEmpty()) {
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < denyBiome.size(); i++) {
				String bn = denyBiome.get(i);
				if (bn != null)
					set.add(bn.toLowerCase());
			}
			b.denyBiomes = set;
		}

		// time: can be a single "min-max" string or a string list of multiple ranges
		List<String> timeList = new ArrayList<String>();
		if (s.isList("time")) {
			List<?> raw = s.getList("time");
			if (raw != null) {
				for (int i = 0; i < raw.size(); i++) {
					Object o = raw.get(i);
					if (o != null)
						timeList.add(String.valueOf(o));
				}
			}
		} else if (s.isString("time")) {
			String raw = s.getString("time", "").trim();
			if (!raw.isEmpty())
				timeList.add(raw);
		}
		if (!timeList.isEmpty()) {
			List<Block.TimeRange> ranges = new ArrayList<Block.TimeRange>(timeList.size());
			for (int i = 0; i < timeList.size(); i++) {
				String raw = timeList.get(i);
				int dash = raw.indexOf('-');
				if (dash > 0) {
					try {
						int min = Integer.parseInt(raw.substring(0, dash).trim());
						int max = Integer.parseInt(raw.substring(dash + 1).trim());
						ranges.add(new Block.TimeRange(min, max));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			if (!ranges.isEmpty())
				b.timeRanges = ranges;
		}

		// weather: can be a single string or a list of strings
		Set<String> weatherSet = new HashSet<String>();
		if (s.isList("weather")) {
			List<?> raw = s.getList("weather");
			if (raw != null) {
				for (int i = 0; i < raw.size(); i++) {
					Object o = raw.get(i);
					if (o == null)
						continue;
					String v = String.valueOf(o).toLowerCase();
					if ("clear".equals(v) || "rain".equals(v) || "thunder".equals(v))
						weatherSet.add(v);
				}
			}
		} else if (s.isString("weather")) {
			String v = s.getString("weather", "").toLowerCase();
			if ("clear".equals(v) || "rain".equals(v) || "thunder".equals(v))
				weatherSet.add(v);
		}
		if (!weatherSet.isEmpty())
			b.weathers = weatherSet;

		// advancements: single string or list (bare "story/mine_stone" or namespaced)
		Set<String> advSet = new HashSet<String>();
		if (s.isList("advancement")) {
			List<?> raw = s.getList("advancement");
			if (raw != null) {
				for (int i = 0; i < raw.size(); i++) {
					Object o = raw.get(i);
					if (o == null)
						continue;
					String v = String.valueOf(o).trim().toLowerCase();
					if (!v.isEmpty())
						advSet.add(v);
				}
			}
		} else if (s.isString("advancement")) {
			String v = s.getString("advancement", "").trim().toLowerCase();
			if (!v.isEmpty())
				advSet.add(v);
		}
		if (!advSet.isEmpty())
			b.advancements = advSet;

		// moon_phase: single int 0–7 or list of ints
		Set<Integer> moonSet = new HashSet<Integer>();
		if (s.isList("moon_phase")) {
			List<?> raw = s.getList("moon_phase");
			if (raw != null) {
				for (int i = 0; i < raw.size(); i++) {
					Object o = raw.get(i);
					try {
						int phase = Integer.parseInt(String.valueOf(o));
						if (phase >= 0 && phase <= 7)
							moonSet.add(Integer.valueOf(phase));
					} catch (NumberFormatException ignored) {
					}
				}
			}
		} else if (s.isInt("moon_phase")) {
			int phase = s.getInt("moon_phase");
			if (phase >= 0 && phase <= 7)
				moonSet.add(Integer.valueOf(phase));
		}
		if (!moonSet.isEmpty())
			b.moonPhases = moonSet;

		return b;
	}

	/**
	 * Convert a single-section Conditions_Any into multiple OR-blocks (one per
	 * key). This runs at config-parse time (plugin load / recipe build), not at
	 * test time.
	 */
	private static List<Block> splitAnySectionIntoOrBlocks(ConfigurationSection s) {
		List<Block> out = new ArrayList<Block>();
		if (s == null)
			return out;

		// world (allow)
		List<String> allow = s.getStringList("world");
		if (allow != null && !allow.isEmpty()) {
			Block b = new Block();
			HashSet<String> set = new HashSet<String>();
			for (int i = 0; i < allow.size(); i++) {
				String w = allow.get(i);
				if (w != null)
					set.add(w.toLowerCase());
			}
			if (!set.isEmpty()) {
				b.allowWorlds = set;
				out.add(b);
			}
		}

		// world_deny
		List<String> deny = s.getStringList("world_deny");
		if (deny != null && !deny.isEmpty()) {
			Block b = new Block();
			HashSet<String> set = new HashSet<String>();
			for (int i = 0; i < deny.size(); i++) {
				String w = deny.get(i);
				if (w != null)
					set.add(w.toLowerCase());
			}
			if (!set.isEmpty()) {
				b.denyWorlds = set;
				out.add(b);
			}
		}

		// biome (allow)
		List<String> allowBiome = s.getStringList("biome");
		if (allowBiome != null && !allowBiome.isEmpty()) {
			Block b = new Block();
			HashSet<String> set = new HashSet<String>();
			for (int i = 0; i < allowBiome.size(); i++) {
				String bn = allowBiome.get(i);
				if (bn != null)
					set.add(bn.toLowerCase());
			}
			if (!set.isEmpty()) {
				b.allowBiomes = set;
				out.add(b);
			}
		}

		// biome_deny
		List<String> denyBiome = s.getStringList("biome_deny");
		if (denyBiome != null && !denyBiome.isEmpty()) {
			Block b = new Block();
			HashSet<String> set = new HashSet<String>();
			for (int i = 0; i < denyBiome.size(); i++) {
				String bn = denyBiome.get(i);
				if (bn != null)
					set.add(bn.toLowerCase());
			}
			if (!set.isEmpty()) {
				b.denyBiomes = set;
				out.add(b);
			}
		}

		// time (single "min-max" or list)
		if (s.contains("time")) {
			List<String> timeList = new ArrayList<String>();
			if (s.isList("time")) {
				List<?> raw = s.getList("time");
				if (raw != null) {
					for (int i = 0; i < raw.size(); i++) {
						Object o = raw.get(i);
						if (o != null)
							timeList.add(String.valueOf(o));
					}
				}
			} else if (s.isString("time")) {
				String raw = s.getString("time", "").trim();
				if (!raw.isEmpty())
					timeList.add(raw);
			}
			if (!timeList.isEmpty()) {
				Block b = new Block();
				ArrayList<Block.TimeRange> ranges = new ArrayList<Block.TimeRange>(timeList.size());
				for (int i = 0; i < timeList.size(); i++) {
					String raw = timeList.get(i);
					int dash = raw.indexOf('-');
					if (dash > 0) {
						try {
							int min = Integer.parseInt(raw.substring(0, dash).trim());
							int max = Integer.parseInt(raw.substring(dash + 1).trim());
							ranges.add(new Block.TimeRange(min, max));
						} catch (NumberFormatException ignored) {
						}
					}
				}
				if (!ranges.isEmpty()) {
					b.timeRanges = ranges;
					out.add(b);
				}
			}
		}

		// weather (single or list)
		if (s.contains("weather")) {
			Block b = new Block();
			HashSet<String> weatherSet = new HashSet<String>();
			if (s.isList("weather")) {
				List<?> raw = s.getList("weather");
				if (raw != null) {
					for (int i = 0; i < raw.size(); i++) {
						Object o = raw.get(i);
						if (o == null)
							continue;
						String v = String.valueOf(o).toLowerCase();
						if ("clear".equals(v) || "rain".equals(v) || "thunder".equals(v))
							weatherSet.add(v);
					}
				}
			} else if (s.isString("weather")) {
				String v = s.getString("weather", "").toLowerCase();
				if ("clear".equals(v) || "rain".equals(v) || "thunder".equals(v))
					weatherSet.add(v);
			}
			if (!weatherSet.isEmpty()) {
				b.weathers = weatherSet;
				out.add(b);
			}
		}

		// advancement (single or list)
		if (s.contains("advancement")) {
			Block b = new Block();
			HashSet<String> advSet = new HashSet<String>();
			if (s.isList("advancement")) {
				List<?> raw = s.getList("advancement");
				if (raw != null) {
					for (int i = 0; i < raw.size(); i++) {
						Object o = raw.get(i);
						if (o == null)
							continue;
						String v = String.valueOf(o).trim().toLowerCase();
						if (!v.isEmpty())
							advSet.add(v);
					}
				}
			} else if (s.isString("advancement")) {
				String v = s.getString("advancement", "").trim().toLowerCase();
				if (!v.isEmpty())
					advSet.add(v);
			}
			if (!advSet.isEmpty()) {
				b.advancements = advSet;
				out.add(b);
			}
		}

		// moon_phase (single int or list)
		if (s.contains("moon_phase")) {
			Block b = new Block();
			HashSet<Integer> moonSet = new HashSet<Integer>();
			if (s.isList("moon_phase")) {
				List<?> raw = s.getList("moon_phase");
				if (raw != null) {
					for (int i = 0; i < raw.size(); i++) {
						Object o = raw.get(i);
						try {
							int phase = Integer.parseInt(String.valueOf(o));
							if (phase >= 0 && phase <= 7)
								moonSet.add(Integer.valueOf(phase));
						} catch (NumberFormatException ignored) {
						}
					}
				}
			} else if (s.isInt("moon_phase")) {
				int phase = s.getInt("moon_phase");
				if (phase >= 0 && phase <= 7)
					moonSet.add(Integer.valueOf(phase));
			}
			if (!moonSet.isEmpty()) {
				b.moonPhases = moonSet;
				out.add(b);
			}
		}

		return out;
	}

	private RecipeConditions() {
	}
}