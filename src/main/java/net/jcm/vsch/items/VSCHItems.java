package net.jcm.vsch.items;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.fluids.VSCHFluids;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.jcm.vsch.items.custom.WrenchItem;
import net.jcm.vsch.items.pipe.OmniNodeItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VSCHItems {
	private static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, VSCHMod.MODID);
	private static final ArrayList<RegistryObject<? extends Item>> TAB_ITEMS = new ArrayList<>();

	public static final RegistryObject<Item> WRENCH = registerTabItem(
		"wrench", 
		() -> new WrenchItem(new Item.Properties())
	);

	public static final RegistryObject<Item> MAGNET_BOOT = registerTabItem(
		"magnet_boot",
		() -> new MagnetBootItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties())
	);

	// register buckets

	public static final RegistryObject<Item> HYDROGEN_BUCKET = registerTabItem(
		"hydrogen_bucket",
		() -> new BucketItem(VSCHFluids.HYDROGEN.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
	);

	public static final RegistryObject<Item> HYDROGEN_PEROXIDE_BUCKET = registerTabItem(
		"hydrogen_peroxide_bucket",
		() -> new BucketItem(VSCHFluids.HYDROGEN_PEROXIDE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
	);

	public static final RegistryObject<Item> OXYGEN_BUCKET = registerTabItem(
		"oxygen_bucket",
		() -> new BucketItem(VSCHFluids.OXYGEN.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
	);

	// register nodes

	public static final RegistryObject<Item> WHITE_OMNI_NODE = registerTabItem(
		"white_omni_node",
		() -> new OmniNodeItem(DyeColor.WHITE, new Item.Properties())
	);

	public static final RegistryObject<Item> ORANGE_OMNI_NODE = registerTabItem(
		"orange_omni_node",
		() -> new OmniNodeItem(DyeColor.ORANGE, new Item.Properties())
	);

	public static final RegistryObject<Item> MAGENTA_OMNI_NODE = registerTabItem(
		"magenta_omni_node",
		() -> new OmniNodeItem(DyeColor.MAGENTA, new Item.Properties())
	);

	public static final RegistryObject<Item> LIGHT_BLUE_OMNI_NODE = registerTabItem(
		"light_blue_omni_node",
		() -> new OmniNodeItem(DyeColor.LIGHT_BLUE, new Item.Properties())
	);

	public static final RegistryObject<Item> YELLOW_OMNI_NODE = registerTabItem(
		"yellow_omni_node",
		() -> new OmniNodeItem(DyeColor.YELLOW, new Item.Properties())
	);

	public static final RegistryObject<Item> LIME_OMNI_NODE = registerTabItem(
		"lime_omni_node",
		() -> new OmniNodeItem(DyeColor.LIME, new Item.Properties())
	);

	public static final RegistryObject<Item> PINK_OMNI_NODE = registerTabItem(
		"pink_omni_node",
		() -> new OmniNodeItem(DyeColor.PINK, new Item.Properties())
	);

	public static final RegistryObject<Item> GRAY_OMNI_NODE = registerTabItem(
		"gray_omni_node",
		() -> new OmniNodeItem(DyeColor.GRAY, new Item.Properties())
	);

	public static final RegistryObject<Item> LIGHT_GRAY_OMNI_NODE = registerTabItem(
		"light_gray_omni_node",
		() -> new OmniNodeItem(DyeColor.LIGHT_GRAY, new Item.Properties())
	);

	public static final RegistryObject<Item> CYAN_OMNI_NODE = registerTabItem(
		"cyan_omni_node",
		() -> new OmniNodeItem(DyeColor.CYAN, new Item.Properties())
	);

	public static final RegistryObject<Item> PURPLE_OMNI_NODE = registerTabItem(
		"purple_omni_node",
		() -> new OmniNodeItem(DyeColor.PURPLE, new Item.Properties())
	);

	public static final RegistryObject<Item> BLUE_OMNI_NODE = registerTabItem(
		"blue_omni_node",
		() -> new OmniNodeItem(DyeColor.BLUE, new Item.Properties())
	);

	public static final RegistryObject<Item> BROWN_OMNI_NODE = registerTabItem(
		"brown_omni_node",
		() -> new OmniNodeItem(DyeColor.BROWN, new Item.Properties())
	);

	public static final RegistryObject<Item> GREEN_OMNI_NODE = registerTabItem(
		"green_omni_node",
		() -> new OmniNodeItem(DyeColor.GREEN, new Item.Properties())
	);

	public static final RegistryObject<Item> RED_OMNI_NODE = registerTabItem(
		"red_omni_node",
		() -> new OmniNodeItem(DyeColor.RED, new Item.Properties())
	);

	public static final RegistryObject<Item> BLACK_OMNI_NODE = registerTabItem(
		"black_omni_node",
		() -> new OmniNodeItem(DyeColor.BLACK, new Item.Properties())
	);

	public static void register(final IEventBus eventBus) {
		REGISTRY.register(eventBus);
	}

	public static <I extends Item> RegistryObject<I> registerTabItem(final String name, final Supplier<I> getter) {
		final RegistryObject<I> object = REGISTRY.register(name, getter);
		TAB_ITEMS.add(object);
		return object;
	}

	public static void registerTab(final Consumer<Item> register) {
		TAB_ITEMS.stream().map(RegistryObject::get).forEach(register);
	}
}
