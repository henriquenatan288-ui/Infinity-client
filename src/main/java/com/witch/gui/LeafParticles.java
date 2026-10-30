package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Folhas caindo no fundo da GUI, desenhadas so com quadradinhos (sem shader). */
public final class LeafParticles {
	private static final int COUNT = 10;
	private static final int[] PALETTE = {0xFF8A2BE2, 0xFFB57BEA, 0xFFD2B0F5, 0xFF6A1FB5};

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
			drawLeaf(g, ox + l.x, oy + l.y, l.size, l.angle, RenderUtil.alpha(l.color, alpha * 0.45f));
		}
		g.disableScissor();
	}

	private void drawLeaf(GuiGraphicsExtractor g, float cx, float cy, float size, float angle, int color) {
		float dx = (float) Math.cos(angle);
		float dy = (float) Math.sin(angle);
		float perpX = -dy;
		float perpY = dx;
		int steps = Math.max(4, (int) (size * 1.2f));
		for (int i = 0; i <= steps; i++) {
			float t = (i / (float) steps) * 2f - 1f;
			float half = (1f - t * t) * size * 0.45f;
			float ax = cx + dx * t * size;
			float ay = cy + dy * t * size;
			int n = Math.max(1, (int) half);
			for (int k = -n; k <= n; k++) {
				float off = (k / (float) n) * half;
				int x = Math.round(ax + perpX * off);
				int y = Math.round(ay + perpY * off);
				g.fill(x, y, x + 2, y + 2, color);
			}
		}
	}
}