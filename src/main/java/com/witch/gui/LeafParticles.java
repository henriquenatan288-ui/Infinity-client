package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Chapeus de bruxa pequenos caindo no fundo da GUI, desenhados so com retangulos (sem shader). */
public final class LeafParticles {
	private static final int COUNT = 10;
	private static final int[] PALETTE = {0xFF4B1A80, 0xFF6A1FB5, 0xFF2E1065, 0xFF5B21B6};
	private static final int BAND = 0xFFB57BEA;

	private static final class Leaf {
		float x, y, vx, vy, size, angle, spin, phase;
		int color;
	}

	private final Leaf[] leaves = new Leaf[COUNT];
	private final Random rng = new Random();
	private boolean ready = false;
	private float time = 0f;

	private Leaf spawn(int w, int h, boolean anywhere) {
		Leaf l = new Leaf();
		l.x = rng.nextFloat() * Math.max(1, w);
		l.y = anywhere ? rng.nextFloat() * Math.max(1, h) : -8f;
		l.vx = -0.05f + rng.nextFloat() * 0.2f;
		l.vy = 0.25f + rng.nextFloat() * 0.45f;
		l.size = 4f + rng.nextFloat() * 3f;
		l.angle = rng.nextFloat() * 6.2832f;
		l.spin = (rng.nextFloat() - 0.5f) * 0.04f;
		l.phase = rng.nextFloat() * 6.2832f;
		l.color = PALETTE[rng.nextInt(PALETTE.length)];
		return l;
	}

	public void update(float speed, int w, int h) {
		if (!ready) {
			for (int i = 0; i < COUNT; i++) leaves[i] = spawn(w, h, true);
			ready = true;
		}
		time += speed / 60f;
		for (int i = 0; i < COUNT; i++) {
			Leaf l = leaves[i];
			l.y += l.vy * speed;
			l.x += (l.vx + (float) Math.sin(time * 1.5f + l.phase) * 0.35f) * speed;
			l.angle += l.spin * speed;
			if (l.y > h + 8 || l.x < -10 || l.x > w + 10) {
				leaves[i] = spawn(w, h, false);
			}
		}
	}

	public void render(GuiGraphicsExtractor g, int ox, int oy, int w, int h, float alpha) {
		g.enableScissor(ox, oy, ox + w, oy + h);
		for (Leaf l : leaves) {
			if (l == null) continue;
			drawHat(g, Math.round(ox + l.x), Math.round(oy + l.y), l.color, alpha * 0.8f);
		}
		g.disableScissor();
	}

	/** Chapeu de bruxa em pixel art (12 x 11 px): ponta torta, cone, faixa e aba. */
	private void drawHat(GuiGraphicsExtractor g, int x, int y, int body, float alpha) {
		int c = RenderUtil.alpha(body, alpha);
		int band = RenderUtil.alpha(BAND, alpha);
		// ponta (inclinada pra direita), cone, faixa e aba; cada celula tem 2 px
		g.fill(x + 6, y, x + 8, y + 2, c);
		g.fill(x + 4, y + 2, x + 8, y + 4, c);
		g.fill(x + 3, y + 4, x + 9, y + 6, c);
		g.fill(x + 2, y + 6, x + 10, y + 8, c);
		g.fill(x + 2, y + 8, x + 10, y + 9, band);
		g.fill(x, y + 9, x + 12, y + 11, c);
	}
}
