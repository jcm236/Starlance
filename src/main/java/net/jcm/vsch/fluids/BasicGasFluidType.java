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

public class BasicGasFluidType extends FluidType {
	private final ResourceLocation stillTexture;
	private final int tintColor;

	public BasicGasFluidType(final ResourceLocation stillTexture, final int tintColor, final Properties properties) {
		super(properties);
		this.stillTexture = stillTexture;
		this.tintColor = tintColor;
	}

	public ResourceLocation getStillTexture() {
		return this.stillTexture;
	}

	public int getTintColor() {
		return this.tintColor;
	}

	@Override
	public void initializeClient(final Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return BasicGasFluidType.this.getStillTexture();
			}

			@Override
			public int getTintColor() {
				return BasicGasFluidType.this.getTintColor();
			}
		});
	}

	@Override
	public boolean isVaporizedOnPlacement(final Level level, final BlockPos pos, final FluidStack stack) {
		return true;
	}

	@Override
	public void onVaporize(final @Nullable Player player, final Level level, final BlockPos pos, final FluidStack stack) {
		// Otherwise it will vaporize like water
		// we can add particles and other fancy stuff here later
	}
}
