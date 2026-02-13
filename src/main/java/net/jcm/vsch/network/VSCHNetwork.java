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
package net.jcm.vsch.network;

import net.jcm.vsch.spacemods.SpaceMods;
import net.jcm.vsch.spacemods.ad_astra.SyncMenuTierS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.network.c2s.ToggleItemPacketC2S;

import java.util.Optional;
import java.util.function.Function;

public final class VSCHNetwork {
	private VSCHNetwork() {}

	@SuppressWarnings("removal")
	private static final String PROTOCOL_VERSION = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
	@SuppressWarnings("removal")
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(VSCHMod.MODID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
	);
	private static int id = 0;

	public static void register() {
		registerC2S(ToggleItemPacketC2S.class, ToggleItemPacketC2S::decode);
        if (SpaceMods.AD_ASTRA.isLoaded()) {
            registerS2C(SyncMenuTierS2C.class, SyncMenuTierS2C::decode);
        }
	}

	public static <T extends INetworkPacket> void registerC2S(final Class<T> clazz, final Function<FriendlyByteBuf, T> decoder) {
		CHANNEL.registerMessage(
			id++,
			clazz,
			INetworkPacket::encode,
			decoder,
			(packet, ctx) -> packet.handle(ctx.get()),
			Optional.of(NetworkDirection.PLAY_TO_SERVER)
		);
	}

	public static <T extends INetworkPacket> void registerS2C(final Class<T> clazz, final Function<FriendlyByteBuf, T> decoder) {
		CHANNEL.registerMessage(
			id++,
			clazz,
			INetworkPacket::encode,
			decoder,
			(packet, ctx) -> packet.handle(ctx.get()),
			Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}

	public static void sendToServer(final INetworkPacket packet) {
		CHANNEL.sendToServer(packet);
	}

	public static void sendToPlayer(final INetworkPacket packet, final ServerPlayer player) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static void sendToTracking(final INetworkPacket packet, final ServerLevel level, final BlockPos pos) {
		level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).forEach((p) -> sendToPlayer(packet, (ServerPlayer) (p)));
	}
}
