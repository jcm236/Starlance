package net.jcm.vsch.util;

public class ShipUtils {
    // /**
    //  * <b>Warning:</b> This function is still affected by the height bug that other addons have,
    //  * and so can't be used in space.
    //  *
    //  * <p></p>
    //  *
    //  * <code>dx</code>, <code>dy</code> and <code>dz</code>
    //  * Are the distance from center block to be assembled. Added in both directions, so
    //  * <code>(center.x - dx)</code> ... <code>(center.x + dx)</code>.
    //  * 
    //  * @see #assembleBlock(ServerLevel, BlockPos)
    //  * 
    //  * @return The ship that was created, or null if it wasn't created
    //  */
    // public static ServerShip assembleBlocks(ServerLevel level, BlockPos center, int dx, int dy, int dz) {
    //     if (dx < 0 || dy < 0 || dz < 0) return null;

    //     DenseBlockPosSet set = new DenseBlockPosSet();
    //     for (int x = -dx; x <= dx; x++) {
    //         for (int y = -dy; y <= dy; y++) {
    //             for (int z = -dz; z <= dz; z++) {
    //                 BlockPos newBlock = new BlockPos(center.getX() + x, center.getY() + y, center.getZ() + z);
    //                 if (!level.getBlockState(newBlock).isAir()) {
    //                     set.add(VectorConversionsMCKt.toJOML(newBlock));
    //                 }
    //             }
    //         }
    //     }

    //     // Otherwise we get massless ships
    //     if (set.isEmpty()) return null;

    //     return ShipAssemblyKt.createNewShipWithBlocks(center, set, level);
    // }

    // /**
    //  * This function IS safe to use in space, but only assembles a single block.
    //  *
    //  * @see #assembleBlocks(ServerLevel, BlockPos, int, int, int)
    //  *
    //  * @return The ship that was created, or null if it wasn't created
    //  */
    // public static ServerShip assembleBlock(ServerLevel level, BlockPos blockPos) {
    //     BlockState blockState = level.getBlockState(blockPos);

    //     if (VSGameUtilsKt.getShipManagingPos(level, blockPos) != null) return null;

    //     if (blockState.isAir()) return null;

    //     String dimensionId = VSGameUtilsKt.getDimensionId(level);


    //     ServerShip serverShip =
    //             VSGameUtilsKt.getShipObjectWorld(level)
    //                     .createNewShipAtBlock(
    //                             VectorConversionsMCKt.toJOML(blockPos),
    //                             false,
    //                             1.0,
    //                             dimensionId
    //                     );

    //     BlockPos centerPos = VectorConversionsMCKt.toBlockPos(serverShip.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i()));

    //     // Move the block from the world to a ship
    //     RelocationUtilKt.relocateBlock(level, blockPos, centerPos, true, serverShip, Rotation.NONE);

    //     return serverShip;
    // }
}
