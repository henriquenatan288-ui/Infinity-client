package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visual de "vidro fosco" do ClickGUI: sombra suave, gradiente, brilho no topo,
 * borda de luz e um reflexo que atravessa o painel. Tudo desenhado por cima do
 * painel e por baixo da constelacao/folhas, entao os efeitos antigos continuam.
 * As cores acompanham o TimeTheme (dia / por do sol / noite).
 */
public final class GlassShader {
	private GlassShader() {}

	private static final int R = 8;               // raio do painel (igual ao do ClickGuiScreen)
	private static final int SHADOW_LAYERS = 6;
	private static final long SHIMMER_MS = 7000L; // tempo de uma passada do reflexo
	private static final int SHIMMER_HALF = 26;   // meia largura do reflexo (px)

	/** Sombra difusa atras do painel. Chamar ANTES de desenhar o painel. */
	public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, float a) {
		for (int i = SHADOW_LAYERS; i >= 1; i--) {
			float t = 1f - i / (SHADOW_LAYERS + 1f);
			int c = RenderUtil.alpha(0xFF000000, a * (0.02f + 0.03f * t));
			RenderUtil.roundRect(g, x - i, y - i + 2, w + i * 2, h + i * 2, R + i, c);
		}
	}

	/** Camadas de vidro por cima do painel. Chamar DEPOIS do painel e ANTES da constelacao. */
	public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, float a) {
		float night = TimeTheme.night;
		int y0 = y + R;
		int y1 = y + h - R;
		if (y1 <= y0) return;

		// Gradiente vertical: claro em cima, tingido de roxo embaixo.
		int topRgb = RenderUtil.mix(0xFFFFFFFF, 0xFFB9A0F0, night) & 0xFFFFFF;
		int botRgb = RenderUtil.mix(0xFFB57BEA, 0xFF6A3FC0, night) & 0xFFFFFF;
		int top = 0x2A000000 | topRgb;
		int bot = 0x20000000 | botRgb;
		float span = y1 - y0;
		for (int yy = y0; yy < y1; yy++) {
			int c = RenderUtil.alpha(RenderUtil.mix(top, bot, (yy - y0) / span), a);
			g.fill(x + 1, yy, x + w - 1, yy + 1, c);
		}

		// Brilho (gloss) na parte de cima.
		int glossH = Math.max(10, h / 6);
		RenderUtil.roundRect(g, x + 2, y + 2, w - 4, glossH, R - 2,
				RenderUtil.alpha(0xFFFFFFFF, a * (0.10f - 0.04f * night)));

		// Borda de luz: forte em cima, acentuada embaixo.
		int edgeTop = RenderUtil.alpha(0xFFFFFFFF, a * (0.60f - 0.25f * night));
		int edgeBot = RenderUtil.alpha(RenderUtil.mix(0xFFB57BEA, 0xFF8A2BE2, night), a * 0.40f);
		int edgeSide = RenderUtil.mix(edgeTop, edgeBot, 0.5f);
		g.fill(x + R, y, x + w - R, y + 1, edgeTop);
		g.fill(x + R, y + h - 1, x + w - R, y + h, edgeBot);
		g.fill(x, y + R, x + 1, y + h - R, edgeSide);
		g.fill(x + w - 1, y + R, x + w, y + h - R, edgeSide);

		// Reflexo que atravessa o painel devagar.
		float cycle = (System.nanoTime() / 1_000_000L % SHIMMER_MS) / (float) SHIMMER_MS;
		int cx = Math.round(x + (cycle * 1.6f - 0.3f) * w);
		float peak = a * 0.09f * (1f - 0.5f * night);
		for (int dx = -SHIMMER_HALF; dx <= SHIMMER_HALF; dx++) {
			int xx = cx + dx;
			if (xx <= x + 1 || xx >= x + w - 1) continue;
			float f = 1f - Math.abs(dx) / (float) SHIMMER_HALF;
			g.fill(xx, y0, xx + 1, y1, RenderUtil.alpha(0xFFFFFFFF, peak * f * f));
		}
	}

	/** Fundo fosco atras da barra lateral de categorias. */
	public static void sidebar(GuiGraphicsExtractor g, int x, int y, int w, int h, float a) {
		int c = RenderUtil.mix(0xFFFFFFFF, 0xFFB57BEA, TimeTheme.night);
		RenderUtil.roundRect(g, x - 2, y - 2, w + 4, h + 4, 6, RenderUtil.alpha(c, a * 0.14f));
	}
}
