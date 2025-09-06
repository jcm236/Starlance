package net.jcm.vsch.api.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;

public class PreShipTravelEvent extends Event {
	private final ServerShip ship;
	private final ResourceKey<Level> oldLevel;
	private final ResourceKey<Level> newLevel;
	private final Vector3dc position;
	private final Quaterniondc rotation;
	private final Vector3d velocity;
	private final Vector3d omega;

	public PreShipTravelEvent(
		final ServerShip ship,
		final ResourceKey<Level> oldLevel,
		final ResourceKey<Level> newLevel,
		final Vector3dc position,
		final Quaterniondc rotation,
		final Vector3d velocity,
		final Vector3d omega
	) {
		this.ship = ship;
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.position = position;
		this.rotation = rotation;
		this.velocity = velocity;
		this.omega = omega;
	}

	public final ServerShip getShip() {
		return this.ship;
	}

	public final ResourceKey<Level> getOldLevel() {
		return this.oldLevel;
	}

	public final ResourceKey<Level> getNewLevel() {
		return this.newLevel;
	}

	public final Vector3dc getPosition() {
		return this.position;
	}

	public final Quaterniondc getRotation() {
		return this.rotation;
	}

	public final Vector3dc getVelocity() {
		return this.velocity;
	}

	public final void setVelocity(final Vector3dc velocity) {
		this.velocity.set(velocity);
	}

	public final Vector3dc getOmega() {
		return this.omega;
	}

	public final void setOmega(final Vector3dc omega) {
		this.omega.set(omega);
	}
}
