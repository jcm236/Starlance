package net.jcm.vsch.commands;

import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;

import net.jcm.vsch.event.Gravity;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class StarlanceCommand {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher_) {
		dispatcher_.register(Commands.literal("starlance").requires((player) -> {
					return player.hasPermission(2);
				}
			).then(Commands.literal("reloadgravity")
				.executes((player) -> {
						return reloadGravity(player.getSource());
					}
				)
			)
		);

	};

	public static int reloadGravity(CommandSourceStack source) {
		try {
			ServerLevel level = source.getLevel();
			Gravity.setAll(VSCHUtils.VSDimToLevel(level.getServer(), VSCHUtils.dimToVSDim("minecraft:overworld")));
		} catch (Exception e) {
			source.sendFailure(Component.literal("Couldn't execute command. See log for more info. "+e.getMessage()));
			LOGGER.error(e.getStackTrace().toString());
			return 0;
		}
		source.sendSuccess(() -> {
			return Component.literal("Successfully reloaded gravity for all CH datapacked dimensions");
		}, true);
		return 1;
	}


}

	
