package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityBlackPantherMob extends EntityHeroMobBase {

    private static final int HELMET = 12, CHEST = 13, LEGS = 14, BOOTS = 15;

    private static final int CD_BURST  = 120;
    private static final int CD_POUNCE = 40;
    private static final int CD_SLASH  = 10;

    private int kineticCharge = 0;
    private static final int MAX_CHARGE = 5;

    public EntityBlackPantherMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 70D; }
    @Override protected double getMeleeDamage()    { return 7D; }
    @Override protected double getMoveSpeed()      { return 0.3D; }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        // Absorb projectile damage as kinetic charge
        if (src.isProjectile()) {
            kineticCharge = Math.min(kineticCharge + 1, MAX_CHARGE);
            return false; // immune to projectiles, absorbs instead
        }
        return super.attackEntityFrom(src, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer target = getNearestPlayer(32D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Kinetic burst - release absorbed energy as AoE explosion
        if (!isOnCooldown("burst") && kineticCharge >= MAX_CHARGE) {
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(8, 4, 8))) {
                EntityPlayer p = (EntityPlayer) obj;
                p.attackEntityFrom(DamageSource.causeExplosionDamage(null), 14F);
                knockbackTarget(p, 1.0F);
            }
            worldObj.newExplosion(this, posX, posY, posZ, 2.0F, false, false);
            kineticCharge = 0;
            startCooldown("burst", CD_BURST);
            playSoundSafe("random.explode", 1F, 0.9F);
        }

        // Pounce
        if (!isOnCooldown("pounce") && dist > 5D && dist < 18D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 1.5D;
            motionZ = (dz / len) * 1.5D;
            motionY = 0.8D;
            startCooldown("pounce", CD_POUNCE);
        }

        // Vibranium claw slash when close
        if (!isOnCooldown("slash") && dist < 3D) {
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 9F);
            target.addPotionEffect(new PotionEffect(Potion.weakness.id, 30, 1));
            startCooldown("slash", CD_SLASH);
        }
    }
}
