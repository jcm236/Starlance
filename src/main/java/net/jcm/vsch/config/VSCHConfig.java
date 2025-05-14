package net.jcm.vsch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.jcm.vsch.ship.thruster.ThrusterData.ThrusterMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;

public class VSCHConfig {
	private static final Gson GSON = new GsonBuilder().create();
	private static final TypeToken<Map<String, Integer>> STRING_INT_MAP_TYPE = new TypeToken<Map<String, Integer>>(){};

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<Boolean> THRUSTER_TOGGLE;
	public static final ForgeConfigSpec.ConfigValue<ThrusterMode> THRUSTER_MODE;

	public static final ForgeConfigSpec.ConfigValue<Number> THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<String> THRUSTER_FUEL_CONSUME_RATES;

	public static final ForgeConfigSpec.ConfigValue<Number> AIR_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> AIR_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Integer> AIR_THRUSTER_MAX_WATER_CONSUME_RATE;

	public static final ForgeConfigSpec.ConfigValue<Number> POWERFUL_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> POWERFUL_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Integer> POWERFUL_THRUSTER_FUEL_CONSUME_RATE;

	public static final ForgeConfigSpec.ConfigValue<Number> GYRO_STRENGTH;
	public static final ForgeConfigSpec.ConfigValue<Integer> GYRO_ENERGY_CONSUME_RATE;

	public static final ForgeConfigSpec.BooleanValue GENERATE_ASTEROID;
	public static final ForgeConfigSpec.IntValue MAX_ASTEROID_COUNT;
	public static final ForgeConfigSpec.IntValue MAX_CLUSTERS_PER_ROUND;
	public static final ForgeConfigSpec.IntValue MAX_ASTEROIDS_PER_CLUSTER;
	public static final ForgeConfigSpec.IntValue MIN_SPAWN_DIST;
	public static final ForgeConfigSpec.IntValue SPAWN_RANGE;

	public static final ForgeConfigSpec.ConfigValue<Number> MAX_DRAG;

	public static final ForgeConfigSpec.ConfigValue<Boolean> LIMIT_SPEED;
	public static final ForgeConfigSpec.ConfigValue<Number> MAX_SPEED;

	public static final ForgeConfigSpec.ConfigValue<Boolean> CANCEL_ASSEMBLY;

	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_MAX_FORCE;
	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_MAX_FORCE;

	static {
		BUILDER.push("Thrusters");

		THRUSTER_TOGGLE = BUILDER.comment("Thruster Mode Toggling").define("thruster_mode_toggle", true);
		THRUSTER_MODE = BUILDER.comment("Default Thruster Mode").defineEnum("thruster_default_mode", ThrusterMode.POSITION);

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

		GYRO_STRENGTH = BUILDER.comment("Max force gyro can apply to the ship on any axis. (N)").define("gyro_strength", 350000);
		GYRO_ENERGY_CONSUME_RATE = BUILDER.comment("Gyro energy consume rate. (FE/t)").define("gyro_energy_consume_rate", 10000);

		BUILDER.pop();

		BUILDER.push("Asteroid");

		GENERATE_ASTEROID = BUILDER.comment("Allow spawn asteroid").define("generate_asteroid", true);
		MAX_ASTEROID_COUNT = BUILDER.comment("Max asteroid count per dimension").defineInRange("max_asteroid_count", 64, 1, 512);
		MAX_CLUSTERS_PER_ROUND = BUILDER.comment("Max asteroid clusters can generate per round").defineInRange("max_clusters_per_round", 8, 1, 32);
		MAX_ASTEROIDS_PER_CLUSTER = BUILDER.comment("Max asteroids can be generated in a cluster").defineInRange("max_asteroids_per_cluster", 8, 1, 64);
		MIN_SPAWN_DIST = BUILDER.comment("Minimum spawn distance of asteroids from players and ships (blocks)").defineInRange("min_spawn_distance", 16 * 8, 16 * 2, 16 * 128);
		SPAWN_RANGE = BUILDER.comment("The area asteroids can spawn after minium spawn distance (blocks)").defineInRange("spawn_range", 16 * 16, 16 * 2, 16 * 128);

		BUILDER.pop();

		BUILDER.push("Misc");

		MAX_DRAG = BUILDER.comment("Max force the drag inducer can use to slow down").define("max_drag", 15000);
		LIMIT_SPEED = BUILDER.comment("Limit speed thrusters can accelerate to. Recommended, as VS ships get funky at high speeds").define("limit_speed", true);
		MAX_SPEED = BUILDER.comment("Max speed to limit to. Blocks/tick I think. Default is highly recommended").define("max_speed", 150);
		CANCEL_ASSEMBLY = BUILDER.comment("Cancel multi-block assemblies when above world height. This is a temporary fix, but for now ships made above world height have issues with starlance.").define("cancel_assembly", true);

		MAGNET_BOOT_DISTANCE = BUILDER.comment("Distance (in blocks) at which magnet boots will pull you in").define("magnet_boot_distance", 6);
		MAGNET_BOOT_MAX_FORCE = BUILDER.comment("Max acceleration magnet boots will apply at close distances to move the player downwards.").define("magnet_boot_max_force", 0.09);
		GRAVITY_DISTANCE = BUILDER.comment("Distance (in blocks) at which gravity generator will pull you in").define("gravity_gen_distance", 6);
		GRAVITY_MAX_FORCE = BUILDER.comment("Max acceleration gravity generator will apply at close distances to move the player downwards.").define("gravity_gen_max_force", 0.09);

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
}
