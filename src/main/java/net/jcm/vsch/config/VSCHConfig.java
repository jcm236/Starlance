package net.jcm.vsch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.jcm.vsch.ship.thruster.ThrusterData.ThrusterMode;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VSCHConfig {
	private static final Gson GSON = new GsonBuilder().create();
	private static final TypeToken<Map<String, Integer>> STRING_INT_MAP_TYPE = new TypeToken<Map<String, Integer>>(){};

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	/* Thrusters */

	public static final ForgeConfigSpec.ConfigValue<Boolean> THRUSTER_TOGGLE;
	public static final ForgeConfigSpec.ConfigValue<ThrusterMode> THRUSTER_MODE;
	public static final ForgeConfigSpec.BooleanValue THRUSTER_FLAME_IMPACT;

	public static final ForgeConfigSpec.ConfigValue<Number> THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<String> THRUSTER_FUEL_CONSUME_RATES;

	public static final ForgeConfigSpec.ConfigValue<Number> AIR_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> AIR_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Integer> AIR_THRUSTER_MAX_WATER_CONSUME_RATE;

	public static final ForgeConfigSpec.ConfigValue<Number> POWERFUL_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> POWERFUL_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Integer> POWERFUL_THRUSTER_FUEL_CONSUME_RATE;

	/* Gyro */

	public static final ForgeConfigSpec.ConfigValue<Number> GYRO_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> GYRO_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Number> GYRO_MAX_SPEED;
	public static final ForgeConfigSpec.ConfigValue<Boolean> GYRO_LIMIT_SPEED;

	public static final ForgeConfigSpec.IntValue ASSEMBLER_ENERGY_CONSUMPTION;
	public static final ForgeConfigSpec.IntValue MAX_ASSEMBLE_BLOCKS;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ASSEMBLE_BLACKLIST;
	private static Set<ResourceLocation> ASSEMBLE_BLACKLIST_SET = null;

	/* Optimize */

	public static final ForgeConfigSpec.BooleanValue ENABLE_EMPTY_SPACE_CHUNK;

	/* Misc */

	public static final ForgeConfigSpec.ConfigValue<Number> MAX_DRAG;

	public static final ForgeConfigSpec.ConfigValue<Boolean> LIMIT_SPEED;
	public static final ForgeConfigSpec.ConfigValue<Number> MAX_SPEED;

	public static final ForgeConfigSpec.ConfigValue<Boolean> CANCEL_ASSEMBLY;

	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_MAX_FORCE;
	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_MAX_FORCE;

	public static final ForgeConfigSpec.BooleanValue ENABLE_PLACE_SHIP_PLATFORM;

	public static final ForgeConfigSpec.BooleanValue PLAYER_FREE_ROTATION_IN_SPACE;

	private static final List<String> DEFAULT_ASSEMBLE_BLACKLIST = List.of(
		"minecraft:barrier",
		"minecraft:bedrock",
		"minecraft:command_block"
	);

	static {
		BUILDER.push("Thrusters");

		THRUSTER_TOGGLE = BUILDER.comment("Thruster Mode Toggling").define("thruster_mode_toggle", true);
		THRUSTER_MODE = BUILDER.comment("Default Thruster Mode").defineEnum("thruster_default_mode", ThrusterMode.POSITION);
		THRUSTER_FLAME_IMPACT = BUILDER.comment("This setting will allow thruster flame to push and to burn entities in its area, and set the first block it hits on fire.").define("thruster_flame_impact", true);

		THRUSTER_STRENGTH = BUILDER.comment("Thruster max force. (Newtons)").define("thruster_strength", 120000);
		THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Thruster energy consume rate. (FE/t)").define("thruster_energy_consume_rate", 0);
		THRUSTER_FUEL_CONSUME_RATES = BUILDER.comment("Thruster fuel consume rates. (mB/t)").define("thruster_fuel_consume_rates", getDefaultThrusterFuelConsumeRates());

		AIR_THRUSTER_STRENGTH = BUILDER.comment("Air thruster max force. (Newtons)").define("air_thruster_strength", 7500);
		AIR_THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Air thruster energy consume rate. (FE/t)").define("air_thruster_energy_consume_rate", 0);
		AIR_THRUSTER_MAX_WATER_CONSUME_RATE = BUILDER.comment("Air thruster water consume rate when in a dimension that has less air density. (mB/t)").define("air_thruster_max_water_consume_rate", 0);

		POWERFUL_THRUSTER_STRENGTH = BUILDER.comment("Powerful thruster max force. (Newtons)").define("powerful_thruster_strength", 450000);
		POWERFUL_THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Powerful thruster energy consume rate. (FE/t)").define("powerful_thruster_energy_consume_rate", 0);
		POWERFUL_THRUSTER_FUEL_CONSUME_RATE = BUILDER.comment("Powerful thruster oxygen consume rate. (mB/t) which hydrogen will consume twice as much.").define("powerful_thruster_fuel_consume_rate", 0);

		BUILDER.pop();

		BUILDER.push("Gyro");

		GYRO_STRENGTH = BUILDER.comment("Max force gyro will apply to the ship on any axis. (N)").define("gyro_strength", 350000);
		GYRO_ENERGY_CONSUME_RATE = BUILDER.comment("Gyro energy consume rate. (FE/t)").define("gyro_energy_consume_rate", 0); //10000 default for next update
		GYRO_LIMIT_SPEED = BUILDER.comment("Should the gyro have its rotational speed limited").define("gyro_limit_speed", true);
		GYRO_MAX_SPEED = BUILDER.comment("Max rotation the gyro will accelerate to (RPM?)").define("gyro_max_speed", 80);

		BUILDER.pop();

		BUILDER.push("RocketAssembler");

		ASSEMBLER_ENERGY_CONSUMPTION = BUILDER.comment("Assemble Energy Consumption").defineInRange("energy_consumption", 0, 0, Integer.MAX_VALUE); // 100 default for next update
		MAX_ASSEMBLE_BLOCKS = BUILDER.comment("Max assemble blocks for rocket assembler").defineInRange("max_assemble_blocks", 16 * 16 * 256 * 9, 0, Integer.MAX_VALUE);
		ASSEMBLE_BLACKLIST = BUILDER.comment("Prevent assemble if contatins any of these blocks").defineList("assemble_blacklist", DEFAULT_ASSEMBLE_BLACKLIST, (o) -> o instanceof String value && value.length() > 0);

		BUILDER.pop();

		BUILDER.push("Optimize");

		ENABLE_EMPTY_SPACE_CHUNK = BUILDER.comment("Do not load or save space chunks.\nThis option will significantly reduce memory allocation and disk usage\nwhen travelling at high speed.\nHowever then you can only build blocks on ships.\nIt is highly recommended to turn on Misc.enable_place_ship_platform at same time.").define("enable_empty_space_chunk", false);

		BUILDER.pop();

		BUILDER.push("Misc");

		MAX_DRAG = BUILDER.comment("Max force the drag inducer can use to slow down").define("max_drag", 15000);
		LIMIT_SPEED = BUILDER.comment("Limit speed thrusters can accelerate to. Recommended, as VS ships get funky at high speeds").define("limit_speed", true);
		MAX_SPEED = BUILDER.comment("Max speed to limit to. Blocks/tick I think. Default is highly recommended").define("max_speed", 150);
		CANCEL_ASSEMBLY = BUILDER.comment("Cancel multi-block assemblies when above world height. This is a temporary fix, but for now ships made above world height have issues with starlance.").define("cancel_assembly", true);

		MAGNET_BOOT_DISTANCE = BUILDER.comment("Distance (in blocks) at which magnet boots will pull you in").define("magnet_boot_distance", 6);
		MAGNET_BOOT_MAX_FORCE = BUILDER.comment("Max acceleration magnet boots will apply at close distances to move the player downwards.").define("magnet_boot_max_force", 0.08);
		GRAVITY_DISTANCE = BUILDER.comment("Distance (in blocks) at which gravity generator will pull you in").define("gravity_gen_distance", 6);
		GRAVITY_MAX_FORCE = BUILDER.comment("Max acceleration gravity generator will apply at close distances to move the player downwards.").define("gravity_gen_max_force", 0.08);

		ENABLE_PLACE_SHIP_PLATFORM = BUILDER.comment("After enabled, the block placed by key N will be spawned as a ship.").define("enable_place_ship_platform", false);

		PLAYER_FREE_ROTATION_IN_SPACE = BUILDER.comment("Allow player to free rotate in space.").define("player_free_rotation_in_space", false);

		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		context.registerConfig(ModConfig.Type.SERVER, VSCHConfig.SPEC, "vsch-config.toml");
	}

	private static String getDefaultThrusterFuelConsumeRates() {
		Map<String, Integer> rates = new HashMap<>();
		// rates.put("minecraft:lava", 32);
		return GSON.toJson(rates);
	}

	public static Map<String, Integer> getThrusterFuelConsumeRates() {
		final String fuels = THRUSTER_FUEL_CONSUME_RATES.get();
		if (fuels.isEmpty()) {
			return new HashMap<>();
		}
		return GSON.fromJson(fuels, STRING_INT_MAP_TYPE);
	}

	public static Set<ResourceLocation> getAssembleBlacklistSet() {
		if (ASSEMBLE_BLACKLIST_SET == null) {
			ASSEMBLE_BLACKLIST_SET = Set.copyOf(ASSEMBLE_BLACKLIST.get().stream().map(ResourceLocation::new).toList());
		}
		return ASSEMBLE_BLACKLIST_SET;
	}
}
