package com.infinity.module;

public abstract class Module {
	public final String name;
	public final Category category;
	private boolean enabled;

	public float hoverAnim = 0f;
	public float toggleAnim = 0f;

	protected Module(String name, Category category) {
		this.name = name;
		this.category = category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (this.enabled == value) return;
		this.enabled = value;
		if (value) onEnable(); else onDisable();
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void onTick() {}

	protected void onEnable() {}

	protected void onDisable() {}
}