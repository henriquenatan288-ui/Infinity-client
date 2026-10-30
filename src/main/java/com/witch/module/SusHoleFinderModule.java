package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Marca buracos de 1x1 (pocos e tunéis estreitos) cercados de bloco solido: entrada tipica de base escondida. */
public class SusHoleFinderModule extends Module implements EspSource {
	private static final int MAX_RESULTS = 48;
	private static final int SCAN_INTERVAL_TICKS = 40;

	private final IntSetting range = add(new IntSetting("Range", 8, 24, 16, ""));
	private final IntSetting minLen = add(new IntSetting("Min length", 3, 12, 4, ""));
	private final IntSetting fill = add(new IntSetting("Fill opacity", 0, 40, 12, "%"));
	private final OreSetting shafts = add(new OreSetting("Vertical holes", true, 1));
	private final OreSetting tunnels = add(new OreSetting("Tunnels", true, 6));

	private volatile List<int[]> results = List.of();
	private int cooldown = 0;

	public SusHoleFinderModule() {
		super("Sus Hole Finder", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		float fillA = fill.get() / 100f;
		for (int[] b : results) {
			esp.box(b[0], b[1], b[2], b[3], b[4], b[5], b[6], fillA);
		}
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
	}

	@Override
	protected void onDisable() {
		results = List.of();
	}

	private static int idx(int d, int ix, int iy, int iz) {
		return (iy * d + iz) * d + ix;
	}

	@Override
	public void onTick() {
		if (--cooldown > 0) return;
		cooldown = SCAN_INTERVAL_TICKS;

		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		if (mc.player == null || level == null) {
			results = List.of();
			return;
		}

		int r = range.get();
		int d = 2 * r + 3;
		BlockPos c = mc.player.blockPosition();
		int ox = c.getX() - r - 1;
		int oy = c.getY() - r - 1;
		int oz = c.getZ() - r - 1;

		// 0 = ar, 1 = bloco solido, 2 = outra coisa (agua, planta, vidro...)
		byte[] g = new byte[d * d * d];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int iy = 0; iy < d; iy++) {
			for (int iz = 0; iz < d; iz++) {
				for (int ix = 0; ix < d; ix++) {
					pos.set(ox + ix, oy + iy, oz + iz);
					BlockState st = level.getBlockState(pos);
					byte t = st.isAir() ? 0 : (st.isSolidRender() ? (byte) 1 : (byte) 2);
					g[idx(d, ix, iy, iz)] = t;
				}
			}
		}

		int min = minLen.get();
		List<int[]> out = new ArrayList<>();

		if (shafts.enabled()) {
			int col = shafts.color();
			for (int ix = 1; ix <= d - 2; ix++) {
				for (int iz = 1; iz <= d - 2; iz++) {
					int run = 0;
					for (int iy = 1; iy <= d - 1; iy++) {
						boolean hit = iy <= d - 2
								&& g[idx(d, ix, iy, iz)] == 0
								&& g[idx(d, ix + 1, iy, iz)] == 1 && g[idx(d, ix - 1, iy, iz)] == 1
								&& g[idx(d, ix, iy, iz + 1)] == 1 && g[idx(d, ix, iy, iz - 1)] == 1;
						if (hit) {
							run++;
						} else {
							if (run >= min) {
								out.add(new int[]{ox + ix, oy + iy - run, oz + iz, ox + ix + 1, oy + iy, oz + iz + 1, col});
							}
							run = 0;
						}
					}
				}
			}
		}

		if (tunnels.enabled()) {
			int col = tunnels.color();
			// tunel ao longo do eixo X
			for (int iy = 1; iy <= d - 2; iy++) {
				for (int iz = 1; iz <= d - 2; iz++) {
					int run = 0;
					for (int ix = 1; ix <= d - 1; ix++) {
						boolean hit = ix <= d - 2
								&& g[idx(d, ix, iy, iz)] == 0
								&& g[idx(d, ix, iy + 1, iz)] == 1 && g[idx(d, ix, iy - 1, iz)] == 1
								&& g[idx(d, ix, iy, iz + 1)] == 1 && g[idx(d, ix, iy, iz - 1)] == 1;
						if (hit) {
							run++;
						} else {
							if (run >= min) {
								out.add(new int[]{ox + ix - run, oy + iy, oz + iz, ox + ix, oy + iy + 1, oz + iz + 1, col});
							}
							run = 0;
						}
					}
				}
			}
			// tunel ao longo do eixo Z
			for (int ix = 1; ix <= d - 2; ix++) {
				for (int iy = 1; iy <= d - 2; iy++) {
					int run = 0;
					for (int iz = 1; iz <= d - 1; iz++) {
						boolean hit = iz <= d - 2
								&& g[idx(d, ix, iy, iz)] == 0
								&& g[idx(d, ix, iy + 1, iz)] == 1 && g[idx(d, ix, iy - 1, iz)] == 1
								&& g[idx(d, ix + 1, iy, iz)] == 1 && g[idx(d, ix - 1, iy, iz)] == 1;
						if (hit) {
							run++;
						} else {
							if (run >= min) {
								out.add(new int[]{ox + ix, oy + iy, oz + iz - run, ox + ix + 1, oy + iy + 1, oz + iz, col});
							}
							run = 0;
						}
					}
				}
			}
		}

		final int px = c.getX();
		final int py = c.getY();
		final int pz = c.getZ();
		out.sort(Comparator.comparingLong(b -> {
			long dx = (b[0] + b[3]) / 2 - px;
			long dy = (b[1] + b[4]) / 2 - py;
			long dz = (b[2] + b[5]) / 2 - pz;
			return dx * dx + dy * dy + dz * dz;
		}));
		if (out.size() > MAX_RESULTS) {
			out = new ArrayList<>(out.subList(0, MAX_RESULTS));
		}
		results = out;
	}
}