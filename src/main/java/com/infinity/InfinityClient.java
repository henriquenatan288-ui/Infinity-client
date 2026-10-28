package com.infinity;

import net.fabricmc.api.ClientModInitializer;

public class InfinityClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		System.out.println("[Infinity] carregado");
	}
}