package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;

/** Voo estilo creative (abilities). Em servidor sem permissao de voo, ele te kicka. */
public class FlyModule extends Module {
	private final IntSetting speed = add(new IntSetting("Speed", 1, 10, 2, "x"));

	public FlyModule() {
		super("Fly", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null || p.isSpectator()) return;
		var ab = p.getAbilities();
		ab.mayfly = true;
		ab.flying = true;
		ab.setFlyingSpeed(speed.get() * 0.05f);
	}

	@Override
	protected void onDisable() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		var ab = p.getAbilities();
		ab.setFlyingSpeed(0.05f);
		if (ab.instabuild || p.isSpectator()) return;
		ab.flying = false;
		ab.mayfly = false;
	}
}