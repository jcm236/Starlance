package net.jcm.vsch.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ThrustParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public static ThrustParticleProvider provider(SpriteSet spriteSet) {
        return new ThrustParticleProvider(spriteSet);
    }

    protected ThrustParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.0F, 0.0F);
        this.quadSize *= 6.0F;
        this.lifetime = 22;
        this.gravity = -0.2F;
        this.hasPhysics = true;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.setSpriteFromAge(spriteSet);
    }

    public int getLightColor(float partialTick) {
        return 15728880;
    }

    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        float angularVelocity = 0.134F;
        this.roll += angularVelocity;
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(this.age % 23 + 1, 23));
        }
    }

    public record ThrustParticleProvider(SpriteSet spriteSet) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ThrustParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
        }
    }
}
