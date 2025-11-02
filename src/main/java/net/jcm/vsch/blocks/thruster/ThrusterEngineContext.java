package net.jcm.vsch.blocks.thruster;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class ThrusterEngineContext {
	private final ServerLevel level;
	private final IEnergyStorage energy;
	private final IFluidHandler tanks;
	private final List<EngineConsumeAction> consumers = new ArrayList<>(2);
	private final int amount;
	private final double scale;
	private double power;
	private boolean rejected = false;

	/**
	 * @param level  The level the thruster is in
	 * @param energy The engine's energy storage, extract only
	 * @param tanks  The engine's fluid tanks, drain only
	 * @param power  The maximum power (in range of [0.0, 1.0]) the engine should maximum run with
	 * @param amount The amount of thrusters
	 * @param scale  The ship's scale
	 */
	public ThrusterEngineContext(ServerLevel level, IEnergyStorage energy, IFluidHandler tanks, double power, int amount, double scale) {
		this.level = level;
		this.energy = energy;
		this.tanks = tanks;
		this.power = power;
		this.amount = amount;
		this.scale = scale;
	}

	public void reject() {
		this.rejected = true;
	}

	public boolean isRejected() {
		return this.rejected;
	}

	public ServerLevel getLevel() {
		return this.level;
	}

	public IEnergyStorage getEnergyStorage() {
		return this.energy;
	}

	public IFluidHandler getFluidHandler() {
		return this.tanks;
	}

	public void addConsumer(EngineConsumeAction consumer) {
		this.consumers.add(consumer);
	}

	public double getPower() {
		return this.power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public int getAmount() {
		return this.amount;
	}

	public double getScale() {
		return this.scale;
	}

	/**
	 * consume should not be invoked by anything other than ThrusterBrain
	 */
	void consume() {
		for (EngineConsumeAction consumer : this.consumers) {
			consumer.consume(this);
		}
	}

	@FunctionalInterface
	public interface EngineConsumeAction {
		void consume(ThrusterEngineContext ctx);
	}
}
