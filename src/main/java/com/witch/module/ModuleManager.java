package com.witch.module;

import com.witch.render.EspRenderer;
import com.witch.render.EspSource;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {}

	public static void init() {
		MODULES.clear();
		EspRenderer.clearSources();

		MODULES.add(new XRayModule());
		MODULES.add(new PlayerEspModule());
		MODULES.add(new StorageEspModule());
		MODULES.add(new FarmEspModule());
		MODULES.add(new TracersModule());
		MODULES.add(new SusHoleFinderModule());
		MODULES.add(new SusChunkFinderModule());
		MODULES.add(new SlowHitModule());
		MODULES.add(new NoGhostBlocksModule());
		MODULES.add(new BreakDelayModule());
		MODULES.add(new ParticlesModule());
		MODULES.add(new FlyModule());
		MODULES.add(new SpeedModule());
		MODULES.add(new JesusModule());
		MODULES.add(new AutoEatModule());
		MODULES.add(new AutoToolModule());

		for (Module m : MODULES) {
			if (m instanceof EspSource s) {
				EspRenderer.register(s);
			}
		}
		EspRenderer.init();
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