package com.infinity.module;

import com.infinity.render.EspRenderer.Esp;
import com.infinity.render.EspSource;
import com.infinity.setting.IntSetting;
import com.infinity.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Caixa em baus, barris, shulkers, spawners etc., atraves de parede. */
public class StorageEspModule extends Module implements EspSource {
	private static final int MAX_BLOCKS = 96;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private static volatile List<int[]> blocks = List.of();

	/** Cada item: {x, y, z, corARGB}. Tambem usado pelos Tracers. */
	public static List<int[]> blocks() {
		return blocks;
	}

	private final Map<Block, OreSetting> byBlock = new HashMap<>();
	private final IntSetting range;
	private final IntSetting fill;
	private final OreSetting shulkers;
	private int cooldown = 0;

	public StorageEspModule() {
		super("Storage ESP", Category.VISUALS);

		range = add(new IntSetting("Range", 8, 24, 16, ""));
		fill = add(new IntSetting("Fill opacity", 0, 40, 10, "%"));

		ore("Chests", true, 4, Blocks.CHEST, Blocks.TRAPPED_CHEST);
		ore("Ender Chest", true, 0, Blocks.ENDER_CHEST);
		ore("Barrels", true, 3, Blocks.BARREL);
		shulkers = ore("Shulkers", true, 6);
		ore("Spawners", true, 5, Blocks.SPAWNER);
		ore("Furnaces", false, 7, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER);
	}

	private OreSetting ore(String name, boolean on, int color, Block... list) {
		OreSetting s = add(new OreSetting(name, on, color));
		for (Block b : list) {
			byBlock.put(b, s);
		}
		return s;
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		float fillA = fill.get() / 100f;
		for (int[] b : blocks) {
			esp.box(b[0], b[1], b[2], b[0] + 1, b[1] + 1, b[2] + 1, b[3], fillA);
		}
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
					Block blk = state.getBlock();
					OreSetting os = byBlock.get(blk);
					if (os == null && blk instanceof ShulkerBoxBlock) os = shulkers;
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