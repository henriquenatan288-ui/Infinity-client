package com.witch.setting;

import org.lwjgl.glfw.GLFW;

/** Tecla que liga/desliga o modulo. Guarda o codigo GLFW (0 = sem tecla). Salva como numero. */
public final class KeybindSetting extends Setting {
	private int key = 0;
	public boolean listening = false;
	public boolean wasDown = false;

	public KeybindSetting() {
		super("Keybind");
	}

	public int key() {
		return key;
	}

	public void setKey(int k) {
		key = Math.max(0, k);
	}

	public String label() {
		if (listening) return "Press a key...";
		if (key == 0) return "None";
		switch (key) {
			case GLFW.GLFW_KEY_SPACE: return "Space";
			case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-Shift";
			case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-Shift";
			case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-Ctrl";
			case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-Ctrl";
			case GLFW.GLFW_KEY_LEFT_ALT: return "L-Alt";
			case GLFW.GLFW_KEY_RIGHT_ALT: return "R-Alt";
			case GLFW.GLFW_KEY_TAB: return "Tab";
			case GLFW.GLFW_KEY_ENTER: return "Enter";
			case GLFW.GLFW_KEY_UP: return "Up";
			case GLFW.GLFW_KEY_DOWN: return "Down";
			case GLFW.GLFW_KEY_LEFT: return "Left";
			case GLFW.GLFW_KEY_RIGHT: return "Right";
			default:
		}
		if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
		String n = GLFW.glfwGetKeyName(key, 0);
		return n != null ? n.toUpperCase() : "Key " + key;
	}

	@Override
	public double toNumber() {
		return key;
	}

	@Override
	public void fromNumber(double v) {
		key = Math.max(0, (int) v);
	}
}
