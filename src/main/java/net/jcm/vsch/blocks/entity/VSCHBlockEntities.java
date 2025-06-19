package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.entity.laser.ScreenBlockEntity;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserDetectProcessorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserEmitterBlockEntity;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserExplosiveProcessorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserReceiverBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserCondensingLenBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserFlatMirrorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserSemiTransparentFlatMirrorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserStrengthDetectorLenBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VSCHMod.MODID);

	public static final RegistryObject<BlockEntityType<ThrusterBlockEntity>> THRUSTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"thruster_block",
		() -> BlockEntityType.Builder.of(ThrusterBlockEntity::new, VSCHBlocks.THRUSTER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<AirThrusterBlockEntity>> AIR_THRUSTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"air_thruster_block",
		() -> BlockEntityType.Builder.of(AirThrusterBlockEntity::new, VSCHBlocks.AIR_THRUSTER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<PowerfulThrusterBlockEntity>> POWERFUL_THRUSTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"powerful_thruster_block",
		() -> BlockEntityType.Builder.of(PowerfulThrusterBlockEntity::new, VSCHBlocks.POWERFUL_THRUSTER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<CreativeThrusterBlockEntity>> CREATIVE_THRUSTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"creative_thruster_block",
		() -> BlockEntityType.Builder.of(CreativeThrusterBlockEntity::new, VSCHBlocks.CREATIVE_THRUSTER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<DragInducerBlockEntity>> DRAG_INDUCER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"drag_inducer_block",
		() -> BlockEntityType.Builder.of(DragInducerBlockEntity::new, VSCHBlocks.DRAG_INDUCER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<GravityInducerBlockEntity>> GRAVITY_INDUCER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"gravity_inducer_block",
		() -> BlockEntityType.Builder.of(GravityInducerBlockEntity::new, VSCHBlocks.GRAVITY_INDUCER_BLOCK.get())
			.build(null)
	);

	/*public static final RegistryObject<BlockEntityType<DockerBlockEntity>> DOCKER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"dock",
		() -> BlockEntityType.Builder.of(DockerBlockEntity::new, VSCHBlocks.DOCKER_BLOCK.get())
			.build(null)
	);*/

	public static final RegistryObject<BlockEntityType<GyroBlockEntity>> GYRO_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"gyro",
		() -> BlockEntityType.Builder.of(GyroBlockEntity::new, VSCHBlocks.GYRO_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserDetectProcessorBlockEntity>> LASER_DETECT_PROCESSOR_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_detect_processor_block",
		() -> BlockEntityType.Builder.of(LaserDetectProcessorBlockEntity::new, VSCHBlocks.LASER_DETECT_PROCESSOR_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserEmitterBlockEntity>> LASER_EMITTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_emitter_block",
		() -> BlockEntityType.Builder.of(LaserEmitterBlockEntity::new, VSCHBlocks.LASER_EMITTER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserExplosiveProcessorBlockEntity>> LASER_EXPLOSIVE_PROCESSOR_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_explosive_processor_block",
		() -> BlockEntityType.Builder.of(LaserExplosiveProcessorBlockEntity::new, VSCHBlocks.LASER_EXPLOSIVE_PROCESSOR_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserReceiverBlockEntity>> LASER_RECEIVER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_receiver_block",
		() -> BlockEntityType.Builder.of(LaserReceiverBlockEntity::new, VSCHBlocks.LASER_RECEIVER_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserCondensingLenBlockEntity>> LASER_CONDENSING_LEN_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_condensing_len_block",
		() -> BlockEntityType.Builder.of(LaserCondensingLenBlockEntity::new, VSCHBlocks.LASER_CONDENSING_LEN_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserFlatMirrorBlockEntity>> LASER_FLAT_MIRROR_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_flat_mirror_block",
		() -> BlockEntityType.Builder.of(LaserFlatMirrorBlockEntity::new, VSCHBlocks.LASER_FLAT_MIRROR_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserSemiTransparentFlatMirrorBlockEntity>> LASER_SEMI_TRANSPARENT_FLAT_MIRROR_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_semi_transparent_flat_mirror_block",
		() -> BlockEntityType.Builder.of(LaserSemiTransparentFlatMirrorBlockEntity::new, VSCHBlocks.LASER_SEMI_TRANSPARENT_FLAT_MIRROR_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<LaserStrengthDetectorLenBlockEntity>> LASER_STRENGTH_DETECTOR_LEN_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"laser_strength_detector_len_block",
		() -> BlockEntityType.Builder.of(LaserStrengthDetectorLenBlockEntity::new, VSCHBlocks.LASER_STRENGTH_DETECTOR_LEN_BLOCK.get())
			.build(null)
	);

	public static final RegistryObject<BlockEntityType<ScreenBlockEntity>> SCREEN_BLOCK_ENTITY = BLOCK_ENTITIES.register(
		"screen_block",
		() -> BlockEntityType.Builder.of(ScreenBlockEntity::new, VSCHBlocks.SCREEN_BLOCK.get())
			.build(null)
	);

	public static void register(IEventBus eventBus) {
		BLOCK_ENTITIES.register(eventBus);
	}
}
