/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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

		@SuppressWarnings("removal")
		private static TagKey<Fluid> tag(String name) {
			return TagKey.create(Registries.FLUID, new ResourceLocation(VSCHMod.MODID, name));
		}

		public static void register() {}
	}
}
