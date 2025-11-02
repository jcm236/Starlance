package net.jcm.vsch.mixin.cosmos;

import net.jcm.vsch.config.VSCHServerConfig;
import net.lointain.cosmos.procedures.PlaceplatformOnKeyPressedProcedure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3i;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlaceplatformOnKeyPressedProcedure.class)
public class MixinPlaceplatformOnKeyPressedProcedure {
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(final LevelAccessor levelAccessor, final Entity entity, final CallbackInfo ci) {
		if (!(levelAccessor instanceof final ServerLevel level)) {
			ci.cancel();
			return;
		}

		if (!VSCHServerConfig.ENABLE_PLACE_SHIP_PLATFORM.get()) {
			if (VSCHServerConfig.ENABLE_EMPTY_SPACE_CHUNK.get()) {
				ci.cancel();
			}
			return;
		}

		ci.cancel();

		if (!(entity instanceof final Player player)) {
			return;
		}

		final ItemStack mainItem = player.getMainHandItem();
		if (!(mainItem.getItem() instanceof BlockItem)) {
			return;
		}

		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);

		final Vec3 view = entity.getViewVector(0);
		final Vec3 target = entity.getEyePosition(0).add(view.scale(entity.getType().getWidth() + 1.2));

		for (final ServerShip ship : shipWorld.getAllShips().getIntersecting(
			new AABBd(target.x - 1.5, target.y - 1.5, target.z - 1.5, target.x + 1.5, target.y + 1.5, target.z + 1.5)
		)) {
			return;
		}

		final Vector3i worldCenter = new Vector3i((int) (target.x), (int) (target.y), (int) (target.z));
		final ServerShip ship = shipWorld.createNewShipAtBlock(worldCenter, false, 1.0, levelId);
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		ship.setSlug(player.getGameProfile().getName() + "-" + ship.getId());

		final UseOnContext useCtx = new UseOnContext(
			level,
			player,
			InteractionHand.MAIN_HAND,
			mainItem,
			BlockHitResult.miss(target, Direction.getNearest(view.x, view.y, view.z).getOpposite(), new BlockPos(shipCenter.x, shipCenter.y, shipCenter.z))
		);

		final InteractionResult result = mainItem.useOn(useCtx);
		if (!result.consumesAction()) {
			shipWorld.deleteShip(ship);
			return;
		}
		player.swing(InteractionHand.MAIN_HAND);

		final Vector3d position = new Vector3d(target.x, target.y, target.z);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();

		shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, levelId, 1.0));
	}
}
