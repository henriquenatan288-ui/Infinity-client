package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/** Ao quebrar um bloco (por padrao so olhando pra baixo), solta ondas que se expandem pelo chao. */
public class BreakWaveModule extends Module implements EspSource {
	private static final int COLOR = 0xFFB57BEA;
	private static final double DURATION = 1.1; // segundos
	private static final int SEGMENTS = 28;
	private static final int MAX_WAVES = 6;

	private final IntSetting radius = add(new IntSetting("Radius", 2, 10, 4, " blocks"));
	private final IntSetting rings = add(new IntSetting("Rings", 1, 4, 3, ""));
	private final IntSetting downOnly = add(new IntSetting("Down only (0/1)", 0, 1, 1, ""));

	private final List<double[]> waves = new ArrayList<>(); // {x, y, z, inicioSeg}
	private BlockPos last = null;

	public BreakWaveModule() {
		super("Break Wave", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	protected void onDisable() {
		waves.clear();
		last = null;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		var level = mc.level;
		if (p == null || level == null) return;

		if (last != null && level.getBlockState(last).isAir()) {
			boolean down = downOnly.get() == 0 || (p.getXRot() > 35f && last.getY() < p.blockPosition().getY());
			if (down) {
				waves.add(new double[]{last.getX() + 0.5, last.getY() + 1.02, last.getZ() + 0.5, now()});
				if (waves.size() > MAX_WAVES) waves.remove(0);
			}
			last = null;
		}
		if (mc.hitResult instanceof BlockHitResult bhr && mc.options.keyAttack.isDown()) {
			BlockPos pos = bhr.getBlockPos();
			if (!level.getBlockState(pos).isAir()) last = pos.immutable();
		}
	}

	private static double now() {
		return System.nanoTime() / 1_000_000_000.0;
	}

	@Override
	public void collect(Esp esp) {
		double t = now();
		waves.removeIf(w -> t - w[3] > DURATION + 0.5);
		int nRings = rings.get();
		for (double[] w : waves) {
			double prog = (t - w[3]) / DURATION;
			for (int k = 0; k < nRings; k++) {
				double pr = prog - k * 0.14;
				if (pr <= 0 || pr >= 1) continue;
				double r = 0.4 + (radius.get() - 0.4) * (1.0 - Math.pow(1.0 - pr, 2.0));
				float alpha = (float) (1.0 - pr) * 0.85f;
				ring(esp, w[0], w[1], w[2], r, alpha);
			}
		}
	}

	private static void ring(Esp esp, double cx, double cy, double cz, double r, float alpha) {
		double px = cx + r;
		double pz = cz;
		for (int i = 1; i <= SEGMENTS; i++) {
			double a = i * (Math.PI * 2 / SEGMENTS);
			double nx = cx + Math.cos(a) * r;
			double nz = cz + Math.sin(a) * r;
			esp.line(px, cy, pz, nx, cy, nz, COLOR, alpha);
			px = nx;
			pz = nz;
		}
	}
}
