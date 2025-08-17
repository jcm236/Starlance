package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerEntity.class)
public abstract class MixinServerEntity {
	@Shadow
	@Final
	private Entity entity;

	@ModifyExpressionValue(
		method = "sendChanges",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundTeleportEntityPacket;")
	)
	public ClientboundTeleportEntityPacket new$ClientboundTeleportEntityPacket(final ClientboundTeleportEntityPacket packet) {
		if (this.entity instanceof FreeRotatePlayerAccessor frp) {
			((EntityRotationPacketAccessor)(packet)).vsch$rotation().set(frp.vsch$getRotation());
		}
		return packet;
	}

	@ModifyExpressionValue(
		method = "sendChanges",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$PosRot;")
	)
	public ClientboundMoveEntityPacket.PosRot new$ClientboundMoveEntityPacket$PosRot(final ClientboundMoveEntityPacket.PosRot packet) {
		if (this.entity instanceof FreeRotatePlayerAccessor frp) {
			((EntityRotationPacketAccessor)(packet)).vsch$rotation().set(frp.vsch$getRotation());
		}
		return packet;
	}

	@ModifyExpressionValue(
		method = "sendChanges",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$Rot;")
	)
	public ClientboundMoveEntityPacket.Rot new$ClientboundMoveEntityPacket$Rot(final ClientboundMoveEntityPacket.Rot packet) {
		if (this.entity instanceof FreeRotatePlayerAccessor frp) {
			((EntityRotationPacketAccessor)(packet)).vsch$rotation().set(frp.vsch$getRotation());
		}
		return packet;
	}
}
