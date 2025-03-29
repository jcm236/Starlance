package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.jcm.vsch.entity.LaserEntity;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.s2c.LaserContextPacketS2C;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public final class LaserUtil {
	private static final int MAX_REDIRECT_PER_TICK = 4;
	private static final Map<Class<? extends Block>, ILaserProcessor> DEFAULT_PROCESSOR_MAP = new HashMap<>();
	private static final Queue<LaserContext> LASER_QUEUE = new ConcurrentLinkedQueue<>();

	private LaserUtil() {}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		for (int remain = LASER_QUEUE.size(); remain > 0; remain--) {
			final LaserContext laser = LASER_QUEUE.remove();
			processLaser(laser);
		}
	}

	private static void processLaser(final LaserContext laser) {
		laser.fire();
		final LaserEmitter emitter = laser.getLastRedirecter();
		if (emitter.getSource() instanceof ILaserSyncedSource source) {
			source.onLaserFired(laser);
		} else {
			syncLaser(laser);
		}
	}

	public static void queueLaser(final LaserContext laser) {
		LASER_QUEUE.add(laser);
	}

	public static void fireLaser(final LaserProperties props, final LaserEmitter emitter) {
		processLaser(new LaserContext(props, emitter));
	}

	public static void fireRedirectedLaser(final LaserContext laser) {
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
			attachment.afterMergeLaser(original, target, targetProps);
		}
	}

	private static void syncLaser(final LaserContext laser) {
		LaserEntity.createAndAdd(laser, 1);
	}

	public static ILaserProcessor registerDefaultBlockProcessor(Class<? extends Block> clazz, Consumer<LaserContext> processor) {
		return registerDefaultBlockProcessor(clazz, ILaserProcessor.fromEndPoint(processor));
	}

	public static ILaserProcessor registerDefaultBlockProcessor(Class<? extends Block> clazz, ILaserProcessor processor) {
		if (processor == null) {
			return DEFAULT_PROCESSOR_MAP.remove(clazz);
		}
		return DEFAULT_PROCESSOR_MAP.put(clazz, processor);
	}

	public static ILaserProcessor getDefaultBlockProcessor(LaserContext laser) {
		if (!(laser.getHitResult() instanceof BlockHitResult hitResult)) {
			return null;
		}
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		final Level level = laser.getLevel();
		final BlockPos pos = hitResult.getBlockPos();
		final BlockState state = level.getBlockState(pos);
		final Block block = state.getBlock();
		for (Class<?> blockClass = block.getClass(); Block.class.isAssignableFrom(blockClass); blockClass = blockClass.getSuperclass()) {
			final ILaserProcessor processor = DEFAULT_PROCESSOR_MAP.get(blockClass);
			if (processor != null) {
				return processor;
			}
		}
		return null;
	}
}
