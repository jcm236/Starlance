package net.jcm.vsch.compat.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.thruster.ThrusterBrain;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.ThrusterData;

public class ThrusterPeripheral implements IPeripheral {
	private final ThrusterBrain brain;

	public ThrusterPeripheral(ThrusterBrain brain) {
		this.brain = brain;
	}

	@Override
	public Object getTarget() {
		return this.brain;
	}

	@Override
	public String getType() {
		return "starlance_thruster";
	}

	@LuaFunction
	public final String getThrusterType() {
		return this.brain.getPeripheralType();
	}

	@LuaFunction(mainThread = true)
	public final MethodResult getMode() {
		ThrusterData.ThrusterMode mode = this.brain.getThrusterMode();
		return MethodResult.of(mode.toString(), mode.ordinal() + 1);
	}

	@LuaFunction(mainThread = true)
	public final void setMode(IArguments args) throws LuaException {
		if (!VSCHConfig.THRUSTER_TOGGLE.get()) {
			throw new LuaException("Thruster mode toggle disabled in server config");
		}
		ThrusterData.ThrusterMode tmode;
		Object arg0 = args.get(0);
		if (arg0 instanceof String mode) {
			try {
				tmode = ThrusterData.ThrusterMode.valueOf(mode.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new LuaException("Unknown thruster mode");
			}
		} else if (arg0 instanceof Number mode) {
			try {
				tmode = ThrusterData.ThrusterMode.values()[mode.intValue() - 1];
			} catch (IndexOutOfBoundsException e) {
				throw new LuaException("Unknown thruster mode");
			}
		} else {
			throw LuaValues.badArgumentOf(args, 0, "string or number");
		}
		this.brain.setThrusterMode(tmode);
	}

	@LuaFunction
	public final boolean getPeripheralMode() {
		return this.brain.getPeripheralMode();
	}

	@LuaFunction
	public final void setPeripheralMode(boolean mode) {
		this.brain.setPeripheralMode(mode);
	}

	@LuaFunction
	public final float getPower() {
		return this.brain.getPower();
	}

	@LuaFunction
	public final void setPower(double power) throws LuaException {
		if (!this.brain.getPeripheralMode()) {
			throw new LuaException("Peripheral mode is off, redstone control only");
		}
		this.brain.setPower((float) power);
	}

	@LuaFunction
	public final float getThrusters() {
		return this.brain.getThrusterCount();
	}

	@LuaFunction
	public final float getEachMaxThrottle() {
		return this.brain.getEngine().getMaxThrottle();
	}

	@LuaFunction
	public final float getEachThrottle() {
		return this.brain.getCurrentThrottle();
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof ThrusterPeripheral otherThruster) {
			return this.brain == otherThruster.brain;
		}
		return false;
	}
}
