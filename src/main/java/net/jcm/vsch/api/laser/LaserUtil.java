package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public final class LaserUtil {
	private static final int MAX_REDIRECT_PER_TICK = 4;
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
}
