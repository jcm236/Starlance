package net.jcm.vsch.items.custom;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.items.IToggleableItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class MagnetBootItem extends ArmorItem implements IToggleableItem {
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

	public boolean getEnabled(final ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return false;
		}
		final CompoundTag tag = stack.getTag();
		return tag == null || !tag.getBoolean(TAG_DISABLED);
	}

	public boolean getReady(final ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return false;
		}
		final CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_READY);
	}

	public boolean isWorking(final ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return false;
		}
		final CompoundTag tag = stack.getTag();
		return tag != null && !tag.getBoolean(TAG_DISABLED) && tag.getBoolean(TAG_READY);
	}

	public static boolean isMagnetized(final LivingEntity entity) {
		final ItemStack stack = entity.getItemBySlot(EquipmentSlot.FEET);
		return !stack.isEmpty() && stack.getItem() instanceof final MagnetBootItem boot && boot.isWorking(stack);
	}

	public Vec3 getDirection(final ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return null;
		}
		final CompoundTag tag = stack.getTag();
		if (tag == null) {
			return null;
		}
		return Vec3.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_DIRECTION)).result().orElse(null);
	}

	@Override
	public void onToggle(final Player owner, final int slot, final ItemStack stack) {
		final CompoundTag tag = stack.getOrCreateTag();
		final boolean disable = !tag.getBoolean(TAG_DISABLED);
		tag.putBoolean(TAG_DISABLED, disable);
		if (owner.level().isClientSide) {
			return;
		}
		owner.displayClientMessage(
			Component.translatable(disable ? "vsch.message.magnet_boot.disabled" :  "vsch.message.magnet_boot.enabled"),
			true
		);
	}

	@Override
	public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot, final boolean selected) {
		if (!(entity instanceof final LivingEntity livingEntity)) {
			return;
		}
		if (livingEntity.getItemBySlot(this.getEquipmentSlot()) != stack) {
			return;
		}

		// Ignore no physics entities
		if (entity.noPhysics || entity.isPassenger()) {
			return;
		}
		if (entity instanceof final Player player && player.getAbilities().flying) {
			return;
		}

		final CompoundTag tag = stack.getOrCreateTag();
		final boolean disabled = tag.getBoolean(TAG_DISABLED);
		final boolean wasReady = tag.getBoolean(TAG_READY);

		final double maxDistance = this.getAttractDistance();

		Vec3 startPos = entity.position(); // Starting position (player's feet position)
		Vec3 direction = new Vec3(0, -1, 0);
		if (entity instanceof FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			startPos = frp.vsch$getFeetPosition();
			direction = frp.vsch$getDownVector();
		}

		final Vec3 endPos = startPos.add(direction.scale(maxDistance)); // End position (straight down)

		final HitResult hitResult = level.clip(new ClipContext(
			startPos,
			endPos,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
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
			final Quaternionf destRot = blockHit.getDirection().getRotation();
			final Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
			if (ship != null) {
				destRot.premul(new Quaternionf().setFromNormalized(ship.getShipToWorld()));
			}
			final Quaternionf rotation = frp.vsch$getBodyRotation();
			frp.vsch$setBodyRotation(rotateTowards(rotation, destRot));
		}

		//level.addParticle(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
	}

	private static Quaternionf rotateTowards(final Quaternionf current, final Quaternionfc target) {
		final Quaternionf proj = projectedRotation(current, target);
		if (proj.conjugate(new Quaternionf()).mul(current).angle() < 0.025f) {
			return current.set(proj);
		}
		return current.slerp(proj, 0.15f);
	}

	private static Quaternionf projectedRotation(final Quaternionfc current, final Quaternionfc target) {
		final Vector3f up = target.transform(new Vector3f(0, 1, 0));
		final Vector3f forward = current.transform(new Vector3f(0, 0, -1));
		final Vector3f forwardProj = forward.fma(-forward.dot(up), up, new Vector3f());
		if (forwardProj.lengthSquared() < 1e-6) {
			forwardProj.set(up.x, up.y, 0);
		}
		forwardProj.normalize();
		final Vector3f right = forwardProj.cross(up, new Vector3f()).normalize();
		return new Quaternionf().setFromNormalized(new Matrix3f(right, up, forwardProj.negate()));
	}
}
