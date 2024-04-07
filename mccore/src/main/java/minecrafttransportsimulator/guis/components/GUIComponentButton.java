package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Custom button class.  This allows for a custom button texture, as well as a cleaner constructor.
 * It also allows us to cut out a few MC methods from their own button class and use our own.
 * The simplest constructor will create a button at the point specified with a height of 20
 * and centered text.  The more complex constructor will create it with the specified height
 * and gives the choice of centered text or not.  The most complex constructor gives all those
 * options, as well as allowing the selection of where on the texture sheet to render the button.
 *
 * @author don_bruce
 */
public abstract class GUIComponentButton extends GUIComponentCutout {
    private static final int DEFAULT_BUTTON_SECTION_WIDTH = 100;
    private static final int DEFAULT_BUTTON_SECTION_HEIGHT = 20;
    private static final int DEFAULT_BUTTON_SECTION_BORDER = 6;
    private static final int DEFAULT_BUTTON_SECTION_WIDTH_OFFSET = 0;
    private static final int DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET = 196;
    public static final int ITEM_BUTTON_SIZE = 18;

    public final boolean centeredText;

    public boolean enabled = true;
    public boolean isDynamicTexture;
    public final ColorRGB textColor;
    protected RenderableData renderable2;
    protected RenderableData renderable2L;
    protected RenderableData renderable3;
    protected RenderableData renderable3L;

    /**
     * A Simple button with a rendered string in grey and center-aligned.
     **/
    public GUIComponentButton(AGUIBase gui, int x, int y, int width, int height, String text) {
        this(gui, x, y, width, height, text, true, ColorRGB.DARK_GRAY, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, DEFAULT_BUTTON_SECTION_WIDTH, DEFAULT_BUTTON_SECTION_HEIGHT);
    }

    /**
     * A button made to render with item slots.  Renders the set item size, and renders the item slot texture off the "Inventory" gui if the final parameter is true.
     **/
    public GUIComponentButton(AGUIBase gui, int x, int y, boolean renderBackground) {
        this(gui, x, y, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE, renderBackground ? 194 : 0, 0, renderBackground ? ITEM_BUTTON_SIZE : 0, renderBackground ? ITEM_BUTTON_SIZE : 0);
    }

    /**
     * A button without texture or text.  Useful when you want a button for something that needs to render as another component.
     **/
    public GUIComponentButton(AGUIBase gui, int x, int y, int width, int height) {
        this(gui, x, y, width, height, "", true, ColorRGB.DARK_GRAY, 0, 0, 0, 0);
    }

    /**
     * A button made to render with custom button textures.  Does not render font, but does provide additional parameters for the size of the button.
     **/
    public GUIComponentButton(AGUIBase gui, int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        this(gui, x, y, width, height, "", true, ColorRGB.DARK_GRAY, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
    }

    /**
     * A complex button with custom height, text alignment, and text color.
     */
    public GUIComponentButton(AGUIBase gui, int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor, boolean renderBackground) {
        this(gui, x, y, width, height, text, centeredText, textColor, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, renderBackground ? DEFAULT_BUTTON_SECTION_WIDTH : 0, renderBackground ? DEFAULT_BUTTON_SECTION_HEIGHT : 0);
    }

    /**
     * A fully-customizable button with custom texture alignment and font color.  Note that making the width or the height of the texture section 0 will result in no texture being rendered.
     **/
    private GUIComponentButton(AGUIBase gui, int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(gui, x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
        this.textPosition.set(centeredText ? position.x + width / 2 : position.x, position.y - (height - 8) / 2, textPosition.z);
        this.text = text;
        this.centeredText = centeredText;
        this.textColor = textColor;
        if (textureSectionWidth != 0 && textureSectionHeight != 0) {
            for (int i = 0; i < (isDynamicTexture ? 1 : 3); ++i) {
                int textureHeightStart = textureYOffset + i * textureSectionHeight;
                RenderableVertices vertexObject = RenderableVertices.createSprite(3, null, null);
                if (i == 0) {
                    setOffsetVertices(gui, textureHeightStart, vertexObject);
                    renderable = new RenderableData(vertexObject, gui.getTexture());
                    renderableL = new RenderableData(vertexObject, gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX));
                    renderableL.setTransucentOverride();
                } else if (i == 1) {
                    setOffsetVertices(gui, textureHeightStart, vertexObject);
                    renderable2 = new RenderableData(vertexObject, gui.getTexture());
                    renderable2L = new RenderableData(vertexObject, gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX));
                    renderable2L.setTransucentOverride();
                } else {
                    setOffsetVertices(gui, textureHeightStart, vertexObject);
                    renderable3 = new RenderableData(vertexObject, gui.getTexture());
                    renderable3L = new RenderableData(vertexObject, gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX));
                    renderable3L.setTransucentOverride();
                }
            }
        }
    }

    /**
     * Returns true if the mouse can click this button, given the mouse
     * position and current button state.  Used to know if we need to call
     * {@link #onClicked(boolean)} to do clicking actions.
     */
    public boolean canClick(int mouseX, int mouseY) {
        return visible && enabled && isMouseInBounds(mouseX, mouseY);
    }

    /**
     * Called when this button is clicked by a mouse.  The button will
     * already have been verified to be both visible and enabled at
     * this point, so this click action is a true action.  Either extend
     * this class and override this method, or simply use generics in a
     * constructor for simple tasks.  The purpose of this is to prevent
     * the need to check every button to see which one was clicked like
     * MC does.
     */
    public abstract void onClicked(boolean leftSide);

    /**
     * Called when the mouse that clicked this button is released.  At this point
     * the mouse may no longer be over the button, but it can be assured that
     * the last thing the user clicked was this button.  Use this method for
     * things that need to have a maintained state while clicked rather than
     * have a single momentary action.
     */
    public void onReleased() {
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (textureSectionWidth != 0 && textureSectionHeight != 0) {
            RenderableData currentRenderable;
            if (enabled && !isDynamicTexture) {
                if (isMouseInBounds(mouseX, mouseY)) {//Highlighted
                    currentRenderable = renderLitTexture ? renderable3L : renderable3;
                } else {//Normal
                    currentRenderable = renderLitTexture ? renderable2L : renderable2;
                }
            } else {//Disabled, or dynamic texture.
                currentRenderable = renderLitTexture ? renderableL : renderable;
                if (isDynamicTexture) {
                    setOffsetVertices(gui, textureYOffset, currentRenderable.vertexObject);
                }
            }

            if (currentRenderable.isTranslucent == blendingEnabled) {
                currentRenderable.setTexture(renderLitTexture ? gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX) : gui.getTexture());
                currentRenderable.setLightValue(gui.worldLightValue);
                currentRenderable.setLightMode(renderBright || ignoreGUILightingState ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.IGNORE_ORIENTATION_LIGHTING);
                currentRenderable.transform.setTranslation(position);
                currentRenderable.render();
            }
        }
    }

    @Override
    public void renderText(boolean renderTextLit, int worldLightValue) {
        RenderText.drawText(text, null, textPosition, textColor, centeredText ? TextAlignment.CENTERED : TextAlignment.LEFT_ALIGNED, 1.0F, false, 0, renderTextLit || ignoreGUILightingState, worldLightValue);
    }

    private void setOffsetVertices(AGUIBase gui, int textureHeightStart, RenderableVertices vertexObject) {
        //Left border.
        vertexObject.setSpriteProperties(0, 0, 0, DEFAULT_BUTTON_SECTION_BORDER, height, textureXOffset / (float) gui.getTextureWidth(), textureHeightStart / (float) gui.getTextureHeight(), (textureXOffset + DEFAULT_BUTTON_SECTION_BORDER) / (float) gui.getTextureWidth(), (textureHeightStart + textureSectionHeight) / (float) gui.getTextureHeight());
        //Center stretched segment.
        vertexObject.setSpriteProperties(1, DEFAULT_BUTTON_SECTION_BORDER, 0, width - 2 * DEFAULT_BUTTON_SECTION_BORDER, height, (textureXOffset + DEFAULT_BUTTON_SECTION_BORDER) / (float) gui.getTextureWidth(), textureHeightStart / (float) gui.getTextureHeight(), (textureXOffset + textureSectionWidth - DEFAULT_BUTTON_SECTION_BORDER) / (float) gui.getTextureWidth(), (textureHeightStart + textureSectionHeight) / (float) gui.getTextureHeight());
        //Right border.
        vertexObject.setSpriteProperties(2, width - DEFAULT_BUTTON_SECTION_BORDER, 0, DEFAULT_BUTTON_SECTION_BORDER, height, (textureXOffset + textureSectionWidth - DEFAULT_BUTTON_SECTION_BORDER) / (float) gui.getTextureWidth(), textureHeightStart / (float) gui.getTextureHeight(), (textureXOffset + textureSectionWidth) / (float) gui.getTextureWidth(), (textureHeightStart + textureSectionHeight) / (float) gui.getTextureHeight());
    }
}
