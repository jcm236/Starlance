package net.jcm.vsch.items.pipe;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.pipe.PipeNodeProvider;
import net.jcm.vsch.pipe.level.NodeLevel;
import net.jcm.vsch.items.custom.WrenchItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class PipeNodeItem<T extends PipeNode<T>> extends Item {
	private final DyeColor color;
	private String descriptionId = null;

	protected PipeNodeItem(final DyeColor color, final Item.Properties props) {
		super(props);
		this.color = color;
	}

	public DyeColor getColor() {
		return this.color;
	}

	protected abstract String getDescriptionName();

	@Override
	protected String getOrCreateDescriptionId() {
		if (this.descriptionId == null) {
			this.descriptionId = "pipe." + BuiltInRegistries.ITEM.getKey(this).getNamespace() + "." + this.getDescriptionName();
		}
		return this.descriptionId;
	}

	@Override
	public Component getDescription() {
		return Component.translatable("color.minecraft." + this.color).append(" ").append(Component.translatable(this.getDescriptionId()));
	}

	@Override
	public Component getName(final ItemStack stack) {
		return Component.translatable("color.minecraft." + this.color).append(" ").append(Component.translatable(this.getDescriptionId(stack)));
	}

	public abstract PipeNodeProvider<T> getPipeNodeProvider(final ItemStack stack);

	@Override
	public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null || context.getHand() != InteractionHand.MAIN_HAND || context.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof WrenchItem)) {
			return InteractionResult.PASS;
		}

		final Level level = context.getLevel();
		final NodeLevel nodeLevel = NodeLevel.get(level);

		final NodePos nodePos = NodePos.fromHitResult(level, context.getClickedPos(), context.getClickLocation(), 4.0 / 16);
		if (nodePos == null) {
			return InteractionResult.PASS;
		}

		final PipeNodeProvider<?> provider = this.getPipeNodeProvider(stack);
		if (provider == null) {
			return InteractionResult.FAIL;
		}
		if (nodeLevel.getNode(nodePos) != null) {
			return InteractionResult.FAIL;
		}
		final PipeNode node = provider.createNode(nodeLevel, nodePos);
		if (!node.canAnchor()) {
			return InteractionResult.FAIL;
		}
		nodeLevel.setNode(nodePos, node);
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
