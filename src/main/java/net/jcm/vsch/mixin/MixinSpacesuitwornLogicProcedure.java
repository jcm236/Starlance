package net.jcm.vsch.mixin;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.items.VSCHItems;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.lointain.cosmos.entity.RocketSeatEntity;
import net.lointain.cosmos.init.CosmosModItems;
import net.lointain.cosmos.item.NickelSuitItem;
import net.lointain.cosmos.item.SteelSuitItem;
import net.lointain.cosmos.item.TitaniumSuitItem;
import net.lointain.cosmos.network.CosmosModVariables.WorldVariables;
import net.lointain.cosmos.procedures.SpacesuitwornLogicProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@Mixin(SpacesuitwornLogicProcedure.class)
public class MixinSpacesuitwornLogicProcedure {

	private static final Logger logger = LogManager.getLogger(VSCHMod.MODID);
	private static final Collection<EquipmentSlot> armorSlots = new ArrayList<>();
	private static final Collection<Class<? extends Item>> validSpacesuits = new ArrayList<>();

	static {
		armorSlots.add(EquipmentSlot.HEAD);
		armorSlots.add(EquipmentSlot.CHEST);
		armorSlots.add(EquipmentSlot.LEGS);
		armorSlots.add(EquipmentSlot.FEET);

		validSpacesuits.add(SteelSuitItem.class);
		validSpacesuits.add(TitaniumSuitItem.class);
		validSpacesuits.add(NickelSuitItem.class);
		validSpacesuits.add(MagnetBootItem.class);
	}

	@Inject(method = "execute", remap = false, at = @At("HEAD"), cancellable = true)
	private static void execute(LevelAccessor world, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity == null) {
			cir.setReturnValue(false);
			return;
		}

		if (!(entity instanceof LivingEntity livingEntity)) {
			cir.setReturnValue(false);
			return;
		}

		String dimension = entity.level().dimension().location().toString();
		String dimensionType = WorldVariables.get(world).dimension_type.getString(dimension);
		if (!dimensionType.equals("space")) {
			cir.setReturnValue(false);
			return;
		}

		if (livingEntity.getVehicle() instanceof RocketSeatEntity) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(isEntityWearingSpaceSuit(livingEntity));
	}

	private static boolean isEntityWearingSpaceSuit(LivingEntity entity) {
		for (EquipmentSlot slot : armorSlots) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.isEmpty()) {
				return false;
			}
			if (!isSpaceSuitItem(stack.getItem())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isSpaceSuitItem(Item item) {
		for (Class<? extends Item> suitClass : validSpacesuits) {
			if (suitClass.isInstance(item)) {
				return true;
			}
		}
		return false;
	}
}
