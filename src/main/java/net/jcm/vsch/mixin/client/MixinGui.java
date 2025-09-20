package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.IGuiAccessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Gui.class)
public abstract class MixinGui implements IGuiAccessor {
	@Shadow
	private Component overlayMessageString;

	@Shadow
	private int overlayMessageTime;

	@Shadow
	private boolean animateOverlayMessageColor;

	@Shadow
	public abstract void setChatDisabledByPlayerShown(boolean value);

	@Override
	public void vsch$setOverlayMessageIfNotExist(final Component component, final int duration) {
		if (this.overlayMessageString != null && this.overlayMessageTime > 21) {
			return;
		}
		this.setChatDisabledByPlayerShown(false);
		this.overlayMessageString = component;
		this.overlayMessageTime = duration;
		this.animateOverlayMessageColor = false;
	}
}
