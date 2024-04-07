package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Class that renders a cut-out area of the currently bound texture.  This is cleaner than doing manual
 * rendering in the draw calls, and allows for standardized component batch-rendering.
 *
 * @author don_bruce
 */
public class GUIComponentCutout extends AGUIComponent {
    public static final String NORMAL_SUFFIX = ".png";
    public static final String LIT_SUFFIX = "_lit.png";

    //Texture variables.
    protected RenderableData renderableL;
    private boolean renderFullScreen;
    public int textureXOffset;
    public int textureYOffset;
    public int textureSectionWidth;
    public int textureSectionHeight;

    /**
     * Standard constructor for a 1:1 rendering.
     **/
    public GUIComponentCutout(AGUIBase gui, int x, int y, int width, int height) {
        this(gui, x, y, width, height, 0, 0, width, height);
        this.renderFullScreen = true;
    }

    /**
     * Shortened constructor for rendering across whole screen.
     **/
    public GUIComponentCutout(AGUIBase gui, int x, int y, int width, int height, int textureXOffset, int textureYOffset) {
        this(gui, x, y, width, height, textureXOffset, textureYOffset, width, height);
    }

    /**
     * Expanded constructor for 1:x rendering.  This allows the rendered texture component to be a different size than the texture.
     **/
    public GUIComponentCutout(AGUIBase gui, int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(x, y, width, height);
        this.textureXOffset = textureXOffset;
        this.textureYOffset = textureYOffset;
        this.textureSectionWidth = textureSectionWidth;
        this.textureSectionHeight = textureSectionHeight;
        RenderableVertices vertexObject = RenderableVertices.createSprite(1, null, null);
        renderable = new RenderableData(vertexObject, gui.getTexture());
        if (gui.getTexture() != null) {
            renderableL = new RenderableData(vertexObject, gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX));
        }
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        RenderableData currentRenderable = renderLitTexture ? renderableL : renderable;
        if (currentRenderable.isTranslucent == blendingEnabled) {
            currentRenderable.setTexture(renderLitTexture ? gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX) : gui.getTexture());
            if (renderFullScreen) {
                currentRenderable.vertexObject.setSpriteProperties(0, 0, 0, width, height, 0, 0, 1, 1);
            } else {
                currentRenderable.vertexObject.setSpriteProperties(0, 0, 0, width, height, textureXOffset / (float) gui.getTextureWidth(), textureYOffset / (float) gui.getTextureHeight(), (textureXOffset + textureSectionWidth) / (float) gui.getTextureWidth(), (textureYOffset + textureSectionHeight) / (float) gui.getTextureHeight());
            }
            currentRenderable.transform.setTranslation(position);
            currentRenderable.setLightValue(gui.worldLightValue);
            currentRenderable.setLightMode(renderBright || ignoreGUILightingState ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.IGNORE_ORIENTATION_LIGHTING);
            currentRenderable.render();
        }
    }
}
