package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.ServerboundMovePlayerPacketAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundMovePlayerPacket.Rot.class)
public abstract class MixinServerboundMovePlayerPacket_Rot extends MixinServerboundMovePlayerPacket {
	@Inject(method = "read", at = @At("RETURN"))
	private static void read(final FriendlyByteBuf buf, final CallbackInfoReturnable<ServerboundMovePlayerPacket.Rot> cir) {
		final ServerboundMovePlayerPacketAccessor packet = (ServerboundMovePlayerPacketAccessor)(cir.getReturnValue());
		packet.vsch$getRotation().set(buf.readQuaternion()).normalize();
	}

	@Inject(method = "write", at = @At("RETURN"))
	public void write(final FriendlyByteBuf buf, final CallbackInfo ci) {
		buf.writeQuaternion(this.vsch$getRotation());
	}
}
