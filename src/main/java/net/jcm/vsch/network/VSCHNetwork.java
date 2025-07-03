package net.jcm.vsch.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.network.s2c.PipeNodeSyncChunkS2C;
import net.jcm.vsch.network.s2c.PipeNodeSyncChunkSectionS2C;

import java.util.Optional;
import java.util.function.Function;

public final class VSCHNetwork {
	private VSCHNetwork() {}

	private static final String PROTOCOL_VERSION = VSCHMod.VERSION;
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(VSCHMod.MODID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
	);
	private static int id = 0;

	public static void register() {
		registerS2C(PipeNodeSyncChunkS2C.class, PipeNodeSyncChunkS2C::decode);
		registerS2C(PipeNodeSyncChunkSectionS2C.class, PipeNodeSyncChunkSectionS2C::decode);
	}

	public static <T extends INetworkPacket> void registerS2C(Class<T> clazz, Function<FriendlyByteBuf, T> decoder) {
		CHANNEL.registerMessage(
			id++,
			clazz,
			INetworkPacket::encode,
			decoder,
			(packet, ctx) -> packet.handle(ctx.get()),
			Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}

	public static void sendToServer(INetworkPacket packet) {
		CHANNEL.sendToServer(packet);
	}

	public static void sendToPlayer(INetworkPacket packet, ServerPlayer player) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static void sendToTracking(INetworkPacket packet, ServerLevel level, BlockPos pos) {
		level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).forEach(p -> sendToPlayer(packet, (ServerPlayer) (p)));
	}
}
