package com.witch;

import com.witch.config.Config;
import com.witch.gui.ClickGuiScreen;
import com.witch.gui.HudRenderer;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.render.XRayRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class WitchClient implements ClientModInitializer {
	private static KeyMapping openGui;
	private static KeyMapping openXraySettings;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		Config.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("witch", "main"));

		openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.witch.gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category
		));

		// Sem tecla por padrao: voce escolhe em Controles > Witch.
		openXraySettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.witch.xray_settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGui.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new ClickGuiScreen());
				}
			}
			while (openXraySettings.consumeClick()) {
				if (client.gui.screen() == null) {
					Module xray = ModuleManager.byName("X-Ray Hologram");
					client.gui.setScreen(new ClickGuiScreen(xray));
				}
			}
			if (client.player != null) {
				ModuleManager.pollKeys(client);
				ModuleManager.tick();
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("witch", "module_list"),
				HudRenderer::extract
		);

		XRayRenderer.init();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			Config.save();
			XRayRenderer.close();
		});
	}
}