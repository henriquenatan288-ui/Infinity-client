package com.infinity.gui;

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

	public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		for (int i = 0; i < h; i++) {
			int inset = 0;
			if (r > 0) {
				if (i < r) {
					inset = cornerInset(r, r - i - 0.5);
				} else if (i >= h - r) {
					inset = cornerInset(r, i + 0.5 - (h - r));
				}
			}
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
		}
	}

	private static int cornerInset(int r, double dy) {
		double dx = Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
		return (int) Math.round(r - dx);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}