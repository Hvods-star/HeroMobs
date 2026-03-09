package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityKillerFrostMob extends EntityHeroMobBase {

    private static final int HELMET = 20, CHEST = 21, LEGS = 22, BOOTS = 23;

    private static final int CD_ICICLE = 5;
    private static final int CD_BLADE  = 8;

    private int cryoHits = 0;
    private static final int CRYO_MAX = 5;

    public EntityKillerFrostMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 40D; }
    @Override protected double getMeleeDamage()    { return 4D; }
    @Override public    boolean isGlowing()        { return true; }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        // Double damage from fire
        if (src.isFireDamage()) amount *= 2F;
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

        EntityPlayer target = getNearestPlayer(25D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Icicle barrage - rapid ranged attacks
        if (!isOnCooldown("icicle") && dist < 18D) {
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 4F);
            target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 1));
            cryoHits++;
            startCooldown("icicle", CD_ICICLE);
            playSoundSafe("random.bow", 0.3F, 1.5F);
        }

        // Every 5 hits, release cryo burst
        if (cryoHits >= CRYO_MAX) {
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 10F);
            target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 100, 3));
            target.addPotionEffect(new PotionEffect(Potion.weakness.id, 80, 2));
            knockbackTarget(target, 0.5F);
            cryoHits = 0;
            playSoundSafe("random.glass", 0.8F, 0.7F);
        }

        // Ice blade slash at melee range
        if (!isOnCooldown("blade") && dist < 3D) {
            target.attackEntityFrom(DamageSource.causeMobDamage(this), 7F);
            target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 2));
            startCooldown("blade", CD_BLADE);
        }
    }
}
