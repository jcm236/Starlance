package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({
	ClientboundMoveEntityPacket.PosRot.class,
	ClientboundMoveEntityPacket.Rot.class,
	ServerboundMovePlayerPacket.PosRot.class,
	ServerboundMovePlayerPacket.Rot.class
})
public abstract class MixinEntityRotationPacketSubClasses implements EntityRotationPacketAccessor {
	@Inject(method = "write", at = @At("RETURN"))
	public void write(final FriendlyByteBuf buf, final CallbackInfo ci) {
		buf.writeQuaternion(this.vsch$getRotation());
	}
}
