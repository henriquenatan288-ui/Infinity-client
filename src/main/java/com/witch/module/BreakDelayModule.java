package com.witch.module;

import com.witch.setting.IntSetting;

/** Diminui o intervalo (em ticks) entre quebrar um bloco e o proximo. Vanilla = 5. A logica fica no MultiPlayerGameModeMixin. */
public class BreakDelayModule extends Module {
	public static BreakDelayModule INSTANCE;
	private final IntSetting cooldown = add(new IntSetting("Cooldown", 0, 5, 0, " ticks"));

	public BreakDelayModule() {
		super("Break Delay", Category.UTILITY);
		INSTANCE = this;
	}

	/** Valor do cooldown se o modulo estiver ligado, senao -1. */
	public static int cooldownOrVanilla() {
		return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE.cooldown.get() : -1;
	}
}
