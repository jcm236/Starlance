package net.jcm.vsch.api.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class CustomNodeRegistry {
	private CustomNodeRegistry() {}

	public static final ResourceKey<Registry<PipeNodeProvider<? extends AbstractCustomNode>>> ID = ResourceKey.createRegistryKey(new ResourceLocation(VSCHMod.MODID, "custom_node"));
	private static final DeferredRegister<PipeNodeProvider<? extends AbstractCustomNode>> REGISTRY = DeferredRegister.create(ID, VSCHMod.MODID);
	private static Supplier<IForgeRegistry<PipeNodeProvider<? extends AbstractCustomNode>>> REGISTERED_REGISTRY = null;

	public static IForgeRegistry<PipeNodeProvider<? extends AbstractCustomNode>> getRegistry() {
		return REGISTERED_REGISTRY.get();
	}

	public static AbstractCustomNode createNode(final ResourceLocation id, final NodeLevel level, final NodePos pos) {
		final IForgeRegistry<PipeNodeProvider<? extends AbstractCustomNode>> registry = REGISTERED_REGISTRY.get();
		if (registry == null) {
			throw new IllegalStateException("Trying to use registry before it get registered");
		}
		final PipeNodeProvider<? extends AbstractCustomNode> provider = registry.getValue(id);
		if (provider == null) {
			return null;
		}
		return provider.createNode(level, pos);
	}

	/**
	 * module private
	 */
	public static void register(final IEventBus bus) {
		REGISTERED_REGISTRY = REGISTRY.makeRegistry(RegistryBuilder::new);
		REGISTRY.register(bus);
	}
}
