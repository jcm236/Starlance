package net.jcm.vsch.ship;

import org.joml.Vector3d;

public class MagnetData {

    public volatile double force = 10.0;

    public final Vector3d direction;

    public MagnetData() {
        direction = null;
    }

    public MagnetData(Vector3d direction, double force) {
        this.direction = direction;
        this.force = force;
    }
    public MagnetData(Vector3d direction) {
        this.direction = direction;
    }
}
