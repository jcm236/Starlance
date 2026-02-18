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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;

public class ThrusterBlockEntity extends AbstractThrusterBlockEntity {

	public ThrusterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.THRUSTER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected String getPeripheralType() {
		return "thruster";
	}

	@Override
	protected ThrusterEngine createThrusterEngine() {
		return new NormalThrusterEngine(
			VSCHServerConfig.THRUSTER_ENERGY_CONSUME_RATE.get().intValue(),
			VSCHServerConfig.THRUSTER_STRENGTH.get().intValue(),
			VSCHServerConfig.getThrusterFuelConsumeRates()
		);
	}

	@Override
	protected double getEvaporateDistance() {
		return 8 * this.getCurrentPower();
	}

	private final static class NormalThrusterEngine extends ThrusterEngine {
		private final Map<String, Integer> fuelConsumeRates;

		public NormalThrusterEngine(int energyConsumeRate, float maxThrottle, Map<String, Integer> fuelConsumeRates) {
			super(1, energyConsumeRate, maxThrottle);
			this.fuelConsumeRates = fuelConsumeRates;
		}

		@Override
		public boolean isValidFuel(int tank, Fluid fluid) {
			String fluidName = BuiltInRegistries.FLUID.getKey(fluid).toString();
			return this.fuelConsumeRates.containsKey(fluidName);
		}

		@Override
		public void tick(ThrusterEngineContext context) {
			super.tick(context);
			if (this.fuelConsumeRates.size() == 0) {
				return;
			}
			final double power = context.getPower();
			if (power == 0) {
				return;
			}
			final Fluid fluid = context.getFluidHandler().getFluidInTank(0).getFluid();
			if (fluid == Fluids.EMPTY) {
				context.setPower(0);
				return;
			}
			final String fluidName = BuiltInRegistries.FLUID.getKey(fluid).toString();
			final int consumeRate = this.fuelConsumeRates.get(fluidName);
			if (consumeRate == 0) {
				return;
			}
			final double scale = context.getScale();
			final int amount = context.getAmount();

			final int needsFuel = (int) (Math.ceil(consumeRate * power * scale * amount));
			final int avaliableFuel = context.getFluidHandler().drain(new FluidStack(fluid, needsFuel), IFluidHandler.FluidAction.SIMULATE).getAmount();
			context.setPower((double) (avaliableFuel) / (consumeRate * amount));
			context.addConsumer((ctx) -> {
				ctx.getFluidHandler().drain(new FluidStack(fluid, (int)(Math.ceil(consumeRate * ctx.getPower() * ctx.getScale() * ctx.getAmount()))), IFluidHandler.FluidAction.EXECUTE);
			});
		}

		@Override
		public void tickBurningObjects(final ThrusterEngineContext context, final List<BlockPos> thrusters, final Direction direction) {
			simpleTickBurningObjects(context, thrusters, direction, 8, 3, 0.1);
		}
	}
}
