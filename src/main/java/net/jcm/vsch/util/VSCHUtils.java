/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.compat.CompatMods;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.procedures.DistanceOrderProviderProcedure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector2i;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * The main class where all handy utility functions used by VSCH are stored.
 */
public class VSCHUtils {

	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Converts a normal dimension id string of
	 * <code>'namespace:dimension_name'</code> to a VS dimension id string
	 * <code>'minecraft:dimension:namespace:dimension_name'</code>
	 * 
	 * @param dimension The normal format dimension id string
	 * @return The converted VS dimension id string
	 * @author Brickyboy
	 */
	public static String dimToVSDim(String dimension) {
		return "minecraft:dimension:" + dimension;
	}

	/**
	 * Get {@link net.minecraft.server.level.ServerLevel ServerLevel} from a VS dimension ID.
	 * 
	 * @param dimension The dimension ID string in format registry_namespace:registry_name:dimension_namespace:dimension_name
	 * @return A {@link net.minecraft.server.level.ServerLevel ServerLevel} instance with the dimension ID given
	 */
	@SuppressWarnings("removal")
	public static ServerLevel registeryDimToLevel(final String dimension) {
		// Split 'minecraft:dimension:namespace:dimension_name' into [minecraft, dimension, namespace, dimension_name]
		final String[] parts = dimension.split(":");
		if (parts.length != 4) {
			throw new IllegalArgumentException("Unexpected dimension ID: " + dimension);
		}
		final ResourceKey levelId = ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation(parts[0], parts[1])), new ResourceLocation(parts[2], parts[3]));
		return ValkyrienSkiesMod.getCurrentServer().getLevel(levelId);
	}

	@SuppressWarnings("removal")
	public static ServerLevel dimToLevel(final String dimensionString) {
		return ValkyrienSkiesMod.getCurrentServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionString)));
	}

	/**
	 * Gets a players Cosmos variables capability.
	 * @param player The player to get the capability of.
	 * @return The player's capability, or null if it does not exists.
	 */
	public static CosmosModVariables.PlayerVariables getPlayerCap(Player player) {
		return player.getCapability(CosmosModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
	}

	/**
	 * Clamps all axis of a Vector3d between -limit and +limit (not abs).
	 * @param force the vector to clamp
	 * @param limit the limit to clamp all axis to
	 * @return clamped {@code force}
	 */
	public static Vector3d clampVector(Vector3d force, double limit) {
		// Clamp each component of the force vector within the range -limit, +limit
		force.x = Math.max(-limit, Math.min(limit, force.x));
		force.y = Math.max(-limit, Math.min(limit, force.y));
		force.z = Math.max(-limit, Math.min(limit, force.z));
		return force;
	}

	public static List<LoadedServerShip> getLoadedShipsInLevel(final ServerLevel level) {
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		final List<LoadedServerShip> loadedships = new ArrayList<>();
		for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (dimId.equals(ship.getChunkClaimDimension())) {
				loadedships.add(ship);
			}
		}
		return loadedships;
	}

	public static Component getWarningComponent() {
		return Component.translatable(VSCHMod.MODID + ".tooltip.may_need_fuel_or_energy").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
	}

	public static boolean testCuriosItems(final LivingEntity entity, final String id, final BiPredicate<ItemStack, Integer> tester) {
		if (!CompatMods.CURIOS.isLoaded()) {
			return false;
		}
		final ICuriosItemHandler curiosInv = CuriosApi.getCuriosInventory(entity).orElse(null);
		if (curiosInv == null) {
			return false;
		}
		final ICurioStacksHandler stacksHandler = curiosInv.getStacksHandler(id).orElse(null);
		if (stacksHandler == null) {
			return false;
		}
		final IDynamicStackHandler stacks = stacksHandler.getStacks();
		for (int slot = 0; slot < stacks.getSlots(); slot++) {
			final ItemStack stack = stacks.getStackInSlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (tester.test(stack, slot)) {
				return true;
			}
		}
		return false;
	}

	public static Vector2i randomPosOnSqaureRing(final RandomSource rnd, final int distance, final Vector2i dest) {
		final int x = rnd.nextInt(distance + 1);
		final int y = distance - x;
		dest.x = rnd.nextBoolean() ? x : -x;
		dest.y = rnd.nextBoolean() ? y : -y;
		return dest;
	}
}
