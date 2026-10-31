package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class RenderUtil {
	private RenderUtil() {}

	public static int alpha(int color, float mul) {
		int a = (int) (((color >>> 24) & 0xFF) * Mth.clamp(mul, 0f, 1f));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = Mth.clamp(t, 0f, 1f);
		int aa = (int) Mth.lerp(t, (a >>> 24) & 0xFF, (b >>> 24) & 0xFF);
		int ar = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int ag = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int ab = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
		return (aa << 24) | (ar << 16) | (ag << 8) | ab;
	}

	/** Retangulo arredondado com borda suavizada (pixel de transicao com alfa parcial). */
	public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		// O miolo (sem cantos) e um retangulo so: 1 fill em vez de 1 por linha.
		int midTop = r;
		int midBottom = h - r;
		if (midBottom > midTop) {
			g.fill(x, y + midTop, x + w, y + midBottom, color);
		}
		for (int i = 0; i < h; i++) {
			if (i >= midTop && i < midBottom) {
				i = midBottom - 1;
				continue;
			}
			double edge = 0.0;
			if (r > 0) {
				if (i < r) {
					edge = cornerEdge(r, r - i - 0.5);
				} else if (i >= h - r) {
					edge = cornerEdge(r, i + 0.5 - (h - r));
				}
			}
			int full = (int) Math.ceil(edge);
			g.fill(x + full, y + i, x + w - full, y + i + 1, color);
			if (full > edge && full >= 1) {
				float coverage = (float) (full - edge);
				int c = alpha(color, coverage);
				g.fill(x + full - 1, y + i, x + full, y + i + 1, c);
				g.fill(x + w - full, y + i, x + w - full + 1, y + i + 1, c);
			}
		}
	}

	private static double cornerEdge(int r, double dy) {
		double dx = Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
		return r - dx;
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}