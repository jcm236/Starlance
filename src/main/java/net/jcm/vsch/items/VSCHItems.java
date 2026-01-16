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
package net.jcm.vsch.items;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.VSCHTab;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.jcm.vsch.items.custom.WrenchItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VSCHMod.MODID);

	public static final RegistryObject<Item> WRENCH = ITEMS.register(
		"wrench", 
		() -> new WrenchItem(new Item.Properties())
	);

	public static final RegistryObject<Item> MAGNET_BOOT = ITEMS.register(
		"magnet_boot",
		() -> new MagnetBootItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties())
	);
	
	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
