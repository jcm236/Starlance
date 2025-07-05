package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.pipe.level.NodeLevel;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public abstract class MixinChunkSerializer {
	@Unique
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	@Unique
	private static final String SECTION_NODES_KEY = "vsch:nodes";

	@WrapOperation(
		method = "read",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;checkConsistencyWithBlocks(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"
		)
	)
	private static void readLevelChunkSection(
		final PoiManager poiManager,
		final SectionPos sectionPos,
		final LevelChunkSection section,
		final Operation<Void> operation,
		final @Local(argsOnly = true) ServerLevel level,
		final @Local(ordinal = 1) CompoundTag sectionData
	) {
		operation.call(poiManager, sectionPos, section);
		if (!(section instanceof INodeLevelChunkSection nodeSection)) {
			return;
		}
		if (!sectionData.contains(SECTION_NODES_KEY)) {
			return;
		}
		final byte[] sectionNodesData = sectionData.getByteArray(SECTION_NODES_KEY);
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(sectionNodesData));
		try {
			nodeSection.vsch$readNodes(NodeLevel.get(level), sectionPos, buf);
		} catch (RuntimeException	e) {
			LOGGER.error("[starlance]: Error when parsing pipe nodes", e);
			throw e;
		}
	}

	@WrapOperation(
		method = "write",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/CompoundTag;put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "CONSTANT",
				args = "stringValue=block_states"
			)
		)
	)
	private static Tag writeBlockStates(
		final CompoundTag sectionData,
		final String key,
		final Tag value,
		final Operation<Tag> operation,
		final @Local LevelChunkSection section
	) {
		if (!key.equals("block_states")) {
			throw new AssertionError("Incorrect injection point, expect block_states, got " + key);
		}
		final Tag oldTag = operation.call(sectionData, key, value);

		if (!(section instanceof INodeLevelChunkSection nodeSection) || !nodeSection.vsch$hasAnyNode()) {
			return oldTag;
		}
		// TODO: investigate if Pooled buffer can give more performance
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(128));
		try {
			nodeSection.vsch$writeNodes(buf);
		} catch (RuntimeException	e) {
			LOGGER.error("[starlance]: Error when encoding pipe nodes", e);
			throw e;
		}
		final byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		sectionData.putByteArray(SECTION_NODES_KEY, bytes);
		return oldTag;
	}
}
