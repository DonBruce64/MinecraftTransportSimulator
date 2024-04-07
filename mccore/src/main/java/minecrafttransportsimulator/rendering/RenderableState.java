package minecrafttransportsimulator.rendering;

import java.util.Locale;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;

/**
 * Class designed to represent the state of a {@link RenderableObject} object.
 * This class links to the object in question and is responsible for maintaining
 * the state of the object.  Only set values on this object via the setter functions;
 * do not modify them directly.
 *
 * @author don_bruce
 */
public class RenderableState {
    /**
     * The Global texture.  This contains all block/item textures for the game.  Used when rendering said blocks/items.
     **/
    public static final String GLOBAL_TEXTURE_NAME = "GLOBAL";

    public boolean changedSinceLastRender;
    public String texture;
    public final ColorRGB color;
    public float alpha;
    public boolean isTranslucent;
    public int worldLightValue;
    public LightingState lighting;
    public boolean enableBrightBlending;

    public RenderableState() {
        this.color = new ColorRGB();
        color.setTo(ColorRGB.WHITE);
        this.alpha = 1.0F;
    }

    public void setTexture(String texture) {
        if (!texture.equals(this.texture)) {
            this.texture = texture;
             texture != null && (texture.toLowerCase(Locale.ROOT).contains(AModelParser.TRANSLUCENT_OBJECT_NAME) || texture.endsWith(GUIComponentCutout.LIT_SUFFIX)
            this.changedSinceLastRender = true;
        }
    }

    public void setAlpha(float alpha) {
        if (this.alpha != alpha) {
            this.alpha = alpha;
            this.changedSinceLastRender = true;
        }
    }

    public void setColor(ColorRGB color) {
        if (!this.color.equals(color)) {
            this.color.setTo(color);
            this.changedSinceLastRender = true;
        }
    }

    public void setLighting(int worldLightValue, boolean disableLighting, boolean ignoreWorldShading) {
        if (this.worldLightValue != worldLightValue) {
            this.worldLightValue = worldLightValue;
            this.changedSinceLastRender = true;
        }
        if (this.disableLighting != disableLighting) {
            this.disableLighting = disableLighting;
            this.changedSinceLastRender = true;
        }
        if (this.ignoreWorldShading != ignoreWorldShading) {
            this.ignoreWorldShading = ignoreWorldShading;
            this.changedSinceLastRender = true;
        }
    }

    public void setBlending(boolean enableBrightBlending) {
        if (this.enableBrightBlending != enableBrightBlending) {
            this.enableBrightBlending = enableBrightBlending;
            this.changedSinceLastRender = true;
        }
    }

    public static enum LightingState {
        NORMAL,
        IGNORE_ORIENTATION_LIGHTING,
        IGNORE_WORLD_LIGHTING,
        IGNORE_ALL_LIGHTING;
    }
}
