/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch;

import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.commands.ModCommands;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.create.ponder.PonderRegister;
import net.jcm.vsch.compat.create.ponder.VSCHPonderPlugin;
import net.jcm.vsch.compat.create.ponder.VSCHPonderRegistrateBlocks;
import net.jcm.vsch.compat.create.ponder.VSCHPonderRegistry;
import net.jcm.vsch.compat.create.ponder.VSCHPonderTags;
import net.jcm.vsch.config.VSCHClientConfig;
import net.jcm.vsch.config.VSCHCommonConfig;
import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.items.VSCHItems;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.ship.VSCHForceInducedShips;

import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VSCHMod.MODID)
public class VSCHMod {
	public static final String MODID = "vsch";

	public VSCHMod(final FMLJavaModLoadingContext context) {
		final IEventBus modBus = context.getModEventBus();

		VSCHBlockEntities.register(modBus);
		VSCHBlocks.register(modBus);
		VSCHClientConfig.register(context);
		VSCHCommonConfig.register(context);
		VSCHEntities.register(modBus);
		VSCHItems.register(modBus);
		VSCHNetwork.register();
		VSCHServerConfig.register(context);
		VSCHTab.register(modBus);
		VSCHTags.register();

		// Register commands (I took this code from another one of my mods, can't be bothered to make it consistent with the rest of this)
		MinecraftForge.EVENT_BUS.register(ModCommands.class);

		modBus.addListener(this::onClientSetup);
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::registerRenderers);

		if (CompatMods.CREATE.isLoaded()) {
			VSCHPonderRegistrateBlocks.register();
		}
	}

	// Idk why but this doesn't work in VSCHEvents (prob its only a server-side event listener)
	private void onClientSetup(final FMLClientSetupEvent event) {
		if (CompatMods.CREATE.isLoaded()) {
			PonderRegister.add();
		}
	}

	private void onCommonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			this.registerAttachments();
		});
	}

	private void registerAttachments() {
		ValkyrienSkiesMod.getApi().registerAttachment(ShipLandingAttachment.class);
		ValkyrienSkiesMod.getApi().registerAttachment(VSCHForceInducedShips.class, (builder) -> {
			builder.useTransientSerializer();
			return null; // blame kotlin
		});
	}

	public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// event.registerEntityRenderer(VSCHEntities.MAGNET_ENTITY.get(), NoopRenderer::new);
	}
}
