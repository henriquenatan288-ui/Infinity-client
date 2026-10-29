package com.infinity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.infinity.module.Module;
import com.infinity.module.ModuleManager;
import com.infinity.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Config() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("infinity.json");
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			JsonObject s = new JsonObject();
			for (Setting st : m.settings()) {
				s.addProperty(st.name, st.toNumber());
			}
			o.add("settings", s);
			root.add(m.name, o);
		}
		try (Writer w = Files.newBufferedWriter(file())) {
			GSON.toJson(root, w);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			JsonElement parsed = JsonParser.parseReader(r);
			if (parsed == null || !parsed.isJsonObject()) return;
			for (Map.Entry<String, JsonElement> e : parsed.getAsJsonObject().entrySet()) {
				Module m = ModuleManager.byName(e.getKey());
				if (m == null) continue;
				JsonElement el = e.getValue();
				if (el.isJsonPrimitive()) {
					// formato antigo: so true/false
					m.setEnabled(el.getAsBoolean());
				} else if (el.isJsonObject()) {
					JsonObject o = el.getAsJsonObject();
					if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
					if (o.has("settings") && o.get("settings").isJsonObject()) {
						JsonObject s = o.getAsJsonObject("settings");
						for (Setting st : m.settings()) {
							if (s.has(st.name)) st.fromNumber(s.get(st.name).getAsDouble());
						}
					}
				}
				m.toggleAnim = m.isEnabled() ? 1f : 0f;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}