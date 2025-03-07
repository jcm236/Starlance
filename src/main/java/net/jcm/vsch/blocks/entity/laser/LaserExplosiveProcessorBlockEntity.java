package net.jcm.vsch.blocks.entity.laser;

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

import net.jcm.vsch.api.laser.ILaserAttachment;
import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;

public class LaserExplosiveProcessorBlockEntity extends AbstractLaserCannonBlockEntity {
	public LaserExplosiveProcessorBlockEntity(BlockPos pos, BlockState state) {
		super("laser_explosive_processor", VSCHBlockEntities.LASER_EXPLOSIVE_PROCESSOR_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean canProcessLaser(Direction dir) {
		return dir == this.facing;
	}

	@Override
	public LaserProperties processLaser(LaserProperties props) {
		return props.withAttachment(new ExplosiveAttachment());
	}

	public class ExplosiveAttachment implements ILaserAttachment {
		@Override
		public void beforeProcessLaser(LaserContext ctx, BlockState oldState, BlockPos pos, ILaserProcessor processor) {
			if (ctx.canceled() || processor != null) {
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
	}
}
