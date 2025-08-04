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

	public void handleTeleport(final Ship ship, final Vector3d newPos) {
		this.greatestOffset = 0;
		this.collectShips(ship, newPos);
		this.handleTeleport();
	}

	private void collectConnected(final Long currentPhysObject, final Vector3dc origin, final Vector3d newPos) {
		if (currentPhysObject == null || shipToPos.containsKey(currentPhysObject)) {
			return;
		}
		final Vector3dc pos = transformFromId(currentPhysObject, shipWorld).getPositionInWorld();

		// TODO: if planet collision position matters for reentry angle THIS SHOULD BE FIXED!! Currently a fix is not needed.
		final double offset = pos.y() - origin.y();
		if ((this.isReturning && offset > this.greatestOffset) || (!this.isReturning && offset < this.greatestOffset)) {
			this.greatestOffset = offset;
		}

		shipToPos.put(currentPhysObject, pos.sub(origin, new Vector3d()).add(newPos));

		final Set<Integer> constraints = SHIP2CONSTRAINTS.get(currentPhysObject);
		if (constraints != null) {
			constraints.forEach(id -> {
				final VSConstraint constraint = ID2CONSTRAINT.get(id);
				this.collectConnected(constraint.getShipId0(), origin, newPos);
				this.collectConnected(constraint.getShipId1(), origin, newPos);
			});
		}
	}

	private void collectShips(Ship ship, Vector3d newPos) {
		final Vector3dc origin = ship.getTransform().getPositionInWorld();
		this.collectConnected(ship.getId(), origin, newPos);
		this.collectNearby(origin, newPos);
	}

	private void collectNearby(Vector3dc origin, Vector3d newPos) {
		final QueryableShipData<LoadedServerShip> loadedShips = shipWorld.getLoadedShips();
		final Vector3d offset = newPos.sub(origin, new Vector3d());
		List.copyOf(this.shipToPos.keySet())
			.stream()
			.map(loadedShips::getById)
			.filter(Objects::nonNull)
			.map(Ship::getWorldAABB)
			.map(box -> new AABBd(
				box.minX() - INTERSECT_SIZE, box.minY() - INTERSECT_SIZE, box.minZ() - INTERSECT_SIZE,
				box.maxX() + INTERSECT_SIZE, box.maxY() + INTERSECT_SIZE, box.maxZ() + INTERSECT_SIZE))
			.map(loadedShips::getIntersecting)
			.flatMap(iterator -> StreamSupport.stream(iterator.spliterator(), false))
			.forEach(intersecting -> this.shipToPos.put(intersecting.getId(), intersecting.getTransform().getPositionInWorld().add(offset, new Vector3d())));
	}

	private void handleTeleport() {
		this.shipToPos.forEach((id, newPos) -> {
			this.collectEntities(id, newPos);
			this.handleShipTeleport(id, newPos);
		});
		this.shipToPos.clear();
		this.teleportEntities();
	}

	private void collectEntities(final long id, final Vector3d shipNewPos) {
		final ServerShip ship = shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
			return;
		}
		final Vector3d transform = shipNewPos.sub(ship.getTransform().getPositionInWorld(), new Vector3d());
		// Entities in range
		final AABBd shipBoxd = ship.getWorldAABB();
		final AABBic shipBoxi = ship.getShipAABB();
		if (shipBoxi != null) {
			final AABBd shipYardBox = new AABBd(
				shipBoxi.minX(), shipBoxi.minY(), shipBoxi.minZ(),
				shipBoxi.maxX(), shipBoxi.maxY(), shipBoxi.maxZ()
			)
			for (final Entity entity : this.originalDim.getEntities(
				((Entity)(null)),
				new AABB(
					shipYardBox.minX - 16, shipYardBox.minY - 16, shipYardBox.minZ - 16,
					shipYardBox.maxX + 16, shipYardBox.maxY + 16, shipYardBox.maxZ + 16
				),
				(entity) -> !entity.isPassenger() && !this.entityToPos.containsKey(entity)
			)) {
				this.entityToPos.put(entity, entity.position());
			}
			shipBoxd.union(shipYardBox.transform(ship.getPrevTickTransform().getShipToWorld()));
		}
		final AABB inflatedBox = new AABB(
			shipBoxd.minX - INTERSECT_SIZE, shipBoxd.minY - INTERSECT_SIZE, shipBoxd.minZ - INTERSECT_SIZE,
			shipBoxd.maxX + INTERSECT_SIZE, shipBoxd.maxY + INTERSECT_SIZE, shipBoxd.maxZ + INTERSECT_SIZE
		);
		for (final Entity entity : this.originalDim.getEntities(
			((Entity)(null)),
			inflatedBox,
			(entity) -> !entity.isPassenger() && !this.entityToPos.containsKey(entity)
		)) {
			this.collectEntityWithTransform(entity, transform);
		}
	}

	private void collectEntityWithTransform(final Entity entity, final Vector3d transform) {
		this.entityToPos.put(entity, entity.position().add(transform.x, transform.y - this.greatestOffset, transform.z));
	}

	private void teleportEntities() {
		this.entityToPos.forEach((entity, newPos) -> {
			teleportToWithPassengers(entity, this.newDim, newPos);
		});
		this.entityToPos.clear();
	}

	private void handleShipTeleport(final long id, final Vector3d newPos) {
		final String vsDimName = VSGameUtilsKt.getDimensionId(this.newDim);
		final Vector3d targetPos = new Vector3d(newPos).add(0, -this.greatestOffset, 0);

		final LoadedServerShip ship = this.shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
			final PhysicsEntityServer physEntity = ((ShipObjectServerWorld) this.shipWorld).getLoadedPhysicsEntities().get(id);
			if (physEntity == null) {
				LOGGER.warn("[starlance]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity!");
				return;
			}
			LOGGER.info("[starlance]: Teleporting physics entity {} to {} {}", id, vsDimName, newPos);
			final ShipTeleportData teleportData = new ShipTeleportDataImpl(targetPos, physEntity.getShipTransform().getShipToWorldRotation(), physEntity.getLinearVelocity(), physEntity.getAngularVelocity(), vsDimName, null);
			this.shipWorld.teleportPhysicsEntity(physEntity, teleportData);
			return;
		}
		LOGGER.info("[starlance]: Teleporting ship {} ({}) to {} {}", ship.getSlug(), id, vsDimName, newPos);
		final Vector3dc veloctiy = new Vector3d(ship.getVelocity());
		final Vector3dc omega = new Vector3d(ship.getOmega());
		final ShipTeleportData teleportData = new ShipTeleportDataImpl(targetPos, ship.getTransform().getShipToWorldRotation(), veloctiy, omega, vsDimName, null);
		this.shipWorld.teleportShip(ship, teleportData);
		if (veloctiy.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					final LoadedServerShip ship2 = this.shipWorld.getLoadedShips().getById(id);
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

	private static <T extends Entity> T teleportToWithPassengers(final T entity, final ServerLevel newLevel, final Vec3 newPos) {
		final Vec3 oldPos = entity.position();
		final List<Entity> passengers = new ArrayList<>(entity.getPassengers());
		final T newEntity;
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
		for (final Entity p : passengers) {
			final Entity newPassenger = teleportToWithPassengers(p, newLevel, p.position().subtract(oldPos).add(newPos));
			if (newPassenger != null) {
				newPassenger.startRiding(newEntity, true);
			}
		}
		return newEntity;
	}
}
