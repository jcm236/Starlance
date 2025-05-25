package net.jcm.vsch.compat.cc.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.GyroBlockEntity;

public class GyroPeripheral implements IPeripheral {
	private final GyroBlockEntity gyro;

	public GyroPeripheral(GyroBlockEntity gyro) {
		this.gyro = gyro;
	}

	@Override
	public Object getTarget() {
		return this.gyro;
	}

	@Override
	public String getType() {
		return "starlance_gyro";
	}

	@LuaFunction
	public final boolean getPeripheralMode() {
		return this.gyro.getPeripheralMode();
	}

	@LuaFunction
	public final void setPeripheralMode(boolean mode) {
		this.gyro.setPeripheralMode(mode);
	}

	protected void assertPeripheralMode() throws LuaException {
		if (!this.gyro.getPeripheralMode()) {
			throw new LuaException("Peripheral mode is off");
		}
	}

	@LuaFunction
	public final void stop() throws LuaException {
		this.assertPeripheralMode();
		this.gyro.resetTorque();
	}

	@LuaFunction
	public final double getTorqueForce() {
		return this.gyro.getTorqueForce();
	}

	@LuaFunction
	public final MethodResult getTorque() {
		return MethodResult.of(this.gyro.getTorqueX(), this.gyro.getTorqueY(), this.gyro.getTorqueZ());
	}

	@LuaFunction
	public final void setTorque(double x, double y, double z) throws LuaException {
		this.assertPeripheralMode();
		this.gyro.setTorque(x, y, z);
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof GyroPeripheral otherGyro) {
			return this.gyro == otherGyro.gyro;
		}
		return false;
	}
}
