package net.jcm.vsch.mixin.valkyrienskies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.config.VSCHConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

@Mixin(ShipAssemblyKt.class)
public class MixinShipAssemblyKt {
	@Unique
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	// Goofy ahhh temporary fix but it'll atleast help out the kids who don't know not to do this
	@Inject(method = "createNewShipWithBlocks", at = @At("HEAD"), remap = false, cancellable = true)
	private static void createNewShipWithBlocks(BlockPos centerBlock, DenseBlockPosSet blocks, ServerLevel level, CallbackInfoReturnable<ServerShip> cir) {
		// If block is higher than overworld height
		if (centerBlock.getY() > VSGameUtilsKt.getYRange(level).getMaxY()) {
			if (VSCHConfig.CANCEL_ASSEMBLY.get()) {
				level.getServer().getPlayerList().broadcastSystemMessage(
					Component.literal("Starlance: Multi-block assembly above world height, cancelling. Instead, use ship creator stick, or assemble in another dimension. You can override this behavior in config, but its not recommended.").withStyle(ChatFormatting.RED), false);
				LOGGER.warn("Starlance cancelled multi-block assembly above overworld build height. You can override this behavior in config, but its not recommended.");
				cir.cancel();
			} else {
				LOGGER.warn("Multi-block assembly above build height NOT cancelled by starlance: be warned");
			}
		}
	}
}








