package net.jcm.vsch.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public final class TaskUtil {
	private static final Queue<Runnable> TICK_START_QUEUE = new ConcurrentLinkedQueue<>();
	private static final Queue<Runnable> TICK_END_QUEUE = new ConcurrentLinkedQueue<>();

	private TaskUtil() {}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		final Queue<Runnable> queue = switch (event.phase) {
			case START -> TICK_START_QUEUE;
			case END -> TICK_END_QUEUE;
		};
		for (int i = queue.size(); i > 0; i--) {
			final Runnable task = queue.remove();
			task.run();
		}
	}

	public static void queueTickStart(final Runnable task) {
		TICK_START_QUEUE.add(task);
	}

	public static void queueTickEnd(final Runnable task) {
		TICK_END_QUEUE.add(task);
	}
}
