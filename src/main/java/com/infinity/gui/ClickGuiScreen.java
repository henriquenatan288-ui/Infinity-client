package com.infinity.gui;

import com.infinity.config.Config;
import com.infinity.module.Category;
import com.infinity.module.Module;
import com.infinity.module.ModuleManager;
import com.infinity.setting.ColorSetting;
import com.infinity.setting.IntSetting;
import com.infinity.setting.Setting;
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
import java.util.Random;

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
	private static final int SWATCH = 14;
	private static final int SWATCH_GAP = 5;
	private static final int BODY_R = 12;

	private static final float[][] STARS = makeStars();

	private final LeafParticles leaves = new LeafParticles();

	private Category selected = Category.VISUALS;
	private Module settingsOf = null;
	private IntSetting dragging = null;
	private int dragX = 0;
	private int dragW = 1;

	private String search = "";
	private boolean searchFocused = false;
	private float scroll = 0f;
	private float scrollTarget = 0f;
	private float openAnim = 0f;
	private long lastNanos = 0L;

	private int panelW = MAX_W;
	private int panelH = MAX_H;
	private int px, py;
	private int bodyX, bodyY;

	public ClickGuiScreen() {
		this(null);
	}

	/** Se 'openSettings' nao for nulo, a tela ja abre nos ajustes desse modulo. */
	public ClickGuiScreen(Module openSettings) {
		super(Component.literal("Infinity"));
		if (openSettings != null && !openSettings.settings().isEmpty()) {
			this.settingsOf = openSettings;
			this.selected = openSettings.category;
		}
	}

	private static float[][] makeStars() {
		Random r = new Random(42);
		float[][] s = new float[14][2];
		for (float[] star : s) {
			star[0] = r.nextFloat();
			star[1] = r.nextFloat();
		}
		return s;
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
		RenderUtil.roundRect(g, px, py, panelW, panelH, 8, RenderUtil.alpha(TimeTheme.panel, a));

		renderSky(g, a);

		leaves.update(speed, panelW, panelH);
		leaves.render(g, px, py, panelW, panelH, a);

		renderSidebar(g, mouseX, mouseY, a);
		renderSearch(g, a);
		if (settingsOf != null && search.isEmpty()) {
			renderSettings(g, mouseX, mouseY, a);
		} else {
			renderModules(g, mouseX, mouseY, speed, a);
		}
	}

	private void renderSky(GuiGraphicsExtractor g, float a) {
		if (TimeTheme.night > 0.05f) {
			float t = System.currentTimeMillis() / 1000f;
			for (int i = 0; i < STARS.length; i++) {
				float tw = 0.5f + 0.5f * (float) Math.sin(t * 2f + i * 1.7f);
				int sx = px + 10 + Math.round(STARS[i][0] * (panelW - 20));
				int sy = py + 8 + Math.round(STARS[i][1] * (panelH * 0.6f));
				int c = RenderUtil.alpha(0xFFFFFFFF, a * TimeTheme.night * (0.35f + 0.5f * tw));
				g.fill(sx, sy, sx + 2, sy + 2, c);
			}
		}

		float t = TimeTheme.bodyT;
		float lift = (float) Math.sin(Math.PI * t);
		bodyX = px + 18 + Math.round(t * (panelW - 36));
		bodyY = Math.max(py + 20, py + panelH - 18 - Math.round(lift * panelH * 0.45f));

		if (TimeTheme.sunUp) {
			float low = Mth.clamp((1f - lift) * 1.6f, 0f, 1f);
			int col = RenderUtil.mix(0xFFFFC83D, 0xFFFF7A3D, low);
			RenderUtil.roundRect(g, bodyX - 22, bodyY - 22, 44, 44, 22, RenderUtil.alpha(col, a * 0.18f));
			RenderUtil.roundRect(g, bodyX - BODY_R, bodyY - BODY_R, BODY_R * 2, BODY_R * 2, BODY_R,
					RenderUtil.alpha(col, a * 0.85f));
		} else {
			RenderUtil.roundRect(g, bodyX - 22, bodyY - 22, 44, 44, 22, RenderUtil.alpha(0xFFB9A4FF, a * 0.12f));
			RenderUtil.roundRect(g, bodyX - BODY_R, bodyY - BODY_R, BODY_R * 2, BODY_R * 2, BODY_R,
					RenderUtil.alpha(0xFFE9E1FF, a * 0.9f));
			// recorte pra virar uma lua crescente
			RenderUtil.roundRect(g, bodyX - BODY_R + 6, bodyY - BODY_R - 3, BODY_R * 2 - 2, BODY_R * 2 - 2,
					BODY_R - 1, RenderUtil.alpha(TimeTheme.panel, a));
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

	private void renderSettings(GuiGraphicsExtractor g, int mx, int my, float a) {
		int x = listX();
		int y = listY();
		int w = listW();
		int h = listH();
		int fh = FontUtil.height();

		g.enableScissor(x, y, x + w, y + h);

		boolean hoverBack = RenderUtil.inside(mx, my, x, y, w, ROW_H - 2);
		if (hoverBack) {
			RenderUtil.roundRect(g, x, y, w, ROW_H - 2, 5, RenderUtil.alpha(TimeTheme.hover, a));
		}
		FontUtil.draw(g, "< " + settingsOf.name, x + 8, y + (ROW_H - 2 - fh) / 2, RenderUtil.alpha(TimeTheme.dim, a));

		int ry = y + ROW_H + 4;
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
			}
			ry += rowH(s);
		}
		g.disableScissor();
	}

	private boolean clickSettings(double mx, double my) {
		int x = listX();
		int y = listY();
		int w = listW();
		int fh = FontUtil.height();

		if (RenderUtil.inside(mx, my, x, y, w, ROW_H - 2)) {
			settingsOf = null;
			return true;
		}

		int ry = y + ROW_H + 4;
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

		// toque no sol/lua (em area vazia): alterna Auto, Dia, Por do sol, Noite
		if (RenderUtil.inside(mx, my, bodyX - 16, bodyY - 16, 32, 32)) {
			TimeTheme.cycleOverride();
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (dragging != null) {
			dragging.setFraction((float) ((click.x() - dragX) / dragW));
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