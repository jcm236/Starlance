package net.jcm.vsch.api.laser;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.accessor.LevelChunkAccessor;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public final class LaserUtil {
	private static final int MAX_REDIRECT_PER_TICK = 4;
	private static final Random RND = new Random();
	private static final Queue<LaserContext> laserQueue = new ConcurrentLinkedQueue<>();

	private LaserUtil() {}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		for (int remain = laserQueue.size(); remain > 0; remain--) {
			final LaserContext laser = laserQueue.remove();
			processLaser(laser);
		}
	}

	private static void processLaser(LaserContext laser) {
		laser.fire();
		final LaserEmitter emitter = laser.getLastRedirecter();
		if (emitter.getSource() instanceof ILaserSource source) {
			source.onLaserFired(laser);
		}
	}

	public static void queueLaser(LaserContext laser) {
		laserQueue.add(laser);
	}

	public static void fireLaser(LaserProperties props, LaserEmitter emitter) {
		processLaser(new LaserContext(props, emitter));
	}

	public static void fireRedirectedLaser(LaserContext laser) {
		if (laser.tickRedirected < MAX_REDIRECT_PER_TICK) {
			processLaser(laser);
			return;
		}
		laser.tickRedirected = 0;
		queueLaser(laser);
	}

	public static void mergeLaser(final LaserContext original, final LaserContext target) {
		final LaserProperties props = original.getLaserOnHitProperties();
		for (ILaserAttachment attachment : props.getAttachments()) {
			attachment.beforeMergeLaser(original, target);
			if (original.canceled()) {
				return;
			}
		}
		final LaserProperties targetProps = target.getLaserProperties();
		targetProps.mergeFrom(props);
		for (ILaserAttachment attachment : props.getAttachments()) {
			attachment.afterMergeLaser(original, target);
		}
	}

	static void blockDestroyProcessor(LaserContext laser) {
		if (!(laser.getHitResult() instanceof BlockHitResult hitResult)) {
			return;
		}
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}
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
		final float digProg = LaserUtil.getDigProgressAt(level, pos) + digSpeed;
		LaserUtil.setDigProgressAt(level, pos, digProg);
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
		if (prog < 0 || prog >= 1) {
			map.remove(key);
			return;
		}
		map.put(key, prog);
	}
}
