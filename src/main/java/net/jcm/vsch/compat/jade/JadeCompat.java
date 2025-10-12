package net.jcm.vsch.compat.jade;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.custom.BaseThrusterBlock;
import net.jcm.vsch.blocks.custom.GyroBlock;
import net.jcm.vsch.compat.jade.componentproviders.GyroBlockComponentProvider;
import net.jcm.vsch.compat.jade.componentproviders.ThrusterBlockComponentProvider;

import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {
	@SuppressWarnings("removal")
	public static final ResourceLocation THRUSTER_BLOCK = new ResourceLocation(VSCHMod.MODID, "thruster_component_config");
	@SuppressWarnings("removal")
	public static final ResourceLocation GYRO_BLOCK = new ResourceLocation(VSCHMod.MODID, "gyro_component_config");

	@Override
	public void register(IWailaCommonRegistration registration) {
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(GyroBlockComponentProvider.INSTANCE, GyroBlock.class);
		registration.registerBlockComponent(ThrusterBlockComponentProvider.INSTANCE, BaseThrusterBlock.class);
	}
}
