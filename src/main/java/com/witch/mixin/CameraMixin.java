package com.witch.mixin;

import com.witch.module.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"), require = 0)
	private void witch$freecam(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!FreecamModule.active()) return;
		float partial = deltaTracker.getGameTimeDeltaPartialTick(true);
		setPosition(FreecamModule.pos(partial));
		setRotation(FreecamModule.yaw(), FreecamModule.pitch());
		detached = true;
	}
}
