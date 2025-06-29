package net.jcm.vsch.blocks;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.custom.*;
import net.jcm.vsch.blocks.entity.AirThrusterBlockEntity;
import net.jcm.vsch.blocks.entity.CreativeThrusterBlockEntity;
import net.jcm.vsch.blocks.entity.PowerfulThrusterBlockEntity;
import net.jcm.vsch.blocks.entity.ThrusterBlockEntity;
import net.jcm.vsch.fluids.VSCHFluids;
import net.jcm.vsch.items.VSCHItems;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShapes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class VSCHBlocks {
	public static final DeferredRegister<Block> BLOCKS =
		DeferredRegister.create(ForgeRegistries.BLOCKS, VSCHMod.MODID);

	public static final RegistryObject<Block> VENT_BLOCK = registerBlock(
		"vent_block",
		() -> new VentBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()
		)
	);

	public static final RegistryObject<Block> THRUSTER_BLOCK = registerBlock(
		"thruster_block",
		() -> new BaseThrusterBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion(),
			DirectionalShape.down(RotShapes.solid()),
			ThrusterBlockEntity::new
		)
	);

	public static final RegistryObject<Block> AIR_THRUSTER_BLOCK = registerBlock(
		"air_thruster_block",
		() -> new BaseThrusterBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion(),
			DirectionalShape.down(RotShapes.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0)),
			AirThrusterBlockEntity::new
		)
	);

	public static final RegistryObject<Block> POWERFUL_THRUSTER_BLOCK = registerBlock(
		"powerful_thruster_block",
		() -> new BaseThrusterBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion(),
			DirectionalShape.down(RotShapes.solid()),
			PowerfulThrusterBlockEntity::new
		)
	);

	public static final RegistryObject<Block> CREATIVE_THRUSTER_BLOCK = registerBlock(
		"creative_thruster_block",
		() -> new BaseThrusterBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion(),
			DirectionalShape.down(RotShapes.solid()),
			CreativeThrusterBlockEntity::new
		)
	);

	public static final RegistryObject<Block> DRAG_INDUCER_BLOCK = registerBlock(
		"drag_inducer_block",
		() -> new DragInducerBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()
		)
	);

	public static final RegistryObject<Block> GRAVITY_INDUCER_BLOCK = registerBlock(
		"gravity_inducer_block",
		() -> new GravityInducerBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()
		)
	);

	/*public static final RegistryObject<Block> DOCKER_BLOCK = registerBlock(
		"dock",
		() -> new DockerBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()
		)
	);*/

	public static final RegistryObject<Block> GYRO_BLOCK = registerBlock(
		"gyro",
		() -> new GyroBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.ANVIL)
				.strength(5f, 3f)
				.noOcclusion()
		)
	);

	/*public static final RegistryObject<Block> MAGNET_BLOCK = registerBlock(
		"magnet_block",
		() -> new MagnetBlock(
			BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
				.sound(SoundType.COPPER)
				.strength(5f)
				.noOcclusion()
		)
	);*/

	public static final RegistryObject<LiquidBlock> HYDROGEN_BLOCK = BLOCKS.register(
		"hydrogen",
		() -> new LiquidBlock(
			VSCHFluids.HYDROGEN.get(),
			BlockBehaviour.Properties.copy(Blocks.WATER)
		)
	);

	public static final RegistryObject<LiquidBlock> OXYGEN_BLOCK = BLOCKS.register(
		"oxygen",
		() -> new LiquidBlock(
			VSCHFluids.OXYGEN.get(),
			BlockBehaviour.Properties.copy(Blocks.WATER)
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

