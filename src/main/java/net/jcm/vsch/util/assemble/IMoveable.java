package net.jcm.vsch.util.assemble;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface IMoveable<T> {
	default void beforeSaveForMove(ServerLevel level, BlockPos origin, BlockPos target) {}

	T beforeMove(ServerLevel level, BlockPos origin, BlockPos target);

	void afterMove(ServerLevel level, BlockPos origin, BlockPos target, T moveData);
}
