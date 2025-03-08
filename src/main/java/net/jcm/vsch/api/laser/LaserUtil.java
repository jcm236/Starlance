package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public final class LaserUtil {
	private static final int MAX_REDIRECT_PER_TICK = 4;
	private static final Map<Class<? extends Block>, Consumer<LaserContext>> DEFAULT_PROCESSOR_MAP = new HashMap<>();
	private static final Queue<LaserContext> LASER_QUEUE = new ConcurrentLinkedQueue<>();

	private LaserUtil() {}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		for (int remain = LASER_QUEUE.size(); remain > 0; remain--) {
			final LaserContext laser = LASER_QUEUE.remove();
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
		LASER_QUEUE.add(laser);
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

	public static Consumer<LaserContext> registerDefaultBlockProcessor(Class<? extends Block> clazz, Consumer<LaserContext> processor) {
		if (processor == null) {
			return DEFAULT_PROCESSOR_MAP.remove(clazz);
		}
		return DEFAULT_PROCESSOR_MAP.put(clazz, processor);
	}

	public static Consumer<LaserContext> getDefaultBlockProcessor(LaserContext laser) {
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
			final Consumer<LaserContext> processor = DEFAULT_PROCESSOR_MAP.get(blockClass);
			if (processor != null) {
				return processor;
			}
		}
		return null;
	}
}
