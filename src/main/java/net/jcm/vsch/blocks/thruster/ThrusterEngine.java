package net.jcm.vsch.blocks.thruster;

import net.minecraft.world.level.material.Fluid;

public abstract class ThrusterEngine {
	private final int tanks;
	private final int energyConsumeRate;
	private final float maxThrottle;

	protected ThrusterEngine(int tanks, int energyConsumeRate, float maxThrottle) {
		this.tanks = tanks;
		this.energyConsumeRate = energyConsumeRate;
		this.maxThrottle = maxThrottle;
	}

	public int getTanks() {
		return this.tanks;
	}

	public int getEnergyConsumeRate() {
		return this.energyConsumeRate;
	}

	public float getMaxThrottle() {
		return this.maxThrottle;
	}

	/**
	 * isValidFuel checks if the fluid can be uses as fuel.
	 * same fluid must <b>NOT</b> be able to fill in two different tanks.
	 *
	 * @param tank  The tank the fuel is going to transfer in
	 * @param fluid The fuel's fluid stack
	 * @return {@code true} if the fluid is consumable, {@code false} otherwise
	 */
	public boolean isValidFuel(int tank, Fluid fluid) {
		return false;
	}

	/**
	 * ticks the engine with given power, which consumes energy and fuel,
	 * and update the actual achieved power based on available energy and fuel.
	 *
	 * @see ThrusterEngineContext
	 */
	public void tick(ThrusterEngineContext context) {
		if (this.energyConsumeRate == 0) {
			return;
		}
		double power = context.getPower();
		if (power == 0) {
			return;
		}
		int amount = context.getAmount();
		int needs = (int)(Math.ceil(this.energyConsumeRate * power * amount));
		int extracted = context.getEnergyStorage().extractEnergy(needs, true);
		context.setPower(extracted / ((double)(this.energyConsumeRate) * amount));
		context.addConsumer((ctx) -> {
			ctx.getEnergyStorage().extractEnergy((int)(Math.ceil(ctx.getPower() * ctx.getAmount() * this.energyConsumeRate)), false);
		});
	}
}
