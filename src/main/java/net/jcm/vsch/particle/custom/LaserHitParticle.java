package net.jcm.vsch.particle.custom;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jcm.vsch.particle.VSCHParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class LaserHitParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected LaserHitParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = spriteSet;

        // Set initial velocity
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.lifetime = 8 + this.random.nextInt(3); // Particle lifespan
        this.alpha = 1.0F;

        // Visual size
        this.quadSize *= 0.3;
        // Air friction
        this.friction = 0.8f;
        // Collision size
        this.setSize(0.01F, 0.01F);

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites); // Cycle textures
        this.alpha = 1.0F - ((float) this.age / this.lifetime); // Fade out effect
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        this.setColor(1.0F, 0.0F, 0.0F);
        super.render(pBuffer, pRenderInfo, pPartialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnConeOfParticles(Level level, float x, float y, float z, Vec3 dir) {
        spawnConeOfParticles(level, x, y, z, dir, 10);
    }

    public static void spawnConeOfParticles(Level level, float x, float y, float z, Vec3 dir, int iter) {
        for (int i = 0; i < iter; i++) {

            // TODO: This is horrible ChatGPT code, PLEASE someone who knows more than me re-do this

            double spreadAngle = Math.toRadians(22.5); // Max cone spread angle
            double randomAngle = Math.random() * 2 * Math.PI; // Random azimuthal rotation
            double deviation = Math.random() * spreadAngle; // Random spread within the cone

            // Ensure we get valid perpendicular vectors for any direction
            Vec3 right;
            if (Math.abs(dir.x) < 0.001 && Math.abs(dir.z) < 0.001) {
                // Special case: If dir is purely vertical, pick a fixed perpendicular vector
                right = new Vec3(1, 0, 0);
            } else {
                // General case: Compute a perpendicular vector
                right = new Vec3(-dir.z, 0, dir.x).normalize();
            }

            // Compute another perpendicular vector
            Vec3 up = dir.cross(right).normalize();

            // Compute offset in cone
            Vec3 offset = right.scale(Math.cos(randomAngle) * deviation)
                    .add(up.scale(Math.sin(randomAngle) * deviation));

            // New direction (perturbed)
            Vec3 newDir = dir.add(offset).normalize();

            // Final velocity components
            double vx = newDir.x / 10;
            double vy = newDir.y / 10;
            double vz = newDir.z / 10;


            level.addParticle(VSCHParticles.LASER_HIT_PARTICLE.get(), x, y, z, vx, vy, vz);
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new LaserHitParticle(level, x, y, z, vx, vy, vz, spriteSet);
        }


    }
}
