package net.jcm.vsch.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class SmokeParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public static SmokeParticleProvider provider(SpriteSet spriteSet) {
        return new SmokeParticleProvider(spriteSet);
    }

    protected SmokeParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.0F, 0.0F);
        this.quadSize *= 9.0F;
        this.lifetime = 22;
        this.gravity = -0.2F;
        this.hasPhysics = true;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.setSpriteFromAge(spriteSet);
    }

    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(this.age % 23 + 1, 23));
        }

    }

    public record SmokeParticleProvider(SpriteSet spriteSet) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new SmokeParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
        }
    }
}
