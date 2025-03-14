package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.blocks.thruster.ThrusterEngine;
import net.jcm.vsch.blocks.thruster.ThrusterEngineContext;
import net.jcm.vsch.config.VSCHConfig;

import net.lointain.cosmos.init.CosmosModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.joml.Vector3d;

public class AirThrusterBlockEntity extends AbstractThrusterBlockEntity {

	public AirThrusterBlockEntity(BlockPos pos, BlockState state) {
		super("air_thruster", VSCHBlockEntities.AIR_THRUSTER_BLOCK_ENTITY.get(), pos, state,
			new AirThrusterEngine(
				VSCHConfig.AIR_THRUSTER_ENERGY_CONSUME_RATE.get().intValue(),
				VSCHConfig.AIR_THRUSTER_STRENGTH.get().floatValue(),
				VSCHConfig.AIR_THRUSTER_MAX_WATER_CONSUME_RATE.get().intValue()
			)
		);
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
	protected void spawnParticles(Vector3d pos, Vector3d direction) {
		Vector3d speed = new Vector3d(direction).mul(-this.getCurrentPower());
		speed.mul(0.118);

		int max = 100;

		for (int i = 0; i < max; i++) {
			level.addParticle(
					this.getThrusterParticleType(),
					pos.x, pos.y, pos.z,
					speed.x, speed.y, speed.z
					);
		}
	}

	private static class AirThrusterEngine extends ThrusterEngine {
		private final int maxWaterConsumeRate;

		public AirThrusterEngine(int energyConsumeRate, float maxThrottle, int maxWaterConsumeRate) {
			super(1, energyConsumeRate, maxThrottle);
			this.maxWaterConsumeRate = maxWaterConsumeRate;
		}

		@Override
		public boolean isValidFuel(int tank, Fluid fluid) {
			return fluid == Fluids.WATER;
		}

		@Override
		public void tick(ThrusterEngineContext context) {
			super.tick(context);
			if (this.maxWaterConsumeRate == 0) {
				return;
			}
			double power = context.getPower();
			if (power == 0) {
				return;
			}
			double density = getLevelAirDensity(context.getLevel());
			if (density >= 1) {
				return;
			}
			int amount = context.getAmount();

			double waterConsumeRate = this.maxWaterConsumeRate * (1 - density);
			int needsWater = (int)(Math.ceil(waterConsumeRate * power * amount));
			int avaliableWater = context.getFluidHandler().drain(new FluidStack(Fluids.WATER, needsWater), IFluidHandler.FluidAction.SIMULATE).getAmount();
			context.setPower(avaliableWater / (waterConsumeRate * amount));
			context.addConsumer((ctx) -> {
				int water = (int)(Math.ceil(this.maxWaterConsumeRate * (1 - density) * ctx.getPower() * ctx.getAmount()));
				ctx.getFluidHandler().drain(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
			});
		}

		/**
		 * @todo use API from somewhere instead
		 * 
		 * @return the air density relative to {@code minecraft:overworld}
		 */
		private static double getLevelAirDensity(ServerLevel level) {
			if (level.dimension() == Level.OVERWORLD) {
				return 1.0;
			}
			if (level.dimension() == Level.NETHER) {
				return 1.2;
			}
			if (level.dimension() == Level.END) {
				return 0.0;
			}
			return 0.0;
		}
	}
}
