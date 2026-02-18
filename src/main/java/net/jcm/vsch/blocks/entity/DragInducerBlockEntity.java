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
package net.jcm.vsch.blocks.entity;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.peripherals.DragInducerPeripheral;
import net.jcm.vsch.ship.dragger.DraggerData;
import net.jcm.vsch.ship.VSCHForceInducedShips;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class DragInducerBlockEntity extends BlockEntity implements ParticleBlockEntity {
	private final DraggerData draggerData;
	private volatile boolean enabled = false;
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();

	public DragInducerBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.DRAG_INDUCER_BLOCK_ENTITY.get(), pos, state);
		this.draggerData = new DraggerData(false);
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		setEnabled(enabled, true);
	}

	protected void setEnabled(boolean enabled, boolean update) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (update) {
			this.setChanged();
			this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 11);
		}
	}

	public boolean getPeripheralMode() {
		return this.isPeripheralMode;
	}

	public void setPeripheralMode(boolean on) {
		if (this.isPeripheralMode != on) {
			this.isPeripheralMode = on;
			this.setChanged();
		}
	}

	@Override
	public void load(CompoundTag data) {
		this.setEnabled(data.getBoolean("Enabled"), false);
		this.isPeripheralMode = CompatMods.COMPUTERCRAFT.isLoaded() && data.getBoolean("PeripheralMode");
		this.draggerData.on = this.isEnabled();
		super.load(data);
	}

	@Override
	public void saveAdditional(CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("Enabled", this.isEnabled());
		data.putBoolean("PeripheralMode", this.getPeripheralMode());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		data.putBoolean("Enabled", this.isEnabled());
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new DragInducerPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		if (!this.isPeripheralMode) {
			this.updateEnabledByRedstone();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updateEnabledByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		if (this.isEnabled() != this.draggerData.on) {
			this.draggerData.on = this.isEnabled();
		}

		// ----- Add this block to the force appliers for the current level ----- //

		int signal = level.getBestNeighborSignal(pos);
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships == null) {
			return;
		}
		if (ships.getDraggerAtPos(pos) != this.draggerData) {
			ships.addDragger(pos, this.draggerData);
		}
	}

	private void updateEnabledByRedstone() {
		this.setEnabled(getEnabledByRedstone(this.getLevel(), this.getBlockPos()));
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		// TODO: add particles depending on where we're thrusting? Might need to go in force inducers
	}

	private static boolean getEnabledByRedstone(Level level, BlockPos pos) {
		return level.getBestNeighborSignal(pos) > 0;
	}
}
