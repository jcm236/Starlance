package net.jcm.vsch.entity.player;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.accessor.EntityAccessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

public class MultiPartPlayer extends PartEntity<Player> {
	private final EntityDimensions size;
	private final boolean isFeet;

	public MultiPartPlayer(final Player parent, final float size, final boolean isFeet) {
		super(parent);
		this.size = EntityDimensions.scalable(size, size);
		this.isFeet = isFeet;
		this.refreshDimensions();
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(final CompoundTag data) {}

	@Override
	protected void addAdditionalSaveData(final CompoundTag data) {}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isAlive() {
		return ((FreeRotatePlayerAccessor)(this.getParent())).vsch$isFreeRotating();
	}

	@Override
	public boolean isSpectator() {
		return !this.isAlive();
	}

	@Override
	public boolean onGround() {
		return this.isFeet && super.onGround();
	}

	@Override
	protected void onInsideBlock(final BlockState block) {
		((EntityAccessor)(this.getParent())).vsch$onInsideBlock(block);
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return this.isAlive() && this.getParent().isPickable();
	}

	@Override
	public ItemStack getPickResult() {
		return this.isAlive() ? this.getParent().getPickResult() : null;
	}

	@Override
	public boolean isShiftKeyDown() {
		return this.getParent().isShiftKeyDown();
	}

	@Override
	public boolean isInvulnerableTo(final DamageSource source) {
		return !this.isAlive() || this.getParent().isInvulnerableTo(source);
	}

	@Override
	public boolean hurt(final DamageSource source, final float damage) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}
		return this.getParent().hurt(source, damage);
	}

	@Override
	public boolean is(final Entity other) {
		return this == other || this.getParent().is(other);
	}

	@Override
	public Entity getVehicle() {
		return this.getParent().getVehicle();
	}

	@Override
	public Entity getRootVehicle() {
		return this.getParent().getRootVehicle();
	}

	@Override
	public EntityDimensions getDimensions(final Pose pose) {
		return this.size;
	}

	@Override
	public Vec3 getDeltaMovement() {
		return this.getParent().getDeltaMovement();
	}

	@Override
	public void setDeltaMovement(final Vec3 vel) {
		if (this.isAlive()) {
			this.getParent().setDeltaMovement(vel);
		}
	}

	@Override
	public void addDeltaMovement(final Vec3 vel) {
		if (this.isAlive()) {
			this.getParent().addDeltaMovement(vel);
		}
	}

	@Override
	public float maxUpStep() {
		return this.getParent().maxUpStep();
	}

	@Override
	public void baseTick() {
		this.firstTick = false;
	}
}
