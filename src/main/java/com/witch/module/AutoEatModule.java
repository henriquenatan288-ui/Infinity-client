package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/** Come sozinho quando a fome fica baixa, usando comida da hotbar (ignora comidas ruins). */
public class AutoEatModule extends Module {
	private static final Set<Item> AVOID = Set.of(
			Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.PUFFERFISH, Items.POISONOUS_POTATO,
			Items.CHICKEN, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.CHORUS_FRUIT
	);

	private final IntSetting hunger = add(new IntSetting("Eat at", 1, 19, 14, ""));

	private boolean eating = false;
	private int prevSlot = -1;

	public AutoEatModule() {
		super("Auto Eat", Category.UTILITY);
	}

	private boolean isGoodFood(ItemStack s) {
		return !s.isEmpty() && s.get(DataComponents.FOOD) != null && !AVOID.contains(s.getItem());
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) {
			eating = false;
			return;
		}
		boolean low = p.getFoodData().getFoodLevel() <= hunger.get() && p.getFoodData().needsFood();
		var inv = p.getInventory();

		if (!eating) {
			if (!low || mc.gui.screen() != null) return;
			int slot = -1;
			for (int i = 0; i < 9; i++) {
				if (isGoodFood(inv.getItem(i))) {
					slot = i;
					break;
				}
			}
			if (slot < 0) return;
			prevSlot = inv.getSelectedSlot();
			inv.setSelectedSlot(slot);
			eating = true;
		}

		if (!low || !isGoodFood(inv.getItem(inv.getSelectedSlot()))) {
			stop();
			return;
		}
		mc.options.keyUse.setDown(true);
	}

	private void stop() {
		Minecraft mc = Minecraft.getInstance();
		mc.options.keyUse.setDown(false);
		var p = mc.player;
		if (eating && p != null && prevSlot >= 0) {
			p.getInventory().setSelectedSlot(prevSlot);
		}
		eating = false;
		prevSlot = -1;
	}

	@Override
	protected void onDisable() {
		if (eating) stop();
	}
}