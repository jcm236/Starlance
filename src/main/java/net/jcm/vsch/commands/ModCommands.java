package net.jcm.vsch.commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModCommands {
	@SubscribeEvent
	public static void onRegisterCommands(final RegisterCommandsEvent event) {
		StarlanceCommand.register(event.getDispatcher());
	}
}
