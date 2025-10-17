package net.jcm.vsch.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class VSCHCommonConfig {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_LOD_WARNING;

	static {
		DISABLE_LOD_WARNING = BUILDER.comment("Disables the warning about lodDetail being below 4096. Its highly recommend you increase lod instead of using this option").define("disable_lod_warning", false);
		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		context.registerConfig(ModConfig.Type.COMMON, VSCHCommonConfig.SPEC, "vsch-common.toml");
	}
}
