package net.jcm.vsch.items.custom;

import net.jcm.vsch.config.VSCHServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MagnetBootItem extends ArmorItem {
	private static final String TAG_DISABLED = "Disabled";
	private static final String TAG_READY = "Ready";
	private static final String TAG_DIRECTION = "Direction";
	private static final double MIN_FORCE = 0.01;

	public MagnetBootItem(ArmorMaterial pMaterial, Type pType, Properties pProperties) {
		super(pMaterial, pType, pProperties);
	}

	public double getAttractDistance() {
		return VSCHServerConfig.MAGNET_BOOT_DISTANCE.get().doubleValue();
	}

	public double getMaxForce() {
		return VSCHServerConfig.MAGNET_BOOT_MAX_FORCE.get().doubleValue();
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

		Vec3 direction = new Vec3(0, -1, 0); // TODO: maybe we can change the direction to match the ship that player stands on?
		Vec3 startPos = entity.position(); // Starting position (player's position)
		Vec3 endPos = startPos.add(direction.scale(maxDistance)); // End position (straight down)

		HitResult hitResult = level.clip(new ClipContext(
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

		//mAtH
		double distance = startPos.distanceToSqr(hitResult.getLocation());
		double scaledForce = Math.min(maxDistance * maxDistance / distance * MIN_FORCE, getMaxForce());

		Vec3 force = direction.scale(scaledForce);
		tag.putDouble("Force", scaledForce);

		entity.push(force.x, force.y, force.z);

		//level.addParticle(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
	}
}
