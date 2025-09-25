package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({
	ClientboundAddPlayerPacket.class,
	ClientboundMoveEntityPacket.class,
	ClientboundPlayerPositionPacket.class,
	ClientboundTeleportEntityPacket.class,
	ServerboundMovePlayerPacket.class
})
public abstract class MixinEntityRotationPackets implements EntityRotationPacketAccessor {
	@Unique
	private Quaternionf rotation = new Quaternionf();
	@Unique
	private float headPitch = 0;
	@Unique
	private float headYaw = 0;

	@Override
	public Quaternionf vsch$rotation() {
		return this.rotation;
	}

	@Override
	public float vsch$getHeadPitch() {
		return this.headPitch;
	}

	@Override
	public void vsch$setHeadPitch(final float pitch) {
		this.headPitch = pitch;
	}

	@Override
	public float vsch$getHeadYaw() {
		return this.headYaw;
	}

	@Override
	public void vsch$setHeadYaw(final float yaw) {
		this.headYaw = yaw;
	}
}
