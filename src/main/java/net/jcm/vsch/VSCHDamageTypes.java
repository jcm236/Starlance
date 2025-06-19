package net.jcm.vsch;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class VSCHDamageTypes {
	public static final ResourceKey<DamageType> LASER_BURN = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(VSCHMod.MODID, "laser_burn"));
}
