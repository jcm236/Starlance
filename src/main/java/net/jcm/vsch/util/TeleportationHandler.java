package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.mixin.valkyrienskies.accessor.ServerShipObjectWorldAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityServer;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.DimensionIdProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.*;
import java.util.stream.StreamSupport;

import static net.jcm.vsch.util.ShipUtils.transformFromId;

@Mod.EventBusSubscriber
public class TeleportationHandler {

	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private static final double INTERSECT_SIZE = 10;

	private static Map<Long, Set<Integer>> SHIP2CONSTRAINTS;
	private static Map<Integer, VSConstraint> ID2CONSTRAINT;

	private final Map<Long, Vector3d> shipToPos = new HashMap<>();
	private final Map<Entity, Vec3> entityToPos = new HashMap<>();
	private final ServerShipWorldCore shipWorld;
	private double greatestOffset;
	private final ServerLevel newDim;
	private final ServerLevel originalDim;
	private final boolean isReturning;

	public TeleportationHandler(ServerLevel newDim, ServerLevel originalDim, boolean isReturning) {
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(newDim);
		this.newDim = newDim;
		this.originalDim = originalDim;
		// Look for the lowest ship when escaping, in order to not collide with the planet.
		// Look for the highest ship when reentering, in order to not collide with the atmosphere.
		this.isReturning = isReturning;
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
		final ServerShipObjectWorldAccessor server = (ServerShipObjectWorldAccessor) VSGameUtilsKt.getShipObjectWorld(event.getServer());
		SHIP2CONSTRAINTS = server.getShipIdToConstraints();
		ID2CONSTRAINT = server.getConstraints();
	}

	public void handleTeleport(Ship ship, Vector3d newPos) {
		this.greatestOffset = 0;
		collectShips(ship, newPos);
		handleTeleport();
	}

	private void collectConnected(Long currentPhysObject, Vector3dc origin, Vector3d newPos) {
		if (currentPhysObject == null || shipToPos.containsKey(currentPhysObject)) {
			return;
		}
		final Vector3dc pos = transformFromId(currentPhysObject, shipWorld).getPositionInWorld();

		// TODO: if planet collision position matters for reentry angle THIS SHOULD BE FIXED!! Currently a fix is not needed.
		final double offset = pos.y() - origin.y();
		if ((isReturning && offset > greatestOffset) || (!isReturning && offset < greatestOffset)) {
			greatestOffset = offset;
		}

		shipToPos.put(currentPhysObject, pos.sub(origin, new Vector3d()).add(newPos));

		final Set<Integer> constraints = SHIP2CONSTRAINTS.get(currentPhysObject);
		if (constraints != null) {
			constraints.forEach(id -> {
				VSConstraint constraint = ID2CONSTRAINT.get(id);
				collectConnected(constraint.getShipId0(), origin, newPos);
				collectConnected(constraint.getShipId1(), origin, newPos);
			});
		}
	}

	private void collectShips(Ship ship, Vector3d newPos) {
		final Vector3dc origin = ship.getTransform().getPositionInWorld();
		collectConnected(ship.getId(), origin, newPos);
		collectNearby(origin, newPos);
	}

	private void collectNearby(Vector3dc origin, Vector3d newPos) {
		final QueryableShipData<LoadedServerShip> loadedShips = shipWorld.getLoadedShips();
		final Vector3d offset = newPos.sub(origin, new Vector3d());
		List.copyOf(shipToPos.keySet())
			.stream()
			.map(loadedShips::getById)
			.filter(Objects::nonNull)
			.map(Ship::getWorldAABB)
			.map(box -> new AABBd(
				box.minX() - INTERSECT_SIZE, box.minY() - INTERSECT_SIZE, box.minZ() - INTERSECT_SIZE,
				box.maxX() + INTERSECT_SIZE, box.maxY() + INTERSECT_SIZE, box.maxZ() + INTERSECT_SIZE))
			.map(loadedShips::getIntersecting)
			.flatMap(iterator -> StreamSupport.stream(iterator.spliterator(), false))
			.forEach(intersecting -> shipToPos.put(intersecting.getId(), intersecting.getTransform().getPositionInWorld().add(offset, new Vector3d())));
	}

	private void handleTeleport() {
		shipToPos.forEach((id, newPos) -> {
			collectEntities(id, newPos);
			handleShipTeleport(id, newPos);
		});
		shipToPos.clear();
		teleportEntities();
	}

	private void collectEntities(Long id, Vector3d shipNewPos) {
		final ServerShip ship = shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
			return;
		}
		final Vector3d transform = shipNewPos.sub(ship.getTransform().getPositionInWorld(), new Vector3d());
		for (Entity entity : originalDim.getAllEntities()) {
			collectEntity(entity, id, ship, transform);
		}
	}

	private boolean collectEntity(Entity entity, Long id, Ship ship, Vector3d transform) {
		if (entity.isPassenger()) {
			return false;
		}
		if (entityToPos.containsKey(entity)) {
			return false;
		}
		// // Entities mounted to shipyard entities
		// if (VSGameUtilsKt.getShipMountedTo(entity) == ship) {
		// 	collectWorldEntity(entity, transform);
		// 	return true;
		// }
		// Shipyard entities
		if (VSGameUtilsKt.getShipObjectManagingPos(originalDim, VectorConversionsMCKt.toJOML(entity.position())) == ship) {
			// Shipyard coordinates are maintained between dimensions
			entityToPos.put(entity, entity.position());
			return true;
		}
		// Entities dragged by ships
		if (ShipUtils.isEntityDraggedByShip(id, entity)) {
			collectWorldEntity(entity, transform);
			return true;
		}
		// Entities in range
		final AABBic shipBox = ship.getShipAABB();
		if (shipBox != null) {
			final AABB inflatedAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), shipBox)).inflate(INTERSECT_SIZE);
			if (entity.getBoundingBox().intersects(inflatedAABB)) {
				collectWorldEntity(entity, transform);
				return true;
			}
		}
		return false;
	}

	private void collectWorldEntity(Entity entity, Vector3d transform) {
		entityToPos.put(entity, entity.position().add(transform.x, transform.y - greatestOffset, transform.z));
	}


	private void teleportEntities() {
		entityToPos.forEach((entity, newPos) -> {
			teleportToWithPassengers(entity, newDim, newPos);
		});
		entityToPos.clear();
	}

	private void handleShipTeleport(final Long id, final Vector3d newPos) {
		final String vsDimName = ((DimensionIdProvider) newDim).getDimensionId();
		final Vector3d targetPos = new Vector3d(newPos).add(0, -greatestOffset, 0);

		final LoadedServerShip ship = shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
			final PhysicsEntityServer physEntity = ((ShipObjectServerWorld) shipWorld).getLoadedPhysicsEntities().get(id);
			if (physEntity == null) {
				LOGGER.warn("[starlance]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity!");
				return;
			}
			LOGGER.info("[starlance]: Teleporting physics entity {} to {} {}", id, vsDimName, newPos);
			final ShipTeleportData teleportData = new ShipTeleportDataImpl(targetPos, physEntity.getShipTransform().getShipToWorldRotation(), physEntity.getLinearVelocity(), physEntity.getAngularVelocity(), vsDimName, null);
			shipWorld.teleportPhysicsEntity(physEntity, teleportData);
			return;
		}
		LOGGER.info("[starlance]: Teleporting ship {} ({}) to {} {}", ship.getSlug(), id, vsDimName, newPos);
		final Vector3dc veloctiy = new Vector3d(ship.getVelocity());
		final Vector3dc omega = new Vector3d(ship.getOmega());
		final ShipTeleportData teleportData = new ShipTeleportDataImpl(targetPos, ship.getTransform().getShipToWorldRotation(), veloctiy, omega, vsDimName, null);
		shipWorld.teleportShip(ship, teleportData);
		if (veloctiy.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					final LoadedServerShip ship2 = shipWorld.getLoadedShips().getById(id);
					if (!transform.getPositionInWorld().equals(nextTransform.getPositionInWorld()) || !transform.getShipToWorldRotation().equals(nextTransform.getShipToWorldRotation())) {
						ship2.setTransformProvider(null);
						return null;
					}
					if (ship2.getVelocity().lengthSquared() == 0 && ship2.getOmega().lengthSquared() == 0) {
						return new NextTransformAndVelocityData(nextTransform, veloctiy, omega);
					}
					return null;
				}
			});
		}
	}

	private static <T extends Entity> T teleportToWithPassengers(T entity, ServerLevel newLevel, Vec3 newPos) {
		Vec3 oldPos = entity.position();
		List<Entity> passengers = new ArrayList<>(entity.getPassengers());
		T newEntity;
		if (entity instanceof ServerPlayer player) {
			player.teleportTo(newLevel, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
			newEntity = entity;
		} else {
			newEntity = (T) entity.getType().create(newLevel);
			if (newEntity == null) {
				return null;
			}
			entity.ejectPassengers();
			newEntity.restoreFrom(entity);
			newEntity.moveTo(newPos.x, newPos.y, newPos.z, newEntity.getYRot(), newEntity.getXRot());
			newEntity.setYHeadRot(entity.getYHeadRot());
			newEntity.setYBodyRot(entity.getVisualRotationYInDegrees());
			newLevel.addDuringTeleport(newEntity);
			entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
		}
		for (Entity p : passengers) {
			Entity newPassenger = teleportToWithPassengers(p, newLevel, p.position().subtract(oldPos).add(newPos));
			if (newPassenger != null) {
				newPassenger.startRiding(newEntity, true);
			}
		}
		return newEntity;
	}
}
