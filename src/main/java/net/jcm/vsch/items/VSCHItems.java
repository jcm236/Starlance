package net.jcm.vsch.items;

import net.jcm.vsch.items.custom.MagnetBootItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.VSCHTab;
import net.minecraft.world.item.ArmorItem.Type;
import net.jcm.vsch.items.custom.WrenchItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, VSCHMod.MODID);

	public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", 
			() -> new WrenchItem(new Item.Properties()));

	public static final RegistryObject<Item> MAGNET_BOOT = ITEMS.register("magnet_boot",
			() -> new MagnetBootItem(ArmorMaterials.IRON, Type.BOOTS, new Item.Properties()));

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
