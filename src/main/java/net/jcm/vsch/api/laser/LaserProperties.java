package net.jcm.vsch.api.laser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LaserProperties {
	public int r;
	public int g;
	public int b;
	private final List<ILaserAttachment> attachments;

	public LaserProperties() {
		this(0, 0, 0, new LinkedList<>());
	}

	private LaserProperties(int r, int g, int b, List<ILaserAttachment> attachments) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.attachments = attachments;
	}

	public LaserProperties(int r, int g, int b) {
		this(r, g, b, new LinkedList<>());
	}

	@Override
	public String toString() {
		return String.format("<LaserProperties (%d, %d, %d) %d attachments>", this.r, this.g, this.b, this.attachments.size());
	}

	@Override
	public LaserProperties clone() {
		return new LaserProperties(
			this.r,
			this.g,
			this.b,
			new LinkedList<>(this.attachments)
		);
	}

	public LaserProperties withAttachment(ILaserAttachment attachment) {
		this.attachments.add(attachment);
		return this;
	}

	public Collection<ILaserAttachment> getAttachments() {
		return Collections.unmodifiableCollection(this.attachments);
	}

	public double getStrength() {
		return ((double) (this.r + this.g + this.b)) / (255 * 3);
	}

	public Vec3 getColor() {
		if (this.r == 0 && this.g == 0 && this.b == 0) {
			return Vec3.ZERO;
		}
		Vec3 color = new Vec3(this.r / 255.0, this.g / 255.0, this.b / 255.0);
		if (color.lengthSqr() > 1) {
			color = color.normalize();
		}
		return color;
	}

	public void mergeFrom(final LaserProperties other) {
		this.r += other.r;
		this.g += other.g;
		this.b += other.b;
	}

	public LaserProperties afterLoss(double loss) {
		return new LaserProperties(
			(int)(r * (1 - loss)),
			(int)(g * (1 - loss)),
			(int)(b * (1 - loss)),
			new LinkedList<>(this.attachments)
		);
	}

	public CompoundTag writeToNBT(CompoundTag data) {
		data.putIntArray("Color", new int[]{this.r, this.g, this.b});
		return data;
	}

	public void readFromNBT(CompoundTag data) {
		final int[] color = data.getIntArray("Color");
		if (color.length == 3) {
			this.r = color[0];
			this.g = color[1];
			this.b = color[2];
		}
	}
}
