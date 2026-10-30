package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Marca chunks com sinais fortes de base de jogador (containers, redstone, decoracao).
 * Cada bloco tem um peso; blocos que nascem naturalmente (tabua, trilho, tocha...) nao contam.
 */
public class SusChunkFinderModule extends Module implements EspSource {
	private static final int SCAN_EVERY_TICKS = 3;
	private static final int HALF_HEIGHT = 48;

	private final IntSetting radius = add(new IntSetting("Chunk radius", 2, 6, 4, ""));
	private final IntSetting threshold = add(new IntSetting("Min score", 5, 200, 30, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 4));

	private final Map<Block, Integer> weights = buildWeights();
	private final Map<Long, int[]> found = new HashMap<>();
	private volatile List<int[]> view = List.of();

	private int[][] offsets = null;
	private int offsetsRadius = -1;
	private int cursor = 0;
	private int cooldown = 0;

	public SusChunkFinderModule() {
		super("Sus Chunk Finder", Category.VISUALS);
	}

	private static int weightOf(String n) {
		// quase nunca aparecem sozinhos
		if (n.endsWith("shulker_box") || n.equals("ender_chest") || n.equals("beacon")
				|| n.equals("enchanting_table")) {
			return 10;
		}
		if (n.equals("hopper") || n.equals("dropper") || n.equals("dispenser") || n.equals("observer")
				|| n.equals("piston") || n.equals("sticky_piston") || n.equals("repeater")
				|| n.equals("comparator") || n.equals("redstone_lamp") || n.equals("note_block")
				|| n.equals("daylight_detector")) {
			return 8;
		}
		// containers e estacoes de trabalho
		if (n.equals("chest") || n.equals("trapped_chest") || n.equals("barrel") || n.equals("furnace")
				|| n.equals("blast_furnace") || n.equals("smoker") || n.equals("anvil")
				|| n.equals("chipped_anvil") || n.equals("damaged_anvil") || n.equals("brewing_stand")
				|| n.equals("smithing_table")) {
			return 4;
		}
		// decoracao que nao nasce natural
		if (n.equals("crafting_table") || n.endsWith("_wool") || n.contains("concrete")
				|| n.endsWith("stained_glass") || n.endsWith("stained_glass_pane")
				|| n.endsWith("glazed_terracotta")) {
			return 2;
		}
		return 0;
	}

	private static Map<Block, Integer> buildWeights() {
		Map<Block, Integer> m = new HashMap<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			int w = weightOf(BuiltInRegistries.BLOCK.getKey(b).getPath());
			if (w > 0) m.put(b, w);
		}
		return m;
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		int argb = color.get();
		for (int[] v : view) {
			int x0 = v[0] * 16;
			int z0 = v[1] * 16;
			esp.box(x0, v[2], z0, x0 + 16, v[3] + 1, z0 + 16, argb, 0f);
		}
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
		cursor = 0;
	}

	@Override
	protected void onDisable() {
		found.clear();
		view = List.of();
	}

	private void buildOffsets(int r) {
		List<int[]> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				list.add(new int[]{dx, dz});
			}
		}
		list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		offsets = list.toArray(new int[0][]);
		offsetsRadius = r;
		cursor = 0;
	}

	@Override
	public void onTick() {
		if (--cooldown > 0) return;
		cooldown = SCAN_EVERY_TICKS;

		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		var player = mc.player;
		if (player == null || level == null) {
			found.clear();
			view = List.of();
			return;
		}

		int r = radius.get();
		if (offsets == null || offsetsRadius != r) buildOffsets(r);

		BlockPos pp = player.blockPosition();
		int pcx = pp.getX() >> 4;
		int pcz = pp.getZ() >> 4;

		int[] off = offsets[cursor];
		cursor = (cursor + 1) % offsets.length;
		int cx = pcx + off[0];
		int cz = pcz + off[1];
		if (level.getChunkSource().hasChunk(cx, cz)) {
			scanChunk(level, cx, cz, pp.getY());
		}

		found.values().removeIf(v -> Math.abs(v[0] - pcx) > r + 1 || Math.abs(v[1] - pcz) > r + 1);
		view = new ArrayList<>(found.values());
	}

	private void scanChunk(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz, int py) {
		int minY = Math.max(level.getMinY(), py - HALF_HEIGHT);
		int maxY = py + HALF_HEIGHT;
		int score = 0;
		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set(cx * 16 + x, y, cz * 16 + z);
					Integer w = weights.get(level.getBlockState(pos).getBlock());
					if (w != null) {
						score += w;
						if (y < lo) lo = y;
						if (y > hi) hi = y;
					}
				}
			}
		}

		long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		if (score >= threshold.get()) {
			found.put(key, new int[]{cx, cz, lo, hi, score});
		} else {
			found.remove(key);
		}
	}
}