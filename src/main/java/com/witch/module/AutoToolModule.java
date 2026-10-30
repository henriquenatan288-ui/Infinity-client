package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Ao quebrar um bloco, troca pra ferramenta mais rapida da hotbar e volta pro slot anterior quando solta o botao. */
public class AutoToolModule extends Module {
	private final IntSetting minDurability = add(new IntSetting("Keep durability", 0, 30, 5, "%"));

	private int prevSlot = -1;

	public AutoToolModule() {
		super("Auto Tool", Category.UTILITY);
	}

	@Override
	protected void onDisable() {
		restore();
	}

	private void restore() {
		var p = Minecraft.getInstance().player;
		if (p != null && prevSlot >= 0) {
			p.getInventory().setSelectedSlot(prevSlot);
		}
		prevSlot = -1;
	}

	/** Velocidade de quebra do item, ou -1 se o item estiver quase quebrando. */
	private float score(ItemStack s, BlockState state) {
		int max = s.getMaxDamage();
		if (max > 0) {
			int remaining = max - s.getDamageValue();
			if (remaining * 100 < max * minDurability.get()) return -1f;
		}
		return s.getDestroySpeed(state);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		var level = mc.level;
		if (p == null || level == null) {
			prevSlot = -1;
			return;
		}

		boolean attacking = mc.options.keyAttack.isDown() && mc.gui.screen() == null;
		if (!attacking) {
			restore();
			return;
		}
		if (p.isCreative()) return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

		BlockState state = level.getBlockState(hit.getBlockPos());
		if (state.isAir()) return;

		var inv = p.getInventory();
		int current = inv.getSelectedSlot();
		int best = current;
		float bestScore = score(inv.getItem(current), state);

		for (int i = 0; i < 9; i++) {
			float sc = score(inv.getItem(i), state);
			if (sc > bestScore) {
				bestScore = sc;
				best = i;
			}
		}

		if (best != current) {
			if (prevSlot < 0) prevSlot = current;
			inv.setSelectedSlot(best);
		}
	}
}