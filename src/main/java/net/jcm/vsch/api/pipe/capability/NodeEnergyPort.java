package net.jcm.vsch.api.pipe.capability;

public interface NodeEnergyPort extends NodePort {
	/**
	 * @param amount   The amount of the energy pushing
	 * @param simulate If this is a simulate action
	 * @return The actual amount of energy pushed
	 */
	int pushEnergy(int amount, boolean simulate);

	/**
	 * @param amount   The amount of the energy pulling
	 * @param simulate If this is a simulate action
	 * @return The actual amount of energy pulled
	 */
	int pullEnergy(int amount, boolean simulate);
}
