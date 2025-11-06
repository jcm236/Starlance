package net.jcm.vsch.blocks.rocketassembler;

import net.jcm.vsch.compat.cc.peripherals.CCGenerics;
import net.jcm.vsch.util.TaskUtil;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RocketAssemblerPeripheral implements IPeripheral {
	public static final String ASSEMBLE_SUCCESS_EVENT_ID = "rocket_assemble_success";
	public static final String ASSEMBLE_FAILED_EVENT_ID = "rocket_assemble_failed";
	private static final Set<String> ADDTIONAL_TYPES = CCGenerics.ENERGY_METHODS.getType().getAdditionalTypes();

	private final RocketAssemblerBlockEntity assembler;
	private final long peripheralId;
	private final AttachedComputerSet computers = new AttachedComputerSet();
	private final AtomicBoolean assembling = new AtomicBoolean();

	private final AssembleCallback assembleCallbackInstance = new AssembleCallback();

	public RocketAssemblerPeripheral(final RocketAssemblerBlockEntity assembler) {
		this.assembler = assembler;
		this.peripheralId = assembler.getBlockPos().asLong();
	}

	@Override
	public Object getTarget() {
		return this.assembler;
	}

	@Override
	public String getType() {
		return "starlance_rocket_assembler";
	}

	@Override
	public Set<String> getAdditionalTypes() {
		return ADDTIONAL_TYPES;
	}

	@Override
	public void attach(final IComputerAccess computer) {
		this.computers.add(computer);
	}

	@Override
	public void detach(final IComputerAccess computer) {
		this.computers.remove(computer);
	}

	@LuaFunction
	public final boolean isAssembling() {
		return this.assembler.isAssembling();
	}

	@LuaFunction
	public final MethodResult assemble(final Optional<String> slug) throws LuaException {
		final String slugStr = slug.orElse(null);
		if (!this.assembling.compareAndSet(false, true)) {
			throw new LuaException("Already assembling");
		}
		TaskUtil.queueTickEnd(() -> {
			if (!this.assembler.startAssemble(slugStr)) {
				this.queueEvent(ASSEMBLE_FAILED_EVENT_ID, this.peripheralId, true, "Already assembling");
			}
		});
		return this.assembleCallbackInstance.pull;
	}

	void onAssembleFinish() {
		final AssembleResult result = this.assembler.getAssembleResult();
		if (result.isSuccess()) {
			this.queueEvent(ASSEMBLE_SUCCESS_EVENT_ID, this.peripheralId);
			return;
		}
		this.queueEvent(ASSEMBLE_FAILED_EVENT_ID, this.peripheralId, false, result.getMessageId());
	}

	protected void queueEvent(final String event, final Object... args) {
		this.computers.forEach(computer -> computer.queueEvent(event, args));
	}

	@Override
	public boolean equals(final IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof RocketAssemblerPeripheral otherAssembler) {
			return this.assembler == otherAssembler.assembler;
		}
		return false;
	}

	//// Generic methods ////

	@LuaFunction(mainThread = true)
	public int getEnergy() {
		return CCGenerics.ENERGY_METHODS.getEnergy(this.assembler.energyStorage);
	}

	@LuaFunction(mainThread = true)
	public int getEnergyCapacity() {
		return CCGenerics.ENERGY_METHODS.getEnergyCapacity(this.assembler.energyStorage);
	}

	private final class AssembleCallback implements ILuaCallback {
		private final MethodResult pull = MethodResult.pullEvent(null, this);

		@Override
		public MethodResult resume(final Object[] args) throws LuaException {
			if (args.length < 2) {
				return this.pull;
			}
			if (!(args[0] instanceof String event)) {
				return this.pull;
			}

			final boolean successed = event.equals(ASSEMBLE_SUCCESS_EVENT_ID);
			if (!successed && !event.equals(ASSEMBLE_FAILED_EVENT_ID)) {
				return this.pull;
			}
			if (!(args[1] instanceof Number peripheralId) || peripheralId.longValue() != RocketAssemblerPeripheral.this.peripheralId) {
				return this.pull;
			}

			if (successed) {
				RocketAssemblerPeripheral.this.assembling.set(false);
				return MethodResult.of(true);
			}
			if (args.length < 4 || !(args[2] instanceof Boolean isCritical) || !(args[3] instanceof String error)) {
				return this.pull;
			}
			if (isCritical) {
				throw new LuaException(error);
			}
			RocketAssemblerPeripheral.this.assembling.set(false);
			return MethodResult.of(false, error);
		}
	}
}
