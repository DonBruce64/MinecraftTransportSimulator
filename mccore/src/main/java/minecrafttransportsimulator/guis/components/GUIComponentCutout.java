package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderableObject;

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
    protected RenderableObject renderableL;
    private boolean renderFullScreen;
    public int textureXOffset;
    public int textureYOffset;
    public int textureSectionWidth;
    public int textureSectionHeight;

    /**
     * Standard constructor for a 1:1 rendering.
     **/
    public GUIComponentCutout(int x, int y, int width, int height) {
        this(x, y, width, height, 0, 0, width, height);
        this.renderFullScreen = true;
    }

    /**
     * Shortened constructor for rendering across whole screen.
     **/
    public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset) {
        this(x, y, width, height, textureXOffset, textureYOffset, width, height);
    }

    /**
     * Expanded constructor for 1:x rendering.  This allows the rendered texture component to be a different size than the texture.
     **/
    public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(x, y, width, height);
        this.textureXOffset = textureXOffset;
        this.textureYOffset = textureYOffset;
        this.textureSectionWidth = textureSectionWidth;
        this.textureSectionHeight = textureSectionHeight;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (renderable == null || !renderable.texture.equals(gui.getTexture())) {
            renderable = new RenderableObject("gui_cutout", gui.getTexture(), ColorRGB.WHITE, FloatBuffer.allocate(8 * 6), false);
            renderableL = new RenderableObject("gui_cutout", gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX), ColorRGB.WHITE, FloatBuffer.allocate(8 * 6), false);
        }
        RenderableObject currentRenderable = renderLitTexture ? renderableL : renderable;
        if (currentRenderable.isTranslucent == blendingEnabled) {
            if (currentRenderable.vertices.position() == 0) {
                if (renderFullScreen) {
                    gui.addRenderToBuffer(currentRenderable.vertices, 0, 0, width, height, 0, 0, width, height, width, height);
                } else {
                    gui.addRenderToBuffer(currentRenderable.vertices, 0, 0, width, height, textureXOffset, textureYOffset, textureXOffset + textureSectionWidth, textureYOffset + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
                }
                currentRenderable.vertices.flip();
            }
            currentRenderable.transform.setTranslation(position);
            currentRenderable.ignoreWorldShading = true;
            currentRenderable.worldLightValue = gui.worldLightValue;
            currentRenderable.disableLighting = renderBright || ignoreGUILightingState;
            currentRenderable.render(null);
        }
    }
}
