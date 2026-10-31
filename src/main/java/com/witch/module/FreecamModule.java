package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Camera livre: seu personagem fica parado e voce voa com a camera (WASD, espaco sobe, shift desce).
 * So enxerga os chunks que o servidor ja enviou ao seu personagem. A camera e feita no CameraMixin.
 */
public class FreecamModule extends Module {
	private static FreecamModule instance;

	private final IntSetting speed = add(new IntSetting("Speed", 1, 20, 5, ""));

	private Vec3 pos = Vec3.ZERO;
	private Vec3 prev = Vec3.ZERO;
	private float yaw;
	private float pitch;
	private ClientInput savedInput;

	public FreecamModule() {
		super("Freecam", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && Minecraft.getInstance().player != null;
	}

	public static Vec3 pos(float partial) {
		return instance.prev.lerp(instance.pos, partial);
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}

	/** Chamado pelo MouseHandlerMixin: o mouse gira a camera, nao o personagem. */
	public static void turn(double dx, double dy) {
		instance.yaw += (float) dx * 0.15f;
		instance.pitch = Mth.clamp(instance.pitch + (float) dy * 0.15f, -90f, 90f);
	}

	@Override
	protected void onEnable() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		pos = p.getEyePosition();
		prev = pos;
		yaw = p.getYRot();
		pitch = p.getXRot();
		savedInput = p.input;
		p.input = new ClientInput(); // personagem sem comandos: fica parado
	}

	@Override
	protected void onDisable() {
		var p = Minecraft.getInstance().player;
		if (p != null && savedInput != null) p.input = savedInput;
		savedInput = null;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		prev = pos;
		if (mc.gui.screen() != null) return;

		var o = mc.options;
		double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double side = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);

		double r = Math.toRadians(yaw);
		double sin = Math.sin(r);
		double cos = Math.cos(r);
		double step = speed.get() * 0.1;
		double dx = (-sin * fwd + cos * side) * step;
		double dz = (cos * fwd + sin * side) * step;
		pos = pos.add(dx, up * step, dz);
	}
}
