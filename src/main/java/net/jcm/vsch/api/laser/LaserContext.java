package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.jcm.vsch.util.SerializeUtil;

import java.util.Random;

public class LaserContext {
	private static final Random RND = new Random();

	private final LaserProperties props;
	private LaserEmitter emitter;
	private LaserEmitter lastRedirecter;
	private int redirected;
	private boolean canceled = false;
	private double maxLength = 0;
	private HitResult hit = null;
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
			double distance = this.hit.getLocation().distanceTo(this.lastRedirecter.getLocation());
			double loss = this.maxLength == -1 ? 0 : 1 - (distance / this.maxLength);
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

	public final HitResult getHitResult() {
		return this.hit;
	}

	/**
	 * onHit set the context's hit result
	 *
	 * @param hit The {@link HitResult}, must not be null
	 * @throws IllegalStateException if the HitResult is already been set
	 */
	protected final void onHit(HitResult hit) {
		if (this.hit != null) {
			throw new IllegalStateException("Laser's hit result has already been set");
		}
		this.hit = hit;
	}

	/**
	 * Prevent the laser to make any effect.
	 *
	 * @throws IllegalStateException if the HitResult is already been set
	 */
	public final void cancel() {
		if (this.hit != null) {
			throw new IllegalStateException("Cannot cancel after laser is hit.");
		}
		this.canceled = true;
	}

	public final boolean canceled() {
		return this.canceled;
	}

	public Vec3 getInputDirection() {
		return this.lastRedirecter.getDirection();
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
		final double length = 128 / Math.max(0.125, airDensity); // TODO; dynamic length based on air density
		this.maxLength = airDensity < 0.125 ? -1 : length;
		final Vec3 dest = origin.add(dir.scale((length)));

		final BlockHitResult result = level.clip(new LaserClipContext(origin, dest, null, blockPos));
		this.onHit(result);
		final BlockPos targetPos = result.getBlockPos();
		final BlockState block = level.getBlockState(targetPos);
		if (result.getType() != HitResult.Type.BLOCK) {
			return;
		}
		for (ILaserAttachment attachment : this.props.getAttachments()) {
			attachment.beforeProcessLaser(this, block, targetPos);
		}
		if (this.canceled) {
			return;
		}
		final ILaserProcessor processor;
		if (block.getBlock() instanceof ILaserProcessor proc) {
			processor = proc;
		} else if (level.getBlockEntity(targetPos) instanceof ILaserProcessor proc) {
			processor = proc;
		} else {
			processor = LaserContext::destroyBlockProcessor;
		}
		processor.onLaserHit(this);
		for (ILaserAttachment attachment : this.props.getAttachments()) {
			attachment.afterProcessLaser(this, block, targetPos);
		}
	}

	public LaserContext redirectWith(LaserProperties props, LaserEmitter newEmitter) {
		return new LaserContext(
			props,
			this.emitter, newEmitter,
			this.redirected + 1, this.tickRedirected + 1
		);
	}

	private static void destroyBlockProcessor(LaserContext laser) {
		if (!(laser.getHitResult() instanceof BlockHitResult hitResult)) {
			return;
		}
		final int strength = laser.props.r / 256;
		final double speed = laser.props.g / 256.0, accurate = laser.props.b / 256.0;
		final double lostChance = accurate / strength;
		final boolean willLost = lostChance < 1 && RND.nextDouble() >= lostChance;
		// level.destroyBlock(hitResult.getBlockPos());
		System.out.println("destorying strength: " + strength + ", speed: " + speed + ", lostChance: " + lostChance);
	}

	public CompoundTag writeToNBT(CompoundTag data) {
		this.props.writeToNBT(data);
		CompoundTag comp = new CompoundTag();
		data.put("LastRedirecter", this.lastRedirecter.writeToNBT(comp));
		data.putInt("Redirected", this.redirected);
		if (this.hit != null) {
			data.put("Hit", SerializeUtil.hitResultToNBT(this.hit));
		}
		return data;
	}

	public void readFromNBT(Level level, CompoundTag data) {
		this.props.readFromNBT(data);
		this.lastRedirecter = LaserEmitter.parseFromNBT(level, data.getCompound("LastRedirecter"));
		this.redirected = data.getInt("Redirected");
		if (data.contains("Hit")) {
			this.hit = SerializeUtil.hitResultFromNBT(level, data.getCompound("Hit"));
		}
	}

	static class LaserClipContext extends ClipContext {
		private final BlockPos source;

		protected LaserClipContext(Vec3 from, Vec3 to, Entity entity, BlockPos source) {
			super(from, to, Block.COLLIDER, Fluid.NONE, null);
			this.source = source;
		}

		@Override
		public VoxelShape getBlockShape(BlockState blockState, BlockGetter level, BlockPos pos) {
			if (this.source != null && this.source.equals(pos)) {
				return Shapes.empty();
			}
			if (!blockState.canOcclude()) {
				return blockState.getVisualShape(level, pos, CollisionContext.empty());
			}
			return blockState.getCollisionShape(level, pos, CollisionContext.empty());
		}
	}
}
