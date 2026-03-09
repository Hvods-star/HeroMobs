package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityTheFlashMob extends EntityHeroMobBase {

    private static final int HELMET = 8, CHEST = 9, LEGS = 10, BOOTS = 11;

    private static final int CD_SPEED  = 80;
    private static final int CD_COMBO  = 30;
    private static final int CD_SLOWMO = 40;

    private int speedBurstTimer = 0;
    private int comboPunchCount = 0;

    public EntityTheFlashMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 50D; }
    @Override protected double getMeleeDamage()    { return 4D; }
    @Override protected double getMoveSpeed()      { return 0.36D; }
    @Override public    boolean isGlowing()        { return true; }

    @Override
    protected ItemStack[] getArmorDrops() {
        return HeroMobEntities.buildArmorDrops(HELMET, CHEST, LEGS, BOOTS);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer target = getNearestPlayer(40D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Speed burst - massively boost velocity toward target
        if (!isOnCooldown("speed") && dist > 3D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 2.5D;
            motionZ = (dz / len) * 2.5D;
            speedBurstTimer = 30;
            startCooldown("speed", CD_SPEED);
            // Give self speed boost
            addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 60, 4));
        }

        // Speed punch combo - 5 rapid hits
        if (!isOnCooldown("combo") && dist < 3D) {
            comboPunchCount = 5;
            startCooldown("combo", CD_COMBO);
        }

        if (comboPunchCount > 0) {
            if (ticksSinceLastAbility % 3 == 0) {
                target.attackEntityFrom(DamageSource.causeMobDamage(this), 5F);
                comboPunchCount--;
            }
        }

        // Slow-motion pulse on all nearby players
        if (!isOnCooldown("slowmo") && dist < 12D) {
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(12, 5, 12))) {
                ((EntityPlayer) obj).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 2));
            }
            startCooldown("slowmo", CD_SLOWMO);
        }

        // Circle strafe when close
        if (dist < 5D) {
            double angle = (ticksSinceLastAbility * 15D) * Math.PI / 180D;
            motionX += Math.cos(angle) * 0.3D;
            motionZ += Math.sin(angle) * 0.3D;
        }
    }
}
