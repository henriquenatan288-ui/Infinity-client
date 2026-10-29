package com.infinity.module;

import com.infinity.render.EspRenderer.Esp;
import com.infinity.render.EspSource;
import com.infinity.setting.ColorSetting;
import com.infinity.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Aura de simbolos do infinito girando em volta do jogador. */
public class ParticlesModule extends Module implements EspSource {
	private static final int DOTS = 18;
	private static final double A = 0.28;   // tamanho do simbolo
	private static final double D = 0.012;  // tamanho de cada pontinho

	private final IntSetting speed = add(new IntSetting("Speed", 1, 10, 4, ""));
	private final IntSetting distance = add(new IntSetting("Distance", 1, 8, 3, ""));
	private final IntSetting amount = add(new IntSetting("Amount", 1, 5, 3, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 0));

	public ParticlesModule() {
		super("Particles", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		var me = Minecraft.getInstance().player;
		if (me == null) return;

		float pt = esp.partial();
		double cx = Mth.lerp(pt, me.xOld, me.getX());
		double cy = Mth.lerp(pt, me.yOld, me.getY()) + me.getBbHeight() * 0.5;
		double cz = Mth.lerp(pt, me.zOld, me.getZ());

		double t = System.nanoTime() / 1.0e9;
		double base = t * speed.get() * 0.4;
		double radius = 0.5 + distance.get() * 0.4;
		int n = amount.get();
		int argb = color.get();

		for (int i = 0; i < n; i++) {
			double ang = base + i * (Math.PI * 2.0 / n);
			double px = cx + Math.cos(ang) * radius;
			double pz = cz + Math.sin(ang) * radius;
			double py = cy + Math.sin(t * 1.3 + i * 2.1) * 0.25;
			double tx = -Math.sin(ang);
			double tz = Math.cos(ang);

			for (int k = 0; k < DOTS; k++) {
				double s = Math.PI * 2.0 * k / DOTS;
				double sn = Math.sin(s);
				double den = 1.0 + sn * sn;
				double cu = A * Math.cos(s) / den;
				double cv = A * sn * Math.cos(s) / den;
				double x = px + tx * cu;
				double y = py + cv;
				double z = pz + tz * cu;
				esp.box(x - D, y - D, z - D, x + D, y + D, z + D, argb, 0f);
			}
		}
	}
}