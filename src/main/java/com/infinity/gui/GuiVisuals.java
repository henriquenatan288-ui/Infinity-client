package com.infinity.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Efeitos decorativos do fundo do painel. Troque o visual novo aqui. */
public final class GuiVisuals {
	private GuiVisuals() {}

	private static final float[][] STARS = makeStars();

	private static float[][] makeStars() {
		Random r = new Random(42);
		float[][] s = new float[14][2];
		for (float[] star : s) {
			star[0] = r.nextFloat();
			star[1] = r.nextFloat();
		}
		return s;
	}

	public static void render(GuiGraphicsExtractor g, int px, int py, int w, int h, float a) {
		if (TimeTheme.night > 0.05f) {
			float t = System.currentTimeMillis() / 1000f;
			for (int i = 0; i < STARS.length; i++) {
				float tw = 0.5f + 0.5f * (float) Math.sin(t * 2f + i * 1.7f);
				int sx = px + 10 + Math.round(STARS[i][0] * (w - 20));
				int sy = py + 8 + Math.round(STARS[i][1] * (h * 0.6f));
				int c = RenderUtil.alpha(0xFFFFFFFF, a * TimeTheme.night * (0.35f + 0.5f * tw));
				g.fill(sx, sy, sx + 2, sy + 2, c);
			}
		}
	}
}