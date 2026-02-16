package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.ship.ShipTierAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class TierBlock extends Block {
    public final int tier;

    public TierBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!(level instanceof ServerLevel serverLevel)) return;

        LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(serverLevel, pos);
        if (ship == null) return;

        ShipTierAttachment.get(ship).addTierMod(pos, tier);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (!(level instanceof ServerLevel serverLevel)) return;

        LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(serverLevel, pos);
        if (ship == null) return;

        ShipTierAttachment.get(ship).removeTierMod(pos);
    }
}
