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
