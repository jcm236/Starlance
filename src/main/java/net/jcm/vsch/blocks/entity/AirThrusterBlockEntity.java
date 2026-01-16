/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.blocks.thruster.ThrusterEngine;
import net.jcm.vsch.blocks.thruster.ThrusterEngineContext;
import net.jcm.vsch.config.VSCHServerConfig;
import net.lointain.cosmos.init.CosmosModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.joml.Vector3d;

import java.util.List;

public class AirThrusterBlockEntity extends AbstractThrusterBlockEntity {

	public AirThrusterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.AIR_THRUSTER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected String getPeripheralType() {
		return "air_thruster";
	}

	@Override
	protected ThrusterEngine createThrusterEngine() {
		return new AirThrusterEngine(
			VSCHServerConfig.AIR_THRUSTER_ENERGY_CONSUME_RATE.get().intValue(),
			VSCHServerConfig.AIR_THRUSTER_STRENGTH.get().floatValue(),
			VSCHServerConfig.AIR_THRUSTER_MAX_WATER_CONSUME_RATE.get().intValue()
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
	protected double getEvaporateDistance() {
		return 1 * this.getCurrentPower();
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

	private final static class AirThrusterEngine extends ThrusterEngine {
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
			final double power = context.getPower();
			if (power == 0) {
				return;
			}
			final double density = getLevelAirDensity(context.getLevel());
			if (density >= 1) {
				return;
			}
			final double scale = context.getScale();
			final int amount = context.getAmount();

			final double waterConsumeRate = this.maxWaterConsumeRate * (1 - density);
			final int needsWater = (int) (Math.ceil(waterConsumeRate * power * scale * amount));
			final int avaliableWater = context.getFluidHandler().drain(new FluidStack(Fluids.WATER, needsWater), IFluidHandler.FluidAction.SIMULATE).getAmount();
			context.setPower(avaliableWater / (waterConsumeRate * amount));
			context.addConsumer((ctx) -> {
				final int water = (int) (Math.ceil(this.maxWaterConsumeRate * (1 - density) * ctx.getPower() * ctx.getScale() * ctx.getAmount()));
				ctx.getFluidHandler().drain(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
			});
		}

		@Override
		public void tickBurningObjects(final ThrusterEngineContext context, final List<BlockPos> thrusters, final Direction direction) {
			//
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
