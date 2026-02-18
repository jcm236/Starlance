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

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.jcm.vsch.compat.create.ponder.scenes.DragInducerScene;
import net.jcm.vsch.compat.create.ponder.scenes.RocketAssemblerScene;
import net.jcm.vsch.compat.create.ponder.scenes.ThrusterScenes;
import net.minecraft.resources.ResourceLocation;

public class VSCHPonderRegistry {
	public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
		HELPER.forComponents(
			VSCHPonderRegistrateBlocks.THRUSTER_BLOCK,
			VSCHPonderRegistrateBlocks.AIR_THRUSTER_BLOCK,
			VSCHPonderRegistrateBlocks.POWERFUL_THRUSTER_BLOCK
		)
	.addStoryBoard("thrusters", ThrusterScenes::thrusters)
	.addStoryBoard("thruster_modes", ThrusterScenes::modes);

		HELPER.forComponents(
			VSCHPonderRegistrateBlocks.DRAG_INDUCER_BLOCK
		)
	.addStoryBoard("drag_inducer", DragInducerScene::inducer);

	HELPER.forComponents(
		VSCHPonderRegistrateBlocks.ROCKET_ASSEMBLER_BLOCK
	)
	.addStoryBoard("rocket_assembler", RocketAssemblerScene::inducer);
	}
}
