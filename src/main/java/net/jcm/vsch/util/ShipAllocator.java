package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Quaterniond;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public final class ShipAllocator extends SavedData {
	private static final String DATA_NAME = VSCHMod.MODID + "_AllocatedShips";
	private static final String CACHED_SHIPS_TAG = "CachedShips";
	public static final String IDLE_SHIP_PREFIX = "+idle+";

	private final ServerLevel level;
	private final ServerShipWorldCore shipWorld;
	private final String dimId;
	private final Set<ServerShip> avaliableShips = new HashSet<>();

	private ShipAllocator(final ServerLevel level) {
		this.level = level;
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		this.dimId = VSGameUtilsKt.getDimensionId(level);
	}

	public static ShipAllocator get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(data -> ShipAllocator.load(level, data), () -> new ShipAllocator(level), DATA_NAME);
	}

	public static ShipAllocator load(final ServerLevel level, final CompoundTag data) {
		System.out.println("loading ShipAllocator " + data);
		final ShipAllocator allocator = new ShipAllocator(level);
		final QueryableShipData<ServerShip> shipStorage = allocator.shipWorld.getAllShips();
		final long[] ids = data.getLongArray(CACHED_SHIPS_TAG);
		for (long id : ids) {
			final ServerShip ship = shipStorage.getById(id);
			if (ship != null && allocator.dimId.equals(ship.getChunkClaimDimension())) {
				System.out.println("found idle ship " + ship.getId());
				allocator.avaliableShips.add(ship);
			}
		}
		return allocator;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray(CACHED_SHIPS_TAG, this.avaliableShips.stream().mapToLong(ServerShip::getId).toArray());
		return data;
	}

	public boolean putShip(final ServerShip ship) {
		if (!this.dimId.equals(ship.getChunkClaimDimension())) {
			this.shipWorld.deleteShip(ship);
			return false;
		}
		ship.setSlug(IDLE_SHIP_PREFIX + ship.getId());
		ship.setStatic(true);
		clearShip(this.level, ship);

		final ShipTeleportData teleportData = new ShipTeleportDataImpl(new Vector3d(0, -1e8, 0), new Quaterniond(), new Vector3d(), new Vector3d(), this.dimId, 1e-6);
		this.shipWorld.teleportShip(ship, teleportData);

		this.avaliableShips.add(ship);
		this.setDirty(true);
		return true;
	}

	public ServerShip allocShip(final Vector3i pos) {
		final Iterator<ServerShip> iterator = this.avaliableShips.iterator();
		if (iterator.hasNext()) {
			final ServerShip ship = iterator.next();
			System.out.println("reusing ship " + ship.getId());
			iterator.remove();
			this.setDirty(true);
			clearShip(this.level, ship);

			final ShipTeleportData teleportData = new ShipTeleportDataImpl(new Vector3d(pos).add(0.5, 0.5, 0.5), new Quaterniond(), new Vector3d(), new Vector3d(), this.dimId, 1.0);
			this.shipWorld.teleportShip(ship, teleportData);

			ship.setStatic(false);
			ship.setSlug(null);

			return ship;
		}
		final ServerShip ship = this.shipWorld.createNewShipAtBlock(pos, false, 1.0, this.dimId);
		return ship;
	}

	private static void clearShip(final ServerLevel level, final ServerShip ship) {
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return;
		}
		for (int x = box.minX(); x < box.maxX(); x++) {
			for (int z = box.minZ(); z < box.maxZ(); z++) {
				for (int y = box.minY(); y < box.maxY(); y++) {
					level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
				}
			}
		}
		level.setBlock(new BlockPos(box.minX(), box.minY(), box.minZ()), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);
	}

	public static class SafeShipIterable implements Iterable<Ship> {
		private final Iterable<? extends Ship> ships;

		public SafeShipIterable(Iterable<? extends Ship> ships) {
			this.ships = ships;
		}

		@Override
		public Iterator<Ship> iterator() {
			return new SafeShipIterator(this.ships.iterator());
		}
	}

	public static class SafeShipIterator implements Iterator<Ship> {
		private final Iterator<? extends Ship> ships;
		private Ship nextShip = null;

		public SafeShipIterator(Iterator<? extends Ship> ships) {
			this.ships = ships;
		}

		@Override
		public boolean hasNext() {
			if (this.nextShip != null) {
				return true;
			}
			while (this.ships.hasNext()) {
				final Ship ship = this.ships.next();
				final String slug = ship.getSlug();
				if (slug == null || !slug.startsWith(IDLE_SHIP_PREFIX)) {
					this.nextShip = ship;
					return true;
				}
			}
			return false;
		}

		@Override
		public Ship next() {
			if (this.nextShip == null) {
				throw new NoSuchElementException();
			}
			final Ship ship = this.nextShip;
			this.nextShip = null;
			return ship;
		}
	}
}
