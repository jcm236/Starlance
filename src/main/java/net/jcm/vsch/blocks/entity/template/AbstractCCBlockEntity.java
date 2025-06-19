package net.jcm.vsch.blocks.entity.template;

import dan200.computercraft.shared.Capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.compat.CompatMods;

public abstract class AbstractCCBlockEntity extends BlockEntity {
	private LazyOptional<?> lazyPeripheral = LazyOptional.empty();

	protected AbstractCCBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = this.getPeripheral();
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	protected abstract LazyOptional<?> getPeripheral();
}
