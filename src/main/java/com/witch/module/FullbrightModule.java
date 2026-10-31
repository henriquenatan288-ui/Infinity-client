package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Ilumina o mundo com uma animacao: o brilho do jogo sobe de 0 ate 100% em alguns segundos
 * e, no fim, liga a visao noturna (efeito so no seu cliente) para clarear de vez.
 */
public class FullbrightModule extends Module {
	private final IntSetting fade = add(new IntSetting("Fade time", 1, 5, 2, " s"));
	private final IntSetting nightVision = add(new IntSetting("Night vision (0/1)", 0, 1, 1, ""));

	private double original = 0.5;
	private int ticks = 0;

	public FullbrightModule() {
		super("Fullbright", Category.VISUALS);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		original = mc.options.gamma().get();
		ticks = 0;
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		mc.options.gamma().set(original);
		if (mc.player != null) {
			mc.player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()));
		}
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		ticks++;
		float t = Math.min(1f, ticks / (fade.get() * 20f));
		float eased = t * t * (3f - 2f * t);
		mc.options.gamma().set(original + (1.0 - original) * eased);

		if (nightVision.get() == 1 && t >= 1f && mc.player != null) {
			var holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value());
			var cur = mc.player.getEffect(holder);
			if (cur == null || cur.getDuration() < 300) {
				mc.player.addEffect(new MobEffectInstance(holder, 420, 0));
			}
		}
	}
}
