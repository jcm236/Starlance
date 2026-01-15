package net.jcm.vsch.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class VSCHCommonConfig {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.BooleanValue DISABLE_LOD_WARNING;
	public static final ForgeConfigSpec.BooleanValue SUPRESSESS_PRESS_Y_HINT;
	public static final ForgeConfigSpec.BooleanValue SUPRESSESS_UNLOCK_HINT;

	static {
		DISABLE_LOD_WARNING = BUILDER.comment("Disables the warning about lodDetail being below 4096. Its highly recommend you increase lod instead of using this option").define("disable_lod_warning", false);
		SUPRESSESS_PRESS_Y_HINT = BUILDER.comment("Supresses the \"Press Y To Open GUI\" hint.").define("supressess_press_y_hint", false);
		SUPRESSESS_UNLOCK_HINT = BUILDER.comment("Supresses the \"/unlock\" hint when changing dimension.").define("supressess_unlock_hint", false);

		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		context.registerConfig(ModConfig.Type.COMMON, VSCHCommonConfig.SPEC, "vsch-common.toml");
	}
}
