package com.infinity.module;

public enum Category {
	MOVEMENT("Movement"),
	VISUALS("Visuals"),
	UTILITY("Utility");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}