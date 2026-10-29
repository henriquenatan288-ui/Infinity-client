package com.infinity.setting;

/** Um minerio: liga/desliga + cor (indice da paleta). Salvo como um unico numero. */
public final class OreSetting extends Setting {
	private boolean enabled;
	private int colorIndex;

	public OreSetting(String name, boolean enabled, int colorIndex) {
		super(name);
		this.enabled = enabled;
		this.colorIndex = clampIndex(colorIndex);
	}

	private static int clampIndex(int i) {
		return Math.max(0, Math.min(ColorSetting.PALETTE.length - 1, i));
	}

	public boolean enabled() {
		return enabled;
	}

	public void toggle() {
		enabled = !enabled;
	}

	public int color() {
		return ColorSetting.PALETTE[colorIndex];
	}

	public void cycleColor() {
		colorIndex = (colorIndex + 1) % ColorSetting.PALETTE.length;
	}

	@Override
	public double toNumber() {
		return colorIndex * 2 + (enabled ? 1 : 0);
	}

	@Override
	public void fromNumber(double v) {
		int n = Math.max(0, (int) Math.round(v));
		enabled = (n & 1) == 1;
		colorIndex = clampIndex(n >> 1);
	}
}