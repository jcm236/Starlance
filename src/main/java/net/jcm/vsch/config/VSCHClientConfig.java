package net.jcm.vsch.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class VSCHClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_DETONATOR_RENDER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_PROJECTOR_RENDER;

    static {

        BUILDER.push("Optimize");

        DISABLE_DETONATOR_RENDER = BUILDER.comment("Disable the beam rendering from the detonator item. Can significantly improve FPS").define("disable_detonator_render", true);
        DISABLE_PROJECTOR_RENDER = BUILDER.comment("Disables the rendering of the projector block. Enable this for more FPS if you don't need the projector").define("disable_projector_render", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register(ModLoadingContext context){
        context.registerConfig(ModConfig.Type.CLIENT, VSCHClientConfig.SPEC, "vsch-client.toml");
    }
}
