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
package net.jcm.vsch.compat.create.ponder;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class VSCHPonderTags {
	public static ResourceLocation STARLANCE_PONDERS = Create.asResource("starlance_ponders");

	/**
	 * Add ponders to the starlance tag here
	 */
	public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
		PonderTagRegistrationHelper<RegistryEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

		helper.registerTag(STARLANCE_PONDERS)
			.addToIndex()
			.item(VSCHPonderRegistrateBlocks.THRUSTER_BLOCK.get(), true, false)
			.title("Starlance")
			.description("Starlance blocks")
			.register();

		HELPER.addToTag(STARLANCE_PONDERS)
			.add(VSCHPonderRegistrateBlocks.THRUSTER_BLOCK)
            .add(VSCHPonderRegistrateBlocks.AIR_THRUSTER_BLOCK)
            .add(VSCHPonderRegistrateBlocks.POWERFUL_THRUSTER_BLOCK)
			.add(VSCHPonderRegistrateBlocks.DRAG_INDUCER_BLOCK);
	}
}
