package com.infinity.module;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XRayModule extends Module {
	private static final int RADIUS = 16;
	private static final int MAX_BLOCKS = 128;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private static XRayModule instance;
	private static volatile List<int[]> blocks = List.of();

	private int cooldown = 0;

	public XRayModule() {
		super("X-Ray Hologram", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static List<int[]> blocks() {
		return blocks;
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
	}

	@Override
	protected void onDisable() {
		blocks = List.of();
	}

	@Override
	public void onTick() {
		if (--cooldown > 0) return;
		cooldown = SCAN_INTERVAL_TICKS;

		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		if (mc.player == null || level == null) {
			blocks = List.of();
			return;
		}

		BlockPos center = mc.player.blockPosition();
		int cx = center.getX();
		int cy = center.getY();
		int cz = center.getZ();

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<int[]> found = new ArrayList<>();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					pos.set(cx + dx, cy + dy, cz + dz);
					BlockState state = level.getBlockState(pos);
					if (isOre(state)) {
						found.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
					}
				}
			}
		}

		found.sort(Comparator.comparingLong(b -> {
			long ddx = b[0] - cx;
			long ddy = b[1] - cy;
			long ddz = b[2] - cz;
			return ddx * ddx + ddy * ddy + ddz * ddz;
		}));
		if (found.size() > MAX_BLOCKS) {
			found = new ArrayList<>(found.subList(0, MAX_BLOCKS));
		}
		blocks = found;
	}

	private static boolean isOre(BlockState s) {
		return s.is(BlockTags.COAL_ORES)
				|| s.is(BlockTags.IRON_ORES)
				|| s.is(BlockTags.COPPER_ORES)
				|| s.is(BlockTags.GOLD_ORES)
				|| s.is(BlockTags.REDSTONE_ORES)
				|| s.is(BlockTags.LAPIS_ORES)
				|| s.is(BlockTags.DIAMOND_ORES)
				|| s.is(BlockTags.EMERALD_ORES)
				|| s.is(Blocks.ANCIENT_DEBRIS);
	}
}