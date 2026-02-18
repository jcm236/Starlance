/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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
