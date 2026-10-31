package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/**
 * Fundo "bruxa" do painel: lua crescente, bolhas de pocao subindo (verde e roxo) e estrelinhas brilhando.
 * Tudo calculado pelo relogio, sem estado, e com poucos desenhos pra nao pesar.
 */
public final class WitchVisuals {
	private WitchVisuals() {}

	private static final int BUBBLES = 11;
	private static final int SPARKLES = 9;
	private static final int GREEN = 0xFF7CFC5A;
	private static final int PURPLE = 0xFFB57BEA;
	private static final int MOON_DAY = 0xFF7B2FBF;
	private static final int MOON_NIGHT = 0xFFFFF2B0;

	private static final float[] BX = new float[BUBBLES];
	private static final float[] BPHASE = new float[BUBBLES];
	private static final float[] BSPEED = new float[BUBBLES];
	private static final int[] BSIZE = new int[BUBBLES];
	private static final float[] SXP = new float[SPARKLES];
	private static final float[] SYP = new float[SPARKLES];
	private static final float[] SPH = new float[SPARKLES];

	static {
		Random r = new Random(13);
		for (int i = 0; i < BUBBLES; i++) {
			BX[i] = 0.12f + r.nextFloat() * 0.8f;
			BPHASE[i] = r.nextFloat();
			BSPEED[i] = 0.04f + r.nextFloat() * 0.05f;
			BSIZE[i] = 3 + r.nextInt(5);
		}
		for (int i = 0; i < SPARKLES; i++) {
			SXP[i] = 0.1f + r.nextFloat() * 0.85f;
			SYP[i] = 0.08f + r.nextFloat() * 0.8f;
			SPH[i] = r.nextFloat() * 6.28f;
		}
	}

	public static void render(GuiGraphicsExtractor g, int px, int py, int w, int h, float a) {
		double sec = System.nanoTime() / 1_000_000_000.0;
		bubbles(g, px, py, w, h, a, sec);
		sparkles(g, px, py, w, h, a, sec);
		moon(g, px + w - 26, py + 24, a);
	}

	private static void bubbles(GuiGraphicsExtractor g, int px, int py, int w, int h, float a, double sec) {
		for (int i = 0; i < BUBBLES; i++) {
			float p = (float) ((sec * BSPEED[i] + BPHASE[i]) % 1.0);
			float rise = 1f - p;
			int s = BSIZE[i];
			float wobble = (float) Math.sin(sec * 1.3 + BPHASE[i] * 20.0) * 4f;
			int x = Math.round(px + BX[i] * w + wobble);
			int y = Math.round(py + 6 + rise * (h - 14));
			float fade = Math.min(1f, Math.min(p * 6f, rise * 6f));
			int base = (i % 3 == 0) ? PURPLE : GREEN;
			RenderUtil.roundRect(g, x, y, s, s, s / 2, RenderUtil.alpha(base, a * fade * 0.28f));
			g.fill(x + 1, y + 1, x + 2, y + 2, RenderUtil.alpha(0xFFFFFFFF, a * fade * 0.55f));
		}
	}

	private static void sparkles(GuiGraphicsExtractor g, int px, int py, int w, int h, float a, double sec) {
		int base = TimeTheme.dim;
		for (int i = 0; i < SPARKLES; i++) {
			float tw = 0.5f + 0.5f * (float) Math.sin(sec * 1.8 + SPH[i]);
			if (tw < 0.15f) continue;
			int x = Math.round(px + SXP[i] * w);
			int y = Math.round(py + SYP[i] * h);
			int len = 1 + Math.round(tw * 3f);
			int c = RenderUtil.alpha(base, a * tw * 0.8f);
			g.fill(x - len, y, x + len + 1, y + 1, c);
			g.fill(x, y - len, x + 1, y + len + 1, c);
			g.fill(x, y, x + 1, y + 1, RenderUtil.alpha(0xFFFFFFFF, a * tw * 0.7f));
		}
	}

	/** Lua crescente: circulo grande menos um circulo deslocado, desenhado por linhas. */
	private static void moon(GuiGraphicsExtractor g, int cx, int cy, float a) {
		int color = RenderUtil.alpha(RenderUtil.mix(MOON_DAY, MOON_NIGHT, TimeTheme.night), a * 0.9f);
		final int R = 9;
		final int shift = 5;
		final int ri = R - 1;
		for (int dy = -R; dy <= R; dy++) {
			int wo = (int) Math.floor(Math.sqrt(R * R - dy * dy));
			int left = cx - wo;
			int right = cx + wo;
			if (Math.abs(dy) <= ri) {
				int wi = (int) Math.floor(Math.sqrt(ri * ri - dy * dy));
				right = Math.min(right, cx + shift - wi);
			}
			if (right >= left) g.fill(left, cy + dy, right + 1, cy + dy + 1, color);
		}
	}
}
