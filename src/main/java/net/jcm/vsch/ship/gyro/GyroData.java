package net.jcm.vsch.ship.gyro;

import org.joml.Vector3d;

public class GyroData {
	public final Vector3d torque;

	public GyroData(Vector3d torque) {
		this.torque = torque;
	}

	public String toString() {
		return "Torque: " + this.torque;
	}
}
