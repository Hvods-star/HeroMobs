package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityCaptainAmericaMob extends EntityHeroMobBase {

    private static final int HELMET = 24, CHEST = 25, LEGS = 26, BOOTS = 27;

    private static final int CD_SHIELD_THROW = 20;
    private static final int CD_LEAP         = 30;
    private static final int CD_BLOCK        = 5;

    private boolean isBlocking = false;

    public EntityCaptainAmericaMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 80D; }
    @Override protected double getMeleeDamage()    { return 7D; }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        // Shield block reduces damage by 50% when blocking
        if (isBlocking && !src.isProjectile()) {
            amount *= 0.5F;
        }
        // Immune to projectiles (shield)
        if (src.isProjectile()) return false;
        return super.attackEntityFrom(src, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer target = getNearestPlayer(32D);
        if (target == null) {
            isBlocking = false;
            return;
        }

        double dist = distanceTo(target);

        // Shield throw - bounces to multiple targets
        if (!isOnCooldown("shieldthrow") && dist < 24D) {
            // Bounce 1
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 8F);
            knockbackTarget(target, 0.4F);
            // Bounce 2 & 3 - find other nearby players
            int bounces = 0;
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(16, 6, 16))) {
                if (bounces >= 2) break;
                EntityPlayer other = (EntityPlayer) obj;
                if (other != target) {
                    other.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 6F);
                    bounces++;
                }
            }
            startCooldown("shieldthrow", CD_SHIELD_THROW);
            playSoundSafe("random.anvil_land", 0.5F, 1.5F);
        }

        // Leap charge
        if (!isOnCooldown("leap") && dist > 6D && dist < 20D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 1.3D;
            motionZ = (dz / len) * 1.3D;
            motionY = 0.6D;
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 10F);
            startCooldown("leap", CD_LEAP);
        }

        // Shield block when player is close
        isBlocking = !isOnCooldown("block") && dist < 5D;
        if (isBlocking) startCooldown("block", CD_BLOCK);

        // Regen - supersoldier serum
        if (ticksSinceLastAbility % 30 == 0 && getHealth() < getMaxHealth()) {
            heal(0.5F);
        }
    }
}
