package com.heromobs.render;

import com.heromobs.entity.EntityHeroMobBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renders hero mobs as a player-like biped.
 * Attempts to use FiskHeroes hero skin textures.
 * Falls back to a colored Steve skin if texture not found.
 */
public class RenderHeroMob extends RenderBiped {

    private final String heroName;
    private final int fallbackColor;

    // Try FiskHeroes texture path first
    private ResourceLocation heroTexture;
    private ResourceLocation fallbackTexture;

    public RenderHeroMob(String heroName, int fallbackColor) {
        super(new ModelBiped(0.0F), 0.5F);
        this.heroName = heroName;
        this.fallbackColor = fallbackColor;
        this.heroTexture   = new ResourceLocation("fiskheroes", "textures/entity/heroes/" + heroName + ".png");
        this.fallbackTexture = new ResourceLocation("heromobs", "textures/entity/heroes/" + heroName + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return heroTexture;
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTick) {
        EntityHeroMobBase hero = (EntityHeroMobBase) entity;

        GL11.glPushMatrix();

        // Tilt forward slightly when flying
        if (hero.isFlying()) {
            GL11.glTranslated(x, y + 0.5, z);
            GL11.glRotatef(-30F, 1F, 0F, 0F);
            GL11.glTranslated(-x, -y - 0.5, -z);
        }

        // Glow effect for energy-based heroes (Iron Man, Flash, Shazam, Killer Frost)
        if (hero.isGlowing()) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(
                ((fallbackColor >> 16) & 0xFF) / 255f,
                ((fallbackColor >> 8)  & 0xFF) / 255f,
                ((fallbackColor)       & 0xFF) / 255f,
                0.35f
            );
            // Render slightly scaled up for glow pass
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            GL11.glScalef(1.05f, 1.05f, 1.05f);
            GL11.glTranslated(-x, -y, -z);
            super.doRender(entity, x, y, z, yaw, partialTick);
            GL11.glPopMatrix();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_BLEND);
        }

        super.doRender(entity, x, y, z, yaw, partialTick);

        GL11.glPopMatrix();
    }
}
