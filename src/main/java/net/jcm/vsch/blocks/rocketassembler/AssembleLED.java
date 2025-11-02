package net.jcm.vsch.blocks.rocketassembler;

import net.minecraft.util.StringRepresentable;

public enum AssembleLED implements StringRepresentable {
	GREEN,
	BLACK,
	RED,
	YELLOW;

	@Override
	public String getSerializedName() {
		return this.toString().toLowerCase();
	}
}
