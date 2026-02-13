package net.jcm.vsch.ship;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.world.PhysLevel;

import java.util.HashMap;

public final class ShipTierAttachment implements ShipPhysicsListener {
    private HashMap<Long, Integer> tierMods = new HashMap<>();

    @Override
    public void physTick(@NotNull PhysShip physShip, @NotNull PhysLevel physLevel) {

    }

    public void addTierMod(BlockPos pos, int tier) {
        tierMods.put(pos.asLong(), tier);
    }

    public void removeTierMod(BlockPos pos) {
        tierMods.remove(pos.asLong());
    }

    public int getHighestTier() {
        return tierMods.values().stream().reduce(1, Integer::max);
    }

    public static ShipTierAttachment get(final LoadedServerShip ship) {
        ShipTierAttachment attachment = ship.getAttachment(ShipTierAttachment.class);
        if (attachment == null) {
            attachment = new ShipTierAttachment();
            ship.setAttachment(attachment);
        }
        return attachment;
    }
}
