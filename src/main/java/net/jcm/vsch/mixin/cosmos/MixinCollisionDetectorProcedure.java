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
package net.jcm.vsch.mixin.cosmos;

import net.lointain.cosmos.procedures.CollisionDetectorProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CollisionDetectorProcedure.class)
public class MixinCollisionDetectorProcedure {
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(final LevelAccessor world, final double x, final double y, final double z, final Entity entity, final CallbackInfo ci) {
		if (VSGameUtilsKt.isBlockInShipyard((Level) (world), entity.getRootVehicle().blockPosition())) {
			ci.cancel();
		}
	}
}
