package net.jcm.vsch.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface INetworkPacket {
	void encode(FriendlyByteBuf buf);

	void handle(NetworkEvent.Context ctx);
}
