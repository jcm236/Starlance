package net.jcm.vsch.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.client.renderer.LaserRenderer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VSCHEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VSCHMod.MODID);

	public static final RegistryObject<EntityType<LaserEntity>> LASER_ENTITY = registerEntity(
		"laser_entity",
		() -> EntityType.Builder.<LaserEntity>of(LaserEntity::new, MobCategory.MISC)
			.sized(0, 0)
			.clientTrackingRange(256)
			.updateInterval(Integer.MAX_VALUE)
			.noSave()
	);

	public static final RegistryObject<EntityType<MagnetEntity>> MAGNET_ENTITY = registerEntity(
		"magnet_entity",
		() -> EntityType.Builder.<MagnetEntity>of(MagnetEntity::new, MobCategory.MISC).sized(0.1F, 0.1F)
	);

	private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> builder) {
		return ENTITIES.register(name, () -> builder.get().build(VSCHMod.MODID + ":" + name));
	}

	public static void register(IEventBus eventBus) {
		ENTITIES.register(eventBus);
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(LASER_ENTITY.get(), LaserRenderer.LaserEntityRenderer::new);
	}
}
