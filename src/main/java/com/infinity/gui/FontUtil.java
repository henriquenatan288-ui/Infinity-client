package com.infinity.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class FontUtil {
	// Troque pra true quando o inter.ttf estiver no repositorio.
	private static final boolean USE_CUSTOM_FONT = false;

	private static final Style STYLE = USE_CUSTOM_FONT
			? Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("infinity", "inter")))
			: Style.EMPTY;

	private FontUtil() {}

	public static Component text(String s) {
		return Component.literal(s).setStyle(STYLE);
	}

	public static void draw(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		g.text(Minecraft.getInstance().font, text(s), x, y, color, false);
	}

	public static int width(String s) {
		return Minecraft.getInstance().font.width(text(s));
	}

	public static int height() {
		return Minecraft.getInstance().font.lineHeight;
	}
}