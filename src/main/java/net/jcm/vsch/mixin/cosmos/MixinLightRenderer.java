package net.jcm.vsch.mixin.cosmos;

import net.lointain.cosmos.procedures.LightRendererer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightRendererer.class)
public abstract class MixinLightRenderer {

    // getPrivateField fails when Cosmos is de-obfuscated in a dev enviroment, so this is here to stop that
    @ModifyVariable(method = "getPrivateField", remap = false, at = @At("HEAD"), argsOnly = true)
    private static String fixFieldName(String fieldName) {

        // Check if we are dev enviroment or obf enviroment
        if (!FMLEnvironment.production) {
            if (fieldName.equals("f_110009_")) {
                return "passes";
            }
            if (fieldName.equals("f_110054_")) {
                return "effect";
            }
        }

        return fieldName;
    }

}

