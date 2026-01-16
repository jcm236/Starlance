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
import net.jcm.vsch.items.VSCHItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VSCHTab {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VSCHMod.MODID);

	public static final RegistryObject<CreativeModeTab> TAB = REGISTRY.register("starlance",
			() -> CreativeModeTab.builder().title(Component.translatable("vsch.itemtab")).icon(() -> new ItemStack(VSCHBlocks.THRUSTER_BLOCK.get())).displayItems((parameters, tabData) -> {

				tabData.accept(VSCHBlocks.THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.AIR_THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.POWERFUL_THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.DRAG_INDUCER_BLOCK.get());
				tabData.accept(VSCHBlocks.GYRO_BLOCK.get());
				tabData.accept(VSCHBlocks.ROCKET_ASSEMBLER_BLOCK.get());

				tabData.accept(VSCHItems.MAGNET_BOOT.get());

				tabData.accept(VSCHItems.WRENCH.get());

			}).build());

	public static void register(IEventBus eventBus) {
		REGISTRY.register(eventBus);
	}
}
