package com.witch.module;

public enum Category {
	MOVEMENT("Movement"),
	VISUALS("Visuals"),
	UTILITY("Utility"),
	EFFECTS("Effects");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}