package net.jcm.vsch.api.pipe;

import net.jcm.vsch.VSCHMod;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class CustomNodeRegistry {
	private CustomNodeRegistry() {}

	public static final ResourceKey<Registry<CustomNodeProvider>> ID = ResourceKey.createRegistryKey(new ResourceLocation(VSCHMod.MODID, "custom_node"));
	private static final DeferredRegister<CustomNodeProvider> REGISTRY = DeferredRegister.create(ID, VSCHMod.MODID);
	private static Supplier<IForgeRegistry<CustomNodeProvider>> REGISTERED_REGISTRY = null;

	public static IForgeRegistry<CustomNodeProvider> getRegistry() {
		return REGISTERED_REGISTRY.get();
	}

	public static AbstractCustomNode getNode(final ResourceLocation id, final DyeColor color) {
		final IForgeRegistry<CustomNodeProvider> registry = REGISTERED_REGISTRY.get();
		if (registry == null) {
			throw new IllegalStateException("Trying to use registry before it get registered");
		}
		final CustomNodeProvider provider = registry.getValue(id);
		if (provider == null) {
			return null;
		}
		return provider.getNode(color);
	}

	/**
	 * module private
	 */
	public static void register(final IEventBus bus) {
		REGISTERED_REGISTRY = REGISTRY.makeRegistry(RegistryBuilder::new);
		REGISTRY.register(bus);
	}
}
