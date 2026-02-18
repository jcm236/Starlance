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

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class VSCHCommonConfig {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.BooleanValue SHOW_ENTERING_PLANET_HINT;
	public static final ForgeConfigSpec.BooleanValue DISABLE_LOD_WARNING;
	public static final ForgeConfigSpec.BooleanValue SUPRESSESS_PRESS_Y_HINT;
	public static final ForgeConfigSpec.BooleanValue SUPRESSESS_UNLOCK_HINT;

	static {
		SHOW_ENTERING_PLANET_HINT = BUILDER.comment("Hint players who are about to entering a planet.").define("show_entering_planet_hint", true);
		DISABLE_LOD_WARNING = BUILDER.comment("Disables the warning about lodDetail being below 4096. Its highly recommend you increase lod instead of using this option").define("disable_lod_warning", false);
		SUPRESSESS_PRESS_Y_HINT = BUILDER.comment("Supresses the \"Press Y To Open GUI\" hint.").define("supressess_press_y_hint", false);
		SUPRESSESS_UNLOCK_HINT = BUILDER.comment("Supresses the \"/unlock\" hint when changing dimension.").define("supressess_unlock_hint", false);

		SPEC = BUILDER.build();
	}

	public static void register(ModLoadingContext context){
		context.registerConfig(ModConfig.Type.COMMON, VSCHCommonConfig.SPEC, "vsch-common.toml");
	}
}
