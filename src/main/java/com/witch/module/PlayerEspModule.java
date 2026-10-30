package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Caixa em volta dos outros jogadores, visivel atraves de parede. */
public class PlayerEspModule extends Module implements EspSource {
	private final IntSetting range = add(new IntSetting("Range", 16, 128, 64, ""));
	private final IntSetting fill = add(new IntSetting("Fill opacity", 0, 40, 10, "%"));
	private final ColorSetting color = add(new ColorSetting("Color", 5));

	public PlayerEspModule() {
		super("Player ESP", Category.VISUALS);
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

		double r2 = (double) range.get() * range.get();
		int argb = color.get();
		float fillA = fill.get() / 100f;
		float pt = esp.partial();

		for (Player p : level.players()) {
			if (p == me || p.isSpectator()) continue;
			double x = Mth.lerp(pt, p.xOld, p.getX());
			double y = Mth.lerp(pt, p.yOld, p.getY());
			double z = Mth.lerp(pt, p.zOld, p.getZ());
			double dx = x - me.getX();
			double dy = y - me.getY();
			double dz = z - me.getZ();
			if (dx * dx + dy * dy + dz * dz > r2) continue;
			double hw = p.getBbWidth() / 2.0;
			esp.box(x - hw, y, z - hw, x + hw, y + p.getBbHeight(), z + hw, argb, fillA);
		}
	}
}