package net.jcm.vsch.compat.cc.peripherals.laser;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.laser.cannon.LaserDetectProcessorBlockEntity;

public class LaserDetectProcessorPeripheral implements IPeripheral {
	private final LaserDetectProcessorBlockEntity target;

	public LaserDetectProcessorPeripheral(final LaserDetectProcessorBlockEntity target) {
		this.target = target;
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public String getType() {
		return "laser_detect_processor";
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		return other instanceof LaserDetectProcessorPeripheral p && this.target == p.target;
	}

	@LuaFunction
	public double getDistance() {
		return this.target.getDistance();
	}

	@LuaFunction(mainThread = true)
	public Object getDetails() {
		return this.target.getDetails();
	}
}
