package net.jcm.vsch.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class AirThrustParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public static AirThrustParticleProvider provider(SpriteSet spriteSet) {
        return new AirThrustParticleProvider(spriteSet);
    }

    protected AirThrustParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.2F, 0.2F);
        this.quadSize *= 1.5F;
        this.lifetime = 8;
        this.gravity = 0.0F;
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
            this.setSprite(this.spriteSet.get(1, 1));
        }
    }

    public record AirThrustParticleProvider(SpriteSet spriteSet) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new AirThrustParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
        }
    }
}
