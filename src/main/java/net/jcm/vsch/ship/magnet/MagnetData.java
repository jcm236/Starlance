package net.jcm.vsch.ship.magnet;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.PhysShip;

public class MagnetData {
	public static final ForceCalculator EMPTY_FORCE = (s, a, b) -> {};
	public volatile Vector3f facing;
	public volatile boolean isGenerator;
	public volatile ForceCalculator forceCalculator = EMPTY_FORCE;

	public MagnetData(Vector3f facing, boolean isGenerator) {
		this.facing = facing;
		this.isGenerator = isGenerator;
	}

	@FunctionalInterface
	public interface ForceCalculator {
		void calc(PhysShip physShip, Vector3d force1, Vector3d force2);
	}
}
