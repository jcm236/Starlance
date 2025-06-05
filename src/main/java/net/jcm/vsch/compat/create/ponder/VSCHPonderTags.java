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
			.add(VSCHPonderRegistrateBlocks.DRAG_INDUCER_BLOCK);
	}
}
