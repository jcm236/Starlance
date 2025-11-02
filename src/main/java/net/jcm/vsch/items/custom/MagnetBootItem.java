package net.jcm.vsch.items.custom;

import net.jcm.vsch.accessor.LivingEntityAccessor;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.curios.MagnetBootCurio;
import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.items.IToggleableItem;
import net.jcm.vsch.util.CollisionUtil;
import net.jcm.vsch.util.VSCHUtils;

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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import top.theillusivec4.curios.api.CuriosCapability;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class MagnetBootItem extends ArmorItem implements IToggleableItem {
	private static final String TAG_DISABLED = "Disabled";
	private static final String TAG_READY = "Ready";
	private static final String TAG_DIRECTION = "Direction";
	private static final String TAG_ATTACHING = "Attaching";
	private static final double MIN_FORCE = 0.01;

	public MagnetBootItem(ArmorMaterial material, Type type, Properties properties) {
		super(material, type, properties);
	}

	public double getAttractDistance() {
		return VSCHServerConfig.MAGNET_BOOT_DISTANCE.get().doubleValue();
	}

	public double getMaxForce() {
		return VSCHServerConfig.MAGNET_BOOT_MAX_FORCE.get().doubleValue();
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

	public Long getWorkingShip(final ItemStack stack) {
		if (!(stack.getItem() instanceof MagnetBootItem)) {
			return null;
		}
		final CompoundTag tag = stack.getTag();
		if (tag == null || tag.getBoolean(TAG_DISABLED) || !tag.getBoolean(TAG_READY)) {
			return null;
		}
		return tag.contains(TAG_ATTACHING) ? tag.getLong(TAG_ATTACHING) : null;
	}

	public static Long getMagnetizedShip(final ItemStack stack) {
		return stack.getItem() instanceof final MagnetBootItem boot ? boot.getWorkingShip(stack) : null;
	}

	public static boolean isMagnetized(final LivingEntity entity) {
		final ItemStack stack = entity.getItemBySlot(EquipmentSlot.FEET);
		if (!stack.isEmpty() && stack.getItem() instanceof final MagnetBootItem boot && boot.isWorking(stack)) {
			return true;
		}
		return VSCHUtils.testCuriosItems(
			entity,
			"boots",
			(stack2, slot) -> stack2.getItem() instanceof final MagnetBootItem boot && boot.isWorking(stack2)
		);
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
	public void onToggle(final Player owner, final ItemStack stack) {
		final CompoundTag tag = stack.getOrCreateTag();
		final boolean disable = !tag.getBoolean(TAG_DISABLED);
		tag.putBoolean(TAG_DISABLED, disable);
		if (owner.level().isClientSide) {
			return;
		}
		owner.displayClientMessage(
			Component.translatable(disable ? "vsch.message.magnet_boot.disabled" : "vsch.message.magnet_boot.enabled"),
			true
		);
	}

	@Override
	public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot, final boolean selected) {
		if (!(entity instanceof final LivingEntity livingEntity)) {
			return;
		}
		if (livingEntity.getItemBySlot(EquipmentSlot.FEET) != stack) {
			return;
		}
		this.onInventoryTick(stack, level, livingEntity);
	}

	public void onInventoryTick(final ItemStack stack, final Level level, final LivingEntity entity) {
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

		// final FreeRotatePlayerAccessor frp = entity instanceof final FreeRotatePlayerAccessor frp0 && frp0.vsch$isFreeRotating() ? frp0 : null;
		// final Vec3 startPos = frp != null ? frp.vsch$getFeetPosition() : entity.position();
		// final Vec3 direction = frp != null ? frp.vsch$getDownVector() : new Vec3(0, -1, 0);
		final Vec3 startPos = entity.position();
		final Vec3 direction = new Vec3(0, -1, 0);

		// final BlockPos blockPos = frp != null
		// 	? frp.vsch$findSupportingBlock((detectBox) -> {
		// 			detectBox.maxY = detectBox.minY + 0.1;
		// 			detectBox.minY -= maxDistance;
		// 		})
		// 	: CollisionUtil.findSupportingBlockNoOrientation(entity, maxDistance);
		final BlockPos blockPos = CollisionUtil.findSupportingBlockNoOrientation(entity, maxDistance);

		if (blockPos == null) {
			if (wasReady) {
				tag.putBoolean(TAG_READY, false);
				tag.remove(TAG_DIRECTION);
			}
			return;
		}

		if (!wasReady) {
			tag.putBoolean(TAG_READY, true);
			tag.put(TAG_DIRECTION, Vec3.CODEC.encodeStart(NbtOps.INSTANCE, direction).result().orElse(null));
		}
		if (disabled) {
			return;
		}

		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
		// TODO: get the accurate distance that repect block shape
		final Vector3d blockCenter = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
		if (ship != null) {
			tag.putLong(TAG_ATTACHING, ship.getId());
			ship.getShipToWorld().transformPosition(blockCenter);
		} else {
			tag.remove(TAG_ATTACHING);
		}
		final Vector3d displacment = new Vector3d(startPos.x, startPos.y, startPos.z).sub(blockCenter);
		// if (frp != null) {
		// 	frp.vsch$getBodyRotation().transformInverse(displacment);
		// }
		final double distance = Math.max(displacment.y, 0);
		if (((LivingEntityAccessor) (entity)).vsch$getTickSinceLastJump() > 30 || distance > 1.5) {
			final double scaledForce = Math.min(maxDistance * maxDistance / distance * MIN_FORCE, getMaxForce());

			final Vec3 force = direction.scale(scaledForce);
			tag.putDouble("Force", scaledForce);

			entity.push(force.x, force.y, force.z);
		} else {
			tag.putDouble("Force", 0);
		}

		// level.addParticle(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 0, 0, 0);

		// if (frp == null) {
		// 	return;
		// }
		// final Vector3d scaledDir = new Vector3d(direction.x, direction.y, direction.z).mul(distance);
		// if (ship != null) {
		// 	ship.getWorldToShip().transformDirection(scaledDir);
		// }

		// final Quaternionf rotation = frp.vsch$getBodyRotation();

		// final int[] dirCounts = new int[Direction.values().length];
		// Direction mostDir = null;
		// int mostDirCount = 0;

		// final double halfWidth = frp.vsch$getVanillaDimensions(entity.getPose()).width / 2.0 + 1e-3;
		// final Vector3d[] checkPoints = new Vector3d[]{
		// 	new Vector3d(0, 0, 0),
		// 	new Vector3d(-halfWidth, 0, -halfWidth),
		// 	new Vector3d(-halfWidth, 0, halfWidth),
		// 	new Vector3d(halfWidth, 0, -halfWidth),
		// 	new Vector3d(halfWidth, 0, halfWidth),
		// };
		// for (final Vector3d checkPoint : checkPoints) {
		// 	rotation.transform(checkPoint);
		// 	checkPoint.add(startPos.x, startPos.y, startPos.z);
		// 	if (ship != null) {
		// 		ship.getWorldToShip().transformPosition(checkPoint);
		// 	}
		// 	final Vec3 checkPos = new Vec3(checkPoint.x, checkPoint.y, checkPoint.z);
		// 	final BlockHitResult hitResult = level.clip(
		// 		new ClipContext(
		// 			checkPos,
		// 			checkPos.add(scaledDir.x, scaledDir.y, scaledDir.z),
		// 			ClipContext.Block.COLLIDER,
		// 			ClipContext.Fluid.NONE,
		// 			entity
		// 		)
		// 	);
		// 	if (hitResult.getType() != HitResult.Type.BLOCK) {
		// 		continue;
		// 	}
		// 	final Direction dir = hitResult.getDirection();
		// 	final int count = ++dirCounts[dir.ordinal()];
		// 	if (count > mostDirCount) {
		// 		mostDir = dir;
		// 		mostDirCount = count;
		// 	} else if (count == mostDirCount) {
		// 		mostDir = null;
		// 	}
		// }

		// if (mostDir != null) {
		// 	final Quaternionf destRot = mostDir.getRotation();
		// 	if (ship != null) {
		// 		destRot.premul(new Quaternionf().setFromNormalized(ship.getShipToWorld()));
		// 	}
		// 	final Vec3 feetPos = startPos;
		// 	frp.vsch$setBodyRotation(rotateTowards(rotation, destRot));
		// 	frp.vsch$setFeetPosition(feetPos.x, feetPos.y, feetPos.z);
		// }
	}

	@Override
	public ICapabilityProvider initCapabilities(final ItemStack stack, final CompoundTag nbt) {
		return new ICapabilityProvider() {
			private LazyOptional<Object> curiosCap = LazyOptional.empty();

			@Override
			public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction dir) {
				if (CompatMods.CURIOS.isLoaded() && cap == CuriosCapability.ITEM) {
					if (!this.curiosCap.isPresent()) {
						this.curiosCap = LazyOptional.of(() -> new MagnetBootCurio(MagnetBootItem.this, stack));
					}
					return this.curiosCap.cast();
				}
				return LazyOptional.empty();
			}
		};
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