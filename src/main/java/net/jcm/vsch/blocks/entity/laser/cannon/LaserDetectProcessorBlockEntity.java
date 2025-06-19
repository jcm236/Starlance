package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.api.laser.ILaserAttachment;
import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.IAnalogOutputBlockEntity;
import net.jcm.vsch.compat.cc.peripherals.laser.LaserDetectProcessorPeripheral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class LaserDetectProcessorBlockEntity extends AbstractLaserCannonBlockEntity implements IAnalogOutputBlockEntity {
	public LaserDetectProcessorBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.LASER_DETECT_PROCESSOR_BLOCK_ENTITY.get(), pos, state);
	}

	private volatile double distance;
	private int analogOutput;
	private final Map<String, Object> details = new HashMap<>();

	public double getDistance() {
		return this.distance;
	}

	@Override
	public int getAnalogOutput() {
		return this.analogOutput;
	}

	private void setAnalogOutput(int output) {
		if (this.analogOutput == output) {
			return;
		}
		this.analogOutput = output;
		this.setChanged();
	}

	public Map<String, Object> getDetails() {
		return this.details;
	}

	private void updateDetails(Consumer<Map<String, Object>> applier) {
		this.details.clear();
		applier.accept(this.details);
	}

	@Override
	public boolean canProcessLaser(Direction dir) {
		return dir == this.facing || dir.getOpposite() == this.facing;
	}

	@Override
	public LaserProperties processLaser(final LaserProperties props) {
		this.distance = -1;
		final DetectorAttachment first = props.getAttachments().stream()
			.filter(DetectorAttachment.class::isInstance)
			.map(DetectorAttachment.class::cast)
			.findFirst()
			.orElse(null);
		if (first == null) {
			return props.withAttachment(new DetectorAttachment(this, props));
		}
		first.blocks.add(this);
		return props;
	}

	@Override
	protected LazyOptional<?> getPeripheral() {
		return LazyOptional.of(() -> new LaserDetectProcessorPeripheral(this));
	}

	private static Map<String, Object> relativePositionToMap(final Vec3 posDiff, final Direction frontDir, final Direction upDir) {
		final Vec3 frontVec = Vec3.atLowerCornerOf(frontDir.getNormal());
		final Vec3 upVec = Vec3.atLowerCornerOf(upDir.getNormal());
		final Vec3 rightVec = frontVec.cross(upVec);

		final Map<String, Object> data = new HashMap<>(3);
		data.put("front", posDiff.dot(frontVec));
		data.put("right", posDiff.dot(rightVec));
		data.put("up", posDiff.dot(upVec));
		return data;
	}

	public static class DetectorAttachment implements ILaserAttachment {
		private static final Collector<String, ?, Map<String, Boolean>> STRING_TO_MAP_COLLECTOR = Collectors.toMap(
			Function.identity(), s -> Boolean.TRUE
		);
		private final List<LaserDetectProcessorBlockEntity> blocks = new ArrayList<>(4);
		private final Direction facing;
		private final LaserProperties initProps;
		private double traveled = 0;

		public DetectorAttachment(final LaserDetectProcessorBlockEntity be, final LaserProperties props) {
			this.blocks.add(be);
			this.facing = be.facing;
			this.initProps = props;
		}

		@Override
		public void beforeProcessLaserOnBlock(
			final LaserContext ctx,
			final BlockState oldState, final BlockPos pos,
			final ILaserProcessor processor
		) {
			if (ctx.canceled()) {
				return;
			}
			this.traveled += ctx.getTraveled();
			if (!processor.isEndPoint()) {
				return;
			}
			final LaserProperties props = ctx.getLaserOnHitProperties();
			if (props.b >= 128) {
				// Laser is too hard, cannot scan
				return;
			}
			ctx.cancel();

			if (this.initProps.r - props.r > props.r) {
				// Laser is too weak, cannot return
				return;
			}

			int infoLevel = props.g >= 128 && this.initProps.g - props.g <= props.g ? props.g * 2 / props.r : 0;
			if (infoLevel > 2 && props.b < 100) {
				infoLevel = 2;
			}

			final Level level = ctx.getLevel();
			final HitResult hitResult = ctx.getHitResult();
			final Vec3 location = hitResult.getLocation();
			int dist = (int) (this.traveled);
			for (final LaserDetectProcessorBlockEntity b : this.blocks) {
				b.distance = this.traveled;
				b.setAnalogOutput(((int) (dist)) & 0xf);
				dist /= 0x10;
			}
			final Map<String, Object> map = new HashMap<>();
			switch (infoLevel) {
			case 3:
				Vec3 origin = Vec3.atCenterOf(this.blocks.get(0).getBlockPos());
				map.put("level", level.dimension().location().toString());
				map.put("position", relativePositionToMap(
					location.subtract(origin),
					this.facing,
					this.facing.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH
				));
			case 1:
				map.put("type", hitResult.getType().toString());
			default:
				map.put("level", infoLevel);
				map.put("distance", this.traveled);
			}

			if (hitResult instanceof BlockHitResult blockHitResult) {
				final BlockState state = level.getBlockState(blockHitResult.getBlockPos());
				final Block block = state.getBlock();
				switch (infoLevel) {
				case 2:
					map.put("tags", BuiltInRegistries.BLOCK.createIntrusiveHolder(block).tags()
						.map(TagKey::location)
						.map(ResourceLocation::toString)
						.collect(STRING_TO_MAP_COLLECTOR)
					);
				case 1:
					map.put("id", BuiltInRegistries.BLOCK.getKey(block).toString());
				}
			}
			this.blocks.forEach((b) -> {
				b.updateDetails((m) -> {
					m.putAll(map);
				});
			});
		}

		@Override
		public void afterMergeLaser(final LaserContext ctx, final LaserContext target, final LaserProperties targetProps) {
			targetProps.withAttachment(this);
		}
	}
}
