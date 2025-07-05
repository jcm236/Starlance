package net.jcm.vsch.api.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.pipe.OmniNode;
import net.jcm.vsch.pipe.PipeNetwork;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PipeNode<T extends PipeNode<T>> {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private final NodeLevel level;
	private final NodePos pos;
	private PipeNetwork network = new PipeNetwork();
	private final Type type;
	private DyeColor color = DyeColor.WHITE;

	protected PipeNode(final NodeLevel level, final NodePos pos, final Type type) {
		this.level = level;
		this.pos = pos;
		this.type = type;
		this.network.addNode(pos);
	}

	public final NodeLevel getLevel() {
		return this.level;
	}

	public final NodePos getPos() {
		return this.pos;
	}

	public final Type getType() {
		return this.type;
	}

	public DyeColor getColor() {
		return this.color;
	}

	public void setColor(final DyeColor color) {
		this.color = color;
	}

	public double getSize() {
		return 4.0 / 16;
	}

	public abstract ItemStack asItemStack();

	public abstract ModelTextures getModel();

	/**
	 * @param dir Direction of another pipe node
	 * @return if pipes can connect from the direction
	 */
	public abstract boolean canConnect(Direction dir);

	/**
	 * @param dir Direction contents flowing from
	 * @return if contents can flow from the direction
	 */
	public abstract boolean canFlowIn(Direction dir);

	/**
	 * @param dir Direction contents flowing towards to
	 * @return if contents can flow towards the direction
	 */
	public abstract boolean canFlowOut(Direction dir);

	/**
	 * Water flow rate used to calculate other fluids flow rate based on their viscosity.
	 *
	 * @return How fast can water transfer in mB/tick
	 * @see FluidType#getViscosity
	 * @see fluidFlowAmount
	 */
	protected abstract int getWaterFlowRate();

	/**
	 * @param dir Direction the fluid flowing towards to
	 * @param fluid The fluid
	 * @return How fast can the fluid transfer in mB/tick
	 *
	 * @see getWaterFlowRate
	 * @see canFlowOut
	 */
	public int fluidFlowAmount(final Direction dir, final Fluid fluid) {
		if (!this.canFlowOut(dir)) {
			return 0;
		}
		return this.getWaterFlowRate() * ForgeMod.WATER_TYPE.get().getViscosity() / fluid.getFluidType().getViscosity();
	}

	public abstract int energyFlowAmount(Direction dir);

	public void writeAdditional(final FriendlyByteBuf buf) {}

	public void readAdditional(final FriendlyByteBuf buf) {}

	public final void writeTo(final FriendlyByteBuf buf) {
		buf.writeByte((this.type.getCode() << 4) | this.color.getId());
		if (this.type == Type.CUSTOM) {
			buf.writeResourceLocation(((AbstractCustomNode) (this)).getId());
		}
		this.writeAdditional(buf);
	}

	public static PipeNode readFrom(final NodeLevel level, final NodePos pos, final FriendlyByteBuf buf) {
		final int typeAndColor = buf.readByte();
		final DyeColor color = DyeColor.byId(typeAndColor & 0xf);
		final int typeCode = (typeAndColor >> 4) & 0xf;
		final Type type = Type.CODE_MAP[typeCode];
		final PipeNode node = switch (type) {
			case OMNI -> new OmniNode(level, pos);
			case CUSTOM -> {
				final ResourceLocation id = buf.readResourceLocation();
				final AbstractCustomNode n = CustomNodeRegistry.createNode(id, level, pos);
				if (n == null) {
					LOGGER.error("[starlance]: custom node with ID " + id + " is not found");
					yield null;
				}
				yield n;
			}
			default -> {
				LOGGER.error("[starlance]: unexpected node type: " + Integer.toString(typeCode, 16));
				yield null;
			}
		};
		if (node == null) {
			return null;
		}
		node.setColor(color);
		node.readAdditional(buf);
		return node;
	}

	public String toString() {
		return String.format("<%s level=%s pos=%s>", this.getClass().getName(), this.level.getLevel(), this.pos);
	}

	public boolean canAnchor() {
		return this.pos.canAnchoredIn(this.level.getLevel(), this.getSize());
	}

	public enum Type {
		// Can connect to all directions; push/pull for all directions
		OMNI(0x0),
		// Can only connect on two opposite directions; push/pull for all directions
		STRAIGHT(0x1),
		// Can only connect on two perpendicular directions; push/pull for all directions
		CORNER(0x2),
		// Can only connect & transfer from one direction to its opposite direction; no interact with machine
		ONEWAY(0x3),
		// Can connect on all directions; no interact with machine
		LIMITED_OMNI(0x4),
		// Can connect on all directions; only one side can push to machine
		LIMITED_PUSH(0x5),
		// Can connect on all directions; only one side can pull from machine
		LIMITED_PULL(0x6),
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
