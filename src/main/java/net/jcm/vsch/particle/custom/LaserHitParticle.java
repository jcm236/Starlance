package net.jcm.vsch.particle.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.jcm.vsch.particle.VSCHParticles;

import java.util.Random;

public class LaserHitParticle extends TextureSheetParticle {
	private static final Random RND = new Random();
	private static final ResourceLocation BLOOM_BUFFER = new ResourceLocation("vsch", "bloom");
	private static final double SPREAD_ANGLE = Math.toRadians(22.5);
	private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
		public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
			AdvancedFbo buffer = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(BLOOM_BUFFER);
			if (buffer != null) buffer.bind(false);
			RenderSystem.depthMask(true);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			p_107455_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		public void end(Tesselator p_107458_) {
			p_107458_.end();
			AdvancedFbo.getMainFramebuffer().bind(true);
		}

		public String toString() {
			return "PARTICLE_SHEET_TRANSLUCENT_BLOOM";
		}
	};
	private final SpriteSet sprites;

	protected LaserHitParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(level, x, y, z, vx, vy, vz);
		this.sprites = spriteSet;



		// Set initial velocity
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;

		this.lifetime = 8 + this.random.nextInt(3); // Particle lifespan
		this.alpha = 1.0f;

		// Visual size
		this.quadSize *= 0.3;
		// Air friction
		this.friction = 0.8f;
		// Collision size
		this.setSize(0.01f, 0.01f);

		this.setSpriteFromAge(spriteSet);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites); // Cycle textures
		this.alpha = 1.0f - ((float) this.age / this.lifetime); // Fade out effect
	}

	@Override
	public ParticleRenderType getRenderType() {
		return RENDER_TYPE;
	}

	@Override
	public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
		super.render(pBuffer, pRenderInfo, pPartialTicks);
	}

	public static void spawnConeOfParticles(Level level, float x, float y, float z, Vec3 dir, int iter, float[] color) {
		for (int i = 0; i < iter; i++) {
			// TODO: This is horrible ChatGPT code, PLEASE someone who knows more than me re-do this

			final double randomAngle = RND.nextDouble() * 2 * Math.PI;
			final double deviation = RND.nextDouble() * SPREAD_ANGLE;

			// Ensure we get valid perpendicular vectors for any direction
			final Vec3 right = Math.abs(dir.x) < 0.001 && Math.abs(dir.z) < 0.001
				? new Vec3(1, 0, 0)
				: new Vec3(-dir.z, 0, dir.x).normalize();

			// Compute another perpendicular vector
			final Vec3 up = dir.cross(right).normalize();

			// Compute offset in cone
			final Vec3 offset = right.scale(Math.cos(randomAngle) * deviation)
					.add(up.scale(Math.sin(randomAngle) * deviation));

			final Vec3 velocity = dir.add(offset).normalize().scale(0.1);

			level.addParticle(
				new LaserHitParticleOption(VSCHParticles.LASER_HIT_PARTICLE.get(), color),
				x, y, z,
				velocity.x, velocity.y, velocity.z);
		}
	}

	public static class Provider implements ParticleProvider<LaserHitParticleOption> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(LaserHitParticleOption option, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
			final LaserHitParticle particle = new LaserHitParticle(level, x, y, z, vx, vy, vz, spriteSet);
			final float[] color = option.getColor();
			particle.setColor(color[0], color[1], color[2]);
			return particle;
		}
	}
}
