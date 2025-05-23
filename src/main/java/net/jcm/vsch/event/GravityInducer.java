package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import net.jcm.vsch.util.VSCHUtils;

public class GravityInducer implements ShipForcesInducer {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);
	public static CompoundTag gravityDataTag;

	public ServerShip ship = null;

	public GravityInducer() {}

	public GravityInducer(ServerShip ship) {
		this.ship = ship;
	}

	@Override
	public void applyForces(@NotNull PhysShip physShip) {
		if (gravityDataTag == null) {
			return;
		}
		if (ship == null) {
			return;
		}

		final String dimId = VSCHUtils.vsDimToDim(ship.getChunkClaimDimension());
		if (!gravityDataTag.contains(dimId)) {
			return;
		}
		float gravityData = gravityDataTag.getFloat(dimId);
		double gravity = (1 - gravityData) * 10 * ((PhysShipImpl) physShip).get_inertia().getShipMass();
		try {
			physShip.applyInvariantForce(new Vector3d(0, gravity, 0));
		} catch (Exception e) {
			LOGGER.error("[starlance]: Gravity Inducer Failed due to {} on ship {}", e, ship.getSlug());
		}
	}

	public static GravityInducer getOrCreate(ServerShip ship) {
		GravityInducer attachment = ship.getAttachment(GravityInducer.class);
		if (attachment == null) {
			attachment = new GravityInducer(ship);
			ship.saveAttachment(GravityInducer.class, attachment);
		}
		if (attachment.ship == null) {
			attachment.ship = ship;
		}
		return attachment;
	}
}
