package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityMartianManhunterMob extends EntityHeroMobBase {

    private static final int HELMET = 32, CHEST = 33, LEGS = 34, BOOTS = 35;

    private static final int CD_INTANG = 60;
    private static final int CD_INVIS  = 200;
    private static final int CD_AMBUSH = 80;

    private boolean isIntangible  = false;
    private boolean isInvisible   = false;
    private int intangibleTimer   = 0;
    private int invisibleTimer    = 0;

    public EntityMartianManhunterMob(World world) {
        super(world);
        isFlying = true;
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 90D; }
    @Override protected double getMeleeDamage()    { return 8D; }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        // Fire makes him panic and flee - takes triple damage
        if (src.isFireDamage()) {
            amount *= 3F;
            isIntangible = false;
            isInvisible  = false;
            // Run away
            motionX = (rand.nextFloat() - 0.5F) * 2D;
            motionZ = (rand.nextFloat() - 0.5F) * 2D;
            motionY = 0.8D;
        }
        // Intangible = immune to most damage
        if (isIntangible && !src.isFireDamage()) return false;
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

        // Tick intangibility
        if (isIntangible) {
            intangibleTimer--;
            if (intangibleTimer <= 0) isIntangible = false;
        }

        // Tick invisibility
        if (isInvisible) {
            invisibleTimer--;
            if (invisibleTimer <= 0) {
                isInvisible = false;
                addPotionEffect(new PotionEffect(Potion.invisibility.id, 1, 0)); // remove
            }
        }

        isFlying = true;

        EntityPlayer target = getNearestPlayer(40D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Hover
        double desiredY = target.posY + 4D;
        if (posY < desiredY) applyFlyingLift(0.06D);

        // Intangibility when low health
        if (!isOnCooldown("intang") && getHealth() < getMaxHealth() * 0.5F && !isIntangible) {
            isIntangible    = true;
            intangibleTimer = 40;
            startCooldown("intang", CD_INTANG);
            addPotionEffect(new PotionEffect(Potion.damageBoost.id, 40, 1));
        }

        // Turn invisible, then ambush
        if (!isOnCooldown("invis") && !isInvisible && dist < 20D) {
            isInvisible    = true;
            invisibleTimer = 100;
            startCooldown("invis", CD_INVIS);
            addPotionEffect(new PotionEffect(Potion.invisibility.id, 120, 0));
        }

        // Ambush attack when invisible and close
        if (!isOnCooldown("ambush") && isInvisible && dist < 4D) {
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 18F);
            knockbackTarget(target, 0.8F);
            target.addPotionEffect(new PotionEffect(Potion.confusion.id, 60, 1));
            isInvisible = false;
            startCooldown("ambush", CD_AMBUSH);
        }
    }
}
