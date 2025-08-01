package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundMoveEntityPacket.PosRot.class)
public abstract class MixinClientboundMoveEntityPacket_PosRot {
	@Inject(method = "read", at = @At("RETURN"))
	private static void read(final FriendlyByteBuf buf, final CallbackInfoReturnable<ClientboundMoveEntityPacket.PosRot> cir) {
		final EntityRotationPacketAccessor packet = (EntityRotationPacketAccessor)(cir.getReturnValue());
		packet.vsch$rotation().set(buf.readQuaternion()).normalize();
	}
}
