package net.jcm.vsch.util;

import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityServer;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import java.util.Objects;

public class ShipUtils {
    /**
    * Gets the transform of a @link{Ship} when given its id.
    * @param id Id of the ship (duh)
    */
    public static ShipTransform transformFromId(Long id, ServerShipWorldCore shipWorld) {
        Ship ship = shipWorld.getAllShips().getById(id);
        if (ship == null) {
            PhysicsEntityServer physicsEntity = ((ShipObjectServerWorld)shipWorld).getLoadedPhysicsEntities().get(id);
            if (physicsEntity == null) return new ShipTransformImpl(new Vector3d(), new Vector3d(), new Quaterniond(), new Vector3d());
            return physicsEntity.getShipTransform();
        }
        return ship.getTransform();
    }

    public static boolean isEntityDraggedByShip(Ship ship, Entity entity) {
        IEntityDraggingInformationProvider provider = (IEntityDraggingInformationProvider) entity;
        if (!provider.vs$shouldDrag()) return false;
        EntityDraggingInformation information = provider.getDraggingInformation();
        if (!information.isEntityBeingDraggedByAShip()) return false;
        return Objects.equals(information.getLastShipStoodOn(), ship.getId());
    }

    public static boolean isEntityDraggedByShip(Long id, Entity entity) {
        IEntityDraggingInformationProvider provider = (IEntityDraggingInformationProvider) entity;
        if (!provider.vs$shouldDrag()) return false;
        EntityDraggingInformation information = provider.getDraggingInformation();
        if (!information.isEntityBeingDraggedByAShip()) return false;
        return Objects.equals(information.getLastShipStoodOn(), id);
    }
}
