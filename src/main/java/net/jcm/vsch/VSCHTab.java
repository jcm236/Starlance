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
	private static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VSCHMod.MODID);

	public static final RegistryObject<CreativeModeTab> TAB = REGISTRY.register(
		VSCHMod.MODID,
		() -> CreativeModeTab.builder()
			.title(Component.translatable("vsch.itemtab"))
			.icon(() -> new ItemStack(VSCHBlocks.THRUSTER_BLOCK.get()))
			.displayItems((parameters, tabData) -> {
				tabData.accept(VSCHItems.WRENCH.get());

				tabData.accept(VSCHBlocks.THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.AIR_THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.POWERFUL_THRUSTER_BLOCK.get());
				tabData.accept(VSCHBlocks.DRAG_INDUCER_BLOCK.get());
				tabData.accept(VSCHBlocks.ROCKET_ASSEMBLER_BLOCK.get());

				tabData.accept(VSCHItems.MAGNET_BOOT.get());

				tabData.accept(VSCHItems.HYDROGEN_BUCKET.get());
				tabData.accept(VSCHItems.HYDROGEN_PEROXIDE_BUCKET.get());
				tabData.accept(VSCHItems.OXYGEN_BUCKET.get());
			})
			.build()
	);

	public static void register(IEventBus eventBus) {
		REGISTRY.register(eventBus);
	}
}
