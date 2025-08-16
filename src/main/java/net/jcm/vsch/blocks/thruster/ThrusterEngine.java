package net.jcm.vsch.blocks.thruster;

import net.jcm.vsch.api.block.IVentBlock;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.util.NoSourceClipContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public abstract class ThrusterEngine {
	private static final Random RND = new Random();
	private static final EntityTypeTest<Entity, Entity> ANY_ENTITY_TESTER = new EntityTypeTest<>() {
		@Override
		public Entity tryCast(Entity entity) {
			return entity;
		}

		@Override
		public Class<Entity> getBaseClass() {
			return Entity.class;
		}
	};

	private final int tanks;
	private final int energyConsumeRate;
	private final float maxThrottle;

	protected ThrusterEngine(int tanks, int energyConsumeRate, float maxThrottle) {
		this.tanks = tanks;
		this.energyConsumeRate = energyConsumeRate;
		this.maxThrottle = maxThrottle;
	}

	public int getTanks() {
		return this.tanks;
	}

	public int getEnergyConsumeRate() {
		return this.energyConsumeRate;
	}

	public float getMaxThrottle() {
		return this.maxThrottle;
	}

	/**
	 * isValidFuel checks if the fluid can be uses as fuel.
	 * same fluid must <b>NOT</b> be able to fill in two different tanks.
	 *
	 * @param tank  The tank the fuel is going to transfer in
	 * @param fluid The fuel's fluid stack
	 * @return {@code true} if the fluid is consumable, {@code false} otherwise
	 */
	public boolean isValidFuel(final int tank, final Fluid fluid) {
		return false;
	}

	/**
	 * ticks the engine with given power, which consumes energy and fuel,
	 * and update the actual achieved power based on available energy and fuel.
	 *
	 * @param context A {@link ThrusterEngineContext}
	 * @see ThrusterEngineContext
	 */
	public void tick(final ThrusterEngineContext context) {
		if (this.energyConsumeRate == 0) {
			return;
		}
		final double power = context.getPower();
		if (power == 0) {
			return;
		}
		final double scale = context.getScale();
		final int amount = context.getAmount();
		final int needs = (int) (Math.ceil(this.energyConsumeRate * power * scale * amount));
		final int extracted = context.getEnergyStorage().extractEnergy(needs, true);
		context.setPower(extracted / ((double) (this.energyConsumeRate) * amount * scale));
		context.addConsumer((ctx) -> {
			ctx.getEnergyStorage().extractEnergy((int) (Math.ceil(ctx.getPower() * ctx.getScale() * ctx.getAmount() * this.energyConsumeRate)), false);
		});
	}

	/**
	 * tickBurningObjects sets on fire entities/blocks that should be burned by the thruster
	 *
	 * @param context   A {@link ThrusterEngineContext}, should never be modified
	 * @param thrusters Thrusters' positions
	 * @param direction Thrusters' facing direction
	 */
	public abstract void tickBurningObjects(ThrusterEngineContext context, List<BlockPos> thrusters, Direction direction);

	/**
	 * simpleTickBurningObjects do some basic operations on the entities / blocks the thruster facing.
	 * <br/>
	 *
	 * <b>Implement details:</b><br/>
	 * <ol>
	 * 	<li>
	 * 		It does a ray detection from middle of a thruster, find the maximum distance flame can go before it hits a block.
	 * 	</li>
	 * 	<li>
	 * 		If the ray hits a block:
	 * 		<ol>
	 * 			<li>If the block (or its super class) is a TNT, ignite it.</li>
	 * 			<li>Otherwise, set the block on fire on each side.</li>
	 * 		</ol>
	 * 	</li>
	 * 	<li>
	 * 		It collects all entity in the flame collision box.
	 * 	</li>
	 * 	<li>
	 * 		If an entity is pushable, push it based on its distance from the thruster
	 * 	</li>
	 * 	<li>
	 * 		If an entity is not {@link Entity#fireImmune fireImmune} ignite the entity up to 15s based on the its distance from thruster
	 * 	</li>
	 * 	<li>
	 * 		Hurt the entity due fire with at least 1 damage, which increase linearly
	 * if the entity is closer than 10% of the thruster's max distance.
	 * 	</li>
	 * </ol>
	 *
	 * @param context       A {@link ThrusterEngineContext}
	 * @param thrusters     Thrusters' positions
	 * @param direction     Thrusters' facing direction
	 * @param maxDistance   Thrusters' max flame length
	 * @param maxBurnDamage Thrusters' flame's max burn damage
	 * @param maxPushVel    Thrusters' flame's max push accleration
	 */
	public static void simpleTickBurningObjects(final ThrusterEngineContext context, final List<BlockPos> thrusters, final Direction direction, final double maxDistance, final int maxBurnDamage, final double maxPushVel) {
		final int maxBurnTime = 15 * 20;
		final ServerLevel level = context.getLevel();
		final double distance = maxDistance * context.getPower() * context.getScale();
		if (distance <= 0) {
			return;
		}
		if (thrusters.isEmpty()) {
			return;
		}

		final Map<Entity, Double> pendingEntities = new HashMap<>();
		final List<Entity> entities = new ArrayList<>();

		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, thrusters.get(0));
		final Vector3d directionVec = new Vector3d(Vec3.atLowerCornerOf(direction.getNormal()).toVector3f());
		if (ship != null) {
			ship.getShipToWorld().transformDirection(directionVec);
		}

		for (final BlockPos pos : thrusters) {
			final Vec3 centerPos = Vec3.atCenterOf(pos);
			final Vec3 centerExtendedPos = centerPos.relative(direction, distance);
			final Vec3 centerPosWorld = VSGameUtilsKt.toWorldCoordinates(level, centerPos);
			final Vec3 centerExtendedPosWorld = VSGameUtilsKt.toWorldCoordinates(level, centerExtendedPos);

			final Vec3 centerFacePos = centerPos.relative(direction, 0.5);
			final Vec3 centerFacePosWorld = VSGameUtilsKt.toWorldCoordinates(level, centerFacePos);

			final BlockHitResult hitResult = level.clip(new ThrustClipContext(centerPosWorld, centerExtendedPosWorld, pos));
			if (hitResult.getType() == HitResult.Type.BLOCK) {
				final BlockPos hitPos = hitResult.getBlockPos();
				final BlockState blockState = level.getBlockState(hitPos);
				final Block hitBlock = blockState.getBlock();
				final FluidState hitFluid = blockState.getFluidState();
				if (hitFluid.isEmpty()) {
					if (hitBlock instanceof TntBlock) {
						TntBlock.explode(level, hitPos);
						level.setBlock(hitPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
					}
					Direction.stream()
						.filter((d) -> d != hitResult.getDirection().getOpposite())
						.map(hitPos::relative)
						.filter((firePos) -> {
							final BlockState firePosState = level.getBlockState(firePos);
							return firePosState.isAir() || firePosState.canBeReplaced();
						})
						.filter((firePos) -> RND.nextFloat() < 0.025f)
						.forEach((firePos) -> {
							level.setBlock(firePos, BaseFireBlock.getState(level, firePos), Block.UPDATE_ALL);
						});
				} else if (!hitFluid.isSource() || hitFluid.is(Fluids.WATER)) {
					if (hitBlock instanceof LiquidBlock) {
						level.setBlock(hitPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					} else if (hitBlock instanceof BucketPickup pickupable) {
						pickupable.pickupBlock(level, hitPos, blockState);
					}
				}
			}

			final BlockHitResult particleHitResult = level.clip(new ParticleClipContext(centerPosWorld, centerExtendedPosWorld, pos));
			double clipDist = distance;
			if (particleHitResult.getType() == HitResult.Type.BLOCK) {
				clipDist = particleHitResult.getLocation().distanceTo(centerPosWorld);
			}

			final Direction.Axis axis = direction.getAxis();
			final Vec3 cornerPos = switch (axis) {
				case X -> new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ());
				case Y -> new Vec3(pos.getX(), pos.getY() + 0.5, pos.getZ());
				case Z -> new Vec3(pos.getX(), pos.getY(), pos.getZ() + 0.5);
			};
			final AABB box = new AABB(
				cornerPos,
				cornerPos
					.relative(direction, clipDist - 0.5)
					.add(
						axis.choose(0, 1, 1),
						axis.choose(1, 0, 1),
						axis.choose(1, 1, 0)
					)
			);
			level.getEntities(
				ANY_ENTITY_TESTER,
				VSGameUtilsKt.transformAabbToWorld(level, box),
				(entity) -> {
					if (entity instanceof Player player && player.isSpectator()) {
						return false;
					}
					if (entity.getPistonPushReaction() == PushReaction.IGNORE && entity.fireImmune()) {
						return false;
					}
					if (ship != null && !VSGameUtilsKt.isBlockInShipyard(level, entity.position())) {
						final AABB entityBox = entity.getBoundingBox();
						final AABBd entityBox2 = new AABBd(entityBox.minX, entityBox.minY, entityBox.minZ, entityBox.maxX, entityBox.maxY, entityBox.maxZ);
						entityBox2.transform(ship.getWorldToShip());
						if (!box.intersects(entityBox2.minX, entityBox2.minY, entityBox2.minZ, entityBox2.maxX, entityBox2.maxY, entityBox2.maxZ)) {
							return false;
						}
					}
					return true;
				},
				entities
			);
			for (final Entity entity : entities) {
				final double dist = entity.getBoundingBox().distanceToSqr(centerFacePosWorld);
				pendingEntities.compute(entity, (k, v) -> v == null || dist > v ? dist : v);
			}
			entities.clear();
		}

		for (final Map.Entry<Entity, Double> entry : pendingEntities.entrySet()) {
			final double dist = Math.sqrt(entry.getValue());
			final double power = (maxDistance - dist) / maxDistance;
			if (power <= 0) {
				continue;
			}
			final Entity entity = entry.getKey();

			if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
				entity.addDeltaMovement(new Vec3(directionVec.x * power * maxPushVel, directionVec.y * power * maxPushVel, directionVec.z * power * maxPushVel));
				if (entity instanceof ServerPlayer player) {
					player.connection.send(new ClientboundSetEntityMotionPacket(player));
				}
			}

			if (entity.fireImmune()) {
				continue;
			}

			int burnTime = (int) (maxBurnTime * power);
			if (entity instanceof LivingEntity livingEntity) {
				burnTime = ProtectionEnchantment.getFireAfterDampener(livingEntity, burnTime);
			}
			if (burnTime <= 0) {
				continue;
			}
			if (entity instanceof Creeper creeper && !creeper.isIgnited()) {
				creeper.ignite();
			}
			if (entity.getRemainingFireTicks() < burnTime) {
				entity.setRemainingFireTicks(burnTime);
			}
			int burnDamage = 1;
			if (burnTime >= (int) (maxBurnTime * 0.9)) {
				burnDamage += Math.round((double) (maxBurnDamage - burnDamage) * burnTime / maxBurnTime);
			}
			entity.hurt(entity.damageSources().onFire(), burnDamage);
		}
	}

	/**
	 * ThrustClipContext skip {@link IVentBlock} if applicable
	 */
	private static class ThrustClipContext extends NoSourceClipContext {
		ThrustClipContext(Vec3 from, Vec3 to, BlockPos source) {
			super(from, to, source);
		}

		@Override
		public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
			final VoxelShape shape = super.getBlockShape(state, level, pos);
			if (shape.isEmpty()) {
				return shape;
			}
			if (state.getBlock() instanceof IVentBlock vent) {
				final BlockHitResult hitResult = shape.clip(this.getFrom(), this.getTo(), pos);
				if (hitResult != null && vent.canThrustPass(hitResult)) {
					return Shapes.empty();
				}
			}
			return shape;
		}
	}

	/**
	 * This clip context only matching full blocks in world since CH particle only respect to full block
	 * and VS2 did not make particle collides with ship yet
	 */
	private static class ParticleClipContext extends NoSourceClipContext {
		ParticleClipContext(final Vec3 from, final Vec3 to, final BlockPos source) {
			super(from, to, source);
		}

		@Override
		public VoxelShape getBlockShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
			if (VSGameUtilsKt.isBlockInShipyard((Level) (level), pos)) {
				return Shapes.empty();
			}
			final VoxelShape shape = super.getBlockShape(state, level, pos);
			return shape == Shapes.block() ? shape : Shapes.empty();
		}

		@Override
		public VoxelShape getFluidShape(final FluidState state, final BlockGetter level, final BlockPos pos) {
			return Shapes.empty();
		}
	}
}
