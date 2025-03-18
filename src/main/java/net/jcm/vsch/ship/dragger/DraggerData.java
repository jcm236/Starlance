package net.jcm.vsch.ship.dragger;

public class DraggerData {

	public volatile boolean on;

	public DraggerData(boolean on) {
		this.on = on;
	}

	public String toString() {
		return " State: " + this.on;
	}
}
