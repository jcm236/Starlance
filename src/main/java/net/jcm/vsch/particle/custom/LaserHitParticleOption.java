package net.jcm.vsch.particle.custom;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class LaserHitParticleOption implements ParticleOptions {
	public static final ParticleOptions.Deserializer<LaserHitParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<LaserHitParticleOption>() {
		public LaserHitParticleOption fromCommand(ParticleType<LaserHitParticleOption> type, StringReader r) throws CommandSyntaxException {
			throw new RuntimeException("Unsupported operation");
			// return new LaserHitParticleOption(type);
		}

		public LaserHitParticleOption fromNetwork(ParticleType<LaserHitParticleOption> type, FriendlyByteBuf buf) {
			final float[] rgb = new float[3];
			rgb[0] = buf.readFloat();
			rgb[1] = buf.readFloat();
			rgb[2] = buf.readFloat();
			return new LaserHitParticleOption(type, rgb);
		}
	};


	public static Codec<LaserHitParticleOption> codec(ParticleType<LaserHitParticleOption> type) {
		return Codec.FLOAT.listOf().xmap(list -> {
			return new LaserHitParticleOption(type, new float[]{list.get(0), list.get(1), list.get(2)});
		}, opt -> {
			final float[] color = opt.getColor();
			return List.of(color[0], color[1], color[2]);
		});
	}


	private final ParticleType<LaserHitParticleOption> type;
	private float[] rgb;

	public LaserHitParticleOption(final ParticleType<LaserHitParticleOption> type, final float[] rgb) {
		this.type = type;
		this.rgb = rgb;
	}

	@Override
	public ParticleType<LaserHitParticleOption> getType() {
		return this.type;
	}

	public float[] getColor() {
		return this.rgb;
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeFloat(this.rgb[0]);
		buf.writeFloat(this.rgb[1]);
		buf.writeFloat(this.rgb[2]);
	}

	@Override
	public String writeToString() {
		return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()).toString();
	}
}
