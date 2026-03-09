package com.heromobs.proxy;

import com.heromobs.entity.*;
import com.heromobs.render.RenderHeroMob;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityIronManMob.class,
            new RenderHeroMob("ironman", 0xFF0000));
        RenderingRegistry.registerEntityRenderingHandler(EntitySpiderManMob.class,
            new RenderHeroMob("spiderman", 0xCC0000));
        RenderingRegistry.registerEntityRenderingHandler(EntityTheFlashMob.class,
            new RenderHeroMob("flash", 0xFFAA00));
        RenderingRegistry.registerEntityRenderingHandler(EntityBlackPantherMob.class,
            new RenderHeroMob("blackpanther", 0x220022));
        RenderingRegistry.registerEntityRenderingHandler(EntityDoctorStrangeMob.class,
            new RenderHeroMob("doctorstrange", 0x003388));
        RenderingRegistry.registerEntityRenderingHandler(EntityKillerFrostMob.class,
            new RenderHeroMob("killerfrost", 0x88DDFF));
        RenderingRegistry.registerEntityRenderingHandler(EntityCaptainAmericaMob.class,
            new RenderHeroMob("captainamerica", 0x0000CC));
        RenderingRegistry.registerEntityRenderingHandler(EntityFalconMob.class,
            new RenderHeroMob("falcon", 0x884400));
        RenderingRegistry.registerEntityRenderingHandler(EntityMartianManhunterMob.class,
            new RenderHeroMob("martianmanhunter", 0x004400));
        RenderingRegistry.registerEntityRenderingHandler(EntityShazamMob.class,
            new RenderHeroMob("shazam", 0xFFFF00));
        RenderingRegistry.registerEntityRenderingHandler(EntityDoctorStrangeClone.class,
            new RenderHeroMob("doctorstrange", 0x003388));
    }
}
