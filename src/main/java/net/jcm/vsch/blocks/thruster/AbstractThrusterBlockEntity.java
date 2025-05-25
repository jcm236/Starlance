package net.jcm.vsch.blocks.thruster;

import net.jcm.vsch.blocks.custom.template.AbstractThrusterBlock;
import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.ship.thruster.ThrusterData;
import net.lointain.cosmos.init.CosmosModParticleTypes;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.joml.Vector3d;
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
	private BlockPos brainPos = null;
	private final Map<Capability<?>, LazyOptional<?>> capsCache = new HashMap<>();

	protected AbstractThrusterBlockEntity(String peripheralType, BlockEntityType<?> type, BlockPos pos, BlockState state, ThrusterEngine engine) {
		super(type, pos, state);

		this.brain = new ThrusterBrain(this, peripheralType, state.getValue(DirectionalBlock.FACING), engine);
	}

	public ThrusterBrain getBrain() {
		return this.brain;
	}

	public void setBrain(ThrusterBrain brain) {
		this.brain = brain;
		this.capsCache.forEach((k, v) -> {
			v.invalidate();
		});
		this.capsCache.clear();
	}

	public ThrusterData.ThrusterMode getThrusterMode() {
		return this.brain.getThrusterMode();
	}

	public void setThrusterMode(ThrusterData.ThrusterMode mode) {
		this.brain.setThrusterMode(mode);
	}

	public float getCurrentPower() {
		return this.brain.getCurrentPower();
	}

	@Override
	public void load(CompoundTag data) {
		super.load(data);
		BlockPos pos = this.getBlockPos();
		if (data.contains(BRAIN_POS_TAG_NAME, 7)) {
			byte[] offset = data.getByteArray(BRAIN_POS_TAG_NAME);
			this.brainPos = pos.offset(offset[0], offset[1], offset[2]);
			if (this.getLevel() != null) {
				this.resolveBrain();
			}
		} else if (data.contains(BRAIN_DATA_TAG_NAME, 10)) {
			CompoundTag brainData = data.getCompound(BRAIN_DATA_TAG_NAME);
			this.brain.readFromNBT(brainData);
		}
	}

	@Override
	public void saveAdditional(CompoundTag data) {
		super.saveAdditional(data);
		AbstractThrusterBlockEntity dataBlock = this.brain.getDataBlock();
		if (this.brainPos != null) {
			BlockPos pos = this.brainPos.subtract(this.getBlockPos());
			data.putByteArray(BRAIN_POS_TAG_NAME, new byte[]{(byte)(pos.getX()), (byte)(pos.getY()), (byte)(pos.getZ())});
		} else if (dataBlock == this) {
			CompoundTag brainData = new CompoundTag();
			this.brain.writeToNBT(brainData);
			data.put(BRAIN_DATA_TAG_NAME, brainData);
		} else {
			BlockPos pos = dataBlock.getBlockPos().subtract(this.getBlockPos());
			data.putByteArray(BRAIN_POS_TAG_NAME, new byte[]{(byte)(pos.getX()), (byte)(pos.getY()), (byte)(pos.getZ())});
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
		BlockEntity be = this.getLevel().getBlockEntity(this.brainPos);
		if (be instanceof AbstractThrusterBlockEntity thruster) {
			ThrusterBrain newBrain = thruster.getBrain();
			if (this.brain != newBrain) {
				newBrain.addThruster(this);
				this.setBrain(newBrain);
			}
			this.brainPos = null;
		} else if (this.getLevel() instanceof ServerLevel) {
			LOGGER.warn("Thruster brain at {} for {} is not found", this.brainPos, this.getBlockPos());
			this.brainPos = null;
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

		boolean isLit = state.getValue(AbstractThrusterBlock.LIT);
		boolean powered = this.brain.getPower() > 0;
		if (powered != isLit) {
			level.setBlockAndUpdate(pos, state.setValue(AbstractThrusterBlock.LIT, powered));
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
				player.displayClientMessage(Component.translatable("vsch.error.thruster_modes_disabled")
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
			player.sendSystemMessage(Component.translatable("vsch.message.toggle")
				.append(Component.translatable("vsch." + blockMode.toString().toLowerCase())));
		}

		return InteractionResult.SUCCESS;
	}

	protected ParticleOptions getThrusterParticleType() {
		return CosmosModParticleTypes.THRUSTED.get();
	}

	protected ParticleOptions getThrusterSmokeParticleType() {
		return CosmosModParticleTypes.THRUST_SMOKE.get();
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
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

		spawnParticles(worldPos, direction);
	}

	protected void spawnParticles(Vector3d pos, Vector3d direction) {
		// Offset the XYZ by a little bit so its at the end of the thruster block
		double x = pos.x + direction.x;
		double y = pos.y + direction.y;
		double z = pos.z + direction.z;

		final Vector3d speed = new Vector3d(direction).mul(this.getCurrentPower());

		speed.mul(0.6);

		// All that for one particle per tick...
		level.addParticle(
			this.getThrusterParticleType(),
			x, y, z,
			speed.x, speed.y, speed.z
		);

		speed.mul(1.06);

		// Ok ok, two particles per tick
		level.addParticle(
			this.getThrusterSmokeParticleType(),
			x, y, z,
			speed.x, speed.y, speed.z
		);
	}

	@Override
	public void onFocusWithWrench(ItemStack stack, Level level, Player player) {
		player.displayClientMessage(
			Component.translatable("vsch.message.mode")
				.append(Component.translatable("vsch." + this.getThrusterMode().toString().toLowerCase())),
			true
		);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		return (float)(level.getBestNeighborSignal(pos)) / 15;
	}
}
