package net.jcm.vsch.mixin.client.cosmos;

import net.lointain.cosmos.procedures.AerialLightRenderer;
import net.lointain.cosmos.procedures.LightRendererer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({AerialLightRenderer.class, LightRendererer.class})
public abstract class MixinDevFieldNameRenderers {

	// getPrivateField fails when Cosmos is de-obfuscated in a dev enviroment, so this is here to stop that
	@ModifyVariable(method = "getPrivateField", remap = false, at = @At("HEAD"), argsOnly = true)
	private static String fixFieldName(final String fieldName) {
		// Check if we are dev enviroment or obf enviroment
		if (!FMLEnvironment.production) {
			switch (fieldName) {
				case "f_110009_":
					return "passes";
				case "f_110054_":
					return "effect";
			}
		}
		return fieldName;
	}

}
