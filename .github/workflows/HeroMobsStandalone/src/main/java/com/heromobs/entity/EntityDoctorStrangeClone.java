package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** A decoy clone spawned by Doctor Strange. Attacks but drops nothing. */
public class EntityDoctorStrangeClone extends EntityHeroMobBase {

    public EntityDoctorStrangeClone(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 20D; }
    @Override protected double getMeleeDamage()    { return 3D; }

    // Clones drop nothing
    @Override
    protected ItemStack[] getArmorDrops() {
        return new ItemStack[0];
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer target = getNearestPlayer(20D);
        if (target != null && distanceTo(target) < 2.5D) {
            if (!isOnCooldown("melee")) {
                target.attackEntityFrom(DamageSource.causeMobDamage(this), 3F);
                startCooldown("melee", 20);
            }
        }
    }
}
