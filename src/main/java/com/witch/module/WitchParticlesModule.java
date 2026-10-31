package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Chapeus de bruxa pequenos girando e subindo em volta de voce, desenhados no mundo 3D. */
public class WitchParticlesModule extends Module implements EspSource {
	private static final int BODY = 0xFF6A1FB5;
	private static final int BAND = 0xFFC9A0F5;
	private static final int BODY_DARK = 0xFF3B0F73;

	private final IntSetting amount = add(new IntSetting("Amount", 1, 24, 10, ""));
	private final IntSetting size = add(new IntSetting("Size", 1, 6, 3, ""));
	private final IntSetting speed = add(new IntSetting("Speed", 1, 10, 4, ""));
	private final IntSetting distance = add(new IntSetting("Distance", 1, 6, 2, " blocks"));

	public WitchParticlesModule() {
		super("Witch Particles", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		float pt = esp.partial();
		double px = Mth.lerp(pt, p.xOld, p.getX());
		double py = Mth.lerp(pt, p.yOld, p.getY());
		double pz = Mth.lerp(pt, p.zOld, p.getZ());

		double t = System.nanoTime() / 1_000_000_000.0;
		int n = amount.get();
		double sp = speed.get() * 0.15;
		double u = size.get() * 0.05;
		for (int i = 0; i < n; i++) {
			double phase = i / (double) n;
			double life = (t * 0.12 * (0.6 + speed.get() * 0.12) + phase) % 1.0;
			double ang = t * sp * (0.7 + 0.3 * ((i * 7) % 5) / 4.0) + i * 2.399963;
			double rad = distance.get() * (0.55 + 0.45 * ((i * 37) % 10) / 9.0);
			double y = py + 0.2 + life * 2.4;
			double x = px + Math.cos(ang) * rad;
			double z = pz + Math.sin(ang) * rad;
			float fade = (float) Math.min(1.0, Math.min(life * 6.0, (1.0 - life) * 6.0));
			hat(esp, x, y, z, u, fade * 0.9f);
		}
	}

	/** Chapeu feito de caixas: aba, cone em 3 camadas, faixa e ponta torta. */
	private static void hat(Esp esp, double x, double y, double z, double u, float a) {
		esp.solid(x - 3 * u, y, z - 3 * u, x + 3 * u, y + 0.4 * u, z + 3 * u, BODY_DARK, a);
		esp.solid(x - 1.8 * u, y + 0.4 * u, z - 1.8 * u, x + 1.8 * u, y + 1.3 * u, z + 1.8 * u, BAND, a);
		esp.solid(x - 1.6 * u, y + 1.3 * u, z - 1.6 * u, x + 1.6 * u, y + 2.6 * u, z + 1.6 * u, BODY, a);
		esp.solid(x - 1.1 * u, y + 2.6 * u, z - 1.1 * u, x + 1.1 * u, y + 3.8 * u, z + 1.1 * u, BODY, a);
		esp.solid(x - 0.5 * u + 0.5 * u, y + 3.8 * u, z - 0.5 * u, x + 0.5 * u + 0.5 * u, y + 5.0 * u, z + 0.5 * u, BODY, a);
	}
}
