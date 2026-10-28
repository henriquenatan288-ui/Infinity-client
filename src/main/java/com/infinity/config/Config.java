package com.infinity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.infinity.module.Module;
import com.infinity.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();

	private Config() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("infinity.json");
	}

	public static void save() {
		Map<String, Boolean> data = new LinkedHashMap<>();
		for (Module m : ModuleManager.all()) {
			data.put(m.name, m.isEnabled());
		}
		try (Writer w = Files.newBufferedWriter(file())) {
			GSON.toJson(data, TYPE, w);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			Map<String, Boolean> data = GSON.fromJson(r, TYPE);
			if (data == null) return;
			for (Map.Entry<String, Boolean> e : data.entrySet()) {
				Module m = ModuleManager.byName(e.getKey());
				if (m != null) {
					m.setEnabled(Boolean.TRUE.equals(e.getValue()));
					m.toggleAnim = m.isEnabled() ? 1f : 0f;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}