package net.jcm.vsch.commands;

import net.jcm.vsch.event.Gravity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class StarlanceCommand {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("starlance")
			.requires((ctx) -> ctx.hasPermission(2))
			.then(Commands.literal("reloadgravity")
				.executes((ctx) -> reloadGravity(ctx.getSource()))
			)
		);
	};

	public static int reloadGravity(CommandSourceStack source) {
		try {
			Gravity.setAll(source.getServer().overworld());
		} catch (Exception e) {
			source.sendFailure(Component.literal("Couldn't execute command. See log for more info. " + e.getMessage()));
			LOGGER.error("Error when reloading gravity", e);
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Successfully reloaded gravity for all CH datapacked dimensions"), true);
		return 1;
	}
}
