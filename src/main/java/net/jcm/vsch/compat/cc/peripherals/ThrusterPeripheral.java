package net.jcm.vsch.compat.cc.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.template.AbstractThrusterBlockEntity;
import net.jcm.vsch.ship.ThrusterData;

public class ThrusterPeripheral implements IPeripheral {
	private final AbstractThrusterBlockEntity entity;

	public ThrusterPeripheral(AbstractThrusterBlockEntity entity) {
		this.entity = entity;
	}

	@Override
	public Object getTarget() {
		return this.entity;
	}

	@Override
	public String getType() {
		return "starlance_thruster";
	}

	@LuaFunction
	public String getThrusterType() {
		return this.entity.getTypeString();
	}

	@LuaFunction(mainThread = true)
	public String getMode() {
		return this.entity.getThrusterMode().toString();
	}

	@LuaFunction(mainThread = true)
	public void setMode(String mode) throws LuaException {
		ThrusterData.ThrusterMode tmode;
		try {
			tmode = ThrusterData.ThrusterMode.valueOf(mode.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new LuaException("unknown thruster mode");
		}
		this.entity.setThrusterMode(tmode);
	}

	@LuaFunction
	public boolean getPeripheralMode() {
		return this.entity.getPeripheralMode();
	}

	@LuaFunction
	public void setPeripheralMode(boolean mode) {
		this.entity.setPeripheralMode(mode);
	}

	@LuaFunction
	public float getPower() {
		return this.entity.getPower();
	}

	@LuaFunction
	public void setPower(double power) throws LuaException {
		if (!this.entity.getPeripheralMode()) {
			throw new LuaException("Peripheral mode is off, redstone control only");
		}
		this.entity.setPower((float) power);
	}

	@LuaFunction
	public float getMaxThrottle() {
		return this.entity.getMaxThrottle();
	}

	@LuaFunction
	public float getThrottle() {
		return this.entity.getThrottle();
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof ThrusterPeripheral otherThruster) {
			return this.entity == otherThruster.entity;
		}
		return false;
	}
}
