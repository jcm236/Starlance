package net.jcm.vsch.items.pipe;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeLevel;
import net.jcm.vsch.items.custom.WrenchItem;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public abstract class PipeNodeItem extends Item {
	private final DyeColor color;

	protected PipeNodeItem(final DyeColor color, final Item.Properties props) {
		super(props);
		this.color = color;
	}

	public DyeColor getColor() {
		return this.color;
	}

	public abstract PipeNode getPipeNode(final ItemStack stack);

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
			return super.useOn(context);
		}
		if (!(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof WrenchItem)) {
			return super.useOn(context);
		}

		final Level level = context.getLevel();
		final NodeLevel nodeLevel = NodeLevel.get(level);

		final LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
		Vec3 pos = context.getClickLocation();
		if (ship != null) {
			final Vector3d worldPos = ship.getTransform().getWorldToShip().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
			pos = new Vec3(worldPos.x, worldPos.y, worldPos.z);
		}
		final NodePos nodePos = NodePos.fromVec3(pos, 4.0 / 16);
		if (nodePos == null) {
			return super.useOn(context);
		}

		final ItemStack stack = context.getItemInHand();
		final PipeNode node = this.getPipeNode(stack);
		if (node == null) {
			return InteractionResult.FAIL;
		}
		if (nodeLevel.getNode(nodePos) != null) {
			return InteractionResult.FAIL;
		}
		nodeLevel.setNode(nodePos, node);
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
