package net.jcm.vsch.mixin.minecraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootParams;

import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserDamageSource;
import net.jcm.vsch.api.laser.LaserProperties;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
	protected MixinLivingEntity() {
		super(null, null);
	}

	@Inject(
		method = "dropFromLootTable",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;create(Lnet/minecraft/world/level/storage/loot/parameters/LootContextParamSet;)Lnet/minecraft/world/level/storage/loot/LootParams;"
		)
	)
	protected void dropFromLootTable(
		final DamageSource source,
		final boolean isPlayer,
		final CallbackInfo ci,
		@Local final LootParams.Builder builder
	) {
		if (!(source instanceof LaserDamageSource laserSource)) {
			return;
		}
		final LaserContext laser = laserSource.getLaser();
		final LaserProperties props = laser.getLaserOnHitProperties();
		final float lucky = props.g / 128.0f;
		builder.withLuck(lucky);
	}
}
