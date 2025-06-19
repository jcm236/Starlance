package net.jcm.vsch.api.laser;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public class LaserDamageSource extends DamageSource {
	private final LaserContext laser;

	public LaserDamageSource(final Holder<DamageType> damageType, final LaserContext laser) {
		this(damageType, laser, laser.getEmitPosition());
	}

	public LaserDamageSource(final Holder<DamageType> damageType, final LaserContext laser, final Vec3 sourcePos) {
		super(damageType, sourcePos);
		this.laser = laser;
	}

	public LaserContext getLaser() {
		return this.laser;
	}
}
