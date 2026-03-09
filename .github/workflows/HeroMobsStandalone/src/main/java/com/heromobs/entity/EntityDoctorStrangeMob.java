package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityDoctorStrangeMob extends EntityHeroMobBase {

    private static final int HELMET = 16, CHEST = 17, LEGS = 18, BOOTS = 19;

    private static final int CD_WHIP    = 50;
    private static final int CD_PUSH    = 80;
    private static final int CD_EARTH   = 800;
    private static final int CD_CLONES  = 1200;
    private static final int CD_SHIELD  = 200;

    private float magicShieldHP = 100F;
    private int clonesSpawned   = 0;

    public EntityDoctorStrangeMob(World world) {
        super(world);
        isFlying = true;
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 45D; }
    @Override protected double getMeleeDamage()    { return 4D; }
    @Override protected boolean isFireImmune()     { return false; }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (magicShieldHP > 0) {
            magicShieldHP -= amount;
            playSoundSafe("random.orb", 0.5F, 1.2F);
            if (magicShieldHP < 0) {
                float overflow = -magicShieldHP;
                magicShieldHP = 0;
                return super.attackEntityFrom(src, overflow);
            }
            return false;
        }
        return super.attackEntityFrom(src, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        // Always levitate
        isFlying = true;
        double desiredY = posY;
        EntityPlayer target = getNearestPlayer(32D);
        if (target != null) desiredY = target.posY + 5D;
        if (posY < desiredY) applyFlyingLift(0.05D);

        if (target == null) return;
        double dist = distanceTo(target);

        // Regenerate magic shield slowly
        if (!isOnCooldown("shield") && magicShieldHP < 100F) {
            magicShieldHP = Math.min(100F, magicShieldHP + 5F);
            startCooldown("shield", CD_SHIELD);
        }

        // Whip spell - fire damage
        if (!isOnCooldown("whip") && dist < 16D) {
            target.attackEntityFrom(DamageSource.inFire, 8F);
            target.setFire(3);
            startCooldown("whip", CD_WHIP);
            playSoundSafe("fire.ignite", 0.8F, 0.9F);
        }

        // Atmospheric push - massive knockback
        if (!isOnCooldown("push") && dist < 12D) {
            knockbackTarget(target, 2.0F);
            target.addPotionEffect(new PotionEffect(Potion.confusion.id, 60, 1));
            startCooldown("push", CD_PUSH);
        }

        // Earth swallowing - massive AoE damage
        if (!isOnCooldown("earth") && dist < 20D) {
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(20, 8, 20))) {
                EntityPlayer p = (EntityPlayer) obj;
                p.attackEntityFrom(DamageSource.causeExplosionDamage(null), 20F);
                p.addPotionEffect(new PotionEffect(Potion.blindness.id, 80, 1));
                knockbackTarget(p, 1.5F);
            }
            startCooldown("earth", CD_EARTH);
            playSoundSafe("random.explode", 1.2F, 0.6F);
        }

        // Spawn clones when low health
        if (!isOnCooldown("clones") && getHealth() < getMaxHealth() * 0.3F && clonesSpawned < 5) {
            for (int i = 0; i < 5 - clonesSpawned; i++) {
                EntityDoctorStrangeClone clone = new EntityDoctorStrangeClone(worldObj);
                clone.setPosition(
                    posX + (rand.nextFloat() - 0.5F) * 6D,
                    posY,
                    posZ + (rand.nextFloat() - 0.5F) * 6D
                );
                worldObj.spawnEntityInWorld(clone);
                clonesSpawned++;
            }
            startCooldown("clones", CD_CLONES);
        }
    }
}
