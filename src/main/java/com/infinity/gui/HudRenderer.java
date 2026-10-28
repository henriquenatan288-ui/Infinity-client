package com.infinity.gui;

import com.infinity.module.Module;
import com.infinity.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer {
	private HudRenderer() {}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.gui.screen() instanceof ClickGuiScreen) return;

		int x = 6;
		int y = 6;
		for (Module m : ModuleManager.all()) {
			if (!m.isEnabled()) continue;
			int w = FontUtil.width(m.name) + 12;
			RenderUtil.roundRect(g, x, y, w, 14, 4, 0xB0201830);
			RenderUtil.roundRect(g, x, y, 3, 14, 1, Theme.ACCENT);
			FontUtil.draw(g, m.name, x + 8, y + (14 - FontUtil.height()) / 2, Theme.TEXT);
			y += 16;
		}
	}
}