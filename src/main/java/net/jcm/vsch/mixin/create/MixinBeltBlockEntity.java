package net.jcm.vsch.mixin.create;

import net.jcm.vsch.util.assemble.IMoveable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(BeltBlockEntity.class)
public abstract class MixinBeltBlockEntity extends MixinKineticBlockEntity implements IMoveable {
	@Shadow(remap = false)
	protected BlockPos controller;

	@Shadow(remap = false)
	public abstract boolean isController();

	@Shadow(remap = false)
	public abstract BeltInventory getInventory();

	@Override
	public Object beforeMove(ServerLevel level, BlockPos origin, BlockPos target) {
		super.beforeMove(level, origin, target);
		if (this.isController()) {
			this.getInventory().getTransportedItems().clear();
		}
		return null;
	}

	@Override
	public void afterMove(ServerLevel level, BlockPos origin, BlockPos target, Object data) {
		super.afterMove(level, origin, target, null);
		if (this.controller != null && !this.controller.equals(BlockPos.ZERO) && !this.isController()) {
			this.controller = this.controller.subtract(origin).offset(target);
		}
	}
}
