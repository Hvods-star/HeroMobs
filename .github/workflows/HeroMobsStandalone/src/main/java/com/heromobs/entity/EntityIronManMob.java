package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityIronManMob extends EntityHeroMobBase {

    // Metadata values for FiskHeroes Iron Man suit
    private static final int HELMET = 0, CHEST = 1, LEGS = 2, BOOTS = 3;

    private static final int CD_REPULSOR = 30;   // 1.5 sec
    private static final int CD_SENTRY   = 200;  // 10 sec
    private static final int CD_BARREL   = 60;   // 3 sec

    private boolean sentryMode = false;
    private int sentryTimer    = 0;

    public EntityIronManMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 80D; }
    @Override protected double getMeleeDamage()    { return 6D; }
    @Override protected boolean isFireImmune()     { return true; }
    @Override public    boolean isGlowing()        { return true; }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer target = getNearestPlayer(32D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Always fly above target
        if (dist < 40D) {
            isFlying = true;
            double desiredY = target.posY + 6D;
            if (posY < desiredY) applyFlyingLift(0.08D);
        }

        // Repulsor blast
        if (!isOnCooldown("repulsor") && dist < 20D) {
            fireRepulsor(target);
            startCooldown("repulsor", CD_REPULSOR);
        }

        // Sentry mode - rapid fire when player is close
        if (!sentryMode && !isOnCooldown("sentry") && dist < 8D) {
            sentryMode  = true;
            sentryTimer = 100;
            startCooldown("sentry", CD_SENTRY);
        }

        if (sentryMode) {
            sentryTimer--;
            if (sentryTimer <= 0) {
                sentryMode = false;
            } else if (sentryTimer % 8 == 0) {
                fireRepulsor(target);
            }
        }

        // Barrel roll dodge when player attacks (tracked via hurt)
        if (!isOnCooldown("barrel") && hurtTime > 0) {
            motionX += (rand.nextFloat() - 0.5D) * 1.4D;
            motionZ += (rand.nextFloat() - 0.5D) * 1.4D;
            motionY += 0.3D;
            startCooldown("barrel", CD_BARREL);
        }
    }

    private void fireRepulsor(EntityLivingBase target) {
        // Damage + knockback + brief blindness
        target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 12F);
        knockbackTarget(target, 0.6F);
        target.addPotionEffect(new PotionEffect(Potion.blindness.id, 15, 0));
        playSoundSafe("random.explode", 0.5F, 1.8F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        // Immune to projectiles and fire
        if (src.isProjectile() || src.isFireDamage()) return false;
        return super.attackEntityFrom(src, amount);
    }
}
