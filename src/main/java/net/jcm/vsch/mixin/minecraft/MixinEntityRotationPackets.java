package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({
	ClientboundMoveEntityPacket.class,
	ServerboundMovePlayerPacket.class
})
public abstract class MixinEntityRotationPackets implements EntityRotationPacketAccessor {
	@Unique
	private Quaternionf rotation = new Quaternionf();

	@Override
	public Quaternionf vsch$getRotation() {
		return this.rotation;
	}
}
