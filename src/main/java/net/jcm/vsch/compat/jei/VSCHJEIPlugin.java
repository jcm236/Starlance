package net.jcm.vsch.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class VSCHJEIPlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath("vsch", "jei_plugin");
	}

	@Override
	public void registerExtraIngredients(IExtraIngredientRegistration registration) {
		registration.addExtraItemStacks(VSCHBlocks.TIER_BLOCKS.stream().map(reg -> new ItemStack(reg.get().asItem())).toList());
	}
}
