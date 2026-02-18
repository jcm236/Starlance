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
package net.jcm.vsch.blocks.thruster;

import net.jcm.vsch.accessor.IGuiAccessor;
import net.jcm.vsch.blocks.custom.BaseThrusterBlock;
import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.ship.thruster.ThrusterData;
import net.jcm.vsch.util.NoSourceClipContext;
import net.lointain.cosmos.init.CosmosModParticleTypes;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;

public abstract class AbstractThrusterBlockEntity extends BlockEntity implements ParticleBlockEntity, WrenchableBlock {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String BRAIN_POS_TAG_NAME = "BrainPos";
	private static final String BRAIN_DATA_TAG_NAME = "BrainData";
	private static final String MODE_TAG_NAME = "Mode";

	private final Direction facing;
	private ThrusterBrain brain;
	/**
	 * brainPos holds temporary brain thruster position before it's resolved.
	 *
	 * @see resolveBrain
	 */
	private BlockPos brainPos = null;
	private boolean brainNeedInit = false;
	private final Map<Capability<?>, LazyOptional<?>> capsCache = new HashMap<>();

	protected AbstractThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		this.facing = state.getValue(DirectionalBlock.FACING);
		this.brain = new ThrusterBrain(this, this.getPeripheralType(), this.facing, this.createThrusterEngine());
	}

	protected abstract String getPeripheralType();

	protected abstract ThrusterEngine createThrusterEngine();

	public ThrusterBrain getBrain() {
		return this.brain;
	}

	public void setBrain(final ThrusterBrain brain) {
		this.brain = brain;
		this.capsCache.forEach((k, v) -> {
			v.invalidate();
		});
		this.capsCache.clear();
		this.sendUpdate();
	}

	public ThrusterData.ThrusterMode getThrusterMode() {
		return this.brain.getThrusterMode();
	}

	public void setThrusterMode(ThrusterData.ThrusterMode mode) {
		this.brain.setThrusterMode(mode);
	}

	public double getCurrentPower() {
		return this.brain.getCurrentPower();
	}

	double getScale() {
		final Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
		if (ship == null) {
			return 1;
		}
		final Vector3dc scaling = ship.getTransform().getShipToWorldScaling();
		return scaling.x() * scaling.y() * scaling.z();
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		final BlockPos pos = this.getBlockPos();
		if (data.contains(BRAIN_POS_TAG_NAME, Tag.TAG_INT_ARRAY)) {
			final int[] offset = data.getIntArray(BRAIN_POS_TAG_NAME);
			this.brainPos = pos.offset(offset[0], offset[1], offset[2]);
		} else if (data.contains(BRAIN_DATA_TAG_NAME, Tag.TAG_COMPOUND)) {
			this.brain.readFromNBT(data.getCompound(BRAIN_DATA_TAG_NAME));
			this.brainNeedInit = true;
		}
		this.brain.setThrusterModeNoUpdate(ThrusterData.ThrusterMode.values()[data.getByte(MODE_TAG_NAME)]);
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		final BlockPos selfPos = this.getBlockPos();
		final AbstractThrusterBlockEntity dataBlock = this.brain.getDataBlock();
		if (dataBlock == this) {
			data.put(BRAIN_DATA_TAG_NAME, this.brain.writeToNBT(new CompoundTag()));
		} else if (this.brainPos != null) {
			final BlockPos offset = this.brainPos.subtract(selfPos);
			data.putIntArray(BRAIN_POS_TAG_NAME, new int[]{offset.getX(), offset.getY(), offset.getZ()});
		} else {
			final BlockPos dataPos = dataBlock.getBlockPos();
			if (dataPos.equals(selfPos)) {
				LOGGER.error("[starlance]: duplicated thruster block entity at {}", selfPos);
				return;
			}
			final BlockPos offset = dataPos.subtract(selfPos);
			data.putIntArray(BRAIN_POS_TAG_NAME, new int[]{offset.getX(), offset.getY(), offset.getZ()});
		}
		data.putByte(MODE_TAG_NAME, (byte) (this.getThrusterMode().ordinal()));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		this.saveAdditional(data);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	void sendUpdate() {
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		LazyOptional<T> result = (LazyOptional<T>) capsCache.computeIfAbsent(cap, (c) -> this.brain.getCapability(c, direction).lazyMap(v -> v));
		if (result.isPresent()) {
			return result;
		}
		return super.getCapability(cap, direction);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		this.brain.neighborChanged(this, block, pos, moving);
	}

	private boolean canMerge(final AbstractThrusterBlockEntity other) {
		if (this.facing != other.facing) {
			return false;
		}
		final BlockPos selfPos = this.getBlockPos();
		final BlockPos otherPos = other.getBlockPos();
		if (this.facing.getAxis().choose(otherPos.getX() - selfPos.getX(), otherPos.getY() - selfPos.getY(), otherPos.getZ() - selfPos.getZ()) != 0) {
			return false;
		}
		return this.getPeripheralType().equals(other.getPeripheralType());
	}

	private void resolveBrain() {
		final Level level = this.getLevel();
		final BlockEntity be = level.getBlockEntity(this.brainPos);
		if (be instanceof final AbstractThrusterBlockEntity thruster) {
			if (thruster.brainNeedInit) {
				thruster.searchThrusters();
			}
			if (level.isClientSide) {
				if (thruster.getBrain().tryMergeBrain(this.brain)) {
					this.brainPos = null;
				}
				return;
			}
			// Thruster may not connect after load, if so create a new group
			if (this.brainPos != null) {
				this.brainPos = null;
				this.searchThrusters();
			}
		} else if (!this.getLevel().isClientSide) {
			// Do not clear brainPos on client, since chunks do not load at same time.
			LOGGER.debug("[starlance]: Thruster brain at {} for {} is not found", this.brainPos, this.getBlockPos());
			this.brainPos = null;
			this.setChanged();
		}
	}

	private void searchThrusters() {
		this.brainNeedInit = false;
		final Level level = this.getLevel();
		if (level.isClientSide) {
			// On client, other thrusters join main thruster by themselves, connection check is not needed.
			return;
		}
		bfs(this.getBlockPos(), (otherPos) -> {
			if (
				!(level.getBlockEntity(otherPos) instanceof final AbstractThrusterBlockEntity other) ||
				other.brainPos == null ||
				!this.canMerge(other)
			) {
				return false;
			}
			if (other.brainPos.equals(this.brainPos)) {
				this.brain.addThruster(other);
				other.setBrain(this.brain);
				other.brainPos = null;
				return true;
			}
			if (this.brain == other.getBrain()) {
				other.brainPos = null;
				return false;
			}
			if (!this.brain.tryMergeBrain(other.getBrain())) {
				return false;
			}
			other.brainPos = null;
			return true;
		});
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.brainNeedInit) {
			this.searchThrusters();
		} else if (this.brainPos != null) {
			this.resolveBrain();
		}
		if (this.brain.getDataBlock() == this) {
			this.brain.tick(level);
		}

		boolean isLit = state.getValue(BaseThrusterBlock.LIT);
		boolean powered = this.brain.getPower() > 0;
		if (powered != isLit) {
			level.setBlockAndUpdate(pos, state.setValue(BaseThrusterBlock.LIT, powered));
		}

		final VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships == null) {
			return;
		}

		final ThrusterData thrusterData = this.brain.getThrusterData();
		if (ships.getThrusterAtPos(pos) != thrusterData) {
			ships.addThruster(pos, thrusterData);
		}
	}

	@Override
	public InteractionResult onUseWrench(UseOnContext ctx) {
		if (ctx.getHand() != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		final Player player = ctx.getPlayer();

		if (!VSCHServerConfig.THRUSTER_TOGGLE.get()) {
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("vsch.error.thruster_modes_disabled")
						.withStyle(ChatFormatting.RED),
					true
				);
			}
			return InteractionResult.PASS;
		}

		ThrusterData.ThrusterMode blockMode = this.getThrusterMode();
		blockMode = blockMode.toggle();
		this.setThrusterMode(blockMode);

		if (player != null) {
			// Send a chat message to them. The wrench class will handle the actionbar
			player.displayClientMessage(
				Component.translatable("vsch.message.toggle")
					.append(Component.translatable("vsch." + blockMode.toString().toLowerCase())),
				true
			);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void onFocusWithWrench(final ItemStack stack, final Level level, final Player player) {
		if (!level.isClientSide) {
			return;
		}
		((IGuiAccessor) (Minecraft.getInstance().gui)).vsch$setOverlayMessageIfNotExist(
			Component.translatable("vsch.message.mode")
				.append(Component.translatable("vsch." + this.getThrusterMode().toString().toLowerCase())),
			25
		);
	}

	protected ParticleOptions getThrusterParticleType() {
		// TODO: Make custom particle
		return ParticleTypes.ASH;//ParticleTypes.ASH;
	}

	protected ParticleOptions getThrusterSmokeParticleType() {
		// TODO: Make custom particle
		return ParticleTypes.ASH;//ParticleTypes.ASH;
	}

	protected abstract double getEvaporateDistance();

	@Override
	public void tickParticles(final Level level, final BlockPos pos, final BlockState state) {
		if (this.brainNeedInit) {
			this.searchThrusters();
		} else if (this.brainPos != null) {
			this.resolveBrain();
			if (this.brainPos == null) {
				return;
			}
		}

		// If we are unpowered, do no particles
		if (this.getCurrentPower() == 0.0) {
			return;
		}

		// BlockPos is always at the corner, getCenter gives us a Vec3 thats centered YAY
		final Vec3 center = pos.getCenter();
		final Vector3d worldPos = new Vector3d(center.x, center.y, center.z);

		// Get blockstate direction, NORTH, SOUTH, UP, DOWN, etc
		final Direction dir = state.getValue(DirectionalBlock.FACING).getOpposite();
		final Vector3d direction = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());

		this.spawnParticles(worldPos, direction);
		this.spawnEvaporateParticles(level, pos, dir);
	}

	protected void spawnParticles(final Vector3d pos, final Vector3d direction) {
		// Offset the XYZ by a little bit so its at the end of the thruster block
		double x = pos.x + direction.x;
		double y = pos.y + direction.y;
		double z = pos.z + direction.z;

		final Vector3d speed = new Vector3d(direction).mul(this.getCurrentPower());

		speed.mul(0.6);

		// All that for one particle per tick...
		this.level.addParticle(
			this.getThrusterParticleType(),
			x, y, z,
			speed.x, speed.y, speed.z
		);

		speed.mul(1.06);

		// Ok ok, two particles per tick
		this.level.addParticle(
			this.getThrusterSmokeParticleType(),
			x, y, z,
			speed.x, speed.y, speed.z
		);
	}

	/**
	 * @see net.jcm.vsch.blocks.thruster.ThrusterEngine#simpleTickBurningObjects
	 */
	protected void spawnEvaporateParticles(final Level level, final BlockPos pos, final Direction direction) {
		final double distance = this.getEvaporateDistance();
		if (distance <= 0) {
			return;
		}
		final Vec3 center = pos.getCenter();
		final Vec3 centerExtendedPos = center.relative(direction, distance);

		final BlockHitResult hitResult = level.clip(new NoSourceClipContext(VSGameUtilsKt.toWorldCoordinates(level, center), VSGameUtilsKt.toWorldCoordinates(level, centerExtendedPos), pos));
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}
		final BlockPos hitPos = hitResult.getBlockPos();
		final FluidState hitFluid = level.getFluidState(hitPos);
		if (!hitFluid.is(FluidTags.WATER)) {
			return;
		}
		final Vec3 waterCenter = hitPos.getCenter();
		for (int i = 0; i < 20; i++) {
			final Vec3 ppos = waterCenter.offsetRandom(level.random, 1.0f);
			final Vec3 speed = Vec3.ZERO.offsetRandom(level.random, 0.5f);
			level.addParticle(
				ParticleTypes.ASH,
				true,
				ppos.x, ppos.y, ppos.z,
				speed.x, speed.y, speed.z
			);
		}
	}

	private static float getPowerByRedstone(final Level level, final BlockPos pos) {
		return (float)(level.getBestNeighborSignal(pos)) / 15;
	}

	private static void bfs(final BlockPos startPos, final Predicate<BlockPos> consumer) {
		final Set<BlockPos> visited = new HashSet<>();
		final Queue<BlockPos> queue = new ArrayDeque<>();
		visited.add(startPos);
		queue.add(startPos);
		while (!queue.isEmpty()) {
			final BlockPos pos = queue.remove();
			for (final Direction dir : Direction.values()) {
				final BlockPos pos2 = pos.relative(dir);
				if (visited.add(pos2) && consumer.test(pos2)) {
					queue.add(pos2);
				}
			}
		}
	}
}
