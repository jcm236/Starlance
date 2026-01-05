package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreShipTravelEvent;
import net.jcm.vsch.ship.ShipLandingAttachment;

import com.github.litermc.vtil.api.connectivity.ShipConnectivityApi;
import com.github.litermc.vtil.api.teleport.TeleportUtil;
import com.github.litermc.vtil.util.ShipQuerier;
import com.github.litermc.vtil.util.TaskUtil;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.physics.PhysicsEntityServer;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class TeleportationHandler {

	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private static final double ENTITY_COLLECT_RANGE = 8;
	private static final double SHIP_COLLECT_RANGE = 10;

	private final Long2ObjectOpenHashMap<TeleportData> ships = new Long2ObjectOpenHashMap<>();
	private final Map<Entity, Vec3> entityToPos = new HashMap<>();
	private VsiServerShipWorld shipWorld;
	private double greatestOffset;
	private ServerLevel oldLevel;
	private ServerLevel newLevel;
	private String oldLevelId;
	private final boolean isReturning;
	private final List<CompletableFuture<Void>> addingShips = new ArrayList<>();

	public TeleportationHandler(final ServerLevel oldLevel, final ServerLevel newLevel, final boolean isReturning) {
		this.shipWorld = newLevel == null ? null : VSGameUtilsKt.getShipObjectWorld(newLevel);
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.oldLevelId = oldLevel == null ? null : VSGameUtilsKt.getDimensionId(oldLevel);
		// Look for the lowest ship when escaping, in order to not collide with the planet.
		// Look for the highest ship when reentering, in order to not collide with the atmosphere.
		this.isReturning = isReturning;
	}

	public void reset(final ServerLevel oldLevel, final ServerLevel newLevel) {
		this.shipWorld = newLevel == null ? null : VSGameUtilsKt.getShipObjectWorld(newLevel);
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.oldLevelId = oldLevel == null ? null : VSGameUtilsKt.getDimensionId(oldLevel);
		this.ships.clear();
		this.entityToPos.clear();
	}

	public boolean addedShip() {
		return !this.addingShips.isEmpty();
	}

	public boolean hasShip(final ServerShip ship) {
		return this.ships.containsKey(ship.getId());
	}

	public void addShip(final ServerShip ship, final Vector3dc newPos, final Quaterniondc rotation) {
		this.addShipWithVelocity(ship, newPos, rotation, null, null);
	}

	public void addShipWithVelocity(final ServerShip ship, final Vector3dc newPos, final Quaterniondc rotation, final Vector3dc velocity, final Vector3dc omega) {
		final long shipId = ship.getId();
		if (this.ships.containsKey(shipId)) {
			return;
		}
		this.greatestOffset = 0;
		final List<Long> collected = new ArrayList<>();
		final Vector3dc origin = ship.getTransform().getPositionInWorld();
		this.addingShips.add(
			this.collectShipAndConnectedWithVelocity(shipId, origin, newPos, rotation, velocity, omega, collected)
				.thenAcceptAsync((void_) -> {
					this.collectNearbyEntities(collected, origin, newPos, rotation);
					this.finalizeCollect(collected, rotation);
				}, this.oldLevel.getServer())
		);
	}

	private LoadedServerShip getOldShip(final long id) {
		final LoadedServerShip ship = this.shipWorld.getLoadedShips().getById(id);
		return ship != null && ship.getChunkClaimDimension().equals(this.oldLevelId) ? ship : null;
	}

	public CompletableFuture<Void> afterShipsAdded() {
		if (this.addingShips.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.allOf(this.addingShips.toArray(new CompletableFuture[this.addingShips.size()]));
	}

	public List<LoadedServerShip> getPendingShips() {
		final List<LoadedServerShip> ships = new ArrayList<>(this.ships.size());
		for (final long id : this.ships.keySet()) {
			final LoadedServerShip ship = this.getOldShip(id);
			if (ship != null) {
				ships.add(ship);
			}
		}
		return ships;
	}

	private void collectShipWithVelocity(
		final long shipId,
		final Vector3dc origin,
		final Vector3dc newPos,
		final Quaterniondc rotation,
		Vector3dc velocity,
		Vector3dc omega,
		final List<Long> collected
	) {
		if (this.ships.containsKey(shipId)) {
			return;
		}
		final LoadedServerShip ship = this.getOldShip(shipId);
		if (ship == null) {
			return;
		}
		final Vector3dc pos = ship.getTransform().getPositionInWorld();
		final ShipLandingAttachment landingAttachment = ship.getAttachment(ShipLandingAttachment.class);
		if (ship.isStatic() && landingAttachment.freezed) {
			velocity = landingAttachment.velocity;
			omega = landingAttachment.omega;
		} else {
			if (velocity == null) {
				velocity = new Vector3d(ship.getVelocity());
			}
			if (omega == null) {
				omega = new Vector3d(ship.getOmega());
			}
		}
		collected.add(shipId);

		final Vector3d relPos = pos.sub(origin, new Vector3d());
		final Quaterniond newRotataion = new Quaterniond(ship.getTransform().getShipToWorldRotation());

		if (!this.isReturning) {
			final double offset = relPos.y;
			if (offset < this.greatestOffset) {
				this.greatestOffset = offset;
			}
		}

		rotation.transform(relPos);
		velocity = rotation.transform(velocity, new Vector3d());
		newRotataion.mul(rotation).normalize();

		if (this.isReturning) {
			final double offset = relPos.y;
			if (offset > this.greatestOffset) {
				this.greatestOffset = offset;
			}
		}

		relPos.add(newPos);
		final Vector3d velocity0 = new Vector3d(velocity);
		final Vector3d omega0 = new Vector3d(omega);

		MinecraftForge.EVENT_BUS.post(this.createPreShipTravelEvent(
			ship, oldLevel.dimension(), newLevel.dimension(), relPos, newRotataion, velocity0, omega0
		));

		this.ships.put(
			shipId,
			new TeleportData(
				relPos,
				newRotataion,
				velocity0,
				omega0
			)
		);
	}

	private CompletableFuture<Void> collectShipAndConnected(final long shipId, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation, final List<Long> collected) {
		return this.collectShipAndConnectedWithVelocity(shipId, origin, newPos, rotation, null, null, collected);
	}

	private CompletableFuture<Void> collectShipAndConnectedWithVelocity(
		final long shipId,
		final Vector3dc origin,
		final Vector3dc newPos,
		final Quaterniondc rotation,
		final Vector3dc velocity,
		final Vector3dc omega,
		final List<Long> collected
	) {
		final CompletableFuture<long[]> connectivityFuture = new CompletableFuture();
		final CompletableFuture<Void> collectFuture = connectivityFuture.thenAcceptAsync((shipIds) -> {
			for (final long sId : shipIds) {
				if (sId == shipId) {
					this.collectShipWithVelocity(sId, origin, newPos, rotation, velocity, omega, collected);
				} else {
					this.collectShipWithVelocity(sId, origin, newPos, rotation, null, null, collected);
				}
			}
		}, this.oldLevel.getServer());
		TaskUtil.queuePhysicsTick(this.oldLevel, (world0) -> {
			final VsiPhysLevel world = (VsiPhysLevel) world0;
			final List<PhysShip> ships = new ArrayList<>();
			ships.add(world.getShipById(shipId));
			this.collectConnectedAndNearbyShips(world, ships);
			connectivityFuture.complete(ships.stream().mapToLong(PhysShip::getId).toArray());
		});
		return collectFuture;
	}

	private void collectConnectedAndNearbyShips(
		final VsiPhysLevel world,
		final List<PhysShip> physShips
	) {
		final Set<PhysShip> physShipSet = new HashSet<>(physShips);
		final Set<PhysShip> connectedShips = new HashSet<>();
		final List<PhysShip> intersectShips = new ArrayList<>();
		// Note: collected list will grow during the loop
		for (int i = 0; i < physShips.size(); i++) {
			final PhysShip ship = physShips.get(i);
			final AABBdc shipBox = ship.getWorldAABB();
			final AABBd box = new AABBd(
				shipBox.minX() - SHIP_COLLECT_RANGE, shipBox.minY() - SHIP_COLLECT_RANGE, shipBox.minZ() - SHIP_COLLECT_RANGE,
				shipBox.maxX() + SHIP_COLLECT_RANGE, shipBox.maxY() + SHIP_COLLECT_RANGE, shipBox.maxZ() + SHIP_COLLECT_RANGE
			);
			ShipConnectivityApi.getAllConnectedShipsAndSelf(world, ship.getId(), connectedShips);
			for (final PhysShip other : connectedShips) {
				if (physShipSet.add(other)) {
					physShips.add(other);
				}
			}
			connectedShips.clear();
			ShipQuerier.getIntersecting(
				world,
				this.oldLevelId,
				box,
				(other) -> !physShipSet.contains(other),
				intersectShips
			);
			physShipSet.addAll(intersectShips);
			physShips.addAll(intersectShips);
			intersectShips.clear();
		}
	}

	private void collectNearbyEntities(final List<Long> collected, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		for (final Long shipId : collected) {
			final ServerShip ship = this.getOldShip(shipId);
			if (ship == null) {
				continue;
			}
			this.collectEntities(ship, origin, newPos, rotation);
		}
	}

	private void finalizeCollect(final List<Long> collected, final Quaterniondc rotation) {
		final Vector3d offset = new Vector3d(0, -this.greatestOffset, 0);
		if (!this.isReturning) {
			rotation.transform(offset);
		}
		for (final Long id : collected) {
			final Vector3d newPos = this.ships.get(id).newPos();
			newPos.add(offset);
		}
	}

	public void finalizeTeleport() {
		final int size = this.ships.size();
		if (size == 0) {
			return;
		}
		this.ships.forEach(this::handleShipTeleport);
		this.ships.clear();
		if (size >= 256) {
			this.ships.trim(32);
		}
		this.teleportEntities();
	}

	private void collectEntities(final ServerShip ship, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		// Entities in range
		final AABBd shipBoxd = new AABBd(ship.getWorldAABB());
		final AABBic shipBoxi = ship.getShipAABB();
		if (shipBoxi != null) {
			final AABBd shipYardBox = new AABBd(
				shipBoxi.minX(), shipBoxi.minY(), shipBoxi.minZ(),
				shipBoxi.maxX(), shipBoxi.maxY(), shipBoxi.maxZ()
			);
			for (final Entity entity : this.oldLevel.getEntities(
				((Entity)(null)),
				new AABB(
					shipYardBox.minX - 16 * 4, shipYardBox.minY - 16 * 4, shipYardBox.minZ - 16 * 4,
					shipYardBox.maxX + 16 * 4, shipYardBox.maxY + 16 * 4, shipYardBox.maxZ + 16 * 4
				),
				(entity) -> !this.entityToPos.containsKey(entity)
			)) {
				this.collectEntity(entity, origin, newPos, rotation);
			}
			shipBoxd.union(shipYardBox.transform(ship.getPrevTickTransform().getShipToWorld()));
		}
		final AABB inflatedBox = new AABB(
			shipBoxd.minX - ENTITY_COLLECT_RANGE, shipBoxd.minY - ENTITY_COLLECT_RANGE, shipBoxd.minZ - ENTITY_COLLECT_RANGE,
			shipBoxd.maxX + ENTITY_COLLECT_RANGE, shipBoxd.maxY + ENTITY_COLLECT_RANGE, shipBoxd.maxZ + ENTITY_COLLECT_RANGE
		);
		for (final Entity entity : this.oldLevel.getEntities(
			((Entity)(null)),
			inflatedBox,
			(entity) -> !this.entityToPos.containsKey(entity)
		)) {
			this.collectEntity(entity, origin, newPos, rotation);
		}
	}

	private void collectEntity(final Entity entity, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		final Entity root = entity.getRootVehicle();
		if (this.entityToPos.containsKey(root)) {
			return;
		}
		Vec3 pos = root.position();
		if (!VSGameUtilsKt.isBlockInShipyard(this.oldLevel, pos)) {
			final Vector3d relPos = new Vector3d(pos.x, pos.y, pos.z).sub(origin);
			rotation.transform(relPos);
			relPos.add(newPos);
			pos = new Vec3(relPos.x, relPos.y, relPos.z);
		}
		this.entityToPos.put(root, pos);
	}

	private void teleportEntities() {
		this.entityToPos.forEach((entity, newPos) -> {
			TeleportUtil.teleportEntity(entity, this.newLevel, newPos);
		});
		this.entityToPos.clear();
	}

	private void handleShipTeleport(final long id, final TeleportData data) {
		final String vsDimName = VSGameUtilsKt.getDimensionId(this.newLevel);
		final Vector3dc newPos = data.newPos();
		final Quaterniondc rotation = data.rotation();
		final Vector3dc velocity = data.velocity();
		final Vector3dc omega = data.omega();

		final LoadedServerShip ship = this.getOldShip(id);
		if (ship == null) {
			final PhysicsEntityServer physEntity = this.shipWorld.retrieveLoadedPhysicsEntities().get(id);
			if (physEntity == null) {
				LOGGER.warn("[starlance]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity! Or not in the same dimension anymore!");
				return;
			}
			LOGGER.info("[starlance]: Teleporting physics entity {} to {} {}", id, vsDimName, newPos);
			final ShipTeleportData teleportData = ValkyrienSkiesMod.getVsCore().newShipTeleportData(
				newPos,
				physEntity.getShipTransform().getShipToWorldRotation(),
				physEntity.getLinearVelocity(),
				physEntity.getAngularVelocity(),
				vsDimName,
				null,
				physEntity.getShipTransform().getPositionInShip()
			);
			this.shipWorld.teleportPhysicsEntity(physEntity, teleportData);
			return;
		}

		LOGGER.info("[starlance]: Teleporting ship {} ({}) to {} {}", ship.getSlug(), id, vsDimName, newPos);
		ship.setStatic(false);
		TeleportUtil.teleportShip(ship, new TeleportUtil.TeleportData(this.newLevel, newPos, rotation, velocity, omega));
	}

	private PreShipTravelEvent createPreShipTravelEvent(
		final ServerShip ship,
		final ResourceKey<Level> oldLevel,
		final ResourceKey<Level> newLevel,
		final Vector3dc position,
		final Quaterniondc rotation,
		final Vector3d velocity,
		final Vector3d omega
	) {
		return this.isReturning
			? new PreShipTravelEvent.SpaceToPlanet(ship, oldLevel, newLevel, position, rotation, velocity, omega)
			: new PreShipTravelEvent.PlanetToSpace(ship, oldLevel, newLevel, position, rotation, velocity, omega);
	}

	private record TeleportData(Vector3d newPos, Quaterniond rotation, Vector3dc velocity, Vector3dc omega) {}
}
