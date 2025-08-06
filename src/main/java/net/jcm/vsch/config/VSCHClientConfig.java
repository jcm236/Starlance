package net.jcm.vsch.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class VSCHClientConfig {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	/* Experimental */

	public static final ForgeConfigSpec.DoubleValue PLAYER_ROLL_SPEED;

	static {
		BUILDER.push("Experimental");

		PLAYER_ROLL_SPEED = BUILDER.comment("How fast does player roll with the roll keys in deg/s").defineInRange("player_roll_speed", 80.0, 1, 720);

		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		context.registerConfig(ModConfig.Type.CLIENT, SPEC, "vsch-client-config.toml");
	}
}
