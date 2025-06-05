package net.jcm.vsch.compat.create.ponder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.jcm.vsch.compat.create.ponder.scenes.DragInducerScene;
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
	}
}
