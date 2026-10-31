package com.witch.mixin;

import com.witch.module.FreecamModule;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Redirect(method = "turnPlayer(D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 0)
	private void witch$turn(LocalPlayer player, double dx, double dy) {
		if (FreecamModule.active()) {
			FreecamModule.turn(dx, dy);
		} else {
			player.turn(dx, dy);
		}
	}
}
