package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.ShipAllocator;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.world.ForgeChunkManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class AsteroidGenerator extends SavedData {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private static final String DATA_NAME = VSCHMod.MODID + "_Asteroids";
	public static final String ASTEROID_SHIP_PREFIX = "+asteroid+";
	public static final String PENDING_ASTEROID_SHIP_PREFIX = "+idle+pending_asteroid+";

	private static final int MAX_ASTEROID_COUNT = 128;
	private static final int MIN_REGENERATE_DIST = 4;
	private static final int MAX_CLUSTERS_PER_ROUND = 8;
	private static final int MAX_ASTEROIDS_PER_CLUSTER = 8;
	private static final int MIN_SPAWN_DIST = 16 * 8;
	private static final int SPAWN_RANGE = 16 * 16;
	private static final int MAX_SPAWN_DIST = MIN_SPAWN_DIST + SPAWN_RANGE;
	private static final int MIN_SPAWN_Y = -5000;
	private static final int MAX_SPAWN_Y = 5000;
	private static final int DISCARD_DIST = MAX_SPAWN_DIST + 16 * MIN_REGENERATE_DIST;

	private static final Vector3d MAGIC_POS = new Vector3d(1e8, -1e8, 1e8);

	private static final Random RNG = new Random();
	private static final Map<ServerPlayer, LevelChunkPos> PLAYER_LAST_POS = new HashMap<>();

	private final ServerLevel level;
	private final ServerShipWorldCore shipWorld;
	private final String dimId;
	private final Set<ServerShip> avaliableAsteroids = new HashSet<>();

	private AsteroidGenerator(final ServerLevel level) {
		this.level = level;
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		this.dimId = VSGameUtilsKt.getDimensionId(level);
	}

	public static AsteroidGenerator get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(data -> AsteroidGenerator.load(level, data), () -> new AsteroidGenerator(level), DATA_NAME);
	}

	public static AsteroidGenerator load(final ServerLevel level, final CompoundTag data) {
		final AsteroidGenerator generator = new AsteroidGenerator(level);
		final QueryableShipData<ServerShip> shipStorage = generator.shipWorld.getAllShips();
		final long[] ids = data.getLongArray("Avaliable");
		for (long id : ids) {
			final ServerShip ship = shipStorage.getById(id);
			if (ship != null && generator.dimId.equals(ship.getChunkClaimDimension())) {
				generator.avaliableAsteroids.add(ship);
			}
		}
		return generator;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray("Avaliable", this.avaliableAsteroids.stream().mapToLong(ServerShip::getId).toArray());
		return data;
	}

	public static void tickLevel(final ServerLevel level) {
		// TODO: add asteroid belt radius instead
		final boolean isSpace = CosmosModVariables.WorldVariables.get(level).dimension_type.getString(level.dimension().location().toString()).equals("space");
		if (!isSpace) {
			return;
		}
		final ShipAllocator allocator = ShipAllocator.get(level);
		final AsteroidGenerator generator = get(level);
		final List<ServerPlayer> players = level.getPlayers(player -> true);
		int asteroidCount = generator.avaliableAsteroids.size();
		int removed = 0;
		for (final ServerShip ship : VSCHUtils.getShipsInLevel(level)) {
			if (!isAsteroidShip(ship)) {
				continue;
			}
			if (removed < 3 && canDespawnAsteroid(level, players, ship)) {
				LOGGER.info("[starlance]: removing asteroid {}", ship.getSlug());
				allocator.putShip(ship);
				removed++;
			} else {
				asteroidCount++;
			}
		}
		if (asteroidCount < MAX_ASTEROID_COUNT) {
			System.out.println("asteroidCount: " + asteroidCount + " / " + MAX_ASTEROID_COUNT);
			for (int i = 0; i < 10; i++) {
				final ServerShip ship = createAsteroid(level);
				if (ship != null) {
					generator.avaliableAsteroids.add(ship);
					generator.setDirty(true);
					break;
				}
			}
		}
		for (final ServerPlayer player : players) {
			generator.tickPlayer(players, player);
		}
	}

	private void tickPlayer(final List<ServerPlayer> players, final ServerPlayer player) {
		final BlockPos blockPos = player.blockPosition();
		if (blockPos.getY() < MIN_SPAWN_Y || MAX_SPAWN_Y < blockPos.getY()) {
			return;
		}
		final ChunkPos chunkPos = new ChunkPos(blockPos);

		// TODO: get the radius from datamap
		final int ASTEROID_MIN_SPAWN_RADIUS = 2750;
		final int ASTEROID_MAX_SPAWN_RADIUS = 3500;

		final int ASTEROID_MIN_SPAWN_RADIUS2 = ASTEROID_MIN_SPAWN_RADIUS - MIN_SPAWN_DIST / 16;
		final int ASTEROID_MAX_SPAWN_RADIUS2 = ASTEROID_MAX_SPAWN_RADIUS + MIN_SPAWN_DIST / 16;
		if (ASTEROID_MAX_SPAWN_RADIUS2 < Math.abs(chunkPos.x) || ASTEROID_MAX_SPAWN_RADIUS2 < Math.abs(chunkPos.z) || (Math.abs(chunkPos.x) < ASTEROID_MIN_SPAWN_RADIUS2 && Math.abs(chunkPos.z) < ASTEROID_MIN_SPAWN_RADIUS2)) {
			return;
		}
		final LevelChunkPos lastPos = PLAYER_LAST_POS.get(player);
		if (lastPos != null && lastPos.level() == this.level && lastPos.chunk().getChessboardDistance(chunkPos) < MIN_REGENERATE_DIST) {
			return;
		}
		PLAYER_LAST_POS.put(player, new LevelChunkPos(this.level, chunkPos));

		for (int i = 0; i < MAX_CLUSTERS_PER_ROUND; i++) {
			this.generateFrom(players, blockPos);
		}
	}

	private void generateFrom(final List<ServerPlayer> players, final BlockPos origin) {
		final BlockPos.MutableBlockPos newPos = origin.mutable().move(randPosInRange(MIN_SPAWN_DIST, MAX_SPAWN_DIST, 0, 64, MIN_SPAWN_DIST, MAX_SPAWN_DIST));
		if (!canSpawnAsteroid(this.level, players, newPos)) {
			return;
		}
		for (int i = 0; i < MAX_ASTEROIDS_PER_CLUSTER; i++) {
			final ServerShip ship = this.spawnAsteroid(newPos);
			if (ship == null) {
				return;
			}

			LOGGER.info("asteroid {} spawned at {}", ship.getSlug(), newPos);

			final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
			int t = 0;
			do {
				t++;
				if (t > 10) {
					return;
				}
				nextPos.setWithOffset(newPos, randPosInRange(32, 64));
			} while (!canSpawnAsteroid(this.level, players, newPos));
			newPos.set(nextPos);
		}
	}

	public static boolean isAsteroidShip(Ship ship) {
		if (ship == null) {
			return false;
		}
		final String slug = ship.getSlug();
		return slug != null && slug.startsWith(ASTEROID_SHIP_PREFIX);
	}

	private static boolean canSpawnAsteroid(ServerLevel level, List<ServerPlayer> players, BlockPos pos) {
		if (pos.getY() < MIN_SPAWN_Y || MAX_SPAWN_Y < pos.getY()) {
			return false;
		}
		final ChunkPos chunkPos = new ChunkPos(pos);
		// TODO: get the radius from datamap
		final int ASTEROID_MIN_SPAWN_RADIUS = 2750;
		final int ASTEROID_MAX_SPAWN_RADIUS = 3500;
		if (ASTEROID_MAX_SPAWN_RADIUS < Math.abs(chunkPos.x) || ASTEROID_MAX_SPAWN_RADIUS < Math.abs(chunkPos.z) || (Math.abs(chunkPos.x) < ASTEROID_MIN_SPAWN_RADIUS && Math.abs(chunkPos.z) < ASTEROID_MIN_SPAWN_RADIUS)) {
			return false;
		}
		for (final ServerPlayer player : players) {
			if (player.blockPosition().distSqr(pos) < MIN_SPAWN_DIST * MIN_SPAWN_DIST) {
				return false;
			}
		}
		int asteroidCount = 0;
		for (Ship ship : VSCHUtils.getShipsInLevel(level)) {
			AABB box = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB());
			if (isAsteroidShip(ship)) {
				asteroidCount++;
				if (asteroidCount >= MAX_ASTEROID_COUNT) {
					return false;
				}
				box = box.inflate(32);
			} else {
				box = box.inflate(MIN_SPAWN_DIST);
			}
			if (box.contains(pos.getX(), pos.getY(), pos.getZ())) {
				return false;
			}
		}
		return true;
	}

	public static boolean canDespawnAsteroid(ServerLevel level, List<ServerPlayer> players, Ship ship) {
		if (!isAsteroidShip(ship)) {
			return false;
		}
		final Vector3dc pos = ship.getTransform().getPositionInWorld();
		final BlockPos blockPos = BlockPos.containing(pos.x(), pos.y(), pos.z());
		boolean shouldRand = false;
		for (final ServerPlayer player : players) {
			final double dist = player.blockPosition().distSqr(blockPos);
			if (dist < DISCARD_DIST * DISCARD_DIST) {
				return false;
			}
		}
		return true;
	}

	private ServerShip spawnAsteroid(final BlockPos pos) {
		final Iterator<ServerShip> iterator = this.avaliableAsteroids.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		final ServerShip ship = iterator.next();
		iterator.remove();
		this.setDirty(true);

		ship.setSlug(ASTEROID_SHIP_PREFIX + ship.getId());

		final Vector3d vel = new Vector3d(0, 0, 0); // TODO: give random velocity
		final ShipTeleportData teleportData = new ShipTeleportDataImpl(new Vector3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, 0.5, 0.5), new Quaterniond(), vel, new Vector3d(), this.dimId, 1.0);
		this.shipWorld.teleportShip(ship, teleportData);

		ship.setStatic(false);
		return ship;
	}

	private static ServerShip createAsteroid(final ServerLevel level) {
		final Map<Vec3i, BlockState> blocks = generateAsteroid(level);
		if (blocks == null || blocks.isEmpty()) {
			return null;
		}
		final ServerShip ship = ShipAllocator.get(level).allocShip(MAGIC_POS, 1e-6);
		ship.setSlug(PENDING_ASTEROID_SHIP_PREFIX + ship.getId());
		ship.setStatic(true);
		final ChunkClaim claim = ship.getChunkClaim();
		final Vector3i center = claim.getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final BlockPos centerPos = new BlockPos(center.x, center.y, center.z);
		blocks.forEach((offset, state) -> level.setBlock(centerPos.offset(offset), state, 0));
		return ship;
	}

	private static Map<Vec3i, BlockState> generateAsteroid(final ServerLevel level) {
		final BlockState stone;
		final List<BlockState> ores = new ArrayList<>();
		final int typeRand = RNG.nextInt(100);
		if (typeRand < 15) {
			// stone asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			ores.add(Blocks.STONE.defaultBlockState());
		} else if (typeRand < 25) {
			// stone-ice asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			ores.add(Blocks.ICE.defaultBlockState());
			ores.add(Blocks.BLUE_ICE.defaultBlockState());
		} else if (typeRand < 30) {
			// pure ice asteroid
			stone = Blocks.ICE.defaultBlockState();
			ores.add(Blocks.PACKED_ICE.defaultBlockState());
			ores.add(Blocks.BLUE_ICE.defaultBlockState());
		} else if (typeRand < 31) {
			// debris asteroid
			stone = Blocks.MAGMA_BLOCK.defaultBlockState();
			ores.add(Blocks.ANCIENT_DEBRIS.defaultBlockState());
		} else if (typeRand < 35) {
			// obsidian asteroid
			stone = Blocks.OBSIDIAN.defaultBlockState();
			ores.add(Blocks.CRYING_OBSIDIAN.defaultBlockState());
		} else if (typeRand < 60) {
			// nether ore asteroid
			stone = Blocks.NETHERRACK.defaultBlockState();
			ores.add(Blocks.MAGMA_BLOCK.defaultBlockState());
			ores.add(Blocks.GLOWSTONE.defaultBlockState());
			BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).forEach(holder -> ores.add(holder.value().defaultBlockState()));
		} else {
			// earth ore asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).forEach(holder -> ores.add(holder.value().defaultBlockState()));
		}
		// TODO: special structure & loot chests etc.
		return generateEllipsoidAsteroid(level, stone, ores.get(RNG.nextInt(ores.size())));
	}

	private static Map<Vec3i, BlockState> generateEllipsoidAsteroid(final ServerLevel level, final BlockState stone, final BlockState ore) {
		final int MAX_RADIUS = 10;
		final int MIN_RADIUS = 2;
		final int MAX_PITS = 10;
		final Map<Vec3i, BlockState> blocks = new HashMap<>();
		final List<Vec3i> outsidePos = new ArrayList<>();
		// generate an ellipsoid
		final int radiusX = RNG.nextInt(MAX_RADIUS - MIN_RADIUS + 1) + MIN_RADIUS;
		final int radiusY = RNG.nextInt(MAX_RADIUS - MIN_RADIUS + 1) + MIN_RADIUS;
		final int radiusZ = RNG.nextInt(MAX_RADIUS - MIN_RADIUS + 1) + MIN_RADIUS;
		final Vec3i dim = new Vec3i(radiusX, radiusY, radiusZ);
		for (int x = 0; x <= radiusX; x++) {
			for (int y = 0; y <= radiusY; y++) {
				for (int z = 0; z <= radiusZ; z++) {
					final Vec3i pos = new Vec3i(x, y, z);
					final double r = (double) (x * x) / (radiusX * radiusX) + (double) (y * y) / (radiusY * radiusY) + (double) (z * z) / (radiusZ * radiusZ);
					if (r > 1) {
						allDirVec3iStream(pos).forEach(outsidePos::add);
						continue;
					}
					if (r == 1) {
						allDirVec3iStream(pos).forEach(outsidePos::add);
					}
					allDirVec3iStream(pos).forEach(p -> blocks.put(p, generateAsteroidBlock(p, dim, r, stone, ore)));
				}
			}
		}
		// generate pits
		final Set<Integer> pits = new HashSet<>();
		final int pitMaxR = midNumber(radiusX, radiusY, radiusZ);
		for (int i = MAX_PITS * (radiusX * radiusY * radiusZ) / (MAX_RADIUS * MAX_RADIUS * MAX_RADIUS); i >= 0; i--) {
			int index = RNG.nextInt(outsidePos.size());
			if (!pits.add(index)) {
				continue;
			}
			final Vec3i c = outsidePos.get(index);
			final int r = RNG.nextInt(pitMaxR);
			final int r2 = r * r;
			for (int x = 0; x <= r; x++) {
				int xy;
				for (int y = 0; (xy = x * x + y * y) <= r2; y++) {
					for (int z = 0; xy + z * z <= r2; z++) {
						allDirVec3iStream(new Vec3i(x, y, z)).map(c::offset).forEach(blocks::remove);
					}
				}
			}
		}
		return blocks;
	}

	private static BlockState generateAsteroidBlock(final Vec3i pos, final Vec3i dim, final double r, final BlockState stone, final BlockState ore) {
		double f = RNG.nextDouble() + (RNG.nextInt(10) == 0 ? 0.01 : 0);
		return f * f >= r ? ore : stone;
	}

	private static int randPNInt(int min, int max) {
		final int range = max - min;
		final int n = RNG.nextInt(range * 2 + 1) - range;
		return n > 0 ? n + min - 1 : n - min;
	}

	private static Vec3i randPosInRange(int min, int max) {
		return randPosInRange(min, max, min, max, min, max);
	}

	private static Vec3i randPosInRange(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		return switch (RNG.nextInt(2)) {
			case 0 -> randPosInRange0(minX, maxX, minY, maxY, minZ, maxZ);
			case 1 -> {
				final Vec3i pos = randPosInRange0(minZ, maxZ, minY, maxY, minX, maxX);
				yield new Vec3i(pos.getZ(), pos.getY(), pos.getX());
			}
			case 2 -> {
				final Vec3i pos = randPosInRange0(minX, maxX, minZ, maxZ, minY, maxY);
				yield new Vec3i(pos.getX(), pos.getZ(), pos.getY());
			}
			default -> throw new RuntimeException("unreachable");
		};
	}

	private static Vec3i randPosInRange0(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		final int x = RNG.nextInt(maxX * 2 + 1) - maxX;
		final int y = RNG.nextInt(maxY * 2 + 1) - maxY;
		final int z = (Math.abs(x) >= minX || Math.abs(y) >= minY) ? RNG.nextInt(maxZ * 2 + 1) - maxZ : randPNInt(minZ, maxZ);
		return new Vec3i(x, y, z);
	}

	private static int midNumber(int a, int b, int c) {
		if (a < b) {
			return a > c ? a : b < c ? b : c;
		}
		return a < c ? a : b > c ? b : c;
	}

	private static Stream<Vec3i> allDirVec3iStream(Vec3i base) {
		Stream.Builder<Vec3i> builder = Stream.builder();
		final boolean flipX = base.getX() != 0;
		final boolean flipY = base.getY() != 0;
		final boolean flipZ = base.getZ() != 0;
		builder.add(base);
		if (flipZ) {
			builder.add(new Vec3i(base.getX(), base.getY(), -base.getZ()));
		}
		if (flipY) {
			builder.add(new Vec3i(base.getX(), -base.getY(), base.getZ()));
			if (flipZ) {
				builder.add(new Vec3i(base.getX(), -base.getY(), -base.getZ()));
			}
		}
		if (flipX) {
			builder.add(new Vec3i(-base.getX(), base.getY(), base.getZ()));
			if (flipZ) {
				builder.add(new Vec3i(-base.getX(), base.getY(), -base.getZ()));
			}
			if (flipY) {
				builder.add(new Vec3i(-base.getX(), -base.getY(), base.getZ()));
				if (flipZ) {
					builder.add(new Vec3i(-base.getX(), -base.getY(), -base.getZ()));
				}
			}
		}
		return builder.build();
	}

	private static void clearShip(final ServerLevel level, final ServerShip ship) {
		clearShipWith(level, ship, Blocks.AIR.defaultBlockState());
	}

	private static void clearShipWith(final ServerLevel level, final ServerShip ship, final BlockState block) {
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return;
		}
		for (int x = box.minX(); x < box.maxX(); x++) {
			for (int z = box.minZ(); z < box.maxZ(); z++) {
				for (int y = box.minY(); y < box.maxY(); y++) {
					level.setBlock(new BlockPos(x, y, z), block, Block.UPDATE_NONE);
				}
			}
		}
	}

	private record LevelChunkPos(ServerLevel level, ChunkPos chunk) {}
	private record WeightedItem<T>(int weight, T item) {}
}
