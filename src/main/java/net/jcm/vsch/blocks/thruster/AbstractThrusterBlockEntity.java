package net.jcm.vsch.blocks.thruster;

import net.jcm.vsch.accessor.IGuiAccessor;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.custom.BaseThrusterBlock;
import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
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
import net.minecraft.world.level.block.Blocks;
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

public abstract class AbstractThrusterBlockEntity extends BlockEntity implements ParticleBlockEntity, WrenchableBlock {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String BRAIN_POS_TAG_NAME = "BrainPos";
	private static final String BRAIN_DATA_TAG_NAME = "BrainData";
	private ThrusterBrain brain;
	/**
	 * brainPos holds temporary brain thruster position before it's resolved.
	 *
	 * @see resolveBrain
	 */
	private BlockPos brainPos = null;
	private final Map<Capability<?>, LazyOptional<?>> capsCache = new HashMap<>();

	protected AbstractThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		this.brain = new ThrusterBrain(this, this.getPeripheralType(), state.getValue(DirectionalBlock.FACING), this.createThrusterEngine());
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
		if (data.contains(BRAIN_POS_TAG_NAME, Tag.TAG_BYTE_ARRAY)) {
			byte[] offset = data.getByteArray(BRAIN_POS_TAG_NAME);
			this.brainPos = pos.offset(offset[0], offset[1], offset[2]);
			if (this.getLevel() != null) {
				this.resolveBrain();
			}
		} else if (data.contains(BRAIN_DATA_TAG_NAME, Tag.TAG_COMPOUND)) {
			CompoundTag brainData = data.getCompound(BRAIN_DATA_TAG_NAME);
			this.brain.readFromNBT(brainData);
		}
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		final BlockPos selfPos = this.getBlockPos();
		final AbstractThrusterBlockEntity dataBlock = this.brain.getDataBlock();
		if (this.brainPos != null) {
			final BlockPos offset = this.brainPos.subtract(selfPos);
			data.putByteArray(BRAIN_POS_TAG_NAME, new byte[]{(byte)(offset.getX()), (byte)(offset.getY()), (byte)(offset.getZ())});
		} else if (dataBlock == this) {
			data.put(BRAIN_DATA_TAG_NAME, this.brain.writeToNBT(new CompoundTag()));
		} else {
			final BlockPos dataPos = dataBlock.getBlockPos();
			if (dataPos.equals(selfPos)) {
				LOGGER.error("[starlance]: duplicated thruster block entity at {}", selfPos);
				return;
			}
			final BlockPos offset = dataPos.subtract(selfPos);
			data.putByteArray(BRAIN_POS_TAG_NAME, new byte[]{(byte)(offset.getX()), (byte)(offset.getY()), (byte)(offset.getZ())});
		}
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

	private void resolveBrain() {
		final BlockEntity be = this.getLevel().getBlockEntity(this.brainPos);
		if (be instanceof AbstractThrusterBlockEntity thruster) {
			final ThrusterBrain newBrain = thruster.getBrain();
			if (this.brain != newBrain) {
				newBrain.addThruster(this);
				this.setBrain(newBrain);
			}
			this.brainPos = null;
		} else if (this.getLevel() instanceof ServerLevel) {
			LOGGER.warn("[starlance]: Thruster brain at {} for {} is not found", this.brainPos, this.getBlockPos());
			this.brainPos = null;
			this.setChanged();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.brainPos != null) {
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

		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships == null) {
			return;
		}

		ThrusterData thrusterData = this.brain.getThrusterData();
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

		if (!VSCHConfig.THRUSTER_TOGGLE.get()) {
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
		((IGuiAccessor) (Minecraft.getInstance().gui)).starlance$setOverlayMessageIfNotExist(
			Component.translatable("vsch.message.mode")
				.append(Component.translatable("vsch." + this.getThrusterMode().toString().toLowerCase())),
			25
		);
	}

	protected ParticleOptions getThrusterParticleType() {
		return CosmosModParticleTypes.THRUSTED.get();
	}

	protected ParticleOptions getThrusterSmokeParticleType() {
		return CosmosModParticleTypes.THRUST_SMOKE.get();
	}

	protected abstract double getEvaporateDistance();

	@Override
	public void tickParticles(final Level level, final BlockPos pos, final BlockState state) {
		if (this.brainPos != null) {
			this.resolveBrain();
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
				CosmosModParticleTypes.AIR_THRUST.get(),
				true,
				ppos.x, ppos.y, ppos.z,
				speed.x, speed.y, speed.z
			);
		}
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		return (float)(level.getBestNeighborSignal(pos)) / 15;
	}
}
