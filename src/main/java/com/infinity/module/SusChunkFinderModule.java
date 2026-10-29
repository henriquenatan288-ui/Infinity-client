package com.infinity.module;

import com.infinity.render.EspRenderer.Esp;
import com.infinity.render.EspSource;
import com.infinity.setting.ColorSetting;
import com.infinity.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Marca chunks com muitos blocos "de construcao" (madeira, vidro, tocha, bau...). Estruturas naturais tambem aparecem. */
public class SusChunkFinderModule extends Module implements EspSource {
	private static final int SCAN_EVERY_TICKS = 3;
	private static final int HALF_HEIGHT = 48;

	private final IntSetting radius = add(new IntSetting("Chunk radius", 2, 6, 4, ""));
	private final IntSetting threshold = add(new IntSetting("Min blocks", 10, 200, 40, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 4));

	private final Set<Block> artificial = buildArtificial();
	private final Map<Long, int[]> found = new HashMap<>();
	private volatile List<int[]> view = List.of();

	private int[][] offsets = null;
	private int offsetsRadius = -1;
	private int cursor = 0;
	private int cooldown = 0;

	public SusChunkFinderModule() {
		super("Sus Chunk Finder", Category.VISUALS);
	}

	private static Set<Block> buildArtificial() {
		Set<Block> s = new HashSet<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			String n = BuiltInRegistries.BLOCK.getKey(b).getPath();
			if (n.endsWith("_planks") || n.endsWith("_wool") || n.contains("concrete")
					|| n.contains("glass") || n.contains("bricks") || n.contains("torch")
					|| n.contains("lantern") || n.contains("chest") || n.contains("fence")
					|| n.contains("rail") || n.contains("carpet") || n.endsWith("_stairs")
					|| n.endsWith("_slab") || n.endsWith("_door") || n.endsWith("_bed")
					|| n.startsWith("polished_") || n.startsWith("smooth_") || n.startsWith("chiseled_")
					|| n.startsWith("cut_") || n.equals("crafting_table") || n.contains("furnace")
					|| n.equals("ladder") || n.equals("bookshelf") || n.equals("barrel")
					|| n.equals("anvil") || n.equals("hay_block") || n.equals("farmland")
					|| n.equals("tnt") || n.equals("end_rod") || n.equals("campfire")) {
				s.add(b);
			}
		}
		return s;
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
		int count = 0;
		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set(cx * 16 + x, y, cz * 16 + z);
					if (artificial.contains(level.getBlockState(pos).getBlock())) {
						count++;
						if (y < lo) lo = y;
						if (y > hi) hi = y;
					}
				}
			}
		}

		long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		if (count >= threshold.get()) {
			found.put(key, new int[]{cx, cz, lo, hi, count});
		} else {
			found.remove(key);
		}
	}
}