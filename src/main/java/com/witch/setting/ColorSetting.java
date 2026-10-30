package com.witch.setting;

public final class ColorSetting extends Setting {
	public static final int[] PALETTE = {
			0xFF8A2BE2, // roxo
			0xFF22D3EE, // ciano
			0xFF34D399, // verde
			0xFFFACC15, // amarelo
			0xFFFB923C, // laranja
			0xFFEF4444, // vermelho
			0xFFF472B6, // rosa
			0xFFFFFFFF  // branco
	};

	private int index;

	public ColorSetting(String name, int defIndex) {
		super(name);
		setIndex(defIndex);
	}

	public int get() {
		return PALETTE[index];
	}

	public int index() {
		return index;
	}

	public void setIndex(int i) {
		index = Math.max(0, Math.min(PALETTE.length - 1, i));
	}

	@Override
	public double toNumber() {
		return index;
	}

	@Override
	public void fromNumber(double v) {
		setIndex((int) Math.round(v));
	}
}