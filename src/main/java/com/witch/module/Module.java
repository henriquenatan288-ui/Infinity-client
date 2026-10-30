package com.witch.module;

import com.witch.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public final String name;
	public final Category category;
	private boolean enabled;
	private final List<Setting> settings = new ArrayList<>();

	public float hoverAnim = 0f;
	public float toggleAnim = 0f;

	protected Module(String name, Category category) {
		this.name = name;
		this.category = category;
	}

	protected <T extends Setting> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting> settings() {
		return settings;
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