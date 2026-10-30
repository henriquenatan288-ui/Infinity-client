package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Linhas do centro da tela ate jogadores e (se o Storage ESP estiver ligado) ate os containers. */
public class TracersModule extends Module implements EspSource {
	private final IntSetting range = add(new IntSetting("Player range", 16, 256, 128, ""));
	private final OreSetting players = add(new OreSetting("Players", true, 5));
	private final OreSetting storage = add(new OreSetting("Storage", true, 4));

	public TracersModule() {
		super("Tracers", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		var me = mc.player;
		if (level == null || me == null) return;

		if (players.enabled()) {
			double r2 = (double) range.get() * range.get();
			float pt = esp.partial();
			int argb = players.color();
			for (Player p : level.players()) {
				if (p == me || p.isSpectator()) continue;
				double x = Mth.lerp(pt, p.xOld, p.getX());
				double y = Mth.lerp(pt, p.yOld, p.getY());
				double z = Mth.lerp(pt, p.zOld, p.getZ());
				double dx = x - me.getX();
				double dy = y - me.getY();
				double dz = z - me.getZ();
				if (dx * dx + dy * dy + dz * dz > r2) continue;
				esp.tracer(x, y + p.getBbHeight() * 0.5, z, argb);
			}
		}

		if (storage.enabled()) {
			int argb = storage.color();
			for (int[] b : StorageEspModule.blocks()) {
				esp.tracer(b[0] + 0.5, b[1] + 0.5, b[2] + 0.5, argb);
			}
		}
	}
}