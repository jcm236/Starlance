package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.VSCHTags;
import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.blocks.thruster.ThrusterEngine;
import net.jcm.vsch.blocks.thruster.ThrusterEngineContext;
import net.jcm.vsch.config.VSCHConfig;
import net.lointain.cosmos.init.CosmosModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public class PowerfulThrusterBlockEntity extends AbstractThrusterBlockEntity {
	private static final int HYDROGEN_SLOT = 0;
	private static final int OXYGEN_SLOT = 1;

	public PowerfulThrusterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.POWERFUL_THRUSTER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected String getPeripheralType() {
		return "powerful_thruster";
	}

	@Override
	protected ThrusterEngine createThrusterEngine() {
		return new PowerfulThrusterEngine(
			VSCHConfig.POWERFUL_THRUSTER_ENERGY_CONSUME_RATE.get().intValue(),
			VSCHConfig.POWERFUL_THRUSTER_STRENGTH.get().floatValue(),
			VSCHConfig.POWERFUL_THRUSTER_FUEL_CONSUME_RATE.get().intValue()
		);
	}

	@Override
	protected ParticleOptions getThrusterParticleType() {
		return CosmosModParticleTypes.BLUETHRUSTED.get();
	}

	@Override
	protected double getEvaporateDistance() {
		return 8 * this.getCurrentPower();
	}

	private final static class PowerfulThrusterEngine extends ThrusterEngine {
		private final int fuelConsumeRate;

		public PowerfulThrusterEngine(int energyConsumeRate, float maxThrottle, int fuelConsumeRate) {
			super(2, energyConsumeRate, maxThrottle);
			this.fuelConsumeRate = fuelConsumeRate;
		}

		@Override
		public boolean isValidFuel(int slot, Fluid fluid) {
			return switch (slot) {
			case HYDROGEN_SLOT -> fluid.is(VSCHTags.Fluids.HYDROGEN);
			case OXYGEN_SLOT -> fluid.is(VSCHTags.Fluids.OXYGEN);
			default -> throw new IllegalArgumentException("fluid slot is not in range [0, 1]");
			};
		}

		@Override
		public void tick(ThrusterEngineContext context) {
			super.tick(context);
			if (this.fuelConsumeRate == 0) {
				return;
			}
			double power = context.getPower();
			if (power == 0) {
				return;
			}
			int amount = context.getAmount();

			int needsOxygen = (int) (Math.ceil(this.fuelConsumeRate * power * amount));
			int needsHydrogen = needsOxygen * 2;
			IFluidHandler fluidHandler = context.getFluidHandler();
			FluidStack oxygenStack = fluidHandler.getFluidInTank(OXYGEN_SLOT);
			FluidStack hydrogenStack = fluidHandler.getFluidInTank(HYDROGEN_SLOT);
			int avaliableHydrogen = Math.min(needsHydrogen, hydrogenStack.getAmount());
			int avaliableOxygen = Math.min(avaliableHydrogen / 2, Math.min(needsOxygen, oxygenStack.getAmount()));
			if (avaliableOxygen == 0) {
				context.setPower(0);
				return;
			}
			context.setPower(avaliableOxygen / ((double) (this.fuelConsumeRate) * amount));
			context.addConsumer((ctx) -> {
				IFluidHandler tanks = ctx.getFluidHandler();
				int oxygen = (int) (Math.ceil(this.fuelConsumeRate * ctx.getPower() * ctx.getAmount()));
				int hydrogen = oxygen * 2;
				tanks.drain(new FluidStack(oxygenStack.getFluid(), oxygen), IFluidHandler.FluidAction.EXECUTE);
				tanks.drain(new FluidStack(hydrogenStack.getFluid(), hydrogen), IFluidHandler.FluidAction.EXECUTE);
			});
		}

		@Override
		public void tickBurningObjects(final ThrusterEngineContext context, final List<BlockPos> thrusters, final Direction direction) {
			simpleTickBurningObjects(context, thrusters, direction, 8, 5, 0.3);
		}
	}
}
