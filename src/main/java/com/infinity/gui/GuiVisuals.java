package com.infinity.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Fundo do painel: constelacao (pontos flutuando, ligados por linhas pontilhadas quando ficam perto). */
public final class GuiVisuals {
	private GuiVisuals() {}

	private static final int N = 14;
	private static final float LINK = 80f;     // distancia maxima pra ligar dois pontos (px da GUI)
	private static final int MAX_LINES = 16;   // limite pra nao pesar no FPS
	private static final float INSET = 8f;     // margem pros pontos nao sairem do painel

	private static final float[] X = new float[N];
	private static final float[] Y = new float[N];
	private static final float[] VX = new float[N];
	private static final float[] VY = new float[N];
	private static final float[] SX = new float[N];
	private static final float[] SY = new float[N];
	private static long last = 0L;

	static {
		Random r = new Random(7);
		for (int i = 0; i < N; i++) {
			X[i] = r.nextFloat();
			Y[i] = r.nextFloat();
			float ang = r.nextFloat() * (float) (Math.PI * 2);
			float sp = 0.02f + r.nextFloat() * 0.03f; // fracao do painel por segundo
			VX[i] = (float) Math.cos(ang) * sp;
			VY[i] = (float) Math.sin(ang) * sp;
		}
	}

	public static void render(GuiGraphicsExtractor g, int px, int py, int w, int h, float a) {
		long now = System.nanoTime();
		float dt = last == 0L ? 0f : Math.min(0.05f, (now - last) / 1_000_000_000f);
		last = now;

		float minX = INSET / w, maxX = 1f - minX;
		float minY = INSET / h, maxY = 1f - minY;

		for (int i = 0; i < N; i++) {
			X[i] += VX[i] * dt;
			Y[i] += VY[i] * dt;
			if (X[i] < minX) { X[i] = minX; VX[i] = Math.abs(VX[i]); }
			if (X[i] > maxX) { X[i] = maxX; VX[i] = -Math.abs(VX[i]); }
			if (Y[i] < minY) { Y[i] = minY; VY[i] = Math.abs(VY[i]); }
			if (Y[i] > maxY) { Y[i] = maxY; VY[i] = -Math.abs(VY[i]); }
			SX[i] = px + X[i] * w;
			SY[i] = py + Y[i] * h;
		}

		int base = TimeTheme.dim;
		int lines = 0;
		for (int i = 0; i < N && lines < MAX_LINES; i++) {
			for (int j = i + 1; j < N && lines < MAX_LINES; j++) {
				float dx = SX[j] - SX[i];
				float dy = SY[j] - SY[i];
				float d2 = dx * dx + dy * dy;
				if (d2 >= LINK * LINK) continue;
				float d = (float) Math.sqrt(d2);
				float t = 1f - d / LINK;
				int c = RenderUtil.alpha(base, a * t * 0.5f);
				int steps = Math.max(1, (int) (d / 2f));
				for (int s = 0; s <= steps; s++) {
					int x = Math.round(SX[i] + dx * s / steps);
					int y = Math.round(SY[i] + dy * s / steps);
					g.fill(x, y, x + 1, y + 1, c);
				}
				lines++;
			}
		}

		int dot = RenderUtil.alpha(base, a * 0.7f);
		for (int i = 0; i < N; i++) {
			int x = Math.round(SX[i]);
			int y = Math.round(SY[i]);
			g.fill(x - 1, y - 1, x + 2, y + 2, dot);
		}
	}
}