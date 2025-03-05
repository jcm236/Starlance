package net.jcm.vsch.api.laser;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class LaserUtil {
	private static final int MAX_TICK_REDIRECT = 4;
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
	}

	public static void queueLaser(LaserContext laser) {
		laserQueue.add(laser);
	}

	public static void fireLaser(LaserProperties props, LaserEmitter emitter) {
		processLaser(new LaserContext(props, emitter));
	}

	public static void fireRedirectedLaser(LaserContext laser) {
		if (laser.tickRedirected < MAX_TICK_REDIRECT) {
			processLaser(laser);
			return;
		}
		laser.tickRedirected = 0;
		queueLaser(laser);
	}
}
