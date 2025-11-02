package net.jcm.vsch.mixin.cosmos;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.jcm.vsch.util.wapi.LevelData;
import net.lointain.cosmos.entity.RocketSeatEntity;
import net.lointain.cosmos.item.NickelSuitItem;
import net.lointain.cosmos.item.SteelSuitItem;
import net.lointain.cosmos.item.TitaniumSuitItem;
import net.lointain.cosmos.procedures.SpacesuitwornLogicProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;

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
	private static void execute(final LevelAccessor world, final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
		if (entity == null) {
			cir.setReturnValue(false);
			return;
		}

		if (!(entity instanceof LivingEntity livingEntity)) {
			cir.setReturnValue(false);
			return;
		}

		if (!LevelData.get((Level) (world)).isSpace()) {
			cir.setReturnValue(false);
			return;
		}

		if (livingEntity.getVehicle() instanceof RocketSeatEntity) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(isEntityWearingSpaceSuit(livingEntity));
	}

	private static boolean isEntityWearingSpaceSuit(final LivingEntity entity) {
		for (final EquipmentSlot slot : armorSlots) {
			final ItemStack stack = entity.getItemBySlot(slot);
			if (stack.isEmpty()) {
				return false;
			}
			if (!isSpaceSuitItem(stack.getItem())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isSpaceSuitItem(final Item item) {
		for (final Class<? extends Item> suitClass : validSpacesuits) {
			if (suitClass.isInstance(item)) {
				return true;
			}
		}
		return false;
	}
}
