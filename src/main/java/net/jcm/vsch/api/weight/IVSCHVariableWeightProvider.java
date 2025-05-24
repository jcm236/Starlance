package net.jcm.vsch.api.weight;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface IVSCHVariableWeightProvider {
    public @NotNull Double getMaxWeightPossible();
    public @NotNull Double getMinWeightPossible();
    public @NotNull Double getCurrentWeight(BlockState state);
}
