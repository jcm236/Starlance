package net.jcm.vsch.ship;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ShipLandingAttachment {
	@JsonProperty
	public boolean launching = false;
	@JsonProperty
	public boolean landing = false;
	public ServerPlayer commander = null;

	@JsonProperty
	public boolean frozen = false;
	public Vector3dc velocity = null;
	public Vector3dc omega = null;

	private final Map<ResourceKey<Level>, ChunkPos> launchPositions = new HashMap<>();

	public ShipLandingAttachment() {}

	public static ShipLandingAttachment get(final LoadedServerShip ship) {
		ShipLandingAttachment attachment = ship.getAttachment(ShipLandingAttachment.class);
		if (attachment == null) {
			attachment = new ShipLandingAttachment();
			ship.setAttachment(attachment);
		}
		return attachment;
	}

	@JsonGetter("launchPositions")
	private Map<String, Long> getLaunchPositions0() {
		final Map<String, Long> positions = new HashMap<>(this.launchPositions.size());
		this.launchPositions.forEach((level, pos) -> positions.put(level.location().toString(), pos.toLong()));
		return positions;
	}

	@SuppressWarnings("removal")
	@JsonSetter("launchPositions")
	private void setLaunchPositions0(final Map<String, Long> positions) {
		this.launchPositions.clear();
		positions.forEach((level, pos) ->
			this.launchPositions.put(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(level)), new ChunkPos(pos))
		);
	}

	public void setLaunching(final ResourceKey<Level> level, final ChunkPos pos) {
		this.landing = false;
		this.launching = true;
		this.launchPositions.put(level, pos);
	}

	public void setLanding() {
		this.launching = false;
		this.landing = true;
	}

	public void freezeShip(final LoadedServerShip ship) {
		this.frozen = true;
		this.velocity = new Vector3d(ship.getVelocity());
		this.omega = new Vector3d(ship.getOmega());
		ship.setStatic(true);
	}

	public Map<ResourceKey<Level>, ChunkPos> getLaunchPositions() {
		return this.launchPositions;
	}

	public ChunkPos getLaunchPosition(final ResourceKey<Level> level) {
		return this.launchPositions.get(level);
	}
}
