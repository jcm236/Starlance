package net.jcm.vsch.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class BasicGasFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final int tintColor;

    public BasicGasFluidType(final ResourceLocation stillTexture,
                             final int tintColor, final Properties properties) {
        super(properties);
        this.stillTexture = stillTexture;
        this.tintColor = tintColor;
    }

    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public int getTintColor() {
        return tintColor;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public int getTintColor() {
                return tintColor;
            }
        });
    }

    @Override
    public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack)
    {
        return true;
    }

    @Override
    public void onVaporize(@Nullable Player player, Level level, BlockPos pos, FluidStack stack) {
        // Otherwise it will vaporize like water
        // we can add particles and other fancy stuff here later
    }
}