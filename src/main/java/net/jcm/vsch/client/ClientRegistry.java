package net.jcm.vsch.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.client.renderer.GyroRenderer;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistry {
	@SubscribeEvent
	public static void registeringRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(VSCHBlockEntities.GYRO_BLOCK_ENTITY.get(), GyroRenderer::new);
	}
}
