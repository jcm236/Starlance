package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.ShipAllocator;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraft.core.registries.BuiltInRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Spliterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class AsteroidGenerator {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	public static final String ASTEROID_SHIP_PREFIX = "+asteroid+";
	public static final String IDLE_SHIP_PREFIX = "+idle+";
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

	private static final Random RNG = new Random();
	private static final Map<ServerPlayer, LevelChunkPos> PLAYER_LAST_POS = new HashMap<>();
	private static final Map<ServerPlayer, Spliterator<Supplier<ServerShip>>> PLAYER_GENERATORS = new HashMap<>();

	private AsteroidGenerator() {}

	public static void tickLevel(ServerLevel level) {
		// TODO: add asteroid belt radius instead
		final boolean isSpace = CosmosModVariables.WorldVariables.get(level).dimension_type.getString(level.dimension().location().toString()).equals("space");
		if (!isSpace) {
			return;
		}
		final ShipAllocator allocator = ShipAllocator.get(level);
		final List<ServerPlayer> players = level.getPlayers(player -> true);
		int removing = 0;
		for (final ServerShip ship : VSCHUtils.getShipsInLevel(level)) {
			if (canDespawnAsteroid(level, players, ship)) {
				LOGGER.info("[starlance]: removing asteroid {}", ship.getSlug());
				allocator.putShip(ship);
				removing++;
				if (removing > 2) {
					break;
				}
			}
		}
		for (final ServerPlayer player : players) {
			tickPlayer(level, players, player);
			final Spliterator<Supplier<ServerShip>> generator = PLAYER_GENERATORS.get(player);
			if (generator != null) {
				final ServerShip ship = StreamSupport.stream(generator, false)
					.map(Supplier::get)
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
				if (ship == null) {
					PLAYER_GENERATORS.remove(player);
					continue;
				}
				LOGGER.info("[starlance]: generated asteroid {}", ship.getSlug());
			}
		}
	}

	private static void tickPlayer(final ServerLevel level, final List<ServerPlayer> players, final ServerPlayer player) {
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
		if (lastPos != null && lastPos.level() == level && lastPos.chunk().getChessboardDistance(chunkPos) < MIN_REGENERATE_DIST) {
			return;
		}
		PLAYER_LAST_POS.put(player, new LevelChunkPos(level, chunkPos));

		final Stream<Supplier<ServerShip>> generator = IntStream.range(0, MAX_CLUSTERS_PER_ROUND)
			.mapToObj(i -> null)
			.flatMap(i -> generateFrom(level, players, blockPos));
		PLAYER_GENERATORS.put(player, generator.spliterator());
	}

	private static Stream<Supplier<ServerShip>> generateFrom(final ServerLevel level, final List<ServerPlayer> players, final BlockPos origin) {
		final BlockPos.MutableBlockPos newPos = origin.mutable().move(randPosInRange(MIN_SPAWN_DIST, MAX_SPAWN_DIST, 0, 64, MIN_SPAWN_DIST, MAX_SPAWN_DIST));
		if (!canSpawnAsteroid(level, players, newPos)) {
			return Stream.empty();
		}
		return IntStream.range(0, MAX_ASTEROIDS_PER_CLUSTER)
			.mapToObj(i -> () -> {
				final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
				int t = 0;
				do {
					t++;
					if (t > 10) {
						return null;
					}
					nextPos.setWithOffset(newPos, randPosInRange(32, 64));
				} while (!canSpawnAsteroid(level, players, newPos));
				newPos.set(nextPos);
				return spawnAsteroid(level, newPos);
			});
	}

	public static boolean isAsteroidShip(Ship ship) {
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

	private static ServerShip spawnAsteroid(ServerLevel level, BlockPos pos) {
		final Map<Vec3i, BlockState> blocks = generateAsteroid(level);
		if (blocks == null || blocks.isEmpty()) {
			return null;
		}
		final ServerShip ship = ShipAllocator.get(level).allocShip(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
		ship.setSlug(ASTEROID_SHIP_PREFIX + ship.getId());
		final Vector3i center = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final BlockPos centerPos = new BlockPos(center.x, center.y, center.z);
		long begin = System.nanoTime();
		blocks.forEach((offset, state) -> level.setBlock(centerPos.offset(offset), state, 0));
		long after = System.nanoTime();
		System.out.println("set block used " + (after - begin) + " avg " + ((after - begin) / blocks.size()) + " ns/block");
		return ship;
	}

	private static Map<Vec3i, BlockState> generateAsteroid(final ServerLevel level) {
		final BlockState stone;
		final List<BlockState> ores = new ArrayList<>();
		final int typeRand = RNG.nextInt(100);
		if (typeRand < 10) {
			// stone asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			ores.add(Blocks.STONE.defaultBlockState());
		} else if (typeRand < 15) {
			// ice asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			ores.add(Blocks.ICE.defaultBlockState());
			ores.add(Blocks.BLUE_ICE.defaultBlockState());
		} else if (typeRand < 45) {
			// nether ore asteroid
			stone = Blocks.NETHERRACK.defaultBlockState();
			ores.add(Blocks.MAGMA_BLOCK.defaultBlockState());
			BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).forEach(holder -> ores.add(holder.value().defaultBlockState()));
		} else {
			// earth ore asteroid
			stone = Blocks.DEEPSLATE.defaultBlockState();
			BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).forEach(holder -> ores.add(holder.value().defaultBlockState()));
		}
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

	private record LevelChunkPos(ServerLevel level, ChunkPos chunk) {}
}
