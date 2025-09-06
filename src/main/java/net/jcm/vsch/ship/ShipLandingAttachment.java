package net.jcm.vsch.ship;

import net.minecraft.server.level.ServerPlayer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ShipLandingAttachment {
	@JsonProperty
	public boolean launching;
	@JsonProperty
	public boolean landing;
	public ServerPlayer commander = null;
	public Vector3dc velocity = null;
	public Vector3dc omega = null;

	public ShipLandingAttachment() {
		this(false, false);
	}

	public ShipLandingAttachment(final boolean launching, final boolean landing) {
		this.launching = launching;
		this.landing = landing;
	}
}
