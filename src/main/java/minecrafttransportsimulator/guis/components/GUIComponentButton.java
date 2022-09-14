package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableObject;

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
    public final ColorRGB textColor;
    protected RenderableObject renderable2;
    protected RenderableObject renderable3;

    /**
     * A Simple button with a rendered string in grey and center-aligned.
     **/
    public GUIComponentButton(int x, int y, int width, int height, String text) {
        this(x, y, width, height, text, true, ColorRGB.DARK_GRAY, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, DEFAULT_BUTTON_SECTION_WIDTH, DEFAULT_BUTTON_SECTION_HEIGHT);
    }

    /**
     * A button made to render with item slots.  Renders the set item size, and renders the item slot texture off the "Inventory" gui if the final parameter is true.
     **/
    public GUIComponentButton(int x, int y, boolean renderBackground) {
        this(x, y, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE, renderBackground ? 194 : 0, 0, renderBackground ? ITEM_BUTTON_SIZE : 0, renderBackground ? ITEM_BUTTON_SIZE : 0);
    }

    /**
     * A button without texture or text.  Useful when you want a button for something that needs to render as another component.
     **/
    public GUIComponentButton(int x, int y, int width, int height) {
        this(x, y, width, height, "", true, ColorRGB.DARK_GRAY, 0, 0, 0, 0);
    }

    /**
     * A button made to render with custom button textures.  Does not render font, but does provide additional parameters for the size of the button.
     **/
    public GUIComponentButton(int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        this(x, y, width, height, "", true, ColorRGB.DARK_GRAY, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
    }

    /**
     * A complex button with custom height, text alignment, and text color.
     */
    public GUIComponentButton(int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor, boolean renderBackground) {
        this(x, y, width, height, text, centeredText, textColor, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, renderBackground ? DEFAULT_BUTTON_SECTION_WIDTH : 0, renderBackground ? DEFAULT_BUTTON_SECTION_HEIGHT : 0);
    }

    /**
     * A fully-customizable button with custom texture alignment and font color.  Note that making the width or the height of the texture section 0 will result in no texture being rendered.
     **/
    private GUIComponentButton(int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
        this.textPosition.set(centeredText ? position.x + width / 2 : position.x, position.y - (height - 8) / 2, textPosition.z);
        this.text = text;
        this.centeredText = centeredText;
        this.textColor = textColor;
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
            if (renderable == null) {
                for (int i = 0; i < 3; ++i) {
                    int textureUStart = textureYOffset + i * textureSectionHeight;

                    FloatBuffer buffer = FloatBuffer.allocate(3 * 8 * 6);
                    //Left border.
                    gui.addRenderToBuffer(buffer, 0, 0, DEFAULT_BUTTON_SECTION_BORDER, height, textureXOffset, textureUStart, textureXOffset + DEFAULT_BUTTON_SECTION_BORDER, textureUStart + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
                    //Center stretched segment.
                    gui.addRenderToBuffer(buffer, DEFAULT_BUTTON_SECTION_BORDER, 0, width - 2 * DEFAULT_BUTTON_SECTION_BORDER, height, textureXOffset + DEFAULT_BUTTON_SECTION_BORDER, textureUStart, textureXOffset + textureSectionWidth - DEFAULT_BUTTON_SECTION_BORDER, textureUStart + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
                    //Right border.
                    gui.addRenderToBuffer(buffer, width - DEFAULT_BUTTON_SECTION_BORDER, 0, DEFAULT_BUTTON_SECTION_BORDER, height, textureXOffset + textureSectionWidth - DEFAULT_BUTTON_SECTION_BORDER, textureUStart, textureXOffset + textureSectionWidth, textureUStart + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
                    buffer.flip();

                    if (i == 0) {
                        renderable = new RenderableObject("gui_button_disabled", gui.getTexture(), ColorRGB.WHITE, buffer, false);
                    } else if (i == 1) {
                        renderable2 = new RenderableObject("gui_button_normal", gui.getTexture(), ColorRGB.WHITE, buffer, false);
                    } else {
                        renderable3 = new RenderableObject("gui_button_highlight", gui.getTexture(), ColorRGB.WHITE, buffer, false);
                    }
                }
            }

            if (enabled) {
                if (isMouseInBounds(mouseX, mouseY)) {//Highlighted
                    renderable3.disableLighting = renderBright || ignoreGUILightingState;
                    renderable3.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
                    renderable3.transform.setTranslation(position);
                    renderable3.render();
                } else {//Normal
                    renderable2.disableLighting = renderBright || ignoreGUILightingState;
                    renderable2.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
                    renderable2.transform.setTranslation(position);
                    renderable2.render();
                }
            } else {//Disabled
                renderable.disableLighting = renderBright || ignoreGUILightingState;
                renderable.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
                renderable.transform.setTranslation(position);
                renderable.render();
            }
        }
    }

    @Override
    public void renderText(boolean renderTextLit) {
        RenderText.drawText(text, null, textPosition, textColor, centeredText ? TextAlignment.CENTERED : TextAlignment.LEFT_ALIGNED, 1.0F, false, 0, renderTextLit || ignoreGUILightingState);
    }
}
