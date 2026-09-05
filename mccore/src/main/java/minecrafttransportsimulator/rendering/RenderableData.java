package minecrafttransportsimulator.rendering;

import java.util.Locale;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Class designed to represent rendering data.  This contains {@link RenderableVertices},
 * for rendering data, a {@link TransformationMatrix} for positional data, and some additional
 * properties for rendering state.  Each rendering instance should have its own state.  However,
 * instances may share vertices to reduce RAM requirements.
 * <br>
 * Only set values on this object via the setter functions. Do not modify them directly.
 * The only exception is the {@link #transform} object, which is assumed to be modified
 * each render call and thus does no state-caching.
 *
 * @author don_bruce
 */
public class RenderableData {
    /**
     * The Global texture.  This contains all block/item textures for the game.  Used when rendering said blocks/items.
     **/
    public static final String GLOBAL_TEXTURE_NAME = "GLOBAL";

    public final TransformationMatrix transform = new TransformationMatrix();

    public final RenderableVertices vertexObject;
    public String texture;
    public final ColorRGB color = new ColorRGB();
    public float alpha = 1.0F;
    public int worldLightValue;
    public LightingMode lightingMode = LightingMode.NORMAL;
    public boolean enableBrightBlending;
    public boolean isTranslucent;
    /**Normalized offsets added to every texture coordinate at render time.**/
    public float textureOffsetU;
    public float textureOffsetV;
    /**Scale applied to texture coordinates before offsets.**/
    public float textureScaleU = 1.0F;
    public float textureScaleV = 1.0F;
    /**If true, texture coordinates outside 0-1 sample transparent pixels instead of wrapping.**/
    public boolean textureClampToTransparent;
    /**Draw order for renderers that batch by texture.  Higher values are rendered later and
     * receive a progressively stronger depth bias to separate coplanar texture layers.**/
    public int renderingOrder;

    private double lastWidthRadius;
    private double lastHeightRadius;
    private double lastDepthRadius;

    private boolean isTextureTranslucent;
    private boolean isTranslucentForced;
    private boolean changedSinceLastRender;

    public RenderableData(RenderableVertices vertexObject, String texture) {
        this.vertexObject = vertexObject;
        if (texture != null) {
            setTexture(texture);
        }
        setColor(ColorRGB.WHITE);
    }

    public RenderableData(RenderableVertices vertexObject) {
        this(vertexObject, null);
    }

    public void setTexture(String texture) {
        if (vertexObject.isErrorPlaceholder) {
            texture = AModelParser.MISSING_MODEL_TEXTURE;
        }
        if (texture == null ? this.texture != null : !texture.equals(this.texture)) {
            this.texture = texture;
            this.isTextureTranslucent = texture != null && (texture.toLowerCase(Locale.ROOT).contains(AModelParser.TRANSLUCENT_OBJECT_NAME) || texture.endsWith(GUIComponentCutout.LIT_SUFFIX));
            this.changedSinceLastRender = true;
            this.isTranslucent = isTranslucentForced || vertexObject.isTranslucent || isTextureTranslucent || alpha < 1.0;
        }
    }

    public void setAlpha(float alpha) {
        if (this.alpha != alpha) {
            this.alpha = alpha;
            this.changedSinceLastRender = true;
            this.isTranslucent = isTranslucentForced || vertexObject.isTranslucent || isTextureTranslucent || alpha < 1.0;
        }
    }

    public void setTransucentOverride() {
        if (!this.isTranslucentForced) {
            this.isTranslucentForced = true;
            this.isTranslucent = true;
            this.changedSinceLastRender = true;
        }
    }

    public void clearTranslucentOverride() {
        if (this.isTranslucentForced) {
            this.isTranslucentForced = false;
            this.isTranslucent = isTranslucentForced || vertexObject.isTranslucent || isTextureTranslucent || alpha < 1.0;
            this.changedSinceLastRender = true;
        }
    }

    public void setColor(ColorRGB color) {
        if (!this.color.equals(color)) {
            this.color.setTo(color);
            this.changedSinceLastRender = true;
        }
    }

    public void setLightValue(int worldLightValue) {
        if (this.worldLightValue != worldLightValue) {
            this.worldLightValue = worldLightValue;
            this.changedSinceLastRender = true;
        }
    }

    public void setLightMode(LightingMode mode) {
        if (this.lightingMode != mode) {
            this.lightingMode = mode;
            this.changedSinceLastRender = true;
        }
    }

    public void setBlending(boolean enableBrightBlending) {
        if (this.enableBrightBlending != enableBrightBlending) {
            this.enableBrightBlending = enableBrightBlending;
            this.changedSinceLastRender = true;
        }
    }

    public void setTextureOffset(float textureOffsetU, float textureOffsetV) {
        if (this.textureOffsetU != textureOffsetU || this.textureOffsetV != textureOffsetV) {
            this.textureOffsetU = textureOffsetU;
            this.textureOffsetV = textureOffsetV;
            this.changedSinceLastRender = true;
        }
    }

    public void setTextureScale(float textureScaleU, float textureScaleV) {
        if (this.textureScaleU != textureScaleU || this.textureScaleV != textureScaleV) {
            this.textureScaleU = textureScaleU;
            this.textureScaleV = textureScaleV;
            this.changedSinceLastRender = true;
        }
    }

    public void setTextureClampToTransparent() {
        if (!this.textureClampToTransparent) {
            this.textureClampToTransparent = true;
            this.changedSinceLastRender = true;
        }
    }

    public void setRenderingOrder(int renderingOrder) {
        if (this.renderingOrder != renderingOrder) {
            this.renderingOrder = renderingOrder;
            this.changedSinceLastRender = true;
        }
    }

    public void setBoxBounds(BoundingBox box, boolean wireframe) {
        if (box.widthRadius != lastWidthRadius || box.heightRadius != lastHeightRadius || box.depthRadius != lastDepthRadius) {
            vertexObject.setBoundingBox(box, wireframe);
            this.lastWidthRadius = box.widthRadius;
            this.lastHeightRadius = box.heightRadius;
            this.lastDepthRadius = box.depthRadius;
            this.changedSinceLastRender = true;
        }
    }

    public void render() {
        InterfaceManager.renderingInterface.renderVertices(this, changedSinceLastRender);
        //Mode 2 streams cached model vertices directly.  Keep the dirty state in that mode so a
        //later switch back to modes 0/1 cannot reuse a VBO baked with stale UV, alpha, or light data.
        if (!vertexObject.cacheVertices || ConfigSystem.client.renderingSettings.renderingMode.value != 2) {
            changedSinceLastRender = false;
        }
    }

    public void destroy() {
        InterfaceManager.renderingInterface.deleteVertices(this);
    }

    public static enum LightingMode {
        NORMAL(false, false),
        IGNORE_ORIENTATION_LIGHTING(true, false),
        IGNORE_WORLD_LIGHTING(false, true),
        IGNORE_ALL_LIGHTING(true, true);

        public final boolean disableTextureShadows;
        public final boolean disableWorldLighting;

        private LightingMode(boolean disableTextureShadows, boolean disableWorldLighting) {
            this.disableTextureShadows = disableTextureShadows;
            this.disableWorldLighting = disableWorldLighting;
        }
    }
}
