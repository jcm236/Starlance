package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({
	ClientboundAddPlayerPacket.class,
	ClientboundMoveEntityPacket.PosRot.class,
	ClientboundMoveEntityPacket.Rot.class,
	ClientboundPlayerPositionPacket.class,
	ClientboundTeleportEntityPacket.class,
	ServerboundMovePlayerPacket.PosRot.class,
	ServerboundMovePlayerPacket.Rot.class
})
public abstract class MixinEntityRotationPackets_write implements EntityRotationPacketAccessor {
	@Inject(method = "write", at = @At("RETURN"))
	public void write(final FriendlyByteBuf buf, final CallbackInfo ci) {
		buf.writeQuaternion(this.vsch$rotation());
		buf.writeFloat(this.vsch$getHeadPitch());
		buf.writeFloat(this.vsch$getHeadYaw());
	}
}
