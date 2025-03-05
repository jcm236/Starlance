package net.jcm.vsch.api.laser;

@FunctionalInterface
public interface ILaserProcessor {
	void onLaserHit(LaserContext ctx);
}
