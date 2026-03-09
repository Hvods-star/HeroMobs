package com.heromobs.entity;

import com.heromobs.init.HeroMobEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityShazamMob extends EntityHeroMobBase {

    private static final int HELMET = 36, CHEST = 37, LEGS = 38, BOOTS = 39;

    private static final int CD_LIGHTNING = 20;
    private static final int CD_CLAP      = 60;
    private static final int CD_SPEED     = 50;

    public EntityShazamMob(World world) {
        super(world);
        setupBaseAI();
    }

    @Override protected double getMaxHealthValue() { return 120D; }
    @Override protected double getMeleeDamage()    { return 10D; }
    @Override protected boolean isFireImmune()     { return true; }
    @Override public    boolean isGlowing()        { return true; }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (src.isFireDamage() || src.isProjectile()) return false;
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

        EntityPlayer target = getNearestPlayer(40D);
        if (target == null) return;

        double dist = distanceTo(target);

        // Lightning chain - jumps between players
        if (!isOnCooldown("lightning") && dist < 28D) {
            boolean first = true;
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(28, 10, 28))) {
                EntityPlayer p = (EntityPlayer) obj;
                // Primary hit harder
                float dmg = first ? 10F : 6F;
                p.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), dmg);
                p.setFire(2);
                p.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 1));
                // 20% chance lightning bolt visual
                if (rand.nextFloat() < 0.2F) {
                    worldObj.addWeatherEffect(
                        new net.minecraft.entity.effect.EntityLightningBolt(
                            worldObj, p.posX, p.posY, p.posZ
                        )
                    );
                }
                first = false;
            }
            startCooldown("lightning", CD_LIGHTNING);
            playSoundSafe("ambient.weather.thunder", 0.8F, 0.9F);
        }

        // Thunder clap - massive AoE knockback + damage
        if (!isOnCooldown("clap") && dist < 10D) {
            for (Object obj : worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    boundingBox.expand(10, 5, 10))) {
                EntityPlayer p = (EntityPlayer) obj;
                p.attackEntityFrom(DamageSource.causeMobDamage(this), 15F);
                knockbackTarget(p, 2.5F);
                p.addPotionEffect(new PotionEffect(Potion.confusion.id, 80, 2));
            }
            worldObj.newExplosion(this, posX, posY, posZ, 3.0F, false, false);
            startCooldown("clap", CD_CLAP);
            playSoundSafe("ambient.weather.thunder", 1.2F, 0.6F);
        }

        // Super speed chase when player tries to flee
        if (!isOnCooldown("speed") && dist > 10D) {
            double dx = target.posX - posX;
            double dz = target.posZ - posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            motionX = (dx / len) * 2.0D;
            motionZ = (dz / len) * 2.0D;
            startCooldown("speed", CD_SPEED);
            addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 40, 3));
        }

        // Regen - magical power
        if (ticksSinceLastAbility % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
    }
}
