package com.witch.setting;

public final class IntSetting extends Setting {
	public final int min;
	public final int max;
	public final String suffix;
	private int value;

	public IntSetting(String name, int min, int max, int def, String suffix) {
		super(name);
		this.min = min;
		this.max = max;
		this.suffix = suffix;
		set(def);
	}

	public int get() {
		return value;
	}

	public void set(int v) {
		value = Math.max(min, Math.min(max, v));
	}

	public float fraction() {
		return max == min ? 0f : (value - min) / (float) (max - min);
	}

	public void setFraction(float f) {
		float c = Math.max(0f, Math.min(1f, f));
		set(min + Math.round(c * (max - min)));
	}

	@Override
	public double toNumber() {
		return value;
	}

	@Override
	public void fromNumber(double v) {
		set((int) Math.round(v));
	}
}