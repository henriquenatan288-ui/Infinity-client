package com.witch.mixin;

import com.witch.module.NoGhostBlocksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$noGhostBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!NoGhostBlocksModule.active()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.isLocalServer()) return;
		BlockState state = mc.level.getBlockState(pos);
		state.getBlock().playerWillDestroy(mc.level, pos, state, mc.player);
		cir.setReturnValue(false);
	}
}
