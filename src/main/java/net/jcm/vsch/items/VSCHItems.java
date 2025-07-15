package net.jcm.vsch.items;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.fluid.VSCHFluids;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.jcm.vsch.items.custom.WrenchItem;
import net.jcm.vsch.items.pipe.OmniNodeItem;
import net.jcm.vsch.items.pipe.PipeNodeItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
	private static final ArrayList<RegistryObject<? extends Item>> COLORED_ITEMS = new ArrayList<>();

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

	public static final RegistryObject<PipeNodeItem> WHITE_OMNI_NODE = registerNodeItem(
		"white_omni_node",
		() -> new OmniNodeItem(DyeColor.WHITE, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> ORANGE_OMNI_NODE = registerNodeItem(
		"orange_omni_node",
		() -> new OmniNodeItem(DyeColor.ORANGE, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> MAGENTA_OMNI_NODE = registerNodeItem(
		"magenta_omni_node",
		() -> new OmniNodeItem(DyeColor.MAGENTA, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> LIGHT_BLUE_OMNI_NODE = registerNodeItem(
		"light_blue_omni_node",
		() -> new OmniNodeItem(DyeColor.LIGHT_BLUE, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> YELLOW_OMNI_NODE = registerNodeItem(
		"yellow_omni_node",
		() -> new OmniNodeItem(DyeColor.YELLOW, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> LIME_OMNI_NODE = registerNodeItem(
		"lime_omni_node",
		() -> new OmniNodeItem(DyeColor.LIME, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> PINK_OMNI_NODE = registerNodeItem(
		"pink_omni_node",
		() -> new OmniNodeItem(DyeColor.PINK, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> GRAY_OMNI_NODE = registerNodeItem(
		"gray_omni_node",
		() -> new OmniNodeItem(DyeColor.GRAY, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> LIGHT_GRAY_OMNI_NODE = registerNodeItem(
		"light_gray_omni_node",
		() -> new OmniNodeItem(DyeColor.LIGHT_GRAY, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> CYAN_OMNI_NODE = registerNodeItem(
		"cyan_omni_node",
		() -> new OmniNodeItem(DyeColor.CYAN, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> PURPLE_OMNI_NODE = registerNodeItem(
		"purple_omni_node",
		() -> new OmniNodeItem(DyeColor.PURPLE, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> BLUE_OMNI_NODE = registerNodeItem(
		"blue_omni_node",
		() -> new OmniNodeItem(DyeColor.BLUE, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> BROWN_OMNI_NODE = registerNodeItem(
		"brown_omni_node",
		() -> new OmniNodeItem(DyeColor.BROWN, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> GREEN_OMNI_NODE = registerNodeItem(
		"green_omni_node",
		() -> new OmniNodeItem(DyeColor.GREEN, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> RED_OMNI_NODE = registerNodeItem(
		"red_omni_node",
		() -> new OmniNodeItem(DyeColor.RED, new Item.Properties())
	);

	public static final RegistryObject<PipeNodeItem> BLACK_OMNI_NODE = registerNodeItem(
		"black_omni_node",
		() -> new OmniNodeItem(DyeColor.BLACK, new Item.Properties())
	);

	public static void register(final IEventBus eventBus) {
		REGISTRY.register(eventBus);
		eventBus.addListener(VSCHItems::onItemColorRegister);
	}

	public static <I extends PipeNodeItem> RegistryObject<I> registerNodeItem(final String name, final Supplier<I> getter) {
		final RegistryObject<I> object = registerTabItem(name, getter);
		COLORED_ITEMS.add(object);
		return object;
	}

	public static <I extends Item> RegistryObject<I> registerTabItem(final String name, final Supplier<I> getter) {
		final RegistryObject<I> object = REGISTRY.register(name, getter);
		TAB_ITEMS.add(object);
		return object;
	}

	public static void registerTab(final Consumer<Item> register) {
		TAB_ITEMS.stream().map(RegistryObject::get).forEach(register);
	}

	private static void onItemColorRegister(final RegisterColorHandlersEvent.Item event) {
		event.register(
			(stack, tintIndex) -> {
				if (!(stack.getItem() instanceof PipeNodeItem nodeItem)) {
					return 0xffffff;
				}
				if (tintIndex != 0) {
					return 0xffffff;
				}
				final float[] textureColor = nodeItem.getColor().getTextureDiffuseColors();
				return (int)(textureColor[0] * 255) << 16 | (int)(textureColor[1] * 255) << 8 | (int)(textureColor[2] * 255);
			},
			COLORED_ITEMS.stream().map(RegistryObject::get).toArray(Item[]::new)
		);
	}
}
