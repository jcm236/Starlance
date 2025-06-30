package net.jcm.vsch.pipe;

import net.minecraft.world.level.Level;

public class NodeLevel {
	private Level level;

	public NodeLevel(final Level level) {
		this.level = level;
	}
 
	public final Level getLevel() {
		return this.level;
	}
}
