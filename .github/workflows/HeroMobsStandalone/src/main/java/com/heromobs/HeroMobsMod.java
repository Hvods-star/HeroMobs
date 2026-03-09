package com.heromobs;

import com.heromobs.init.HeroMobEntities;
import com.heromobs.proxy.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = HeroMobsMod.MODID, name = HeroMobsMod.NAME, version = HeroMobsMod.VERSION)
public class HeroMobsMod {

    public static final String MODID   = "heromobs";
    public static final String NAME    = "Hero Mobs";
    public static final String VERSION = "1.0.0";

    @Instance(MODID)
    public static HeroMobsMod instance;

    @SidedProxy(
        clientSide = "com.heromobs.proxy.ClientProxy",
        serverSide = "com.heromobs.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        HeroMobEntities.registerEntities();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        HeroMobEntities.registerSpawns();
        proxy.registerRenderers();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Attempt to hook FiskHeroes items at runtime (safe - won't crash if not present)
        HeroMobEntities.tryHookFiskHeroesItems();
    }
}
