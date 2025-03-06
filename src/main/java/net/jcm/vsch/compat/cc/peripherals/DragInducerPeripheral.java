package net.jcm.vsch.compat.cc.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.DragInducerBlockEntity;

public class DragInducerPeripheral implements IPeripheral {
	private final DragInducerBlockEntity entity;

	public DragInducerPeripheral(DragInducerBlockEntity entity) {
		this.entity = entity;
	}

	@Override
	public Object getTarget() {
		return this.entity;
	}

	@Override
	public String getType() {
		return "starlance_drag_inducer";
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
	public boolean isEnabled() {
		return this.entity.isEnabled();
	}

	@LuaFunction
	public void setEnabled(boolean enabled) throws LuaException {
		if (!this.entity.getPeripheralMode()) {
			// Instead of returning a string as an error, which is weird.
			throw new LuaException("Peripheral mode is off, redstone control only");
		}
		this.entity.setEnabled(enabled);
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof DragInducerPeripheral otherThruster) {
			return this.entity == otherThruster.entity;
		}
		return false;
	}
}
