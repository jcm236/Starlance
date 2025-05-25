package net.jcm.vsch.compat.cc.peripherals.laser;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.laser.cannon.LaserEmitterBlockEntity;

public final class LaserEmitterPeripheral implements IPeripheral {
	private final LaserEmitterBlockEntity target;

	public LaserEmitterPeripheral(final LaserEmitterBlockEntity target) {
		this.target = target;
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public String getType() {
		return "laser_emitter";
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		return other instanceof LaserEmitterPeripheral p && this.target == p.target;
	}

	@LuaFunction
	public MethodResult getColor() {
		final int[] color = this.target.getColor();
		return MethodResult.of(color[0], color[1], color[2]);
	}

	/**
	 * it is on purpose that setColor does not exist here, to prevent people can change laser strengths too easy.
	 */

	@LuaFunction(mainThread = true)
	public boolean fire() {
		return this.target.fireLaser();
	}
}
