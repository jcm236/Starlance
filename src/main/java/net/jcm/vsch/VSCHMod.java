package net.jcm.vsch;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.valkyrienskies.core.impl.hooks.VSEvents;

import net.jcm.vsch.blocks.DefaultLaserProcessors;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.commands.ModCommands;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.create.ponder.VSCHPonderRegistrateBlocks;
import net.jcm.vsch.compat.create.ponder.VSCHPonderRegistry;
import net.jcm.vsch.compat.create.ponder.VSCHPonderTags;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.items.VSCHItems;
import net.jcm.vsch.particle.VSCHParticles;

@Mod(VSCHMod.MODID)
public class VSCHMod {
	public static final String MODID = "vsch";
	public static final String VERSION = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();

	public VSCHMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		VSCHBlockEntities.register(modBus);
		VSCHBlocks.register(modBus);
		VSCHConfig.register(ModLoadingContext.get());
		VSCHEntities.register(modBus);
		VSCHItems.register(modBus);
		VSCHParticles.register(modBus);
		VSCHTab.register(modBus);
		VSCHTags.register();

		DefaultLaserProcessors.register();

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
			VSCHPonderRegistry.register();
			VSCHPonderTags.register();
		}
	}

	public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// event.registerEntityRenderer(VSCHEntities.MAGNET_ENTITY.get(), NoopRenderer::new);
	}
}





