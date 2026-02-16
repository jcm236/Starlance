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
package net.jcm.vsch.network.c2s;

import net.jcm.vsch.CompatMods;
import net.jcm.vsch.items.IToggleableItem;
import net.jcm.vsch.network.INetworkPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public class ToggleItemPacketC2S implements INetworkPacket {
	public final boolean isCurios;
	public final int slot;
	public final String id;

	public ToggleItemPacketC2S(final int slot) {
		this.isCurios = false;
		this.slot = slot;
		this.id = null;
	}

	public ToggleItemPacketC2S(final String id, final int slot) {
		this.isCurios = true;
		this.slot = slot;
		this.id = id;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		buf.writeBoolean(this.isCurios);
		buf.writeVarInt(this.slot);
		if (this.isCurios) {
			buf.writeUtf(this.id, 256);
		}
	}

	public static ToggleItemPacketC2S decode(final FriendlyByteBuf buf) {
		final boolean isCurios = buf.readBoolean();
		final int slot = buf.readVarInt();
		return isCurios ? new ToggleItemPacketC2S(buf.readUtf(256), slot) : new ToggleItemPacketC2S(slot);
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		final ServerPlayer player = ctx.getSender();
		if (player == null) {
			return;
		}
		if (this.isCurios) {
			if (!CompatMods.CURIOS.isLoaded()) {
				return;
			}
			ctx.enqueueWork(() -> {
				final ICuriosItemHandler curiosInv = CuriosApi.getCuriosInventory(player).orElse(null);
				if (curiosInv == null) {
					return;
				}
				final SlotResult result = curiosInv.findCurio(this.id, this.slot).orElse(null);
				if (result == null) {
					return;
				}
				final ItemStack stack = result.stack();
				if (stack.isEmpty()) {
					return;
				}
				if (stack.getItem() instanceof final IToggleableItem item) {
					item.onToggle(player, stack);
				}
			});
			return;
		}
		ctx.enqueueWork(() -> {
			final ItemStack stack = player.getInventory().getItem(this.slot);
			if (stack.isEmpty()) {
				return;
			}
			if (stack.getItem() instanceof final IToggleableItem item) {
				item.onToggle(player, stack);
			}
		});
	}
}
