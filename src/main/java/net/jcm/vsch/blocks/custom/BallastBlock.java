package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.api.weight.IVSCHVariableWeightProvider;
import net.jcm.vsch.config.VSCHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;

public class BallastBlock extends Block implements IVSCHVariableWeightProvider {

    public static final IntegerProperty WEIGHT = IntegerProperty.create("weight",0,16);

    public BallastBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WEIGHT,1));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WEIGHT);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState pOldState, boolean pMovedByPiston) {
        super.onPlace(state, level, pos, pOldState, pMovedByPiston);
        refreshWeight(state,level,pos);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        super.neighborChanged(pState, pLevel, pPos, pNeighborBlock, pNeighborPos, pMovedByPiston);
        refreshWeight(pState,pLevel,pPos);
    }


    void refreshWeight(BlockState state, LevelReader level, BlockPos pos){
        int current = level.getBestNeighborSignal(pos)+1;
        if (state.getValue(WEIGHT) != current){
            state.setValue(WEIGHT,current);
        }
    }

    @Override
    public @NotNull Double getMaxWeightPossible() {
        return VSCHConfig.BALLAST_WEIGHT.get().doubleValue() * 16;
    }

    @Override
    public @NotNull Double getMinWeightPossible() {
        return VSCHConfig.BALLAST_WEIGHT.get().doubleValue();
    }

    @Override
    public @NotNull Double getCurrentWeight(BlockState state) {
        return VSCHConfig.BALLAST_WEIGHT.get().doubleValue() * state.getValue(WEIGHT).doubleValue();
    }
}
