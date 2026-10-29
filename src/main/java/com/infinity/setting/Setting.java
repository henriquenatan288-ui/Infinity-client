package com.infinity.setting;

public abstract class Setting {
	public final String name;

	protected Setting(String name) {
		this.name = name;
	}

	public abstract double toNumber();

	public abstract void fromNumber(double v);
}