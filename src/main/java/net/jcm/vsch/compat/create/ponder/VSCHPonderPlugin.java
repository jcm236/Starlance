package net.jcm.vsch.compat.create.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.jcm.vsch.VSCHMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class VSCHPonderPlugin implements PonderPlugin {
	@Override
	public @NotNull String getModId() {
		return VSCHMod.MODID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		VSCHPonderRegistry.register(helper);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		VSCHPonderTags.register(helper);
	}
}
