package net.jcm.vsch.api.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.pipe.OmniNode;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public abstract class PipeNode<T extends PipeNode<T>> {
	private final DyeColor color;
	private final Type type;

	protected PipeNode(final DyeColor color, final Type type) {
		this.color = color;
		this.type = type;
	}

	public final DyeColor getColor() {
		return this.color;
	}

	public final Type getType() {
		return this.type;
	}

	public abstract T withColor(DyeColor color);

	public abstract ItemStack asItemStack();

	public abstract boolean canConnect(NodeLevel level, NodePos pos, Direction dir);

	public abstract boolean canFluidFlow(NodeLevel level, NodePos pos, Direction dir, Fluid fluid);

	public final void writeTo(final FriendlyByteBuf buf) {
		buf.writeByte((this.type.getCode() << 4) & this.color.getId());
		if (this.type == Type.CUSTOM) {
			buf.writeResourceLocation(((AbstractCustomNode) (this)).getId());
		}
		this.writeAdditional(buf);
	}

	public abstract void writeAdditional(FriendlyByteBuf buf);

	public static PipeNode readFrom(final FriendlyByteBuf buf) {
		final int typeAndColor = buf.readByte();
		final DyeColor color = DyeColor.byId(typeAndColor & 0xf);
		final int typeCode = (typeAndColor >> 4) & 0xf;
		final Type type = Type.CODE_MAP[typeCode];
		return switch (type) {
			case OMNI -> OmniNode.getByColor(color);
			case CUSTOM -> {
				final ResourceLocation id = buf.readResourceLocation();
				final AbstractCustomNode node = CustomNodeRegistry.getNode(id, color);
				if (node == null) {
					// TODO: find a safe way to remove the node instead
					throw new RuntimeException("Starlance: pipe node with ID " + id + " is not found");
				}
				node.readAdditional(buf);
				yield node;
			}
			default -> throw new IllegalArgumentException("Unknown node type: " + typeCode);
		};
	}

	public enum Type {
		// Can connect, push, or pull for all directions
		OMNI(0x0),
		// Can only connect on two opposite directions, push/pull for all directions
		STRAIGHT(0x1),
		// Can only connect on two perpendicular directions, push/pull for all directions
		CORNER(0x2),
		// Can only transfer from one direction to its opposite direction, push/pull for all directions
		ONEWAY(0x3),
		// Can connect on all directions, but only one side can push to machine
		LIMIT_PUSH(0x4),
		// Can connect on all directions, but only one side can pull from machine
		LIMIT_PULL(0x5),
		// Custom node
		CUSTOM(0xf);

		private final int code;

		static final Type[] CODE_MAP = new Type[16];

		static {
			for (final Type t : Type.values()) {
				CODE_MAP[t.getCode()] = t;
			}
		}

		private Type(final int code) {
			this.code = code;
		}

		public int getCode() {
			return this.code;
		}
	}
}
