package net.jcm.vsch.ship;

import kotlin.Triple;
import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.weight.IVSCHVariableWeightProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.DefaultBlockStateInfoProvider;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;


import java.util.List;

public class VSCHBlockStateInfoProvider implements BlockStateInfoProvider {

    public static VSCHBlockStateInfoProvider INSTANCE = new VSCHBlockStateInfoProvider();
    private static Logger LOGGER = LogManager.getLogger("Starlance");
    public static void register(){
        Registry.register(BlockStateInfo.INSTANCE.getREGISTRY(), new ResourceLocation(VSCHMod.MODID,"weight_provider"),INSTANCE);
    }

    private VSCHBlockStateInfoProvider(){

    }


    @Override
    public @NotNull List<Triple<Integer, Integer, Integer>> getBlockStateData() {
        return List.of();
    }

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    public @Nullable Double getBlockStateMass(@NotNull BlockState blockState) {

        if (blockState.getBlock() instanceof IVSCHVariableWeightProvider provider){
            return provider.getCurrentWeight(blockState);
        }
        return null;
    }

    @Override
    public @Nullable BlockType getBlockStateType(@NotNull BlockState blockState) {
        return null;
    }

    @Override
    public @NotNull List<Lod1SolidBlockState> getSolidBlockStates() {
        return List.of();
    }

    @Override
    public @NotNull List<Lod1LiquidBlockState> getLiquidBlockStates() {
        return List.of();
    }
}
