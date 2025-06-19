package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.api.laser.ILaserAttachment;
import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;

public class LaserExplosiveProcessorBlockEntity extends AbstractLaserCannonBlockEntity {
	public LaserExplosiveProcessorBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSCHBlockEntities.LASER_EXPLOSIVE_PROCESSOR_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean canProcessLaser(final Direction dir) {
		return dir == this.facing;
	}

	@Override
	public LaserProperties processLaser(final LaserProperties props) {
		if (props.getAttachments().contains(ExplosiveAttachment.INSTANCE)) {
			return props;
		}
		return props.withAttachment(ExplosiveAttachment.INSTANCE);
	}

	@Override
	protected LazyOptional<?> getPeripheral() {
		return LazyOptional.empty();
	}

	public static class ExplosiveAttachment implements ILaserAttachment {
		public static final ExplosiveAttachment INSTANCE = new ExplosiveAttachment();

		@Override
		public void beforeProcessLaserOnBlock(
			final LaserContext ctx,
			final BlockState oldState, final BlockPos pos,
			final ILaserProcessor processor
		) {
			if (ctx.canceled() || !processor.isEndPoint()) {
				return;
			}
			ctx.cancel();

			final Level level = ctx.getLevel();
			final HitResult hitResult = ctx.getHitResult();
			final Vec3 location = hitResult.getLocation().subtract(ctx.getInputDirection().scale(0.01));
			final LaserProperties props = ctx.getLaserOnHitProperties();
			final Entity entity = null;
			final DamageSource source = null;
			final ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator();
			final double heat = props.r / 256.0;
			final double focus = props.g / 256.0;
			final double hard = props.b / 256.0;
			final float radius = (float) Math.min(Math.max(Math.sqrt(Math.max(hard * hard - heat * heat, 0)) * focus, 0.5), 16);
			final boolean fire = props.r >= 256;
			level.explode(entity, source, damageCalculator, location, radius, fire, Level.ExplosionInteraction.TNT);
		}

		@Override
		public void afterMergeLaser(LaserContext ctx, LaserContext target, LaserProperties props) {
			if (!props.getAttachments().contains(this)) {
				props.withAttachment(this);
			}
		}
	}
}
