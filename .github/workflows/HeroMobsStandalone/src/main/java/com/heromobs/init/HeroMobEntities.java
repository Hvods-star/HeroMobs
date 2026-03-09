package com.heromobs.init;

import com.heromobs.HeroMobsMod;
import com.heromobs.entity.*;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.*;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class HeroMobEntities {

    private static int entityId = 1;

    // FiskHeroes armor item - resolved at runtime if the mod is present
    public static Item fiskArmorItem = null;

    public static void registerEntities() {
        register(EntityIronManMob.class,          "IronManMob");
        register(EntitySpiderManMob.class,        "SpiderManMob");
        register(EntityTheFlashMob.class,         "TheFlashMob");
        register(EntityBlackPantherMob.class,     "BlackPantherMob");
        register(EntityDoctorStrangeMob.class,    "DoctorStrangeMob");
        register(EntityKillerFrostMob.class,      "KillerFrostMob");
        register(EntityCaptainAmericaMob.class,   "CaptainAmericaMob");
        register(EntityFalconMob.class,           "FalconMob");
        register(EntityMartianManhunterMob.class,  "MartianManhunterMob");
        register(EntityShazamMob.class,            "ShazamMob");
        register(EntityDoctorStrangeClone.class,   "DoctorStrangeClone");
    }

    private static void register(Class clazz, String name) {
        EntityRegistry.registerModEntity(clazz, name, entityId++, HeroMobsMod.instance, 80, 3, true);
    }

    public static void registerSpawns() {
        // Iron Man - plains
        EntityRegistry.addSpawn(EntityIronManMob.class, 3, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.PLAINS));
        // Spider-Man - forest
        EntityRegistry.addSpawn(EntitySpiderManMob.class, 4, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.FOREST));
        // Flash - plains
        EntityRegistry.addSpawn(EntityTheFlashMob.class, 3, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.PLAINS));
        // Black Panther - jungle
        EntityRegistry.addSpawn(EntityBlackPantherMob.class, 3, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.JUNGLE));
        // Doctor Strange - swamp
        EntityRegistry.addSpawn(EntityDoctorStrangeMob.class, 2, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SWAMP));
        // Killer Frost - snowy
        EntityRegistry.addSpawn(EntityKillerFrostMob.class, 4, 1, 3,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SNOWY));
        // Captain America - plains
        EntityRegistry.addSpawn(EntityCaptainAmericaMob.class, 3, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.PLAINS));
        // Falcon - hills
        EntityRegistry.addSpawn(EntityFalconMob.class, 3, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.HILLS));
        // Martian Manhunter - sparse/rare everywhere
        EntityRegistry.addSpawn(EntityMartianManhunterMob.class, 1, 1, 1,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SPARSE));
        // Shazam - mountain
        EntityRegistry.addSpawn(EntityShazamMob.class, 2, 1, 2,
            EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.MOUNTAIN));
    }

    /**
     * Called in postInit - tries to find the FiskHeroes armor item.
     * This is done via string lookup so the mod compiles with zero FiskHeroes dependency.
     * If FiskHeroes is not installed, this simply does nothing and mobs drop iron armor.
     */
    public static void tryHookFiskHeroesItems() {
        try {
            fiskArmorItem = (Item) Item.itemRegistry.getObject("fiskheroes:superhero_armor");
            if (fiskArmorItem != null) {
                System.out.println("[HeroMobs] Successfully hooked FiskHeroes armor item!");
            } else {
                System.out.println("[HeroMobs] FiskHeroes not found - mobs will drop iron armor instead.");
            }
        } catch (Exception e) {
            System.out.println("[HeroMobs] Could not hook FiskHeroes: " + e.getMessage());
        }
    }

    /**
     * Helper used by each mob to build its armor drop set.
     * If FiskHeroes is present, returns the real suit items.
     * If not, returns iron armor as fallback.
     *
     * @param helmet   FiskHeroes metadata for helmet slot
     * @param chest    FiskHeroes metadata for chest slot
     * @param legs     FiskHeroes metadata for legs slot
     * @param boots    FiskHeroes metadata for boots slot
     */
    public static ItemStack[] buildArmorDrops(int helmet, int chest, int legs, int boots) {
        if (fiskArmorItem != null) {
            return new ItemStack[]{
                new ItemStack(fiskArmorItem, 1, helmet),
                new ItemStack(fiskArmorItem, 1, chest),
                new ItemStack(fiskArmorItem, 1, legs),
                new ItemStack(fiskArmorItem, 1, boots)
            };
        } else {
            // Fallback: iron armor
            return new ItemStack[]{
                new ItemStack(net.minecraft.init.Items.iron_helmet),
                new ItemStack(net.minecraft.init.Items.iron_chestplate),
                new ItemStack(net.minecraft.init.Items.iron_leggings),
                new ItemStack(net.minecraft.init.Items.iron_boots)
            };
        }
    }
}
