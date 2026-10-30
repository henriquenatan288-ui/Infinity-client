package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** Ataca sozinho quando a mira esta em cima de um mob ou jogador. Nao mexe na camera. */
public class TriggerBotModule extends Module {
	private final IntSetting cooldown = add(new IntSetting("Charge", 50, 100, 90, "%"));
	private final IntSetting players = add(new IntSetting("Players (0/1)", 0, 1, 1, ""));
	private final IntSetting mobs = add(new IntSetting("Mobs (0/1)", 0, 1, 1, ""));

	public TriggerBotModule() {
		super("Trigger Bot", Category.UTILITY);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
		if (p.isSpectator() || !p.isAlive() || p.isUsingItem()) return;
		if (!(mc.hitResult instanceof EntityHitResult ehr)) return;

		Entity e = ehr.getEntity();
		if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isInvulnerable()) return;
		boolean isPlayer = e instanceof Player;
		if (isPlayer ? players.get() == 0 : mobs.get() == 0) return;
		if (p.getAttackStrengthScale(0f) < cooldown.get() / 100f) return;

		mc.gameMode.attack(p, e);
		p.swing(InteractionHand.MAIN_HAND, p.getMainHandItem().getInteractAnimation(), false);
	}
}
