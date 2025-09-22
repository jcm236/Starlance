package net.jcm.vsch.client;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.client.key.DoubleClickKeyMapping;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

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

	public static final DoubleClickKeyMapping UNLOCK_HEAD_ROTATION = new DoubleClickKeyMapping(
		"key." + VSCHMod.MODID + ".unlock_head_rotation",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_LEFT_ALT,
		"key.categories.movement"
	);

	public static final KeyMapping TOGGLE_MAGNET_BOOT = new KeyMapping(
		"key." + VSCHMod.MODID + ".toggle_magnet_boot",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_GRAVE_ACCENT,
		"key.categories." + VSCHMod.MODID
	);

	@SubscribeEvent
	public static void register(final RegisterKeyMappingsEvent event) {
		event.register(ROLL_COUNTER_CLOCKWISE);
		event.register(ROLL_CLOCKWISE);
		event.register(UNLOCK_HEAD_ROTATION);
		event.register(TOGGLE_MAGNET_BOOT);
	}
}
