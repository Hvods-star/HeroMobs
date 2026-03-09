package com.heromobs.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all HeroMobs.
 *
 * KEY BEHAVIOUR:
 *   - Looks like a player (biped model, FiskHeroes skin texture)
 *   - PASSIVE by default: wanders around, watches players, does NOT attack
 *   - Once HIT by a player, becomes permanently aggressive toward that player
 *   - Full ability cooldown system (per-ability, NBT-persisted)
 *   - Drops FiskHeroes armor on death (falls back to iron armor if FiskHeroes absent)
 *   - Flying mobs immune to fall damage
 */
public abstract class EntityHeroMobBase extends EntityCreature implements IMob {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    protected final Map<String, Integer> cooldowns = new HashMap<>();
    protected int ticksSinceLastAbility = 0;

    public boolean isFlying   = false;
    protected boolean isCharging = false;

    /** True once the mob has been hit - switches from passive to aggressive */
    private boolean isAggressive = false;

    /** The player who first provoked this mob */
    private EntityPlayer provokingPlayer = null;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public EntityHeroMobBase(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        isImmuneToFire = isFireImmune();
    }

    // -----------------------------------------------------------------------
    // Abstract contracts
    // -----------------------------------------------------------------------
    protected abstract ItemStack[] getArmorDrops();
    protected boolean isFireImmune()        { return false; }
    protected abstract double getMaxHealthValue();
    protected abstract double getMeleeDamage();
    protected double getMoveSpeed()         { return 0.28D; }
    /** Return true if this hero type should emit a glow pass in the renderer */
    public boolean isGlowing()              { return false; }
    public boolean isFlying()               { return isFlying; }

    // -----------------------------------------------------------------------
    // Attributes
    // -----------------------------------------------------------------------
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(getMaxHealthValue());
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(getMoveSpeed());
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(getMeleeDamage());
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(48.0D);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(0.1D);
    }

    // -----------------------------------------------------------------------
    // AI - passive wander set + aggressive attack set
    // Both are registered; the attack tasks only fire once isAggressive=true
    // -----------------------------------------------------------------------
    @Override
    protected void entityInit() {
        super.entityInit();
    }

    /**
     * Subclasses call super.setupBaseAI() then add their ability AI tasks.
     * Aggressive targeting is added here but only activates after provocation.
     */
    protected void setupBaseAI() {
        tasks.addTask(0, new EntityAISwimming(this));

        // Passive: wander and observe
        tasks.addTask(6, new EntityAIWander(this, 1.0D));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        tasks.addTask(8, new EntityAILookIdle(this));

        // Aggressive: only runs when getAttackTarget() is set (after provocation)
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, getMoveSpeed() * 1.2D, false));
        tasks.addTask(5, new EntityAIMoveTowardsTarget(this, getMoveSpeed() * 1.1D, 32.0F));

        // Target tasks - only trigger after we set the target manually on provocation
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
    }

    // -----------------------------------------------------------------------
    // Provocation - called when mob takes damage from a player
    // -----------------------------------------------------------------------
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean result = super.attackEntityFrom(source, amount);
        if (result && !isAggressive) {
            if (source.getEntity() instanceof EntityPlayer) {
                isAggressive    = true;
                provokingPlayer = (EntityPlayer) source.getEntity();
                setAttackTarget(provokingPlayer);
                // Alert nearby same-type mobs (pack aggro within 16 blocks)
                alertNearbyAllies();
            }
        }
        return result;
    }

    private void alertNearbyAllies() {
        java.util.List nearby = worldObj.getEntitiesWithinAABB(
            this.getClass(),
            boundingBox.expand(16, 8, 16)
        );
        for (Object obj : nearby) {
            EntityHeroMobBase ally = (EntityHeroMobBase) obj;
            if (!ally.isAggressive && provokingPlayer != null) {
                ally.isAggressive    = true;
                ally.provokingPlayer = provokingPlayer;
                ally.setAttackTarget(provokingPlayer);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Update loop
    // -----------------------------------------------------------------------
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // Tick cooldowns
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (entry.getValue() > 0) entry.setValue(entry.getValue() - 1);
        }
        ticksSinceLastAbility++;

        // Flying: suppress gravity / fall damage
        if (isFlying) {
            motionY    = Math.max(motionY, -0.05D);
            fallDistance = 0F;
        }

        // If aggressive and lost target, keep chasing nearest player
        if (isAggressive && getAttackTarget() == null) {
            EntityPlayer nearest = worldObj.getClosestPlayerToEntity(this, 48D);
            if (nearest != null) setAttackTarget(nearest);
        }
    }

    // -----------------------------------------------------------------------
    // Cooldown helpers
    // -----------------------------------------------------------------------
    public void startCooldown(String key, int ticks) { cooldowns.put(key, ticks); }
    public boolean isOnCooldown(String key) {
        Integer v = cooldowns.get(key);
        return v != null && v > 0;
    }
    public int getCooldownTicks(String key) {
        Integer v = cooldowns.get(key);
        return v != null ? v : 0;
    }

    // -----------------------------------------------------------------------
    // Drops
    // -----------------------------------------------------------------------
    @Override
    protected void dropFewItems(boolean recentlyHit, int lootingLevel) {
        for (ItemStack stack : getArmorDrops()) {
            if (stack != null) entityDropItem(stack, 0.5F);
        }
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagCompound cdTag = new NBTTagCompound();
        for (Map.Entry<String, Integer> e : cooldowns.entrySet()) {
            cdTag.setInteger(e.getKey(), e.getValue());
        }
        tag.setTag("HeroCooldowns", cdTag);
        tag.setBoolean("IsFlying",     isFlying);
        tag.setBoolean("IsCharging",   isCharging);
        tag.setBoolean("IsAggressive", isAggressive);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("HeroCooldowns")) {
            NBTTagCompound cdTag = tag.getCompoundTag("HeroCooldowns");
            for (Object k : cdTag.func_150296_c()) {
                cooldowns.put((String) k, cdTag.getInteger((String) k));
            }
        }
        isFlying     = tag.getBoolean("IsFlying");
        isCharging   = tag.getBoolean("IsCharging");
        isAggressive = tag.getBoolean("IsAggressive");
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------
    protected void playSoundSafe(String sound, float vol, float pitch) {
        if (!worldObj.isRemote) worldObj.playSoundAtEntity(this, sound, vol, pitch);
    }

    protected void applyFlyingLift(double dy) {
        motionY += dy;
        fallDistance = 0F;
        isFlying = true;
    }

    protected void knockbackTarget(EntityLivingBase target, float power) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) target.addVelocity((dx / dist) * power, 0.4D, (dz / dist) * power);
    }

    protected EntityPlayer getNearestPlayer(double range) {
        return worldObj.getClosestPlayerToEntity(this, range);
    }

    protected double distanceTo(EntityLivingBase target) {
        double dx = posX - target.posX;
        double dy = posY - target.posY;
        double dz = posZ - target.posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override public boolean isAIEnabled()          { return true; }
    @Override protected String getLivingSound()     { return null; }
    @Override protected String getHurtSound()       { return "damage.hit"; }
    @Override protected String getDeathSound()      { return "damage.hit"; }
}
