package net.jcm.vsch.event;

import net.jcm.vsch.util.VSCHUtils;
import net.jcm.vsch.util.wapi.LevelData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class GravityInducer implements ShipForcesInducer {
	private String dimension = null;
	private volatile double gravity = 0;

	public GravityInducer() {}

	@Override
	public void applyForces(final @NotNull PhysShip physShip) {
		final double gravityForce = (1 - this.gravity) * 10 * ((PhysShipImpl) (physShip)).getInertia().getShipMass();
		if (!Double.isFinite(gravityForce)) {
			return;
		}
		physShip.applyInvariantForce(new Vector3d(0, gravityForce, 0));
	}

	public static GravityInducer tickOnShip(final LoadedServerShip ship) {
		// TODO: remove the attachment clear logic in later version
		if (ship instanceof final org.valkyrienskies.core.impl.game.ships.ShipObjectServer shipObject && shipObject.getShipData().getAttachment(GravityInducer.class) != null) {
			shipObject.saveAttachment(GravityInducer.class, null);
		}
		GravityInducer attachment = ship.getAttachment(GravityInducer.class);
		if (attachment == null) {
			attachment = new GravityInducer();
			// Attachment will be cleared when ship moved to another dimension.
			ship.setAttachment(GravityInducer.class, attachment);
		}
		final String shipDim = ship.getChunkClaimDimension();
		if (attachment.dimension == null) {
			attachment.dimension = shipDim;
			attachment.gravity = LevelData.get(VSCHUtils.registeryDimToLevel(shipDim)).getGravity();
		}
		return attachment;
	}
}
