package com.witch.module;

import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XRayModule extends Module {
	private static final int MAX_BLOCKS = 64;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private static XRayModule instance;
	private static volatile List<int[]> blocks = List.of();

	private final Map<Block, OreSetting> byBlock = new HashMap<>();

	private final IntSetting range;
	private final IntSetting fill;

	private int cooldown = 0;

	public XRayModule() {
		super("X-Ray Hologram", Category.VISUALS);
		instance = this;

		range = add(new IntSetting("Range", 8, 24, 16, ""));
		fill = add(new IntSetting("Fill opacity", 0, 50, 15, "%"));

		// nome, ligado por padrao, cor (indice da paleta), blocos
		ore("Diamond", true, 1, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
		ore("Emerald", true, 2, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
		ore("Gold", true, 3, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
		ore("Lapis", true, 0, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
		ore("Redstone", true, 5, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
		ore("Ancient Debris", true, 4, Blocks.ANCIENT_DEBRIS);
		ore("Iron", false, 6, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		ore("Copper", false, 4, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
		ore("Coal", false, 7, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
		ore("Quartz", false, 7, Blocks.NETHER_QUARTZ_ORE);
	}

	private void ore(String name, boolean on, int color, Block... list) {
		OreSetting s = add(new OreSetting(name, on, color));
		for (Block b : list) {
			byBlock.put(b, s);
		}
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Cada item: {x, y, z, corARGB}. */
	public static List<int[]> blocks() {
		return blocks;
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
					OreSetting os = byBlock.get(state.getBlock());
					if (os != null && os.enabled()) {
						found.add(new int[]{pos.getX(), pos.getY(), pos.getZ(), os.color()});
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