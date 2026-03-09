package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntitySpiderManMob extends EntityHeroMobBase {

    private static final int HELMET = 4, CHEST = 5, LEGS = 6, BOOTS = 7;

    private static final int CD_WEB_ZIP  = 20;
    private static final int CD_WEB_SHOT = 13;
    private static final int CD_LEAP     = 40;
    private static final int CD_SLOWMO   = 60;

    public EntitySpiderManMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 60D; }
    @Override protected double getMeleeDamage()    { return 5D; }
    @Override protected double getMoveSpeed()      { return 0.32D; }

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

        // Web zip - teleport close to target
        if (!isOnCooldown("webzip") && dist > 8D && dist < 25D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            setPosition(posX + (dx / len) * (dist - 3D), posY, posZ + (dz / len) * (dist - 3D));
            startCooldown("webzip", CD_WEB_ZIP);
            playSoundSafe("random.bow", 0.6F, 0.8F);
        }

        // Sticky web - slow and root the player
        if (!isOnCooldown("webshot") && dist < 15D) {
            target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 60, 3));
            target.addPotionEffect(new PotionEffect(Potion.weakness.id, 40, 1));
            startCooldown("webshot", CD_WEB_SHOT);
            playSoundSafe("random.bow", 0.4F, 1.2F);
        }

        // Leap attack
        if (!isOnCooldown("leap") && dist < 6D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 1.2D;
            motionZ = (dz / len) * 1.2D;
            motionY = 0.7D;
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 8F);
            startCooldown("leap", CD_LEAP);
        }

        // Slow-motion pulse - nearby players slowed
        if (!isOnCooldown("slowmo") && dist < 10D) {
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(10, 5, 10))) {
                ((EntityPlayer) obj).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 30, 1));
            }
            startCooldown("slowmo", CD_SLOWMO);
        }

        // Regen
        if (ticksSinceLastAbility % 40 == 0 && getHealth() < getMaxHealth()) {
            heal(0.5F);
        }
    }
}
