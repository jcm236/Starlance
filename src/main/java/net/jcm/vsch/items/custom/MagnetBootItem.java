package net.jcm.vsch.items.custom;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.config.VSCHConfig;
import net.lointain.cosmos.item.SteelarmourItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class MagnetBootItem extends ArmorItem {
	private static final String TAG_DISABLED = "Disabled";
	private static final String TAG_READY = "Ready";
	private static final String TAG_DIRECTION = "Direction";
	private static final double MIN_FORCE = 0.01;

	public MagnetBootItem(ArmorMaterial pMaterial, Type pType, Properties pProperties) {
		super(pMaterial, pType, pProperties);
	}

	public double getAttractDistance() {
		return VSCHConfig.MAGNET_BOOT_DISTANCE.get().doubleValue();
	}

	public double getMaxForce() {
		return VSCHConfig.MAGNET_BOOT_MAX_FORCE.get().doubleValue();
	}

	public boolean getEnabled(ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return false;
		}
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.getBoolean(TAG_DISABLED);
	}

	public boolean getReady(ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return false;
		}
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_READY);
	}

	public Vec3 getDirection(ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return null;
		}
		CompoundTag tag = stack.getTag();
		if (tag == null) {
			return null;
		}
		return Vec3.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_DIRECTION)).result().orElse(null);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return;
		}
		if (livingEntity.getItemBySlot(this.getEquipmentSlot()) != stack) {
			return;
		}

		// Ignore no physics entities
		if (entity.noPhysics || entity.isPassenger()) {
			return;
		}
		if (entity instanceof Player player && player.getAbilities().flying) {
			return;
		}

		CompoundTag tag = stack.getOrCreateTag();
		boolean disabled = tag.getBoolean(TAG_DISABLED);
		boolean wasReady = tag.getBoolean(TAG_READY);

		double maxDistance = getAttractDistance();

		Vec3 startPos = entity.position(); // Starting position (player's feet position)
		Vec3 direction = new Vec3(0, -1, 0);
		// TODO: maybe we can change the direction to match the ship that player stands on?
		if (entity instanceof FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			startPos = frp.vsch$getFeetPosition();
			direction = frp.vsch$getDownVector();
		}

		final Vec3 endPos = startPos.add(direction.scale(maxDistance)); // End position (straight down)

		final HitResult hitResult = level.clip(new ClipContext(
			startPos,
			endPos,
			ClipContext.Block.COLLIDER, // Raycast considers block collision shapes, maybe we don't want this?
			ClipContext.Fluid.NONE,     // Ignore fluids
			entity
		));

		if (hitResult.getType() != HitResult.Type.BLOCK) {
			if (wasReady) {
				tag.putBoolean(TAG_READY, false);
				tag.remove(TAG_DIRECTION);
			}
			return;
		}
		if (!wasReady) {
			tag.putBoolean(TAG_READY, true);
			Vec3.CODEC.encodeStart(NbtOps.INSTANCE, direction).result().ifPresent(pos -> tag.put(TAG_DIRECTION, pos));
		}
		if (disabled) {
			return;
		}

		final BlockHitResult blockHit = ((BlockHitResult)(hitResult));

		// mAtH
		final double distance = startPos.distanceToSqr(hitResult.getLocation());
		final double scaledForce = Math.min(maxDistance * maxDistance / distance * MIN_FORCE, getMaxForce());

		final Vec3 force = direction.scale(scaledForce);
		tag.putDouble("Force", scaledForce);

		entity.push(force.x, force.y, force.z);

		if (entity instanceof FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			final BlockPos blockPos = blockHit.getBlockPos();
			final Direction face = blockHit.getDirection();
			final Quaternionf destRot = new Quaternionf().rotateTo(0, 1, 0, face.getStepX(), face.getStepY(), face.getStepZ());
			final Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
			if (ship != null) {
				new Quaternionf().setFromNormalized(ship.getShipToWorld()).mul(destRot, destRot);
			}
			frp.vsch$setRotation(frp.vsch$getRotation().slerp(destRot, 0.2f));
		}

		//level.addParticle(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
	}
}
