package com.ptdteam.magicadventurecore.client.particle;

import com.ptdteam.magicadventurecore.world.particle.LifeOrbParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Красный орб, летящий к резервуару: сначала слегка разлетается «облачком»,
 * затем ускоряется по направлению к цели.
 */
public class LifeOrbParticle extends TextureSheetParticle {
    private static final double ARRIVE_DISTANCE = 0.45D;
    private static final float BASE_SIZE = 0.12F;
    private static final float SIZE_VARIATION = 0.08F;

    private final Vec3 target;

    protected LifeOrbParticle(ClientLevel level, double x, double y, double z, Vec3 target, SpriteSet sprites) {
        super(level, x, y, z);
        this.target = target;
        this.setSprite(sprites.get(this.random));
        // Белый ванильный спрайт glow красим в красный — свой PNG не нужен.
        this.setColor(1.0F, 0.22F, 0.22F);
        this.lifetime = 30 + this.random.nextInt(20);
        this.quadSize = BASE_SIZE + this.random.nextFloat() * SIZE_VARIATION;
        this.hasPhysics = false;
        this.friction = 0.94F;
        this.xd = (this.random.nextDouble() - 0.5D) * 0.12D;
        this.yd = (this.random.nextDouble() - 0.5D) * 0.12D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.12D;
        this.setAlpha(0.95F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.target != null && !this.target.equals(Vec3.ZERO)) {
            Vec3 toTarget = this.target.subtract(this.x, this.y, this.z);
            if (toTarget.length() <= ARRIVE_DISTANCE) {
                this.remove();
                return;
            }
            double speed = 0.06D + this.age * 0.004D;
            Vec3 dir = toTarget.normalize().scale(speed);
            this.xd = this.xd * 0.68D + dir.x;
            this.yd = this.yd * 0.68D + dir.y;
            this.zd = this.zd * 0.68D + dir.z;
        }
        this.setAlpha(1.0F - (float) this.age / (float) this.lifetime);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
