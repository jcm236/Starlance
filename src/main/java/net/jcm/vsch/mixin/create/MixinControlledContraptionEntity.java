package net.jcm.vsch.mixin.create;

import net.jcm.vsch.accessor.ControlledContraptionEntityAccessor;

import net.minecraft.core.BlockPos;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(ControlledContraptionEntity.class)
public class MixinControlledContraptionEntity implements ControlledContraptionEntityAccessor {
	@Shadow(remap = false)
	protected BlockPos controllerPos;

	@Override
	public BlockPos starlance$getControllerPos() {
		return this.controllerPos;
	}

	@Override
	public void starlance$setControllerPos(BlockPos pos) {
		this.controllerPos = pos;
	}
}
