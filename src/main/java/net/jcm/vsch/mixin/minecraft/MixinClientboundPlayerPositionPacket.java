package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.ClientboundPlayerPositionPacketAccessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundPlayerPositionPacket.class)
public abstract class MixinClientboundPlayerPositionPacket implements Packet<ClientGamePacketListener>, ClientboundPlayerPositionPacketAccessor {
	@Unique
	private Quaternionf rotation = new Quaternionf();

	@Override
	public Quaternionf vsch$getRotation() {
		return this.rotation;
	}

	@Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
	public void init$read(final FriendlyByteBuf buf, final CallbackInfo ci) {
		this.rotation.set(buf.readQuaternion()).normalize();
	}

	@Inject(method = "write", at = @At("RETURN"))
	public void write(final FriendlyByteBuf buf, final CallbackInfo ci) {
		buf.writeQuaternion(this.rotation);
	}
}
