package net.jcm.vsch.spacemods.ad_astra;

import net.jcm.vsch.network.INetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncMenuTierS2C implements INetworkPacket {
    public final int tier;

    public SyncMenuTierS2C(int tier) {
        this.tier = tier;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(tier);
    }

    public static SyncMenuTierS2C decode(final FriendlyByteBuf buf) {
        return new SyncMenuTierS2C(buf.readInt());
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.setPacketHandled(true);

        ClientValues.storedTier = tier;
    }
}
