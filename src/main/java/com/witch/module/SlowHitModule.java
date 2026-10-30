package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;

/** So o visual: deixa a animacao do braco lenta, como na fadiga de mineracao. Nao muda o dano nem a velocidade de quebrar. */
public class SlowHitModule extends Module {
	private final IntSetting speed = add(new IntSetting("Swing speed", 20, 100, 50, "%"));
	private float acc = 0f;

	public SlowHitModule() {
		super("Slow Hit Visual", Category.VISUALS);
	}

	@Override
	protected void onDisable() {
		acc = 0f;
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		if (!p.swinging) {
			acc = 0f;
			return;
		}
		acc += 1f - speed.get() / 100f;
		if (acc >= 1f) {
			acc -= 1f;
			if (p.swingTime > 0) p.swingTime--;
		}
	}
}