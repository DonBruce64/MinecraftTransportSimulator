package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * Custom label class.  Allows for batch rendering of text, and easier rendering of labels using
 * state variables rather than actual text boxes.  Also allows for linking with either a
 * {@link GUIComponentButton} or {@link GUIComponentTextBox} to avoid having to set visibility
 * and instead to use the visibility of those objects.
 *
 * @author don_bruce
 */
public class GUIComponentLabel extends AGUIComponent {
    public final ColorRGB color;
    public final String fontName;
    public final TextAlignment alignment;
    public final int wrapWidth;
    public final float scale;
    public final boolean autoScale;

    private GUIComponentButton button;
    private GUIComponentTextBox box;

    public GUIComponentLabel(int x, int y, ColorRGB color, String text) {
        this(x, y, color, text, TextAlignment.LEFT_ALIGNED, 1.0F, 0, null, false);
    }

    public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale) {
        this(x, y, color, text, alignment, scale, 0, null, false);
    }

    public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale, int wrapWidth) {
        this(x, y, color, text, alignment, scale, wrapWidth, null, false);
    }

    public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale, int wrapWidth, String fontName, boolean autoScale) {
        super(x, y, 0, 0);
        this.color = color;
        this.text = text;
        this.fontName = fontName;
        this.alignment = alignment;
        this.scale = scale;
        this.wrapWidth = wrapWidth;
        this.autoScale = autoScale;
    }

    /**
     * Sets the associated button for this class.  This will make
     * this label render only when the button is visible.
     * Returns this object for ease of constructing.
     */
    public GUIComponentLabel setButton(GUIComponentButton button) {
        this.button = button;
        return this;
    }

    /**
     * Sets the associated textBox for this class.  This will make
     * this label render only when the textBox is visible.
     * Returns this object for ease of constructing.
     */
    public GUIComponentLabel setBox(GUIComponentTextBox box) {
        this.box = box;
        return this;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        //Don't render anything for the label background.
    }

    @Override
    public void renderText(boolean renderTextLit) {
        if (button == null ? (box == null || box.visible) : button.visible) {
            RenderText.drawText(text, fontName, textPosition, color, alignment, scale, autoScale, wrapWidth, renderTextLit || ignoreGUILightingState);
        }
    }
}
