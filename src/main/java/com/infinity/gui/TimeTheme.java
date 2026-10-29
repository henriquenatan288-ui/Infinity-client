package com.infinity.gui;

import net.minecraft.util.Mth;

import java.time.LocalTime;

/** Paleta da GUI que muda conforme a hora do dia (relogio real do aparelho). */
public final class TimeTheme {
	private TimeTheme() {}

	// panel, search, dim, placeholder, input, hover
	private static final int[] DAY = {0xF2FFFFFF, 0xFFF0E6FB, 0xFF7B2FBF, 0xFF8C6BB5, 0xFF4B1A80, 0x228A2BE2};
	private static final int[] SUNSET = {0xF2FFE9DC, 0xFFFFD9C2, 0xFF9B2F9E, 0xFFB9708F, 0xFF5A1A55, 0x22D9488A};
	private static final int[] NIGHT = {0xF2171126, 0xFF2A2142, 0xFFC4A8F0, 0xFF8F7BB8, 0xFFEDE3FF, 0x33B57BEA};

	private static final float[] KEY_H = {0f, 5f, 6.25f, 8f, 16f, 17.75f, 19.5f, 24f};
	private static final int[][] KEY_P = {NIGHT, NIGHT, SUNSET, DAY, DAY, SUNSET, NIGHT, NIGHT};
	private static final float[] KEY_NIGHT = {1f, 1f, 0.3f, 0f, 0f, 0.3f, 1f, 1f};

	private static final float[] OVERRIDE_HOURS = {12f, 17.9f, 23f};
	private static int override = -1; // -1 auto, 0 dia, 1 por do sol, 2 noite

	public static int panel = DAY[0];
	public static int search = DAY[1];
	public static int dim = DAY[2];
	public static int placeholder = DAY[3];
	public static int input = DAY[4];
	public static int hover = DAY[5];

	public static float night = 0f;
	public static boolean sunUp = true;
	public static float bodyT = 0.5f;

	public static void cycleOverride() {
		override = override >= 2 ? -1 : override + 1;
	}

	public static void update() {
		float h;
		if (override >= 0) {
			h = OVERRIDE_HOURS[override];
		} else {
			LocalTime t = LocalTime.now();
			h = t.getHour() + t.getMinute() / 60f + t.getSecond() / 3600f;
		}

		int i = 0;
		while (i < KEY_H.length - 2 && h > KEY_H[i + 1]) i++;
		float span = KEY_H[i + 1] - KEY_H[i];
		float f = span <= 0f ? 0f : Mth.clamp((h - KEY_H[i]) / span, 0f, 1f);

		int[] a = KEY_P[i];
		int[] b = KEY_P[i + 1];
		panel = RenderUtil.mix(a[0], b[0], f);
		search = RenderUtil.mix(a[1], b[1], f);
		dim = RenderUtil.mix(a[2], b[2], f);
		placeholder = RenderUtil.mix(a[3], b[3], f);
		input = RenderUtil.mix(a[4], b[4], f);
		hover = RenderUtil.mix(a[5], b[5], f);
		night = Mth.lerp(f, KEY_NIGHT[i], KEY_NIGHT[i + 1]);

		if (h >= 6f && h < 18f) {
			sunUp = true;
			bodyT = (h - 6f) / 12f;
		} else {
			sunUp = false;
			float hh = h >= 18f ? h - 18f : h + 6f;
			bodyT = hh / 12f;
		}
	}
}