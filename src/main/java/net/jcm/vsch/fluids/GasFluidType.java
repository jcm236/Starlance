package net.jcm.vsch.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class GasFluidType extends FluidType {
	private final ResourceLocation stillTexture;
	private final ResourceLocation flowTexture;
	private final int tintColor;

	public GasFluidType(final ResourceLocation stillTexture, final ResourceLocation flowTexture, final int tintColor, final Properties properties) {
		super(properties);
		this.stillTexture = stillTexture;
		this.flowTexture = flowTexture;
		this.tintColor = tintColor;
	}

	@Override
	public void initializeClient(final Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return GasFluidType.this.stillTexture;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return GasFluidType.this.flowTexture;
			}

			@Override
			public int getTintColor() {
				return GasFluidType.this.tintColor;
			}
		});
	}

	@Override
	public boolean isVaporizedOnPlacement(final Level level, final BlockPos pos, final FluidStack stack) {
		final int temperature = level.dimensionType().ultraWarm() ? 1000 : 273;
		return temperature > this.getTemperature();
	}

	@Override
	public void onVaporize(final @Nullable Player player, final Level level, final BlockPos pos, final FluidStack stack) {
		// Otherwise it will vaporize like water
		// we can add particles and other fancy stuff here later
	}
}
