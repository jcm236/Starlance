package net.jcm.vsch.api.laser;

public interface ILaserProcessor {
	int getMaxLaserStrength();
	void onLaserHit(LaserContext ctx);
}
