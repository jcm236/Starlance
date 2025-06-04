package net.jcm.vsch.util.assemble;

import net.jcm.vsch.compat.CompatMods;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

public final class MoveUtil {
	private MoveUtil() {}

	private static final Map<Class<?>, IMoveable<?>> DEFAULT_MOVERS = new HashMap<>();

	public static IMoveable<?> registerDefaultMover(final Class<?> blockClass, final IMoveable<?> mover) {
		if (mover == null) {
			return DEFAULT_MOVERS.remove(blockClass);
		}
		return DEFAULT_MOVERS.put(blockClass, mover);
	}

	/**
	 * getMover will check mover registered for class and subclass first,
	 * and then check the mover registered on the class implemented interfaces
	 * and subclasses implemented interfaces.
	 */
	public static IMoveable<?> getMover(final Object block) {
		if (block == null) {
			return null;
		}
		if (block instanceof final IMoveable<?> mover) {
			return mover;
		}
		for (Class<?> blockClass = block.getClass(); blockClass != null; blockClass = blockClass.getSuperclass()) {
			final IMoveable<?> mover = DEFAULT_MOVERS.get(blockClass);
			if (mover != null) {
				return mover;
			}
		}
		for (Class<?> blockClass = block.getClass(); blockClass != null; blockClass = blockClass.getSuperclass()) {
			for (Class<?> intf : (Iterable<Class<?>>) (Stream.of(blockClass.getInterfaces()).flatMap(MoveUtil::streamClassAnsSubInterfaces)::iterator)) {
				final IMoveable<?> mover = DEFAULT_MOVERS.get(intf);
				if (mover != null) {
					return mover;
				}
			}
		}
		return null;
	}

	public static void registerDefaultMovers() {
		if (CompatMods.CREATE.isLoaded()) {
			registerDefaultMover(IControlContraption.class, MoveableIControlContraption.INSTANCE);
		}
	}

	private static Stream<Class<?>> streamClassAnsSubInterfaces(Class<?> clazz) {
		return Stream.concat(Stream.of(clazz), Stream.of(clazz.getInterfaces()).flatMap(MoveUtil::streamClassAnsSubInterfaces));
	}
}
