package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.blocks.custom.laser.ScreenBlock;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;

public class ScreenBlockEntity extends BlockEntity implements ILaserProcessor, ParticleBlockEntity {
	private Vec3 color = Vec3.ZERO;
	private int lightUpdate = 0;

	public ScreenBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(VSCHBlockEntities.SCREEN_BLOCK_ENTITY.get(), pPos, pBlockState);
	}

	public Vec3 getColor() {
		return this.color;
	}

	private void setColor(Vec3 color) {
		if (this.color.equals(color)) {
			return;
		}
		this.color = color;
		this.setChanged();

		final Level level = this.getLevel();
		final BlockPos pos = this.getBlockPos();
		final BlockState state = this.getBlockState();
		final int lightLvl = Math.round(((float) (this.color.length())) * 15);
		final BlockState newState = state.setValue(ScreenBlock.LIGHT_LEVEL, lightLvl);
		level.setBlock(pos, newState, 2);
		level.sendBlockUpdated(pos, state, newState, Block.UPDATE_ALL_IMMEDIATE);
	}

	@Override
	public int getMaxLaserStrength() {
		return 256 * 3;
	}

	@Override
	public void onLaserHit(final LaserContext ctx) {
		final LaserProperties props = ctx.getLaserOnHitProperties();
		this.setColor(props.getColor());
		this.lightUpdate = 4;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		final ListTag color = data.getList("Color", Tag.TAG_DOUBLE);
		this.color = new Vec3(color.getDouble(0), color.getDouble(1), color.getDouble(2));
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		final ListTag color = new ListTag();
		color.add(DoubleTag.valueOf(this.color.x));
		color.add(DoubleTag.valueOf(this.color.y));
		color.add(DoubleTag.valueOf(this.color.z));
		data.put("Color", color);
		super.saveAdditional(data);
	}

	@Override
	public CompoundTag getUpdateTag() {
		final CompoundTag data = super.getUpdateTag();
		final ListTag color = new ListTag();
		color.add(DoubleTag.valueOf(this.color.x));
		color.add(DoubleTag.valueOf(this.color.y));
		color.add(DoubleTag.valueOf(this.color.z));
		data.put("Color", color);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.lightUpdate <= 0) {
			this.setColor(Vec3.ZERO);
			return;
		}
		this.lightUpdate--;
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		//
	}
}
