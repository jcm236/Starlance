package net.jcm.vsch.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.blocks.custom.template.AbstractCannonBlock;
import net.jcm.vsch.blocks.entity.laser.AbstractLaserCannonBlockEntity;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;

import java.util.function.BiFunction;

public class LaserCannonBlock<T extends AbstractLaserCannonBlockEntity> extends AbstractCannonBlock<T> {
	private static final RotShape SHAPE = RotShapes.box(2.0, 0.0, 3.0, 14.0, 16.0, 14.0);

	private final BiFunction<BlockPos, BlockState, T> blockEntityFactory;

	public LaserCannonBlock(Properties properties, BiFunction<BlockPos, BlockState, T> blockEntityFactory) {
		super(properties, DirectionalShape.down(SHAPE));
		this.blockEntityFactory = blockEntityFactory;
	}

	@Override
	public T newBlockEntity(BlockPos pos, BlockState state) {
		return this.blockEntityFactory.apply(pos, state);
	}
}
