package net.jcm.vsch.blocks.entity.template;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ParticleBlockEntity {

	default void clientTick(Level level, BlockPos pos, BlockState state, ParticleBlockEntity be) {
		tickParticles(level, pos, state);
	}

	default void serverTick(Level level, BlockPos pos, BlockState state, ParticleBlockEntity be) {
		if (level instanceof ServerLevel serverLevel) {
			tickForce(serverLevel, pos, state);
		}
	}

	void tickForce(ServerLevel level, BlockPos pos, BlockState state);

	void tickParticles(Level level, BlockPos pos, BlockState state);

	public static void clientTicker(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		ParticleBlockEntity be = (ParticleBlockEntity) blockEntity;
		be.clientTick(level, pos, state, be);
	}

	public static void serverTicker(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		ParticleBlockEntity be = (ParticleBlockEntity) blockEntity;
		be.serverTick(level, pos, state, be);
	}
}
