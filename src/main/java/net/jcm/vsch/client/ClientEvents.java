package net.jcm.vsch.client;

// import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.items.IToggleableItem;
import net.jcm.vsch.items.custom.MagnetBootItem;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public final class ClientEvents {
	private static final Vector3f CAMERA_ROT_VEC = new Vector3f();

	// @SubscribeEvent
	// public static void onComputeCamera(final ViewportEvent.ComputeCameraAngles event) {
	// 	final Camera camera = event.getCamera();
	// 	if (!(camera.getEntity() instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
	// 		return;
	// 	}
	// 	final Quaternionf rotation = camera.rotation();
	// 	rotation.getEulerAnglesYXZ(CAMERA_ROT_VEC);
	// 	event.setPitch(CAMERA_ROT_VEC.x * Mth.RAD_TO_DEG);
	// 	if (Math.abs(Math.abs(CAMERA_ROT_VEC.x) - Mth.HALF_PI) < 0.025f) {
	// 		// Lovely gimbal lock
	// 		event.setYaw((float) (-Math.toDegrees(Math.atan2(
	// 			-2 * (rotation.x * rotation.z - rotation.y * rotation.w),
	// 			1 - 2 * (rotation.y * rotation.y + rotation.z * rotation.z)
	// 		))));
	// 		event.setRoll(0);
	// 	} else {
	// 		event.setYaw(-CAMERA_ROT_VEC.y * Mth.RAD_TO_DEG);
	// 		event.setRoll(CAMERA_ROT_VEC.z * Mth.RAD_TO_DEG);
	// 	}
	// }

	@SubscribeEvent
	public static void onClientTick(final TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		final LocalPlayer player = Minecraft.getInstance().player;
		final Inventory inventory = player == null ? null : player.getInventory();
		final int bootSlot = EquipmentSlot.FEET.getIndex(Inventory.INVENTORY_SIZE);
		while (VSCHKeyBindings.TOGGLE_MAGNET_BOOT.consumeClick()) {
			if (inventory == null) {
				continue;
			}
			if (inventory.getItem(bootSlot).getItem() instanceof MagnetBootItem && IToggleableItem.toggleSlot(player, bootSlot)) {
				continue;
			}
			IToggleableItem.toggleCuriosSlot(player, "boots", (stack) -> stack.getItem() instanceof MagnetBootItem);
		}
	}
}
