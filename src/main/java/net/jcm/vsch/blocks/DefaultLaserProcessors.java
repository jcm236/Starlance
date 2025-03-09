package net.jcm.vsch.blocks;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

import net.jcm.vsch.accessor.LevelChunkAccessor;
import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;

import java.util.Random;

public final class DefaultLaserProcessors {
	private static final Random RND = new Random();

	private DefaultLaserProcessors() {}

	public static void register() {
		LaserUtil.registerDefaultBlockProcessor(Block.class, DefaultLaserProcessors::blockDestroyProcessor);
		final ILaserProcessor stainedGlassProcessor = new StainedGlassProcessor();
		LaserUtil.registerDefaultBlockProcessor(StainedGlassBlock.class, stainedGlassProcessor);
		LaserUtil.registerDefaultBlockProcessor(StainedGlassPaneBlock.class, stainedGlassProcessor);
	}

	private static void blockDestroyProcessor(LaserContext laser) {
		final BlockHitResult hitResult = (BlockHitResult) (laser.getHitResult());
		final Level level = laser.getLevel();
		final BlockPos pos = hitResult.getBlockPos();
		final BlockState state = level.getBlockState(pos);
		final LaserProperties props = laser.getLaserOnHitProperties();
		final float accurate = props.r / 128.0f;
		final float speed = props.g / 256.0f + 0.5f;
		final int strength = props.b / 256;

		final int tire = getTire(state);
		if (strength < tire) {
			return;
		}

		final float destroySpeed = state.getDestroySpeed(level, pos);
		if (destroySpeed == -1) {
			return;
		}

		final float digSpeed = speed / destroySpeed / 20;
		final float digProg = getDigProgressAt(level, pos) + digSpeed;
		setDigProgressAt(level, pos, digProg);
		final int prog = (int) (digProg * 10);
		if (prog < 10) {
			level.destroyBlockProgress(-1, pos, prog);
			return;
		}
		level.destroyBlockProgress(-1, pos, -1);
		level.destroyBlock(pos, false);
		final double dropChance = Math.log(accurate) / (strength * speed);
		if (dropChance >= 1 || dropChance > 0 && RND.nextDouble() < dropChance) {
			Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
		}
	}

	private static final class StainedGlassProcessor implements ILaserProcessor {
		@Override
		public int getMaxLaserStrength() {
			return 256 * 6;
		}

		@Override
		public void onLaserHit(LaserContext laser) {
			final BlockHitResult hitResult = (BlockHitResult) (laser.getHitResult());
			final Level level = laser.getLevel();
			final BlockPos pos = hitResult.getBlockPos();
			final BlockState state = level.getBlockState(pos);
			final LaserProperties props = laser.getLaserOnHitProperties();

			if (!(state.getBlock() instanceof BeaconBeamBlock beamBlock)) {
				return;
			}
			final DyeColor dyeColor = beamBlock.getColor();
			final int color = dyeColor.getTextColor();
			props.r = (props.r * ((color >> 16) & 0xff)) / 0xff;
			props.g = (props.g * ((color >> 8) & 0xff)) / 0xff;
			props.b = (props.b * (color & 0xff)) / 0xff;
			LaserUtil.fireRedirectedLaser(
				laser.redirectWith(
					props,
					LaserEmitter.fromBlock(level, hitResult.getLocation(), laser.getInputDirection(), pos, null)
				)
			);
		}
	}

	private static int getTire(BlockState state) {
		if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
			return 5;
		}
		if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
			return 4;
		}
		if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
			return 3;
		}
		if (state.requiresCorrectToolForDrops()) {
			return 2;
		}
		return 1;
	}

	private static Int2FloatOpenHashMap getDigProgressMap(Level level, BlockPos pos) {
		final LevelChunk chunk = level.getChunkAt(pos);
		if (chunk == null) {
			return null;
		}
		return ((LevelChunkAccessor) (chunk)).getDestroyProgressMap();
	}

	private static int blockPosToChunkInt(BlockPos pos) {
		return (pos.getX() & 0xf) | ((pos.getZ() & 0xf) << 4) | (pos.getY() << 8);
	}

	private static float getDigProgressAt(Level level, BlockPos pos) {
		final Int2FloatOpenHashMap map = getDigProgressMap(level, pos);
		if (map == null) {
			return 0;
		}
		return map.get(blockPosToChunkInt(pos));
	}

	private static void setDigProgressAt(Level level, BlockPos pos, float prog) {
		final Int2FloatOpenHashMap map = getDigProgressMap(level, pos);
		if (map == null) {
			return;
		}
		final int key = blockPosToChunkInt(pos);
		if (prog <= 0 || prog >= 1) {
			map.remove(key);
			return;
		}
		map.put(key, prog);
	}
}
