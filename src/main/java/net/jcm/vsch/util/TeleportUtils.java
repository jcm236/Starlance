package net.jcm.vsch.util;

import net.jcm.vsch.mixin.accessor.ServerShipObjectWorldAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.entity.handling.VSEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import org.valkyrienskies.mod.common.entity.handling.WorldEntityHandler;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.*;

/**
 * Deprecated. Use {@link TeleportationHandler} instead
 */
@Deprecated(forRemoval = true)
public class TeleportUtils {
    public static void teleportShipAndConstrained(Ship ship, ServerLevel level, String newDim, double x, double y, double z) {
        ServerShipObjectWorldAccessor shipWorld = (ServerShipObjectWorldAccessor) VSGameUtilsKt.getShipObjectWorld(level);
        teleportShipAndConstrained(ship, level, newDim, x, y, z, new HashSet<>(), shipWorld.getShipIdToConstraints(), shipWorld.getConstraints());
    }

    /**
     * Recursively teleport nearby ships, keeping track of which ones were already teleported.
     * @param currentShip Current ship (duh)
     * @param level  The ships current level
     * @param newDim Normal dimension id string format (not VS format)
     * @param x x position in world to tp the ship to
     * @param y y position in world to tp the ship to
     * @param z z position in world to tp the ship to
     * @param teleported Already teleported ships
     * @param shipIdToConstraints Pass the shipid -> constraint function along instead of computing it every iteration
     * @param constraintIdToConstraint Pass the shipid -> constraint function along instead of computing it every iteration
     */
    private static void teleportShipAndConstrained(Ship currentShip, ServerLevel level, String newDim, double x, double y, double z, Collection<Ship> teleported, Map<Long, Set<Integer>> shipIdToConstraints, Map<Integer, VSConstraint> constraintIdToConstraint) {
        if (teleported.contains(currentShip)) {
            return;
        }
        teleported.add(currentShip);
        Set<Integer> constraints = shipIdToConstraints.get(currentShip.getId());
        if (constraints != null) {
            constraints.iterator().forEachRemaining(id -> {
                VSConstraint constraint = constraintIdToConstraint.get(id);
                QueryableShipData<ServerShip> allShips = VSGameUtilsKt.getShipObjectWorld(level).getAllShips();
                Ship ship0 = allShips.getById(constraint.getShipId0());
                System.out.println("ship0: " + ship0.getId());
                Ship ship1 = allShips.getById(constraint.getShipId1());
                System.out.println("ship1: " + ship1.getId());
                Vector3d offset = ship0.getTransform().getPositionInWorld().sub(ship1.getTransform().getPositionInWorld(), new Vector3d());
                teleportShipAndConstrained(ship0, level, newDim, x - offset.x, y - offset.y, z - offset.z, teleported, shipIdToConstraints, constraintIdToConstraint);
                teleportShipAndConstrained(ship1, level, newDim, x + offset.x, y + offset.y, z + offset.z, teleported, shipIdToConstraints, constraintIdToConstraint);
            });
        }
        dimensionTeleportShip(currentShip, level, newDim, x, y, z);
    }

    /**
     * See
     * {@link #dimensionTeleportShip(Ship, ServerLevel, String, double, double, double)
     * dimensionTeleportShip} for documentation. This overload simply takes in a
     * Vec3 instead of 3 doubles.
     */
    public static void dimensionTeleportShip(Ship ship, ServerLevel level, String newDim, Vec3 newPos) {
        dimensionTeleportShip(ship, level, newDim, newPos.x, newPos.y, newPos.z);
    }

    /**
     * This function took us like a week days to make. You better appreciate it. <br>
     * </br>
     * It will teleport the given ship, using the level, to the
     * dimension with id of newDim at x, y, z. <br>
     * </br>
     * But most importantly, it will also teleport any players or entities
     * (including create contraptions) that are
     * currently being dragged by the ship to the new dimension, and their correct
     * position relative to the ship that was moved.
     *
     * @param ship   The ship to move
     * @param level  The ships current level
     * @param newDim Normal dimension id string format (not VS format)
     * @param x x position in world to tp the ship to
     * @param y y position in world to tp the ship to
     * @param z z position in world to tp the ship to
     *
     */
    public static void dimensionTeleportShip(Ship ship, ServerLevel level, String newDim, double x, double y, double z) {
        // ----- Prepare dimension destination ----- //

        // Convert back into a stupid stupid VS dimension string
        String VSnewDimension = VSCHUtils.dimToVSDim(newDim);

        // Prepare ship teleport info for later
        ShipTeleportData teleportData = new ShipTeleportDataImpl(new Vector3d(x, y, z), ship.getTransform().getShipToWorldRotation(), new Vector3d(), new Vector3d(), VSnewDimension, null);

        // ----- AABB magic ----- //

        // Get the AABB of the last tick and the AABB of the current tick
        AABB prevWorldAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), ship.getShipAABB())).inflate(10);
        AABB currentWorldAABB = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()).inflate(10);

        // Combine the AABB's into one big one
        //		AABB totalAABB = currentWorldAABB.minmax(prevWorldAABB);

        Vec3 oldShipCenter = prevWorldAABB.deflate(10).getCenter();
        Vec3 newoldShipCenter = currentWorldAABB.deflate(10).getCenter();

        // ---------- //

        // ----- Get all entities BEFORE teleporting ship ----- //

        // Save the distances from entities to the ship for afterwards
        Map<Entity, Vec3> entityOffsets = new HashMap<Entity, Vec3>();
        Map<Entity, Vec3> shipyardentityOffsets = new HashMap<Entity, Vec3>();

        // Save entities that actually need to be teleported

        entityOffsets.putAll(calculateOffsetsNonShipyard(level, (ServerShip) ship, oldShipCenter));
        entityOffsets.putAll(calculateOffsetsNonShipyard(level, (ServerShip) ship, newoldShipCenter));
//        entityOffsets.putAll(calculateOffsets(level,currentWorldAABB,newoldShipCenter,false));
        shipyardentityOffsets.putAll(calculateOffsets(level,currentWorldAABB,newoldShipCenter,true));
        // ---------- //

        // ----- Teleport ship ----- //

        // Do we actually use this? Eh can't be bothered to check
        // Yes we do. Don't remove this
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);

        // Teleport ship to new dimension at origin
        ServerShip serverShip = (ServerShip) ship;
        shipWorld.teleportShip(serverShip, teleportData);

        // ---------- //

        // ----- Teleport entities AFTER ship ----- //

        // Get a level object from the VS dimension string of the dim we're going to
        ServerLevel newLevel = VSCHUtils.VSDimToLevel(level.getServer(), VSnewDimension);
        Vec3 newShipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()).getCenter();
        Vec3 newShipyardCenter = VectorConversionsMCKt.toMinecraft(ship.getShipAABB().center(new Vector3d(0, 0, 0)));

        teleportEntitiesToOffsets(entityOffsets, newShipCenter, newLevel);
        teleportEntitiesToOffsets(shipyardentityOffsets, newShipyardCenter, newLevel);
    }

    public static HashMap<Entity, Vec3> calculateOffsetsNonShipyard(ServerLevel level, ServerShip ship, Vec3 center) {
        ServerShipWorldCore shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
        HashMap<Entity, Vec3> offsets = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            IEntityDraggingInformationProvider draggingProvider = (IEntityDraggingInformationProvider) entity;
            if (!draggingProvider.vs$shouldDrag()) continue;

            EntityDraggingInformation information = draggingProvider.getDraggingInformation();
            Long lastStoodId = information.getLastShipStoodOn();
            if (lastStoodId == null) continue;
            ServerShip stoodOnShip = shipObjectWorld.getLoadedShips().getById(information.getLastShipStoodOn());
            if (ship != stoodOnShip) continue;
            Vec3 entityShipOffset = entity.position().subtract(center);
            offsets.put(entity, entityShipOffset);
        }
        return offsets;
    }

    public static HashMap<Entity, Vec3> calculateOffsets(ServerLevel level, AABB aabb, Vec3 center, Boolean shipyard) {

        // Find all entities nearby the ship
        HashMap<Entity, Vec3> offsets = new HashMap<>();
        for (Entity entity : level.getEntities(null, aabb)) {

            VSEntityHandler handler = VSEntityManager.INSTANCE.getHandler(entity);
            if (shipyard) {
                if (handler.getClass() == WorldEntityHandler.class) {
                    // If the entity is riding another
                    if (entity.getVehicle() != null) {
                        // Dismount them
                        entity.dismountTo(entity.getX(), entity.getY(), entity.getZ());
                    }

                    // Get the offset from the entities position to the ship
                    Vec3 entityShipOffset = entity.getPosition(0).subtract(center);

                    offsets.put(entity, entityShipOffset);
                }
            } else {
                if (handler.getClass() != WorldEntityHandler.class) {
                    // If the entity is riding another
                    if (entity.getVehicle() != null) {
                        // Dismount them
                        entity.dismountTo(entity.getX(), entity.getY(), entity.getZ());
                    }

                    // Get the offset from the entities position to the ship
                    Vec3 entityShipOffset = entity.getPosition(0).subtract(center);

                    offsets.put(entity, entityShipOffset);
                }
            }
        }
        return offsets;
    }

    public static void teleportEntitiesToOffsets(Map<Entity, Vec3> entityOffsets, Vec3 center, ServerLevel newLevel){
        for (Entity entity : entityOffsets.keySet()) {
            if (entity instanceof ServerPlayer player) {
                Vec3 shipOffset = entityOffsets.get(player);
                Vec3 newPosition = center.add(shipOffset);

                // Players need a different teleport command to entities
                player.teleportTo(newLevel, newPosition.x, newPosition.y, newPosition.z, player.getYRot(), player.getXRot());
            } else {
                Vec3 shipOffset = entityOffsets.get(entity);
                Vec3 newPosition = center.add(shipOffset);

                entity.teleportTo(newLevel, newPosition.x, newPosition.y, newPosition.z, null, entity.getYRot(), entity.getXRot());
            }
        }
    }
}
