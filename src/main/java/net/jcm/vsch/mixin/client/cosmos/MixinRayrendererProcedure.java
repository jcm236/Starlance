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
package net.jcm.vsch.mixin.client.cosmos;

import net.jcm.vsch.config.VSCHClientConfig;
import net.lointain.cosmos.procedures.RayrendererProcedure;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RayrendererProcedure.class)
public class MixinRayrendererProcedure {
	@Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DD)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(final Event event, final LevelAccessor world, final double partialTick, final double ticks, final CallbackInfo ci) {
		if (VSCHClientConfig.DISABLE_DETONATOR_RENDER.get()) {
			ci.cancel();
		}
	}
}