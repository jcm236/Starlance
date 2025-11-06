package net.jcm.vsch.network.s2c;

import net.jcm.vsch.network.INetworkPacket;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;

public abstract class PlanetDataUpdateS2C implements INetworkPacket {
	protected final ResourceKey<Level> planet;
	protected final Vec3 position;
	protected final Quaterniondc rotation;

	protected PlanetDataUpdateS2C(final ResourceKey<Level> planet, final Vec3 position, final Quaterniondc rotation) {
		this.planet = planet;
		this.position = position;
		this.rotation = rotation;
	}

	public final class Pos extends PlanetDataUpdateS2C {
		public Pos(final ResourceKey<Level> planet, final Vec3 position) {
			super(planet, position, null);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			buf.writeVec3(this.position);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Vec3 position = buf.readVec3();
			return new Pos(planet, position);
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
				planetData.setPosition(this.position);
			});
		}
	}

	public final class Rot extends PlanetDataUpdateS2C {
		public Rot(final ResourceKey<Level> planet, final Quaterniondc rotation) {
			super(planet, null, rotation);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			writeQuaterniond(buf, this.rotation);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Quaterniondc rotation = readQuaterniond(buf, new Quaterniond());
			return new Rot(planet, rotation);
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
				planetData.setRotation(this.rotation);
			});
		}
	}

	public final class PosRot extends PlanetDataUpdateS2C {
		public PosRot(final ResourceKey<Level> planet, final Vec3 position, final Quaterniondc rotation) {
			super(planet, position, rotation);
		}

		@Override
		public void encode(final FriendlyByteBuf buf) {
			buf.writeResourceKey(this.planet);
			buf.writeVec3(this.position);
			writeQuaterniond(buf, this.rotation);
		}

		public static PlanetDataUpdateS2C decode(final FriendlyByteBuf buf) {
			final ResourceKey<Level> planet = buf.readResourceKey(Registries.DIMENSION);
			final Vec3 position = buf.readVec3();
			final Quaterniondc rotation = readQuaterniond(buf, new Quaterniond());
			return new PosRot(planet, position, rotation);
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
				planetData.setPosition(this.position);
				planetData.setRotation(this.rotation);
			});
		}
	}

	private static void writeQuaterniond(final FriendlyByteBuf buf, final Quaterniondc quat) {
		buf.writeDouble(quat.x());
		buf.writeDouble(quat.y());
		buf.writeDouble(quat.z());
		buf.writeDouble(quat.w());
	}

	private static Quaterniond readQuaterniond(final FriendlyByteBuf buf, final Quaterniond dest) {
		dest.x = buf.readDouble();
		dest.y = buf.readDouble();
		dest.z = buf.readDouble();
		dest.w = buf.readDouble();
		return dest;
	}
}
