package com.infinity.module;

import com.infinity.setting.ColorSetting;
import com.infinity.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class XRayModule extends Module {
	private static final int MAX_BLOCKS = 64;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private static final Set<Block> ORES = Set.of(
			Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
			Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
			Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
			Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.NETHER_QUARTZ_ORE,
			Blocks.ANCIENT_DEBRIS
	);

	private static XRayModule instance;
	private static volatile List<int[]> blocks = List.of();

	private final IntSetting range = add(new IntSetting("Range", 8, 24, 16, ""));
	private final ColorSetting color = add(new ColorSetting("Line color", 0));
	private final IntSetting fill = add(new IntSetting("Fill opacity", 0, 50, 15, "%"));

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

	public static float[] lineRgba() {
		int c = instance == null ? 0xFF8A2BE2 : instance.color.get();
		return new float[]{((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, 1f};
	}

	public static float fillAlpha() {
		return instance == null ? 0f : instance.fill.get() / 100f;
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

		int r = range.get();
		BlockPos center = mc.player.blockPosition();
		int cx = center.getX();
		int cy = center.getY();
		int cz = center.getZ();

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<int[]> found = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					pos.set(cx + dx, cy + dy, cz + dz);
					BlockState state = level.getBlockState(pos);
					if (ORES.contains(state.getBlock())) {
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
}