package net.jcm.vsch.network.s2c;

import net.jcm.vsch.network.INetworkPacket;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public abstract class PlanetDataUpdateS2C implements INetworkPacket {
	protected final ResourceKey<Level> planet;
	protected final Vector3dc position;
	protected final Quaterniondc rotation;

	protected PlanetDataUpdateS2C(final ResourceKey<Level> planet, final Vector3dc position, final Quaterniondc rotation) {
		this.planet = planet;
		this.position = position;
		this.rotation = rotation;
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		ctx.enqueueWork(() -> {
			final LevelData levelData = LevelData.getClientData(this.planet);
			if (levelData == null) {
				return;
			}
			final LevelData upperData = LevelData.getClientData(levelData.getUpperDimension());
			if (upperData == null) {
				return;
			}
			final PlanetData planetData = upperData.getPlanet(this.planet);
			if (planetData == null) {
				return;
			}
			if (this.position != null) {
				planetData.setPosition(this.position);
			}
			if (this.rotation != null) {
				planetData.setRotation(this.rotation);
			}
		});
	}

	public final class Pos extends PlanetDataUpdateS2C {
		public Pos(final ResourceKey<Level> planet, final Vector3dc position) {
			super(planet, position, null);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			buf.writeVec3(this.position);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Vector3dc position = SerializeUtil.readVector3d(buf);
			return new Pos(planet, position);
		}
	}

	public final class Rot extends PlanetDataUpdateS2C {
		public Rot(final ResourceKey<Level> planet, final Quaterniondc rotation) {
			super(planet, null, rotation);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			SerializeUtil.writeQuaterniond(buf, this.rotation);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Quaterniondc rotation = SerializeUtil.readQuaterniond(buf);
			return new Rot(planet, rotation);
		}
	}

	public final class PosRot extends PlanetDataUpdateS2C {
		public PosRot(final ResourceKey<Level> planet, final Vector3dc position, final Quaterniondc rotation) {
			super(planet, position, rotation);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			buf.writeVec3(this.position);
			SerializeUtil.writeQuaterniond(buf, this.rotation);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Vector3dc position = SerializeUtil.readVector3d(buf);
			final Quaterniondc rotation = SerializeUtil.readQuaterniond(buf);
			return new PosRot(planet, position, rotation);
		}
	}
}
