package com.infinity.gui;

import com.infinity.config.Config;
import com.infinity.module.Category;
import com.infinity.module.Module;
import com.infinity.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGuiScreen extends Screen {
	private static final int W = 420;
	private static final int H = 300;
	private static final int PAD = 10;
	private static final int SIDEBAR_W = 120;
	private static final int CAT_H = 28;
	private static final int ROW_H = 20;
	private static final int SEARCH_H = 22;

	private Category selected = Category.VISUALS;
	private String search = "";
	private boolean searchFocused = false;
	private float scroll = 0f;
	private float scrollTarget = 0f;
	private float openAnim = 0f;
	private long lastNanos = 0L;

	private int px, py;

	public ClickGuiScreen() {
		super(Component.literal("Infinity"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private List<Module> visibleModules() {
		if (search.isEmpty()) return ModuleManager.byCategory(selected);
		String q = search.toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
		}
		return out;
	}

	private int listX() { return px + PAD + SIDEBAR_W + PAD; }
	private int listY() { return py + PAD + SEARCH_H + 8; }
	private int listW() { return W - PAD * 3 - SIDEBAR_W; }
	private int listH() { return H - (listY() - py) - PAD; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float speed = lastNanos == 0 ? 1f : Math.min(3f, (now - lastNanos) / 16_666_667f);
		lastNanos = now;

		openAnim = Mth.lerp(Math.min(1f, speed * 0.2f), openAnim, 1f);
		float a = openAnim;

		px = (this.width - W) / 2;
		py = (this.height - H) / 2 + (int) ((1f - a) * 12f);

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x66000000, a));
		RenderUtil.roundRect(g, px, py, W, H, 10, RenderUtil.alpha(Theme.PANEL, a));

		renderSidebar(g, mouseX, mouseY, a);
		renderSearch(g, a);
		renderModules(g, mouseX, mouseY, speed, a);
	}

	private void renderSidebar(GuiGraphicsExtractor g, int mx, int my, float a) {
		int x = px + PAD;
		int y = py + PAD;
		for (Category c : Category.values()) {
			boolean active = c == selected && search.isEmpty();
			boolean hover = RenderUtil.inside(mx, my, x, y, SIDEBAR_W, CAT_H - 4);
			if (active) {
				RenderUtil.roundRect(g, x, y, SIDEBAR_W, CAT_H - 4, 6, RenderUtil.alpha(Theme.SIDEBAR_ITEM_ACTIVE, a));
			} else if (hover) {
				RenderUtil.roundRect(g, x, y, SIDEBAR_W, CAT_H - 4, 6, RenderUtil.alpha(Theme.MODULE_HOVER, a));
			}
			int ty = y + (CAT_H - 4 - FontUtil.height()) / 2;
			FontUtil.draw(g, c.label, x + 12, ty, RenderUtil.alpha(active ? Theme.TEXT : Theme.TEXT_DIM, a));
			y += CAT_H;
		}
	}

	private void renderSearch(GuiGraphicsExtractor g, float a) {
		int x = listX();
		int y = py + PAD;
		int w = listW();
		RenderUtil.roundRect(g, x, y, w, SEARCH_H, 6, RenderUtil.alpha(Theme.SEARCH_BG, a));

		String shown;
		int color;
		if (search.isEmpty() && !searchFocused) {
			shown = "Search all modules...";
			color = Theme.TEXT_DIM;
		} else {
			boolean blink = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
			shown = search + (blink ? "|" : "");
			color = Theme.TEXT;
		}
		FontUtil.draw(g, shown, x + 8, y + (SEARCH_H - FontUtil.height()) / 2, RenderUtil.alpha(color, a));
	}

	private void renderModules(GuiGraphicsExtractor g, int mx, int my, float speed, float a) {
		List<Module> mods = visibleModules();
		int x = listX();
		int y = listY();
		int w = listW();
		int h = listH();

		int contentH = mods.size() * ROW_H;
		float maxScroll = Math.max(0, contentH - h);
		scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
		scroll = Mth.lerp(Math.min(1f, speed * 0.25f), scroll, scrollTarget);

		if (mods.isEmpty()) {
			FontUtil.draw(g, "No modules", x + 8, y + 4, RenderUtil.alpha(Theme.TEXT_DIM, a));
			return;
		}

		g.enableScissor(x, y, x + w, y + h);
		int ry = y - Math.round(scroll);
		for (Module m : mods) {
			boolean hover = RenderUtil.inside(mx, my, x, ry, w, ROW_H - 2)
					&& RenderUtil.inside(mx, my, x, y, w, h);

			m.hoverAnim = Mth.lerp(Math.min(1f, speed * 0.25f), m.hoverAnim, hover ? 1f : 0f);
			m.toggleAnim = Mth.lerp(Math.min(1f, speed * 0.25f), m.toggleAnim, m.isEnabled() ? 1f : 0f);

			if (m.hoverAnim > 0.01f) {
				RenderUtil.roundRect(g, x, ry, w, ROW_H - 2, 5,
						RenderUtil.alpha(Theme.MODULE_HOVER, a * m.hoverAnim));
			}
			if (m.toggleAnim > 0.01f) {
				RenderUtil.roundRect(g, x, ry, w, ROW_H - 2, 5,
						RenderUtil.alpha(Theme.MODULE_ON, a * m.toggleAnim));
			}

			int textColor = RenderUtil.mix(Theme.TEXT_DIM, Theme.TEXT, Math.max(m.toggleAnim, m.hoverAnim));
			FontUtil.draw(g, m.name, x + 8, ry + (ROW_H - 2 - FontUtil.height()) / 2,
					RenderUtil.alpha(textColor, a));
			ry += ROW_H;
		}
		g.disableScissor();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mx = click.x();
		double my = click.y();
		int button = click.button();

		searchFocused = RenderUtil.inside(mx, my, listX(), py + PAD, listW(), SEARCH_H);
		if (searchFocused) return true;

		int x = px + PAD;
		int y = py + PAD;
		for (Category c : Category.values()) {
			if (RenderUtil.inside(mx, my, x, y, SIDEBAR_W, CAT_H - 4)) {
				selected = c;
				search = "";
				scroll = 0f;
				scrollTarget = 0f;
				return true;
			}
			y += CAT_H;
		}

		int lx = listX();
		int ly = listY();
		int lw = listW();
		int lh = listH();
		if (RenderUtil.inside(mx, my, lx, ly, lw, lh)) {
			int ry = ly - Math.round(scroll);
			for (Module m : visibleModules()) {
				if (RenderUtil.inside(mx, my, lx, ry, lw, ROW_H - 2)) {
					if (button == 0) {
						m.toggle();
						Config.save();
					}
					return true;
				}
				ry += ROW_H;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scrollTarget -= (float) (verticalAmount * 16.0);
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		int cp = input.codepoint();
		if (searchFocused && cp >= 32 && cp != 127 && search.length() < 24) {
			search += new String(Character.toChars(cp));
			scroll = 0f;
			scrollTarget = 0f;
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		int key = input.key();
		if (searchFocused) {
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				searchFocused = false;
				return true;
			}
		}
		if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			this.onClose();
			return true;
		}
		return super.keyPressed(input);
	}
}