package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.blocks.thruster.ThrusterEngine;
import net.jcm.vsch.blocks.thruster.ThrusterEngineContext;
import net.lointain.cosmos.init.CosmosModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector3d;

import java.util.List;

public class CreativeThrusterBlockEntity extends AbstractThrusterBlockEntity {

	private int maxThrottle = 500000;

	public CreativeThrusterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
	}

	// TODO: add creative peripheral
	@Override
	protected String getPeripheralType() {
		return "creative_thruster";
	}

	@Override
	protected ThrusterEngine createThrusterEngine() {
		return this.new CreativeThrusterEngine();
	}

	@Override
	protected ParticleOptions getThrusterParticleType() {
		return CosmosModParticleTypes.AIR_THRUST.get();
	}

	@Override
	protected ParticleOptions getThrusterSmokeParticleType() {
		return CosmosModParticleTypes.AIR_THRUST.get();
	}

	@Override
	protected double getEvaporateDistance() {
		return 0;
	}

	@Override
	protected void spawnParticles(Vector3d pos, Vector3d direction) {
		final Vector3d speed = new Vector3d(direction).mul(this.getCurrentPower());

		speed.mul(0.118);

		int amount = 100;
		for (int i = 0; i < amount; i++) {
			level.addParticle(
				this.getThrusterParticleType(),
				pos.x, pos.y, pos.z,
				speed.x, speed.y, speed.z
			);
		}
	}

	@Override
	public InteractionResult onUseWrench(UseOnContext ctx) {
		if(ctx.getPlayer().isShiftKeyDown()) {
			maxThrottle += 100000;

			// Don't want to use % since we should be able to get to exactly 500,000
			if (maxThrottle > 500000) {
				maxThrottle = 100000;
			}

			return InteractionResult.SUCCESS;
		}
		return super.onUseWrench(ctx);
	}

	@Override
	public void onFocusWithWrench(ItemStack stack, Level level, Player player) {
		if (!level.isClientSide && player.isShiftKeyDown()) {
			player.displayClientMessage(
				Component.literal("Max Throttle: ")
					.append(Component.literal(String.format("%dN", maxThrottle))),
				true
			);
		}
		super.onFocusWithWrench(stack, level, player);
 	}

	private final class CreativeThrusterEngine extends ThrusterEngine {

		public CreativeThrusterEngine() {
			super(0, 0, 0);
		}

		@Override
		public float getMaxThrottle() {
			return CreativeThrusterBlockEntity.this.maxThrottle;
		}

		@Override
		public boolean isValidFuel(int tank, Fluid fluid) {
			return false;
		}

		@Override
		public void tick(ThrusterEngineContext context) {
			super.tick(context);

			double power = context.getPower();
			//context.setPower(getMaxThrottle());
			if (power == 0) {
				return;
			}
		}

		@Override
		public void tickBurningObjects(final ThrusterEngineContext context, final List<BlockPos> thrusters, final Direction direction) {
			//
		}
	}
}
