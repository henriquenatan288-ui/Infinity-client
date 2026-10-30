package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Corrida mais rapida no chao/ar (nao mexe na agua nem no voo). */
public class SpeedModule extends Module {
	private static final double BASE = 0.2806; // velocidade de sprint, blocos/tick
	private final IntSetting boost = add(new IntSetting("Boost", 0, 100, 30, "%"));

	public SpeedModule() {
		super("Speed", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || p.isSpectator() || p.isPassenger() || p.isInWater() || p.getAbilities().flying) return;

		float fwd = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
		float str = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
		if (fwd == 0f && str == 0f) return;

		double yaw = Math.toRadians(p.getYRot());
		double dx = -Math.sin(yaw) * fwd + Math.cos(yaw) * str;
		double dz = Math.cos(yaw) * fwd + Math.sin(yaw) * str;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-6) return;
		dx /= len;
		dz /= len;

		double sp = BASE * (1.0 + boost.get() / 100.0);
		Vec3 v = p.getDeltaMovement();
		p.setDeltaMovement(dx * sp, v.y, dz * sp);
	}
}