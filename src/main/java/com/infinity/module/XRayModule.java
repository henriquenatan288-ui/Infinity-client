package com.infinity.module;

public class XRayModule extends Module {
	private static XRayModule instance;

	public XRayModule() {
		super("X-Ray Hologram", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}