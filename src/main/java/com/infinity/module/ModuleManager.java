package com.infinity.module;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {}

	public static void init() {
		MODULES.clear();
		MODULES.add(new XRayModule());
		MODULES.add(new FlyModule());
		MODULES.add(new SpeedModule());
		MODULES.add(new JesusModule());
		MODULES.add(new AutoEatModule());
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> byCategory(Category category) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) out.add(m);
		}
		return out;
	}

	public static Module byName(String name) {
		for (Module m : MODULES) {
			if (m.name.equals(name)) return m;
		}
		return null;
	}

	public static void tick() {
		for (Module m : MODULES) {
			if (m.isEnabled()) m.onTick();
		}
	}
}