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
package net.jcm.vsch.client;

import net.jcm.vsch.VSCHMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VSCHKeyBindings {
	public static final KeyMapping TOGGLE_MAGNET_BOOT = new KeyMapping(
		"key." + VSCHMod.MODID + ".toggle_magnet_boot",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_X,
		"key.categories." + VSCHMod.MODID
	);

	@SubscribeEvent
	public static void register(final RegisterKeyMappingsEvent event) {
		event.register(TOGGLE_MAGNET_BOOT);
	}
}