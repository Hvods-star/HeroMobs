package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityFalconMob extends EntityHeroMobBase {

    private static final int HELMET = 28, CHEST = 29, LEGS = 30, BOOTS = 31;

    private static final int CD_GUN    = 12;
    private static final int CD_BOOST  = 200;
    private static final int CD_SHIELD = 100;
    private static final int CD_DIVE   = 60;

    private float wingShieldHP = 50F;
    private int gunBurstCount  = 0;

    public EntityFalconMob(World world) {
        super(world);
        isFlying = true;
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 55D; }
    @Override protected double getMeleeDamage()    { return 5D; }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (wingShieldHP > 0 && src.isProjectile()) {
            wingShieldHP -= amount;
            // Barrel roll dodge
            motionX += (rand.nextFloat() - 0.5F) * 1.2D;
            motionZ += (rand.nextFloat() - 0.5F) * 1.2D;
            motionY += 0.3D;
            if (wingShieldHP < 0) wingShieldHP = 0;
            return false;
        }
        return super.attackEntityFrom(src, amount);
    }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        // Always fly
        isFlying = true;

        EntityPlayer target = getNearestPlayer(40D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Hover at altitude
        double desiredY = target.posY + 8D;
        if (posY < desiredY) applyFlyingLift(0.07D);

        // Gun burst - 3 shots
        if (!isOnCooldown("gun") && dist < 25D) {
            if (gunBurstCount < 3) {
                target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 5F);
                gunBurstCount++;
                startCooldown("gun", CD_GUN);
                playSoundSafe("random.bow", 0.5F, 1.3F);
            } else {
                gunBurstCount = 0;
                startCooldown("gun", CD_GUN * 5);
            }
        }

        // Super boost - massive speed burst
        if (!isOnCooldown("boost") && dist > 15D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 3D;
            motionZ = (dz / len) * 3D;
            startCooldown("boost", CD_BOOST);
        }

        // Wing dive - dive bomb from above
        if (!isOnCooldown("dive") && posY > target.posY + 5D && dist < 12D) {
            motionY = -1.5D;
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 12F);
            knockbackTarget(target, 1.0F);
            startCooldown("dive", CD_DIVE);
        }

        // Regen wing shield slowly
        if (!isOnCooldown("shield") && wingShieldHP < 50F) {
            wingShieldHP = Math.min(50F, wingShieldHP + 2F);
            startCooldown("shield", CD_SHIELD);
        }
    }
}
