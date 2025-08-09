package net.jcm.vsch.client;

import net.jcm.vsch.VSCHMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VSCHKeyBindings {
	public static final KeyMapping ROLL_CLOCKWISE = new KeyMapping(
		"key." + VSCHMod.MODID + ".roll_clockwise",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		-1,
		"key.categories.movement"
	);

	public static final KeyMapping ROLL_COUNTER_CLOCKWISE = new KeyMapping(
		"key." + VSCHMod.MODID + ".roll_counter_clockwise",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		-1,
		"key.categories.movement"
	);

	@SubscribeEvent
	public static void register(final RegisterKeyMappingsEvent event) {
		event.register(ROLL_COUNTER_CLOCKWISE);
		event.register(ROLL_CLOCKWISE);
	}
}
