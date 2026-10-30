package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Sobe e flutua na superficie da agua/lava (segure Shift pra afundar). Nao e andar "em cima" da agua de verdade. */
public class JesusModule extends Module {
	public JesusModule() {
		super("Jesus", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || p.isSpectator() || p.isPassenger() || p.getAbilities().flying) return;
		if (!(p.isInWater() || p.isInLava())) return;
		if (mc.options.keyShift.isDown()) return;

		Vec3 v = p.getDeltaMovement();
		if (v.y < 0.11) {
			p.setDeltaMovement(v.x, 0.11, v.z);
		}
	}
}