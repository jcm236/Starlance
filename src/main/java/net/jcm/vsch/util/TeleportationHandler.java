package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.entity.ISpecialTeleportLogicEntity;
import net.jcm.vsch.mixin.valkyrienskies.accessor.ServerShipObjectWorldAccessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
import org.joml.primitives.AABBdc;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber
public class TeleportationHandler {

	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private static final double ENTITY_COLLECT_RANGE = 8;
	private static final double SHIP_COLLECT_RANGE = 10;

	private static Map<Long, Set<Integer>> SHIP2CONSTRAINTS;
	private static Map<Integer, VSConstraint> ID2CONSTRAINT;

	private final Long2ObjectOpenHashMap<Vector3d> shipToPos = new Long2ObjectOpenHashMap<>();
	private final Map<Entity, Vec3> entityToPos = new HashMap<>();
	private final ServerShipWorldCore shipWorld;
	private double greatestOffset;
	private final ServerLevel newDim;
	private final ServerLevel originalDim;
	private final boolean isReturning;

	public TeleportationHandler(final ServerLevel newDim, final ServerLevel originalDim, final boolean isReturning) {
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(newDim);
		this.newDim = newDim;
		this.originalDim = originalDim;
		// Look for the lowest ship when escaping, in order to not collide with the planet.
		// Look for the highest ship when reentering, in order to not collide with the atmosphere.
		this.isReturning = isReturning;
	}

	@SubscribeEvent
	public static void onServerStart(final ServerStartedEvent event) {
		final ServerShipObjectWorldAccessor server = (ServerShipObjectWorldAccessor) VSGameUtilsKt.getShipObjectWorld(event.getServer());
		SHIP2CONSTRAINTS = server.getShipIdToConstraints();
		ID2CONSTRAINT = server.getConstraints();
	}

	public void addShip(final ServerShip ship, final Vector3dc newPos) {
		final long shipId = ship.getId();
		if (this.shipToPos.containsKey(shipId)) {
			return;
		}
		this.greatestOffset = 0;
		final List<ServerShip> collected = new ArrayList<>();
		final Vector3dc origin = ship.getTransform().getPositionInWorld();
		this.collectShipAndConnected(shipId, origin, newPos, collected);
		this.collectNearbyShips(collected, origin, newPos);
		this.finalizeCollect(collected);
	}

	private void collectShipAndConnected(final long shipId, final Vector3dc origin, final Vector3dc newPos, final List<ServerShip> collected) {
		if (this.shipToPos.containsKey(shipId)) {
			return;
		}
		final ServerShip ship = this.getShip(shipId);
		if (ship == null) {
			return;
		}
		collected.add(ship);
		final Vector3dc pos = ship.getTransform().getPositionInWorld();

		// TODO: if planet collision position matters for reentry angle THIS SHOULD BE FIXED!! Currently a fix is not needed.
		final double offset = pos.y() - origin.y();
		if ((this.isReturning && offset > this.greatestOffset) || (!this.isReturning && offset < this.greatestOffset)) {
			this.greatestOffset = offset;
		}

		this.shipToPos.put(shipId, pos.sub(origin, new Vector3d()).add(newPos));

		final Set<Integer> constraints = SHIP2CONSTRAINTS.get(shipId);
		if (constraints != null) {
			constraints.stream().map(ID2CONSTRAINT::get).forEach((constraint) -> {
				this.collectShipAndConnected(constraint.getShipId0(), origin, newPos, collected);
				this.collectShipAndConnected(constraint.getShipId1(), origin, newPos, collected);
			});
		}
	}

	private void collectNearbyShips(final List<ServerShip> collected, final Vector3dc origin, final Vector3dc newPos) {
		final QueryableShipData<LoadedServerShip> loadedShips = this.shipWorld.getLoadedShips();
		final Vector3d offset = newPos.sub(origin, new Vector3d());
		// Note: collected list will grow during the loop
		for (int i = 0; i < collected.size(); i++) {
			final AABBdc shipBox = collected.get(i).getWorldAABB();
			final AABBd box = new AABBd(
				shipBox.minX() - SHIP_COLLECT_RANGE, shipBox.minY() - SHIP_COLLECT_RANGE, shipBox.minZ() - SHIP_COLLECT_RANGE,
				shipBox.maxX() + SHIP_COLLECT_RANGE, shipBox.maxY() + SHIP_COLLECT_RANGE, shipBox.maxZ() + SHIP_COLLECT_RANGE);
			for (final ServerShip ship : loadedShips.getIntersecting(box)) {
				this.collectShipAndConnected(ship.getId(), origin, newPos, collected);
			}
		}
	}

	private void finalizeCollect(final List<ServerShip> collected) {
		final double greatestOffset = -this.greatestOffset;
		for (final ServerShip ship : collected) {
			final long id = ship.getId();
			final Vector3d newPos = this.shipToPos.get(id);
			newPos.y += greatestOffset;
			this.collectEntities(id, newPos);
		}
	}

	public void finalizeTeleport() {
		this.shipToPos.forEach(this::handleShipTeleport);
		this.shipToPos.clear();
		this.teleportEntities();
	}

	private void collectEntities(final long id, final Vector3dc shipNewPos) {
		final ServerShip ship = this.getShip(id);
		if (ship == null) {
			return;
		}
		final Vector3d transform = shipNewPos.sub(ship.getTransform().getPositionInWorld(), new Vector3d());
		// Entities in range
		final AABBd shipBoxd = new AABBd(ship.getWorldAABB());
		final AABBic shipBoxi = ship.getShipAABB();
		if (shipBoxi != null) {
			final AABBd shipYardBox = new AABBd(
				shipBoxi.minX(), shipBoxi.minY(), shipBoxi.minZ(),
				shipBoxi.maxX(), shipBoxi.maxY(), shipBoxi.maxZ()
			);
			for (final Entity entity : this.originalDim.getEntities(
				((Entity)(null)),
				new AABB(
					shipYardBox.minX - 16 * 4, shipYardBox.minY - 16 * 4, shipYardBox.minZ - 16 * 4,
					shipYardBox.maxX + 16 * 4, shipYardBox.maxY + 16 * 4, shipYardBox.maxZ + 16 * 4
				),
				(entity) -> !this.entityToPos.containsKey(entity)
			)) {
				this.collectEntity(entity, transform);
			}
			shipBoxd.union(shipYardBox.transform(ship.getPrevTickTransform().getShipToWorld()));
		}
		final AABB inflatedBox = new AABB(
			shipBoxd.minX - ENTITY_COLLECT_RANGE, shipBoxd.minY - ENTITY_COLLECT_RANGE, shipBoxd.minZ - ENTITY_COLLECT_RANGE,
			shipBoxd.maxX + ENTITY_COLLECT_RANGE, shipBoxd.maxY + ENTITY_COLLECT_RANGE, shipBoxd.maxZ + ENTITY_COLLECT_RANGE
		);
		for (final Entity entity : this.originalDim.getEntities(
			((Entity)(null)),
			inflatedBox,
			(entity) -> !this.entityToPos.containsKey(entity)
		)) {
			this.collectEntity(entity, transform);
		}
	}

	private void collectEntity(final Entity entity, final Vector3d transform) {
		final Entity root = entity.getRootVehicle();
		if (this.entityToPos.containsKey(root)) {
			return;
		}
		Vec3 pos = root.position();
		if (!VSGameUtilsKt.isBlockInShipyard(this.originalDim, pos)) {
			pos = pos.add(transform.x, transform.y - this.greatestOffset, transform.z);
		}
		this.entityToPos.put(root, pos);
	}

	private void teleportEntities() {
		this.entityToPos.keySet().forEach((entity) -> {
			if (entity instanceof ISpecialTeleportLogicEntity specialEntity) {
				specialEntity.starlance$beforeTeleport();
			}
		});
		this.entityToPos.forEach((entity, newPos) -> {
			teleportToWithPassengers(entity, this.newDim, newPos);
		});
		this.entityToPos.clear();
	}

	private void handleShipTeleport(final long id, final Vector3dc newPos) {
		final String vsDimName = VSGameUtilsKt.getDimensionId(this.newDim);

		final LoadedServerShip ship = this.shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
			final PhysicsEntityServer physEntity = ((ShipObjectServerWorld) this.shipWorld).getLoadedPhysicsEntities().get(id);
			if (physEntity == null) {
				LOGGER.warn("[starlance]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity!");
				return;
			}
			LOGGER.info("[starlance]: Teleporting physics entity {} to {} {}", id, vsDimName, newPos);
			final ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos, physEntity.getShipTransform().getShipToWorldRotation(), physEntity.getLinearVelocity(), physEntity.getAngularVelocity(), vsDimName, null);
			this.shipWorld.teleportPhysicsEntity(physEntity, teleportData);
			return;
		}

		LOGGER.info("[starlance]: Teleporting ship {} ({}) to {} {}", ship.getSlug(), id, vsDimName, newPos);
		final Vector3dc veloctiy = new Vector3d(ship.getVelocity());
		final Vector3dc omega = new Vector3d(ship.getOmega());
		final ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos, ship.getTransform().getShipToWorldRotation(), veloctiy, omega, vsDimName, null);
		this.shipWorld.teleportShip(ship, teleportData);
		if (veloctiy.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					final LoadedServerShip ship2 = TeleportationHandler.this.shipWorld.getLoadedShips().getById(id);
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
		passengers.forEach((e) -> {
			if (e instanceof ISpecialTeleportLogicEntity specialEntity) {
				specialEntity.starlance$beforeTeleport();
			}
		});
		final T newEntity;
		if (entity instanceof ServerPlayer player) {
			player.teleportTo(newLevel, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
			newEntity = entity;
		} else {
			newEntity = (T) entity.getType().create(newLevel);
			if (newEntity == null) {
				if (entity instanceof ISpecialTeleportLogicEntity specialEntity) {
					specialEntity.starlance$afterTeleport(null);
				}
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
		if (newEntity instanceof ISpecialTeleportLogicEntity specialEntity) {
			specialEntity.starlance$afterTeleport((ISpecialTeleportLogicEntity)(entity));
		}
		return newEntity;
	}

	private ServerShip getShip(final long shipId) {
		final ServerShip ship = this.shipWorld.getLoadedShips().getById(shipId);
		if (ship != null) {
			return ship;
		}
		return this.shipWorld.getAllShips().getById(shipId);
	}
}
