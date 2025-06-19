package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.jcm.vsch.util.RayCastUtil;
import net.jcm.vsch.util.SerializeUtil;

import java.util.function.Consumer;

public class LaserContext {
	public static final int MAX_LENGTH = 128; // Laser's max length in blocks per 256 RGB

	private final LaserProperties props;
	private LaserEmitter emitter;
	private LaserEmitter lastRedirecter;
	private int redirected;
	private boolean canceled = false;
	protected double traveled = -1;
	private double maxLength = 0;
	private HitResult hit = null;
	private boolean isEndPointProcessor = false;
	private LaserProperties onHitProps = null;
	int tickRedirected;

	public LaserContext() {
		this(new LaserProperties(), null, null, 0, 0);
	}

	protected LaserContext(
		LaserProperties props,
		LaserEmitter emitter, LaserEmitter lastRedirecter,
		int redirected, int tickRedirected
	) {
		this.props = props;
		this.emitter = emitter;
		this.lastRedirecter = lastRedirecter;
		this.redirected = redirected;
		this.tickRedirected = tickRedirected;
	}

	public LaserContext(LaserProperties props, LaserEmitter emitter) {
		this(props, emitter, emitter, 0, 0);
	}

	/**
	 * getLaserProperties returns the laser's original properties.
	 * If you want to process based on the properties the laser has,
	 * you usuallay should use {@link getLaserOnHitProperties} instead.
	 *
	 * @return the {@link LaserProperties} when laser is emitting
	 * @see getLaserOnHitProperties
	 * @see LaserProperties
	 */
	public LaserProperties getLaserProperties() {
		return this.props;
	}

	/**
	 * getLaserOnHitProperties returns the laser's properties after hit.
	 * it should always returns same property instance on same context.
	 *
	 * @return the {@link LaserProperties} when laser is hit
	 * @throws IllegalStateException if laser is not fired
	 * @see getLaserProperties
	 * @see LaserProperties
	 */
	public LaserProperties getLaserOnHitProperties() {
		if (this.hit == null) {
			throw new IllegalStateException("Laser not hit yet!");
		}
		if (this.onHitProps == null) {
			double loss = this.maxLength == -1 ? 0 : this.traveled / this.maxLength;
			this.onHitProps = this.props.afterLoss(loss);
		}
		return this.onHitProps;
	}

	public final Vec3 getColor() {
		return this.getLaserProperties().getColor();
	}

	public final LaserEmitter getEmitter() {
		return this.emitter;
	}

	public final LaserEmitter getLastRedirecter() {
		return this.lastRedirecter;
	}

	public final Level getLevel() {
		return this.lastRedirecter.getLevel();
	}

	public final Vec3 getEmitPosition() {
		return this.lastRedirecter.getLocation();
	}

	public final Vec3 getHitPosition() {
		return this.hit == null ? null : this.hit.getLocation();
	}

	public final boolean hasRedirected() {
		return this.redirected > 0;
	}

	public final int redirected() {
		return this.redirected;
	}

	/**
	 * @see onHit
	 */
	public final HitResult getHitResult() {
		return this.hit;
	}

	/**
	 * @see onHit
	 */
	public final double getTraveled() {
		return this.traveled;
	}

	/**
	 * onHit set the context's hit result and traveled distance
	 *
	 * @param hit The {@link HitResult}, must not be null
	 * @throws IllegalStateException if the HitResult is already been set
	 *
	 * @see getHitResult
	 * @see getTraveled
	 */
	protected void onHit(HitResult hit) {
		if (this.hit != null) {
			throw new IllegalStateException("Laser's hit result has already been set");
		}
		this.hit = hit;
		this.traveled = hit.getLocation().distanceTo(this.getEmitPosition());
	}

	public boolean shouldRenderOnHitParticles() {
		return this.isEndPointProcessor;
	}

	/**
	 * Prevent the laser to make any effect.
	 *
	 * @see canceled
	 */
	public final void cancel() {
		this.canceled = true;
	}

	/**
	 * Check if the context has been canceled.
	 *
	 * @see cancel
	 */
	public final boolean canceled() {
		return this.canceled;
	}

	public Vec3 getInputDirection() {
		return this.lastRedirecter.getDirection();
	}

	/**
	 * Check if the block can block the laser
	 * 
	 * @return {@code true} if the laser should be processed on the block, {@code false} otherwise
	 */
	public boolean canHitBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
		for (final ILaserAttachment attachment : this.props.getAttachments()) {
			final Boolean res = attachment.canPassThroughBlock(this, state, level, pos);
			if (this.canceled) {
				return true;
			}
			if (res != null) {
				return !res;
			}
		}
		final Block block = state.getBlock();
		if (block == Blocks.GLASS_PANE) {
			return false;
		}
		if (block instanceof AbstractGlassBlock && !(block instanceof StainedGlassBlock)) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the entity can block the laser.
	 *
	 * By default, entity will not block if:
	 * <ul>
	 * <li>
	 *   the entity does not have a processor, or
	 * </li>
	 * <li>
	 *   the entity is smaller than the length of laser's red component
	 * </li>
	 * </ul>
	 *
	 * @return {@code true} if the laser should be processed on the entity, {@code false} otherwise
	 * @see LaserUtil#hasEntityProcessor
	 */
	public boolean canHitEntity(final Level level, final Entity entity) {
		if (entity.isSpectator() || !entity.isAttackable()) {
			return false;
		}
		for (final ILaserAttachment attachment : this.props.getAttachments()) {
			final Boolean res = attachment.canPassThroughEntity(this, entity);
			if (this.canceled) {
				return true;
			}
			if (res != null) {
				return !res;
			}
		}
		if (!LaserUtil.hasEntityProcessor(entity)) {
			return false;
		}
		final AABB box = entity.getBoundingBox();
		final double xSize = box.getXsize(), ySize = box.getYsize(), zSize = box.getZsize();
		final double size = xSize * xSize + ySize * ySize + zSize * zSize;
		final double leng = this.props.r / (256.0 * 2);
		if (size < leng * leng) {
			return false;
		}
		return true;
	}

	/**
	 * fires the laser.
	 * If the laser hit any target, this method should invoke {@code onHit} with the {@link HitResult}.
	 */
	public void fire() {
		final Level level = this.lastRedirecter.getLevel();
		final BlockPos blockPos = this.lastRedirecter instanceof LaserEmitter.BlockLaserEmitter ble ? ble.getSourceBlock() : null;
		final Vec3 origin = this.lastRedirecter.getLocation();
		final Vec3 dir = this.lastRedirecter.getDirection();
		final double airDensity = 1; // TODO: Api.getAirDensity(level);
		final double length = MAX_LENGTH * this.props.getStrength() / Math.max(0.125, airDensity);
		this.maxLength = airDensity < 0.125 ? -1 : length;
		final Vec3 dest = origin.add(dir.scale((length)));

		final BlockHitResult blockHit = level.clip(new LaserClipContext(this, origin, dest, null, blockPos));
		final EntityHitResult entityHit = RayCastUtil.rayCastEntity(level, origin, dest, (entity) -> this.canHitEntity(level, entity));

		final HitResult result = entityHit != null && entityHit.getLocation().distanceToSqr(origin) < blockHit.getLocation().distanceToSqr(origin) ? entityHit : blockHit;

		this.onHit(result);
		if (this.canceled) {
			return;
		}

		final LaserProperties hitProps = this.getLaserOnHitProperties();

		if (result == entityHit) {
			final Entity entity = entityHit.getEntity();
			ILaserProcessor processor;
			if (entity instanceof ILaserProcessor proc) {
				processor = proc;
			} else {
				processor = LaserUtil.getDefaultEntityProcessor(this);
			}
			if (processor != null) {
				this.isEndPointProcessor = processor.isEndPoint();
			}
			if (processor == null || !processor.isEndPoint() && processor.getMaxLaserStrength() < Math.max(Math.max(hitProps.r, hitProps.g), hitProps.b)) {
				processor = LaserUtil.getDefaultEntityProcessor(this);
				if (processor == null) {
					return;
				}
			}
			for (ILaserAttachment attachment : this.props.getAttachments()) {
				attachment.beforeProcessLaserOnEntity(this, entity, processor);
			}
			if (this.canceled) {
				return;
			}
			processor.onLaserHit(this);
			for (ILaserAttachment attachment : hitProps.getAttachments()) {
				attachment.afterProcessLaserOnEntity(this, entity);
			}
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			final BlockPos targetPos = blockHit.getBlockPos();
			final BlockState block = level.getBlockState(targetPos);
			ILaserProcessor processor;
			if (block.getBlock() instanceof ILaserProcessor proc) {
				processor = proc;
			} else if (level.getBlockEntity(targetPos) instanceof ILaserProcessor proc) {
				processor = proc;
			} else {
				processor = LaserUtil.getDefaultBlockProcessor(this);
			}
			if (processor != null) {
				this.isEndPointProcessor = processor.isEndPoint();
			}
			if (processor == null || !processor.isEndPoint() && processor.getMaxLaserStrength() < Math.max(Math.max(hitProps.r, hitProps.g), hitProps.b)) {
				processor = LaserUtil.getDefaultBlockProcessor(this);
				if (processor == null) {
					return;
				}
			}
			for (ILaserAttachment attachment : this.props.getAttachments()) {
				attachment.beforeProcessLaserOnBlock(this, block, targetPos, processor);
			}
			if (this.canceled) {
				return;
			}
			processor.onLaserHit(this);
			for (ILaserAttachment attachment : hitProps.getAttachments()) {
				attachment.afterProcessLaserOnBlock(this, block, targetPos);
			}
		}
	}

	public LaserContext redirectWith(LaserProperties props, LaserEmitter newEmitter) {
		return new LaserContext(
			props,
			this.emitter, newEmitter,
			this.redirected + 1, this.tickRedirected + 1
		);
	}

	public CompoundTag writeToNBT(CompoundTag data) {
		this.props.writeToNBT(data);
		CompoundTag comp = new CompoundTag();
		data.put("LastRedirecter", this.lastRedirecter.writeToNBT(comp));
		data.putInt("Redirected", this.redirected);
		data.putBoolean("Ended", this.isEndPointProcessor);
		if (this.hit != null) {
			data.put("Hit", SerializeUtil.hitResultToNBT(this.hit));
		}
		return data;
	}

	public LaserContext readFromNBT(Level level, CompoundTag data) {
		this.props.readFromNBT(data);
		this.lastRedirecter = LaserEmitter.parseFromNBT(level, data.getCompound("LastRedirecter"));
		this.redirected = data.getInt("Redirected");
		this.isEndPointProcessor = data.getBoolean("Ended");
		if (data.contains("Hit")) {
			this.hit = SerializeUtil.hitResultFromNBT(level, data.getCompound("Hit"));
		}
		return this;
	}

	private VoxelShape getBlockCollisionShape(BlockState state, BlockGetter level, BlockPos pos) {
		final Block block = state.getBlock();

		if (!this.canHitBlock(state, level, pos)) {
			return Shapes.empty();
		}
		if (this.canceled) {
			return Shapes.block();
		}
		return state.getCollisionShape(level, pos, CollisionContext.empty());
	}

	static class LaserClipContext extends ClipContext {
		private final LaserContext laser;
		private final BlockPos source;

		protected LaserClipContext(LaserContext laser, Vec3 from, Vec3 to, Entity entity, BlockPos source) {
			super(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
			this.laser = laser;
			this.source = source;
		}

		@Override
		public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
			if (this.source != null && this.source.equals(pos)) {
				return Shapes.empty();
			}
			return this.laser.getBlockCollisionShape(state, level, pos);
		}
	}
}
