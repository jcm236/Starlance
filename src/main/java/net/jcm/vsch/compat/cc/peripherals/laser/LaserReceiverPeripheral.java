package net.jcm.vsch.compat.cc.peripherals.laser;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.laser.cannon.LaserReceiverBlockEntity;

public class LaserReceiverPeripheral implements IPeripheral {
	private final LaserReceiverBlockEntity target;

	public LaserReceiverPeripheral(final LaserReceiverBlockEntity target) {
		this.target = target;
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public String getType() {
		return "laser_receiver";
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		return other instanceof LaserReceiverPeripheral p && this.target == p.target;
	}

	@LuaFunction
	public MethodResult getColor() {
		final int[] color = this.target.getColor();
		return MethodResult.of(color[0], color[1], color[2]);
	}

	@LuaFunction
	public void setColor(int r, int g, int b) {
		this.target.setColor(r, g, b);
	}
}
