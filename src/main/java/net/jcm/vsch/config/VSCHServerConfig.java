/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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

public class VSCHServerConfig {
	private static final Gson GSON = new GsonBuilder().create();
	private static final TypeToken<Map<String, Integer>> STRING_INT_MAP_TYPE = new TypeToken<Map<String, Integer>>(){};

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	/* Landing */

	public static final ForgeConfigSpec.EnumValue<ShipLandingMode> SHIP_LANDING_MODE;
	public static final ForgeConfigSpec.IntValue SHIP_LANDING_ACCURACY;
	public static final ForgeConfigSpec.IntValue SHIP_FIRST_LANDING_SPAWN_RANGE;

	/* Thrusters */

	public static final ForgeConfigSpec.BooleanValue THRUSTER_TOGGLE;
	public static final ForgeConfigSpec.ConfigValue<ThrusterMode> THRUSTER_MODE;
	public static final ForgeConfigSpec.BooleanValue THRUSTER_FLAME_IMPACT;

	public static final ForgeConfigSpec.ConfigValue<Number> THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.IntValue THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<String> THRUSTER_FUEL_CONSUME_RATES;

	public static final ForgeConfigSpec.ConfigValue<Number> AIR_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.IntValue AIR_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.IntValue AIR_THRUSTER_MAX_WATER_CONSUME_RATE;

	public static final ForgeConfigSpec.ConfigValue<Number> POWERFUL_THRUSTER_STRENGTH;
	public static final ForgeConfigSpec.IntValue POWERFUL_THRUSTER_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.IntValue POWERFUL_THRUSTER_FUEL_CONSUME_RATE;

	/* Gyro */

	public static final ForgeConfigSpec.ConfigValue<Number> GYRO_STRENGTH;
	public static final ForgeConfigSpec.IntValue GYRO_ENERGY_CONSUME_RATE;
	public static final ForgeConfigSpec.ConfigValue<Number> GYRO_MAX_SPEED;
	public static final ForgeConfigSpec.BooleanValue GYRO_LIMIT_SPEED;

	/* Magnetics */

	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> MAGNET_BOOT_MAX_FORCE;

	public static final ForgeConfigSpec.ConfigValue<Double> MAGNET_BLOCK_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Double> MAGNET_BLOCK_MAX_FORCE;
	public static final ForgeConfigSpec.ConfigValue<Integer> MAGNET_BLOCK_CONSUME_ENERGY;

	/* Assembler */

	public static final ForgeConfigSpec.IntValue ASSEMBLER_ENERGY_CONSUMPTION;
	public static final ForgeConfigSpec.IntValue MAX_ASSEMBLE_BLOCKS;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ASSEMBLE_BLACKLIST;
	private static Set<ResourceLocation> ASSEMBLE_BLACKLIST_SET = null;

	/* Optimize */

	public static final ForgeConfigSpec.BooleanValue ENABLE_EMPTY_SPACE_CHUNK;

	/* Misc */

	public static final ForgeConfigSpec.ConfigValue<Number> MAX_DRAG;

	public static final ForgeConfigSpec.BooleanValue LIMIT_SPEED;
	public static final ForgeConfigSpec.ConfigValue<Number> MAX_SPEED;

	public static final ForgeConfigSpec.BooleanValue CANCEL_ASSEMBLY;

	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Number> GRAVITY_MAX_FORCE;

	public static final ForgeConfigSpec.BooleanValue ENABLE_PLACE_SHIP_PLATFORM;

	private static final List<String> DEFAULT_ASSEMBLE_BLACKLIST = List.of(
		"minecraft:barrier",
		"minecraft:bedrock",
		"minecraft:command_block"
	);

	static {
		BUILDER.push("Landing");

		SHIP_LANDING_MODE = BUILDER.comment("Defines how the ship will land at planets.\nPLAYER_MENU: Always use the player menu for landing location. Ships cannot land without a player nearby.\nHISTORY: The saved launch position will be used. If no launch has been saved, the origin will be used.\nAUTO_HISTORY: Use PLAYER_MENU mode if a player is nearby, and use HISTORY mode otherwise.").defineEnum("ship_landing_mode", ShipLandingMode.HISTORY);
		SHIP_LANDING_ACCURACY = BUILDER.comment("Define how accurate the ships landing position is, distance in chunks.").defineInRange("ship_landing_accuracy", 0, 0, 128);
		SHIP_FIRST_LANDING_SPAWN_RANGE = BUILDER.comment("Define how far from the origin the ship will be teleported, in chunks.\nOnly have effect when a ship never been to the planet and is trying to use HISTORY mode.").defineInRange("ship_first_landing_spawn_range", 128, 0, 32768);

		BUILDER.pop();

		BUILDER.push("Thrusters");

		THRUSTER_TOGGLE = BUILDER.comment("Thruster Mode Toggling").define("thruster_mode_toggle", true);
		THRUSTER_MODE = BUILDER.comment("Default Thruster Mode").defineEnum("thruster_default_mode", ThrusterMode.POSITION);
		THRUSTER_FLAME_IMPACT = BUILDER.comment("This setting will allow thruster flame to push and to burn entities in its area, and set the first block it hits on fire.").define("thruster_flame_impact", true);

		THRUSTER_STRENGTH = BUILDER.comment("Thruster max force. (Newtons)").define("thruster_strength", 120000);
		THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Thruster energy consume rate. (FE/t)").defineInRange("thruster_energy_consume_rate", 0, 0, Integer.MAX_VALUE);
		THRUSTER_FUEL_CONSUME_RATES = BUILDER.comment("Thruster fuel consume rates. (mB/t)").define("thruster_fuel_consume_rates", getDefaultThrusterFuelConsumeRates());

		AIR_THRUSTER_STRENGTH = BUILDER.comment("Air thruster max force. (Newtons)").define("air_thruster_strength", 7500);
		AIR_THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Air thruster energy consume rate. (FE/t)").defineInRange("air_thruster_energy_consume_rate", 0, 0, Integer.MAX_VALUE);
		AIR_THRUSTER_MAX_WATER_CONSUME_RATE = BUILDER.comment("Air thruster water consume rate when in a dimension that has less air density. (mB/t)").defineInRange("air_thruster_max_water_consume_rate", 0, 0, Integer.MAX_VALUE);

		POWERFUL_THRUSTER_STRENGTH = BUILDER.comment("Powerful thruster max force. (Newtons)").define("powerful_thruster_strength", 450000);
		POWERFUL_THRUSTER_ENERGY_CONSUME_RATE = BUILDER.comment("Powerful thruster energy consume rate. (FE/t)").defineInRange("powerful_thruster_energy_consume_rate", 0, 0, Integer.MAX_VALUE);
		POWERFUL_THRUSTER_FUEL_CONSUME_RATE = BUILDER.comment("Powerful thruster oxygen consume rate. (mB/t) which hydrogen will consume twice as much.").defineInRange("powerful_thruster_fuel_consume_rate", 0, 0, Integer.MAX_VALUE);

		BUILDER.pop();

		BUILDER.push("Gyro");

		GYRO_STRENGTH = BUILDER.comment("Max force gyro will apply to the ship on any axis. (N)").define("gyro_strength", 350000);
		GYRO_ENERGY_CONSUME_RATE = BUILDER.comment("Gyro energy consume rate. (FE/t)").defineInRange("gyro_energy_consume_rate", 0, 0, Integer.MAX_VALUE); //10000 default for next update
		GYRO_LIMIT_SPEED = BUILDER.comment("Should the gyro have its rotational speed limited").define("gyro_limit_speed", true);
		GYRO_MAX_SPEED = BUILDER.comment("Max rotation the gyro will accelerate to (RPM?)").define("gyro_max_speed", 80);

		BUILDER.pop();

		BUILDER.push("Magnetics");

		MAGNET_BOOT_DISTANCE = BUILDER.comment("Distance (in blocks) at which magnet boots will pull you in").define("magnet_boot_distance", 6);
		MAGNET_BOOT_MAX_FORCE = BUILDER.comment("Max acceleration magnet boots will apply at close distances to move the player downwards.").define("magnet_boot_max_force", 0.08);

		MAGNET_BLOCK_DISTANCE = BUILDER.comment("Distance (in blocks) at which magnet blocks will pull ships").define("magnet_block_distance", 6.0);
		MAGNET_BLOCK_MAX_FORCE = BUILDER.comment("Max force one magnet block will apply at 1 block distance.").define("magnet_block_max_force", 50000.0);
		MAGNET_BLOCK_CONSUME_ENERGY = BUILDER.comment("The energy a magnet block will consume when activate at max power.").define("magnet_block_consume_energy", 0);

		BUILDER.pop();

		BUILDER.push("RocketAssembler");

		ASSEMBLER_ENERGY_CONSUMPTION = BUILDER.comment("Assemble Energy Consumption").defineInRange("energy_consumption", 0, 0, Integer.MAX_VALUE); // 100 default for next update
		MAX_ASSEMBLE_BLOCKS = BUILDER.comment("Max blocks the rocket assembler can assemble").defineInRange("max_assemble_blocks", 16 * 16 * 256 * 9, 0, Integer.MAX_VALUE);
		ASSEMBLE_BLACKLIST = BUILDER.comment("Assembly is cancelled if it includes any of these blocks").defineList("assemble_blacklist", DEFAULT_ASSEMBLE_BLACKLIST, (o) -> o instanceof String value && value.length() > 0);

		BUILDER.pop();

		BUILDER.push("Optimize");

		ENABLE_EMPTY_SPACE_CHUNK = BUILDER.comment("Do not load or save space chunks.\nThis option will significantly reduce memory allocation and disk usage\nwhen travelling at high speed.\nHowever then you can only build blocks on ships.\nIt is highly recommended to turn on Misc.enable_place_ship_platform at same time.").define("enable_empty_space_chunk", false);

		BUILDER.pop();

		BUILDER.push("Misc");

		MAX_DRAG = BUILDER.comment("Max force the drag inducer can use to slow down").define("max_drag", 15000);
		LIMIT_SPEED = BUILDER.comment("Limit speed thrusters can accelerate to. Recommended, as VS ships get funky at high speeds").define("limit_speed", true);
		MAX_SPEED = BUILDER.comment("Max speed to limit to. Blocks/tick I think. Default is highly recommended").define("max_speed", 150);
		CANCEL_ASSEMBLY = BUILDER.comment("Cancel multi-block assemblies when above world height. This is a temporary fix, but for now ships made above world height have issues with starlance.").define("cancel_assembly", true);

		GRAVITY_DISTANCE = BUILDER.comment("Distance (in blocks) at which gravity generator will pull you in").define("gravity_gen_distance", 6);
		GRAVITY_MAX_FORCE = BUILDER.comment("Max acceleration gravity generator will apply at close distances to move the player downwards.").define("gravity_gen_max_force", 0.08);

		ENABLE_PLACE_SHIP_PLATFORM = BUILDER.comment("After enabled, the block placed by key N will be spawned as a ship.").define("enable_place_ship_platform", false);

		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		// vsch-config.toml to not break existing config files, otherwise we would use vsch-server.toml
		context.registerConfig(ModConfig.Type.SERVER, VSCHServerConfig.SPEC, "vsch-config.toml");
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

	@SuppressWarnings("removal")
	public static Set<ResourceLocation> getAssembleBlacklistSet() {
		if (ASSEMBLE_BLACKLIST_SET == null) {
			ASSEMBLE_BLACKLIST_SET = Set.copyOf(ASSEMBLE_BLACKLIST.get().stream().map(ResourceLocation::new).toList());
		}
		return ASSEMBLE_BLACKLIST_SET;
	}
}
