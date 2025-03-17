package net.jcm.vsch.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.jcm.vsch.items.VSCHItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.custom.*;
import net.jcm.vsch.blocks.custom.laser.ScreenBlock;
import net.jcm.vsch.blocks.custom.laser.cannon.LaserCannonBlock;
import net.jcm.vsch.blocks.custom.laser.cannon.LaserDetectProcessorBlock;
import net.jcm.vsch.blocks.custom.laser.cannon.LaserEmitterBlock;
import net.jcm.vsch.blocks.custom.laser.cannon.LaserReceiverBlock;
import net.jcm.vsch.blocks.custom.laser.len.LaserFlatLenBlock;
import net.jcm.vsch.blocks.custom.laser.len.LaserStrengthDetectorLenBlock;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserDetectProcessorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.cannon.LaserExplosiveProcessorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserCondensingLenBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserFlatMirrorBlockEntity;
import net.jcm.vsch.blocks.entity.laser.len.LaserSemiTransparentFlatMirrorBlockEntity;

import java.util.function.Supplier;

public class VSCHBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VSCHMod.MODID);

	public static final RegistryObject<Block> THRUSTER_BLOCK = registerBlock("thruster_block",
		() -> new ThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
			.strength(5f)
			.noOcclusion()));

	public static final RegistryObject<Block> AIR_THRUSTER_BLOCK = registerBlock("air_thruster_block",
		() -> new AirThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
			.strength(5f)
			.noOcclusion()));

	public static final RegistryObject<Block> POWERFUL_THRUSTER_BLOCK = registerBlock("powerful_thruster_block",
		() -> new PowerfulThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
			.strength(5f)
			.noOcclusion()));

	public static final RegistryObject<Block> DRAG_INDUCER_BLOCK = registerBlock("drag_inducer_block",
		() -> new DragInducerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()));

	public static final RegistryObject<Block> GRAVITY_INDUCER_BLOCK = registerBlock("gravity_inducer_block",
		() -> new GravityInducerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
			.strength(5f)
			.noOcclusion()));

	public static final RegistryObject<Block> DOCKER_BLOCK = registerBlock("dock",
		() -> new DockerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()));

	/*public static final RegistryObject<Block> MAGNET_BLOCK = registerBlock("magnet_block",
			() -> new MagnetBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));*/

	public static final RegistryObject<Block> LASER_DETECT_PROCESSOR_BLOCK = registerBlock("laser_detect_processor_block",
		() -> new LaserDetectProcessorBlock(
			BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
				.sound(SoundType.GLASS)
				.strength(6f, 1f)
				.noOcclusion()
		)
	);

	public static final RegistryObject<Block> LASER_EMITTER_BLOCK = registerBlock("laser_emitter_block",
		() -> new LaserEmitterBlock(
			BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
				.sound(SoundType.GLASS)
				.strength(6f, 1f)
				.noOcclusion()
		)
	);

	public static final RegistryObject<Block> LASER_EXPLOSIVE_PROCESSOR_BLOCK = registerBlock("laser_explosive_processor_block",
		() -> new LaserCannonBlock<>(
			BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
				.sound(SoundType.GLASS)
				.strength(6f, 1f)
				.noOcclusion(),
			LaserExplosiveProcessorBlockEntity::new
		)
	);

	public static final RegistryObject<Block> LASER_RECEIVER_BLOCK = registerBlock("laser_receiver_block",
		() -> new LaserReceiverBlock(
			BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
				.sound(SoundType.GLASS)
				.strength(6f, 1f)
				.noOcclusion()
		)
	);

	public static final RegistryObject<Block> LASER_FLAT_MIRROR_BLOCK = registerBlock("laser_flat_mirror_block",
		() -> new LaserFlatLenBlock<>(
			BlockBehaviour.Properties.copy(Blocks.GLASS)
				.strength(1f, 0.3f)
				.noOcclusion(),
			LaserFlatMirrorBlockEntity::new
		)
	);

	public static final RegistryObject<Block> LASER_CONDENSING_LEN_BLOCK = registerBlock("laser_condensing_len_block",
		() -> new LaserFlatLenBlock<>(
			BlockBehaviour.Properties.copy(Blocks.GLASS)
				.strength(1f, 0.3f)
				.noOcclusion(),
			LaserCondensingLenBlockEntity::new
		)
	);
	public static final RegistryObject<Block> LASER_SEMI_TRANSPARENT_FLAT_MIRROR_BLOCK = registerBlock("laser_semi_transparent_flat_mirror_block",
		() -> new LaserFlatLenBlock<>(
			BlockBehaviour.Properties.copy(Blocks.GLASS)
				.strength(1f, 0.3f)
				.noOcclusion(),
			LaserSemiTransparentFlatMirrorBlockEntity::new
		)
	);


	public static final RegistryObject<Block> LASER_STRENGTH_DETECTOR_LEN_BLOCK = registerBlock("laser_strength_detector_len_block",
		() -> new LaserStrengthDetectorLenBlock(
			BlockBehaviour.Properties.copy(Blocks.GLASS)
				.strength(1f, 0.3f)
				.noOcclusion()
		)
	);


	public static final RegistryObject<Block> SCREEN_BLOCK = registerBlock("screen_block",
		() -> new ScreenBlock(
			BlockBehaviour.Properties.copy(Blocks.GLASS)
				.strength(1f, 0.3f)
				.lightLevel(ScreenBlock::getLightLevel)
				.noOcclusion()
		)
	);

	private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
		RegistryObject<T> toReturn = BLOCKS.register(name, block);
		registerBlockItem(name, toReturn);
		return toReturn;
	}

	private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
		return VSCHItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
	}

	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}
}

