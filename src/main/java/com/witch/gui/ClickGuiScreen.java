package com.witch.gui;

import com.witch.config.Config;
import com.witch.module.Category;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import com.witch.setting.Setting;
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
	private static final int MAX_W = 420;
	private static final int MAX_H = 300;
	private static final int MARGIN = 8;
	private static final int PAD = 8;
	private static final int SIDEBAR_W = 100;
	private static final int CAT_H = 24;
	private static final int ROW_H = 20;
	private static final int SEARCH_H = 20;
	private static final int DOTS_W = 30;
	private static final int INT_ROW_H = 30;
	private static final int COLOR_ROW_H = 38;
	private static final int ORE_ROW_H = 18;
	private static final int CHIP_W = 28;
	private static final int SWATCH = 14;
	private static final int SWATCH_GAP = 5;

	private final LeafParticles leaves = new LeafParticles();

	private Category selected = Category.VISUALS;
	private Module settingsOf = null;
	private IntSetting dragging = null;
	private int dragX = 0;
	private int dragW = 1;
	private Runnable pendingClick = null;
	private double dragAccum = 0;

	private String search = "";
	private boolean searchFocused = false;
	private float scroll = 0f;
	private float scrollTarget = 0f;
	private float openAnim = 0f;
	private long lastNanos = 0L;

	private int panelW = MAX_W;
	private int panelH = MAX_H;
	private int px, py;

	public ClickGuiScreen() {
		this(null);
	}

	/** Se 'openSettings' nao for nulo, a tela ja abre nos ajustes desse modulo. */
	public ClickGuiScreen(Module openSettings) {
		super(Component.literal("Witch Client"));
		if (openSettings != null && !openSettings.settings().isEmpty()) {
			this.settingsOf = openSettings;
			this.selected = openSettings.category;
		}
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
	private int listY() { return py + PAD + SEARCH_H + 6; }
	private int listW() { return panelW - PAD * 3 - SIDEBAR_W; }
	private int listH() { return panelH - (listY() - py) - PAD; }

	private static int rowH(Setting s) {
		if (s instanceof OreSetting) return ORE_ROW_H;
		return s instanceof ColorSetting ? COLOR_ROW_H : INT_ROW_H;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float speed = lastNanos == 0 ? 1f : Math.min(3f, (now - lastNanos) / 16_666_667f);
		lastNanos = now;

		TimeTheme.update();

		openAnim = Mth.lerp(Math.min(1f, speed * 0.2f), openAnim, 1f);
		float a = openAnim;

		panelW = Math.max(220, Math.min(MAX_W, this.width - MARGIN * 2));
		panelH = Math.max(120, Math.min(MAX_H, this.height - MARGIN * 2));
		px = (this.width - panelW) / 2;
		py = (this.height - panelH) / 2 + (int) ((1f - a) * 8f);

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x44000000, a));
		GlassShader.shadow(g, px, py, panelW, panelH, a);
		RenderUtil.roundRect(g, px, py, panelW, panelH, 8, RenderUtil.alpha(TimeTheme.panel, a));
		GlassShader.glass(g, px, py, panelW, panelH, a);
		GlassShader.sidebar(g, px + PAD, py + PAD, SIDEBAR_W, panelH - PAD * 2, a);

		GuiVisuals.render(g, px, py, panelW, panelH, a);

		leaves.update(speed, panelW, panelH);
		leaves.render(g, px, py, panelW, panelH, a);

		renderSidebar(g, mouseX, mouseY, a);
		renderSearch(g, a);
		if (settingsOf != null && search.isEmpty()) {
			renderSettings(g, mouseX, mouseY, speed, a);
		} else {
			renderModules(g, mouseX, mouseY, speed, a);
		}
	}

	private void renderSidebar(GuiGraphicsExtractor g, int mx, int my, float a) {
		int x = px + PAD;
		int y = py + PAD;
		for (Category c : Category.values()) {
			boolean active = c == selected && search.isEmpty();
			boolean hover = RenderUtil.inside(mx, my, x, y, SIDEBAR_W, CAT_H - 4);
			if (active) {
				RenderUtil.roundRect(g, x, y, SIDEBAR_W, CAT_H - 4, 5, RenderUtil.alpha(Theme.SIDEBAR_ITEM_ACTIVE, a));
			} else if (hover) {
				RenderUtil.roundRect(g, x, y, SIDEBAR_W, CAT_H - 4, 5, RenderUtil.alpha(TimeTheme.hover, a));
			}
			int ty = y + (CAT_H - 4 - FontUtil.height()) / 2;
			FontUtil.draw(g, c.label, x + 10, ty, RenderUtil.alpha(active ? Theme.TEXT : TimeTheme.dim, a));
			y += CAT_H;
		}
	}

	private void renderSearch(GuiGraphicsExtractor g, float a) {
		int x = listX();
		int y = py + PAD;
		int w = listW();
		RenderUtil.roundRect(g, x, y, w, SEARCH_H, 5, RenderUtil.alpha(TimeTheme.search, a));

		String shown;
		int color;
		if (search.isEmpty() && !searchFocused) {
			shown = "Search all modules...";
			color = TimeTheme.placeholder;
		} else {
			boolean blink = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
			shown = search + (blink ? "|" : "");
			color = TimeTheme.input;
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
			FontUtil.draw(g, "No modules", x + 8, y + 4, RenderUtil.alpha(TimeTheme.placeholder, a));
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
						RenderUtil.alpha(TimeTheme.hover, a * m.hoverAnim));
			}
			if (m.toggleAnim > 0.01f) {
				RenderUtil.roundRect(g, x, ry, w, ROW_H - 2, 5,
						RenderUtil.alpha(Theme.MODULE_ON, a * m.toggleAnim));
			}

			int textColor = RenderUtil.mix(TimeTheme.dim, Theme.TEXT, m.toggleAnim);
			int ty = ry + (ROW_H - 2 - FontUtil.height()) / 2;
			FontUtil.draw(g, m.name, x + 8, ty, RenderUtil.alpha(textColor, a));
			if (!m.settings().isEmpty()) {
				String dots = "...";
				FontUtil.draw(g, dots, x + w - 8 - FontUtil.width(dots), ty, RenderUtil.alpha(textColor, a));
			}
			ry += ROW_H;
		}
		g.disableScissor();
	}

	private void renderSettings(GuiGraphicsExtractor g, int mx, int my, float speed, float a) {
		int x = listX();
		int y = listY();
		int w = listW();
		int h = listH();
		int fh = FontUtil.height();

		// linha "< Modulo" fica fixa no topo
		boolean hoverBack = RenderUtil.inside(mx, my, x, y, w, ROW_H - 2);
		if (hoverBack) {
			RenderUtil.roundRect(g, x, y, w, ROW_H - 2, 5, RenderUtil.alpha(TimeTheme.hover, a));
		}
		FontUtil.draw(g, "< " + settingsOf.name, x + 8, y + (ROW_H - 2 - fh) / 2, RenderUtil.alpha(TimeTheme.dim, a));

		int ly = y + ROW_H;
		int lh = h - ROW_H;
		int contentH = 4;
		for (Setting s : settingsOf.settings()) {
			contentH += rowH(s);
		}
		float maxScroll = Math.max(0, contentH - lh);
		scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
		scroll = Mth.lerp(Math.min(1f, speed * 0.25f), scroll, scrollTarget);

		g.enableScissor(x, ly, x + w, ly + lh);
		int ry = ly + 4 - Math.round(scroll);
		for (Setting s : settingsOf.settings()) {
			if (s instanceof IntSetting is) {
				FontUtil.draw(g, is.name, x + 8, ry, RenderUtil.alpha(TimeTheme.dim, a));
				String val = is.get() + is.suffix;
				FontUtil.draw(g, val, x + w - 8 - FontUtil.width(val), ry, RenderUtil.alpha(TimeTheme.input, a));

				int tx = x + 8;
				int tw = w - 16;
				int ty = ry + fh + 6;
				RenderUtil.roundRect(g, tx, ty, tw, 6, 3, RenderUtil.alpha(TimeTheme.search, a));
				int fw = Math.round(tw * is.fraction());
				if (fw > 0) {
					RenderUtil.roundRect(g, tx, ty, Math.max(fw, 4), 6, 3, RenderUtil.alpha(Theme.ACCENT, a));
				}
				RenderUtil.roundRect(g, tx + fw - 4, ty - 2, 8, 10, 3, RenderUtil.alpha(Theme.ACCENT, a));
			} else if (s instanceof ColorSetting cs) {
				FontUtil.draw(g, cs.name, x + 8, ry, RenderUtil.alpha(TimeTheme.dim, a));
				int sy = ry + fh + 6;
				for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
					int sx = x + 8 + i * (SWATCH + SWATCH_GAP);
					if (i == cs.index()) {
						RenderUtil.roundRect(g, sx - 2, sy - 2, SWATCH + 4, SWATCH + 4, 4,
								RenderUtil.alpha(TimeTheme.dim, a));
					}
					RenderUtil.roundRect(g, sx, sy, SWATCH, SWATCH, 3,
							RenderUtil.alpha(ColorSetting.PALETTE[i], a));
				}
			} else if (s instanceof OreSetting os) {
				int rh = ORE_ROW_H - 2;
				boolean hover = RenderUtil.inside(mx, my, x, ry, w, rh)
						&& RenderUtil.inside(mx, my, x, ly, w, lh);
				if (hover) {
					RenderUtil.roundRect(g, x, ry, w, rh, 5, RenderUtil.alpha(TimeTheme.hover, a));
				}
				int bx = x + 8;
				int by = ry + (rh - 12) / 2;
				RenderUtil.roundRect(g, bx, by, 12, 12, 3, RenderUtil.alpha(TimeTheme.search, a));
				if (os.enabled()) {
					RenderUtil.roundRect(g, bx + 2, by + 2, 8, 8, 2, RenderUtil.alpha(Theme.ACCENT, a));
				}
				int textColor = os.enabled() ? TimeTheme.input : TimeTheme.placeholder;
				FontUtil.draw(g, os.name, bx + 18, ry + (rh - fh) / 2, RenderUtil.alpha(textColor, a));

				int cx = x + w - 8 - CHIP_W;
				RenderUtil.roundRect(g, cx - 1, by - 2, CHIP_W + 2, 16, 5, RenderUtil.alpha(TimeTheme.dim, a * 0.5f));
				RenderUtil.roundRect(g, cx, by - 1, CHIP_W, 14, 4, RenderUtil.alpha(os.color(), a));
			}
			ry += rowH(s);
		}
		g.disableScissor();

		// barra de rolagem
		if (maxScroll > 0) {
			int thumbH = Math.max(10, Math.round(lh * (lh / (float) contentH)));
			int thumbY = ly + Math.round((lh - thumbH) * (scroll / maxScroll));
			RenderUtil.roundRect(g, x + w - 3, thumbY, 2, thumbH, 1, RenderUtil.alpha(TimeTheme.dim, a * 0.6f));
		}
	}

	private boolean clickSettings(double mx, double my) {
		int x = listX();
		int y = listY();
		int w = listW();
		int h = listH();
		int fh = FontUtil.height();

		if (RenderUtil.inside(mx, my, x, y, w, ROW_H - 2)) {
			settingsOf = null;
			scroll = 0f;
			scrollTarget = 0f;
			return true;
		}

		int ly = y + ROW_H;
		int lh = h - ROW_H;
		dragAccum = 0;
		if (!RenderUtil.inside(mx, my, x, ly, w, lh)) return true;

		int ry = ly + 4 - Math.round(scroll);
		for (Setting s : settingsOf.settings()) {
			if (s instanceof IntSetting is) {
				int tx = x + 8;
				int tw = w - 16;
				int ty = ry + fh + 6;
				if (RenderUtil.inside(mx, my, tx - 4, ty - 6, tw + 8, 18)) {
					dragging = is;
					dragX = tx;
					dragW = Math.max(1, tw);
					is.setFraction((float) ((mx - tx) / dragW));
					return true;
				}
			} else if (s instanceof ColorSetting cs) {
				int sy = ry + fh + 6;
				for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
					int sx = x + 8 + i * (SWATCH + SWATCH_GAP);
					if (RenderUtil.inside(mx, my, sx - 2, sy - 2, SWATCH + 4, SWATCH + 4)) {
						cs.setIndex(i);
						Config.save();
						return true;
					}
				}
			} else if (s instanceof OreSetting os) {
				if (RenderUtil.inside(mx, my, x, ry, w, ORE_ROW_H - 2)) {
					final boolean onChip = mx >= x + w - 8 - CHIP_W - 4;
					// so age ao soltar, e so se o dedo nao arrastou (arrastar = rolar a lista)
					pendingClick = () -> {
						if (onChip) os.cycleColor(); else os.toggle();
						Config.save();
					};
					return true;
				}
			}
			ry += rowH(s);
		}
		return true;
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
				settingsOf = null;
				search = "";
				scroll = 0f;
				scrollTarget = 0f;
				return true;
			}
			y += CAT_H;
		}

		if (settingsOf != null && search.isEmpty()) {
			if (RenderUtil.inside(mx, my, listX(), listY(), listW(), listH())) {
				return clickSettings(mx, my);
			}
		} else {
			int lx = listX();
			int ly = listY();
			int lw = listW();
			int lh = listH();
			if (RenderUtil.inside(mx, my, lx, ly, lw, lh)) {
				int ry = ly - Math.round(scroll);
				for (Module m : visibleModules()) {
					if (RenderUtil.inside(mx, my, lx, ry, lw, ROW_H - 2)) {
						boolean hasSettings = !m.settings().isEmpty();
						boolean onDots = hasSettings && mx >= lx + lw - DOTS_W;
						if (hasSettings && (button == 1 || (button == 0 && onDots))) {
							settingsOf = m;
							search = "";
							scroll = 0f;
							scrollTarget = 0f;
							return true;
						}
						if (button == 0) {
							m.toggle();
							Config.save();
						}
						return true;
					}
					ry += ROW_H;
				}
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (dragging != null) {
			dragging.setFraction((float) ((click.x() - dragX) / dragW));
			return true;
		}
		if (settingsOf != null && search.isEmpty()) {
			dragAccum += Math.abs(offsetX) + Math.abs(offsetY);
			scrollTarget -= (float) offsetY;
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (dragging != null) {
			dragging = null;
			Config.save();
			return true;
		}
		if (pendingClick != null) {
			Runnable r = pendingClick;
			pendingClick = null;
			if (dragAccum < 6) r.run();
			dragAccum = 0;
			return true;
		}
		return super.mouseReleased(click);
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