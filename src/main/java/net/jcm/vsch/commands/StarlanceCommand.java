package net.jcm.vsch.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.jcm.vsch.event.Gravity;
import net.jcm.vsch.util.ShipUtils;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class StarlanceCommand {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("starlance")
			.requires((ctx) -> ctx.hasPermission(2))
			.then(Commands.literal("reloadgravity")
				.executes((ctx) -> reloadGravity(ctx.getSource()))
			)
			// .then(Commands.literal("assemble")
			// 	.then(Commands.argument("pos", BlockPosArgument.blockPos())
			// 		.executes(
			// 			(ctx) -> assembleSingleBlock(
			// 					ctx.getSource(),
			// 					BlockPosArgument.getBlockPos(ctx, "pos")
			// 			)
			// 		)
			// 		.then(Commands.argument("dx", IntegerArgumentType.integer(0))
			// 			.then(Commands.argument("dy", IntegerArgumentType.integer(0))
			// 				.then(Commands.argument("dz", IntegerArgumentType.integer(0))
			// 					.executes(
			// 						(ctx) -> assembleBlocks(
			// 							ctx.getSource(),
			// 							BlockPosArgument.getBlockPos(ctx, "pos"),
			// 							IntegerArgumentType.getInteger(ctx, "dx"),
			// 							IntegerArgumentType.getInteger(ctx, "dy"),
			// 							IntegerArgumentType.getInteger(ctx, "dz")
			// 						)
			// 					)
			// 				)
			// 			)
			// 		)
			// 	)
			// )
		);
	}

	// private static int assembleSingleBlock(CommandSourceStack source, BlockPos pos) {
	// 	return ShipUtils.assembleBlock(source.getLevel(), pos) != null ? 1 : 0;
	// }

	// private static int assembleBlocks(CommandSourceStack source, BlockPos pos, int dx, int dy, int dz) {
	// 	return ShipUtils.assembleBlocks(source.getLevel(), pos, dx, dy, dz) != null ? 1 : 0;
	// }

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
