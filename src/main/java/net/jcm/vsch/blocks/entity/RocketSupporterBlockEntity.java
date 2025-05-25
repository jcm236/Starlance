package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.custom.RocketSupporterBlock;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.util.Pair;
import net.jcm.vsch.util.assemble.IMoveable;
import net.jcm.vsch.util.assemble.MoveUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import org.joml.Quaterniond;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RocketSupporterBlockEntity extends BlockEntity implements ParticleBlockEntity {
	private static final int MAX_SIZE = 256 * 16;

	private boolean triggering = false;
	private boolean assembling = false;
	private String assembleResult = null;
	private final Queue<BlockPos> queueing = new ArrayDeque<>();
	private final DenseBlockPosSet blocks = new DenseBlockPosSet();
	private final DenseBlockPosSet checked = new DenseBlockPosSet();
	private final AABBi box = new AABBi();

	public RocketSupporterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.ROCKET_SUPPORTER_BLOCK_ENTITY.get(), pos, state);
	}

	public boolean isAssembling() {
		return this.assembling;
	}

	public boolean isAssembleSuccessed() {
		return !this.isAssembling() && this.assembleResult == null;
	}

	public String getAssembleResult() {
		return this.assembleResult;
	}

	public void neighborChanged(Block neighbor, BlockPos neighborPos, boolean moving) {
		final Level level = this.getLevel();
		final BlockPos pos = this.getBlockPos();
		final boolean shouldTrigger = Direction.stream()
			.filter(dir -> dir != this.getBlockState().getValue(DirectionalBlock.FACING))
			.anyMatch(dir -> level.getSignal(pos.relative(dir), dir) > 0);
		if (this.triggering == shouldTrigger) {
			return;
		}
		this.triggering = shouldTrigger;
		if (shouldTrigger) {
			this.assemble();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.assembling) {
			this.assembleTick(level);
		}
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		//
	}

	private void assemble() {
		if (this.assembling) {
			return;
		}
		this.assembling = true;
		this.assembleResult = null;
		this.queueing.clear();
		this.blocks.clear();
		this.checked.clear();
		final BlockPos facingBlockPos = this.getBlockPos().relative(this.getBlockState().getValue(DirectionalBlock.FACING));
		this.queueing.add(facingBlockPos);
		this.box
			.setMin(facingBlockPos.getX(), facingBlockPos.getY(), facingBlockPos.getZ())
			.setMax(facingBlockPos.getX(), facingBlockPos.getY(), facingBlockPos.getZ());
	}

	private void finishAssemble(String error) {
		// TODO: change texture based on if assemble successed
		// TODO: show assemble error message somewhere
		this.assembling = false;
		this.assembleResult = error;
		this.queueing.clear();
		this.blocks.clear();
		this.checked.clear();
	}

	private void assembleTick(final ServerLevel level) {
		final BlockPos selfPos = this.getBlockPos();
		int ticked = 0;
		while (true) {
			final BlockPos pos = this.queueing.poll();
			if (pos == null) {
				this.checked.clear();
				if (ticked > 0) {
					return;
				}
				break;
			}
			if (this.blocks.size() >= VSCHConfig.MAX_ASSEMBLE_BLOCKS.get()) {
				this.finishAssemble("too many blocks");
				return;
			}
			if (pos.equals(selfPos)) {
				this.finishAssemble("cannot assemble itself");
				return;
			}
			if (!this.checkBlock(pos)) {
				return;
			}
			ticked++;
			if (ticked > 16 * 16 * 16) {
				return;
			}
		}
		if (this.blocks.isEmpty()) {
			this.finishAssemble("no block to assemble");
			return;
		}
		if (this.box.lengthX() > MAX_SIZE || this.box.lengthY() > MAX_SIZE || this.box.lengthZ() > MAX_SIZE) {
			this.finishAssemble("excess size limit");
			return;
		}
		this.createShip(level);
	}

	private boolean checkBlock(final BlockPos pos) {
		final Level level = this.getLevel();
		if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
			this.finishAssemble("assemble unloaded chunk");
			return false;
		}
		final BlockState block = level.getBlockState(pos);
		if (this.isAirBlock(block)) {
			return true;
		}
		if (!this.canAssembleBlock(block)) {
			this.finishAssemble("cannot assemble block");
			return false;
		}
		this.box.union(pos.getX(), pos.getY(), pos.getZ());
		this.blocks.add(pos.getX(), pos.getY(), pos.getZ());
		for (final Direction dir : Direction.values()) {
			final BlockPos p = pos.relative(dir);
			final BlockState targetState = level.getBlockState(p);
			if (targetState.getBlock() instanceof RocketSupporterBlock && targetState.getValue(DirectionalBlock.FACING) == dir.getOpposite()) {
				final RocketSupporterBlockEntity otherSupporter = (RocketSupporterBlockEntity) (level.getBlockEntity(p));
				if (otherSupporter != this && otherSupporter.isAssembling()) {
					this.finishAssemble(null);
					return false;
				}
				continue;
			}
			if (this.checked.add(p.getX(), p.getY(), p.getZ())) {
				this.queueing.add(p);
			}
		}
		return true;
	}

	private boolean isAirBlock(final BlockState state) {
		return state.isAir();
	}

	private boolean canAssembleBlock(final BlockState state) {
		final Block block = state.getBlock();
		if (block == Blocks.BEDROCK) {
			return false;
		}
		// TODO: load blacklist from config
		return true;
	}

	private void createShip(final ServerLevel level) {
		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final Vector3i worldCenter = new Vector3i(this.box.center(new Vector3d()), RoundingMode.TRUNCATE);
		final ServerShip ship = shipWorld.createNewShipAtBlock(worldCenter, false, 1.0, levelId);
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i offset = shipCenter.sub(worldCenter, new Vector3i());
		final List<Pair<BlockPos, BlockState>> blockStates = new ArrayList<>(this.blocks.size());
		final List<Entity> entities = new ArrayList<>();

		// get attachable entities
		for (final Entity entity : level.getEntities(null, new AABB(this.box.minX - 1, this.box.minY - 1, this.box.minZ - 1, this.box.maxX + 2, this.box.maxY + 2, this.box.maxZ + 2))) {
			if (entity instanceof final HangingEntity he) {
				final BlockPos hanging = he.getPos().relative(he.getDirection().getOpposite());
				if (this.blocks.contains(hanging.getX(), hanging.getY(), hanging.getZ())) {
					entities.add(entity);
				}
			} else if (CompatMods.CREATE.isLoaded()) {
				if (entity instanceof final SeatEntity seat) {
					final BlockPos p = seat.blockPosition();
					if (this.blocks.contains(p.getX(), p.getY(), p.getZ())) {
						entities.add(entity);
					}
				} else if (entity instanceof final SuperGlueEntity glue) {
					final AABB box = glue.getBoundingBox();
					if (streamBlocksInAABB(box).anyMatch(p -> this.blocks.contains(p.getX(), p.getY(), p.getZ()))) {
						entities.add(entity);
					}
				}
			}
		}

		// move blocks
		this.blocks.forEach((x, y, z) -> {
			final BlockPos pos = new BlockPos(x, y, z);
			final BlockPos target = pos.offset(offset.x, offset.y, offset.z);
			BlockState state = level.getBlockState(pos);

			final BlockEntity be = level.getBlockEntity(pos);
			IMoveable<?> moveableOld = MoveUtil.getMover(be);
			if (moveableOld == null) {
				moveableOld = MoveUtil.getMover(state.getBlock());
			}
			if (moveableOld != null) {
				moveableOld.beforeSaveForMove(level, pos, target);
			}

			state = level.getBlockState(pos);
			blockStates.add(new Pair<>(pos, state));
			final CompoundTag nbt = level.getChunkAt(pos).getBlockEntityNbtForSaving(pos);
			if (nbt != null) {
				final BlockPos targetPos = pos.offset(offset.x, offset.y, offset.z);
				nbt.putInt("x", targetPos.getX());
				nbt.putInt("y", targetPos.getY());
				nbt.putInt("z", targetPos.getZ());
				level.getChunkAt(targetPos).setBlockEntityNbt(nbt);
			}

			Clearable.tryClear(be);

			final Object moveData = moveableOld != null ? moveableOld.beforeMove(level, pos, target) : null;
			level.setBlock(target, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			IMoveable<?> moveableNew = MoveUtil.getMover(level.getBlockEntity(target));
			if (moveableNew == null) {
				moveableNew = MoveUtil.getMover(state.getBlock());
			}
			if (moveableNew != null) {
				((IMoveable) (moveableNew)).afterMove(level, pos, target, moveData);
			}
			return null;
		});

		// move entities
		for (final Entity entity : entities) {
			final Vec3 pos = entity.position();
			entity.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
		}

		// update blocks
		for (final Pair<BlockPos, BlockState> value : blockStates) {
			final BlockPos pos = value.left();
			final Block block = value.right().getBlock();
			level.blockUpdated(pos, block);
			level.blockUpdated(pos.offset(offset.x, offset.y, offset.z), block);
		}

		final AABBic box = ship.getShipAABB();
		final Vector3d absPosition = ship.getTransform().getPositionInWorld().add(ship.getInertiaData().getCenterOfMassInShip(), new Vector3d()).sub(shipCenter.x, shipCenter.y, shipCenter.z);
		final Vector3d position = new Vector3d(absPosition);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		final Vector3d scaling = new Vector3d(1);
		double scale = 1.0;

		final ServerShip selfShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
		if (selfShip != null) {
			final ShipTransform selfTransform = selfShip.getTransform();
			selfTransform.getShipToWorld().transformPosition(position);
			rotation.set(selfTransform.getShipToWorldRotation());
			velocity.set(selfShip.getVelocity());
			omega.set(selfShip.getOmega());
			scaling.set(selfTransform.getShipToWorldScaling());
			scale = Math.sqrt(scaling.lengthSquared() / 3);
		}
		shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, levelId, scale));

		// fix new ship's velocity and omega
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					if (!transform.getPositionInWorld().equals(nextTransform.getPositionInWorld()) || !transform.getShipToWorldRotation().equals(nextTransform.getShipToWorldRotation())) {
						ship.setTransformProvider(null);
						return null;
					}
					if (ship.getVelocity().lengthSquared() == 0 && ship.getOmega().lengthSquared() == 0) {
						if (selfShip != null) {
							final ShipTransform selfTransform2 = selfShip.getTransform();
							selfTransform2.getShipToWorld().transformPosition(absPosition, position);
							rotation.set(selfTransform2.getShipToWorldRotation());
							velocity.set(selfShip.getVelocity());
							omega.set(selfShip.getOmega());
							scaling.set(selfTransform2.getShipToWorldScaling());
						}
						return new NextTransformAndVelocityData(new ShipTransformImpl(position, nextTransform.getPositionInShip(), rotation, scaling), velocity, omega);
					}
					return null;
				}
			});
		}

		this.finishAssemble(null);
	}

	private static Stream<BlockPos> streamBlocksInAABB(AABB box) {
		final int
			minX = (int) (Math.round(box.minX)), maxX = (int) (Math.round(box.maxX)),
			minY = (int) (Math.round(box.minY)), maxY = (int) (Math.round(box.maxY)),
			minZ = (int) (Math.round(box.minZ)), maxZ = (int) (Math.round(box.maxZ));
		final int widthX = maxX - minX, widthY = maxY - minY, widthZ = maxZ - minZ;
		return IntStream.range(0, widthX * widthY * widthZ).mapToObj((i) -> {
			final int x = i % widthX + minX;
			i /= widthX;
			final int z = i % widthZ + minZ;
			i /= widthZ;
			final int y = i + minY;
			return new BlockPos(x, y, z);
		});
	}
}
