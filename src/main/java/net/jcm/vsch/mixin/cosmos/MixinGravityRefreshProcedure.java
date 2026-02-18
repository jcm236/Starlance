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

import net.jcm.vsch.config.VSCHCommonConfig;
import net.lointain.cosmos.procedures.GravityRefreshProcedure;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GravityRefreshProcedure.class)
public class MixinGravityRefreshProcedure {
	@WrapOperation(
		method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
			remap = true
		),
		remap = false
	)
	private static void execute(
		final Player player,
		final Component message,
		final boolean isOverlay,
		final Operation<Void> displayClientMessage
	) {
		if (!VSCHCommonConfig.SUPRESSESS_UNLOCK_HINT.get()) {
			displayClientMessage.call(player, message, isOverlay);
		}
	}
}
