package com.witch.module;

/** Ao quebrar um bloco, o cliente espera o servidor confirmar antes de remove-lo da tela. A logica fica no MultiPlayerGameModeMixin. */
public class NoGhostBlocksModule extends Module {
	public static NoGhostBlocksModule INSTANCE;

	public NoGhostBlocksModule() {
		super("No Ghost Blocks", Category.UTILITY);
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
