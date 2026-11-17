package com.ptdteam.magicadventurecore.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Фиолетовый «крестик», который вылетает из аркан-экстрактора облачком
 * и постепенно ускоряется в сторону ближайшего резервуара эссенции.
 */
public class ArcaneCrossParticle extends TextureSheetParticle {
    private static final double ARRIVE_DISTANCE = 0.35D;
    /** Размер квада частицы в мировых единицах (ванильные частицы ~0.1). */
    private static final float BASE_SIZE = 0.045F;
    private static final float SIZE_VARIATION = 0.03F;

    private final Vec3 target;

    protected ArcaneCrossParticle(ClientLevel level, double x, double y, double z, Vec3 target, SpriteSet sprites) {
        super(level, x, y, z);
        this.target = target;
        this.setSprite(sprites.get(this.random));
        this.lifetime = 40 + this.random.nextInt(30);
        this.quadSize = BASE_SIZE + this.random.nextFloat() * SIZE_VARIATION;
        this.hasPhysics = false;
        this.friction = 0.96F;
        this.xd = (this.random.nextDouble() - 0.5D) * 0.06D;
        this.yd = (this.random.nextDouble() - 0.5D) * 0.06D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.06D;
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
            double speed = 0.035D + this.age * 0.0025D;
            Vec3 dir = toTarget.normalize().scale(speed);
            this.xd = this.xd * 0.72D + dir.x;
            this.yd = this.yd * 0.72D + dir.y;
            this.zd = this.zd * 0.72D + dir.z;
        }
        // Плавное затухание к концу жизни.
        this.setAlpha(1.0F - (float) this.age / (float) this.lifetime);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
