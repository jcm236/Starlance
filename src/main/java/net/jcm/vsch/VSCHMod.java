package net.jcm.vsch;

import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.commands.ModCommands;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.create.ponder.PonderRegister;
import net.jcm.vsch.compat.create.ponder.VSCHPonderPlugin;
import net.jcm.vsch.compat.create.ponder.VSCHPonderRegistrateBlocks;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.items.VSCHItems;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.valkyrienskies.core.impl.hooks.VSEvents;

@Mod(VSCHMod.MODID)
public class VSCHMod {
	public static final String MODID = "vsch";

	public VSCHMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		VSCHItems.register(modBus);
		VSCHBlocks.register(modBus);
		VSCHBlockEntities.register(modBus);
		VSCHConfig.register(ModLoadingContext.get());
		VSCHTab.register(modBus);
		VSCHEntities.register(modBus);
		VSCHTags.register();
		// Register commands (I took this code from another one of my mods, can't be bothered to make it consistent with the rest of this)
		MinecraftForge.EVENT_BUS.register(ModCommands.class);

		VSEvents.ShipLoadEvent.Companion.on((shipLoadEvent) -> {
			GravityInducer.getOrCreate(shipLoadEvent.getShip());
		});

		modBus.addListener(this::onClientSetup);
		modBus.addListener(this::registerRenderers);

		if (CompatMods.CREATE.isLoaded()) {
			VSCHPonderRegistrateBlocks.register();
		}
	}

	// Idk why but this doesn't work in VSCHEvents (prob its only a server-side event listener)
	private void onClientSetup(FMLClientSetupEvent event) {
		if (CompatMods.CREATE.isLoaded()) {
			PonderRegister.add();
		}
	}

	public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// event.registerEntityRenderer(VSCHEntities.MAGNET_ENTITY.get(), NoopRenderer::new);
	}
}





