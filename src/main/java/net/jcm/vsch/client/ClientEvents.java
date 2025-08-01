package net.jcm.vsch.client;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

@Mod.EventBusSubscriber
public final class ClientEvents {
	@SubscribeEvent
	public static void onComputeCamera(final ViewportEvent.ComputeCameraAngles event) {
		final Camera camera = event.getCamera();
		if (camera.getEntity() instanceof FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			event.setRoll(camera.rotation().getEulerAnglesYXZ(new Vector3f()).z * Mth.RAD_TO_DEG);
			event.setPitch(event.getPitch() + frp.vsch$getHeadPitch());
		}
	}
}
