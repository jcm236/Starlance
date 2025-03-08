package net.jcm.vsch;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

// TODO: generate tags by listening GatherDataEvent
public final class VSCHTags {
	private VSCHTags() {}

	public static void register() {
		Fluids.register();
	}

	public static final class Fluids {
		private Fluids() {}

		public static final TagKey<Fluid> HYDROGEN = tag("liquid_hydrogen");
		public static final TagKey<Fluid> OXYGEN = tag("liquid_oxygen");

		private static TagKey<Fluid> tag(String name) {
			return TagKey.create(Registries.FLUID, new ResourceLocation(VSCHMod.MODID, name));
		}

		public static void register() {}
	}
}
