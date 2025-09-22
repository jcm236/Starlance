package net.jcm.vsch.network.c2s;

import net.jcm.vsch.items.IToggleableItem;
import net.jcm.vsch.network.INetworkPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ToggleItemPacketC2S implements INetworkPacket {
	public final int slot;

	public ToggleItemPacketC2S(final int slot) {
		this.slot = slot;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		buf.writeVarInt(this.slot);
	}

	public static ToggleItemPacketC2S decode(final FriendlyByteBuf buf) {
		return new ToggleItemPacketC2S(buf.readVarInt());
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		final ServerPlayer player = ctx.getSender();
		if (player == null) {
			return;
		}
		ctx.enqueueWork(() -> {
			final ItemStack stack = player.getInventory().getItem(this.slot);
			if (stack.isEmpty()) {
				return;
			}
			if (stack.getItem() instanceof final IToggleableItem item) {
				item.onToggle(player, this.slot, stack);
			}
		});
	}
}
