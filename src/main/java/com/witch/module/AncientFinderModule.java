package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procura Ancient Debris (netherita) nos chunks carregados e marca atraves de parede.
 * So enxerga o que o servidor realmente enviou: em servidor com anti-xray, os blocos escondidos
 * chegam falsos, e nenhum cliente consegue recuperar isso.
 */
public class AncientFinderModule extends Module implements EspSource {
	private static final int MAX_BLOCKS = 64;
	private static final int HALF_HEIGHT = 64;

	private final IntSetting range = add(new IntSetting("Range", 1, 8, 4, " chunks"));
	private final IntSetting fill = add(new IntSetting("Fill opacity", 0, 40, 14, "%"));
	private final IntSetting maxCluster = add(new IntSetting("Max cluster", 1, 16, 4, " blocks"));
	private final OreSetting debris = add(new OreSetting("Ancient Debris", true, 2));
	/** So o toggle importa; a cor do chip nao e usada. */
	private final OreSetting lavaOnly = add(new OreSetting("Lava only", false, 0));

	private static final int SCAN_EVERY_TICKS = 4;
	private int tickCounter = 0;

	private final Map<Long, List<int[]>> perChunk = new HashMap<>();
	private final List<long[]> queue = new ArrayList<>();
	private volatile List<int[]> blocks = List.of();

	public AncientFinderModule() {
		super("Ancient Finder", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		float fillA = fill.get() / 100f;
		int color = debris.color();
		for (int[] b : blocks) {
			esp.box(b[0], b[1], b[2], b[0] + 1, b[1] + 1, b[2] + 1, color, fillA);
		}
	}

	@Override
	protected void onEnable() {
		reset();
	}

	@Override
	protected void onDisable() {
		reset();
	}

	private void reset() {
		perChunk.clear();
		queue.clear();
		blocks = List.of();
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		if (mc.player == null || level == null) {
			reset();
			return;
		}

		if (++tickCounter % SCAN_EVERY_TICKS != 0) return;

		int pcx = mc.player.blockPosition().getX() >> 4;
		int pcz = mc.player.blockPosition().getZ() >> 4;
		int r = range.get();

		if (queue.isEmpty()) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int cx = pcx + dx;
					int cz = pcz + dz;
					if (level.getChunkSource().hasChunk(cx, cz)) queue.add(new long[]{cx, cz});
				}
			}
			queue.sort(Comparator.comparingLong(c -> {
				long a = c[0] - pcx;
				long b = c[1] - pcz;
				return a * a + b * b;
			}));
			final int fr = r;
			perChunk.keySet().removeIf(k -> {
				int kx = (int) (k >> 32);
				int kz = (int) (long) k;
				return Math.abs(kx - pcx) > fr || Math.abs(kz - pcz) > fr;
			});
		}

		if (!queue.isEmpty()) {
			long[] c = queue.remove(0);
			scanChunk(level, (int) c[0], (int) c[1], mc.player.blockPosition().getY());
		}

		int px = mc.player.blockPosition().getX();
		int py = mc.player.blockPosition().getY();
		int pz = mc.player.blockPosition().getZ();
		List<int[]> all = new ArrayList<>();
		for (List<int[]> l : perChunk.values()) all.addAll(l);
		all = dropBigClusters(all, maxCluster.get());
		all.sort(Comparator.comparingLong(b -> {
			long dx = b[0] - px;
			long dy = b[1] - py;
			long dz = b[2] - pz;
			return dx * dx + dy * dy + dz * dz;
		}));
		blocks = all.size() > MAX_BLOCKS ? new ArrayList<>(all.subList(0, MAX_BLOCKS)) : all;
	}

	private void scanChunk(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz, int py) {
		int minY = Math.max(level.getMinY(), py - HALF_HEIGHT);
		int maxY = py + HALF_HEIGHT;
		boolean lava = lavaOnly.enabled();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
		List<int[]> found = new ArrayList<>();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set((cx << 4) + x, y, (cz << 4) + z);
					if (!level.getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)) continue;
					if (lava && !touchesLava(level, pos, n)) continue;
					found.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
				}
			}
		}
		long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
		if (found.isEmpty()) perChunk.remove(key); else perChunk.put(key, found);
	}

	private static boolean touchesLava(net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos, BlockPos.MutableBlockPos n) {
		for (Direction d : Direction.values()) {
			n.setWithOffset(pos, d);
			if (level.getFluidState(n).is(FluidTags.LAVA)) return true;
		}
		return false;
	}

	/** Remove grupos de debris colados com mais de "max" blocos (tipico de isca de anti-xray). Vizinhanca 3x3x3. */
	private static List<int[]> dropBigClusters(List<int[]> all, int max) {
		Map<Long, Integer> index = new HashMap<>();
		for (int i = 0; i < all.size(); i++) {
			int[] b = all.get(i);
			index.put(BlockPos.asLong(b[0], b[1], b[2]), i);
		}
		boolean[] seen = new boolean[all.size()];
		List<int[]> keep = new ArrayList<>();
		for (int i = 0; i < all.size(); i++) {
			if (seen[i]) continue;
			List<Integer> group = new ArrayList<>();
			ArrayDeque<Integer> stack = new ArrayDeque<>();
			stack.push(i);
			seen[i] = true;
			while (!stack.isEmpty()) {
				int cur = stack.pop();
				group.add(cur);
				int[] b = all.get(cur);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) continue;
							Integer j = index.get(BlockPos.asLong(b[0] + dx, b[1] + dy, b[2] + dz));
							if (j != null && !seen[j]) {
								seen[j] = true;
								stack.push(j);
							}
						}
					}
				}
			}
			if (group.size() <= max) {
				for (int g : group) keep.add(all.get(g));
			}
		}
		return keep;
	}
}
