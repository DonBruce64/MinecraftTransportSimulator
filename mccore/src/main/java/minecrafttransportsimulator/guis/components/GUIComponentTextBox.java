package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * Custom text box class.  This class is designed to remove the need to check for clicking of
 * text boxes and to manually set their text, as well as the odd getter/setter methods
 * that MC's text boxes have that aren't in other components.  Provides a more streamlined
 * object this way.  This text box also allows for word-wrapping of text.  Something
 * sorely needed in the base text box rendering routines...
 * Note that the box's background is defined as a section on the currently-bound GUi texture, so keep this in mind.
 *
 * @author don_bruce
 */
public class GUIComponentTextBox extends GUIComponentCutout {
    private static final String VALID_SPECIAL_CHARS = "/*!@#$%^&*()\"{}_[]|\\?/<>,.- ";

    public ColorRGB fontColor;

    public boolean enabled = true;
    public boolean focused = false;

    private int textIndex;
    private final int maxTextLength;
    private String flashText;

    public GUIComponentTextBox(int x, int y, int width, String text) {
        this(x, y, width, 20, text, ColorRGB.WHITE, 32);
    }

    public GUIComponentTextBox(int x, int y, int width, int height, String text, ColorRGB fontColor, int maxTextLength) {
        this(x, y, width, height, text, fontColor, maxTextLength, AGUIBase.STANDARD_COLOR_WIDTH_OFFSET, AGUIBase.STANDARD_BLACK_HEIGHT_OFFSET, AGUIBase.STANDARD_COLOR_WIDTH, AGUIBase.STANDARD_COLOR_HEIGHT);
    }

    public GUIComponentTextBox(int x, int y, int width, int height, String text, ColorRGB fontColor, int maxTextLength, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
        this.textPosition.set(position.x + 4, position.y - (height >= 20 ? 5 : 1 + height / 10), textPosition.z);
        this.fontColor = fontColor;
        this.maxTextLength = maxTextLength;
        setText(text);
    }

    /**
     * Updates the focused property depending on mouse position and state.  Should be called whenever
     * the mouse is clicked to focus the text box and allow the GUI to send user input to it.
     */
    public void updateFocus(int mouseX, int mouseY) {
        focused = visible && enabled && isMouseInBounds(mouseX, mouseY);
        if (focused && text == null) {
            text = "";
            flashText = "_";
        }
    }

    /**
     * Handles the current key input.  Depending on the key, this will either add
     * the typed character to the text, delete a character, or change the current
     * cursor position.  Note that keyCode is one of the pre-defined {@link TextBoxControlKey}s.
     * This allows for consistent handling of inputs locally, and makes it up to the caller of
     * this function to decide what inputs constitute the control keys.  The main reason
     * for this is that it prevents the need to call in any specific LWJGL libraries that may
     * or may not change for the heck of it.
     */
    public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
        //Handle control operation, or basic key typed.
        if (control != null) {
            if (control.equals(TextBoxControlKey.BACKSPACE)) {
                if (textIndex != 0) {
                    //Remove the char before the current position.
                    text = text.substring(0, textIndex - 1) + text.substring(textIndex);
                    --textIndex;
                    if (isTextValid(text)) {
                        handleTextChange();
                    }
                }
            } else if (control.equals(TextBoxControlKey.DELETE)) {
                if (textIndex != text.length()) {
                    //Remove the char at the current position.
                    text = text.substring(0, textIndex) + text.substring(textIndex + 1);
                    if (isTextValid(text)) {
                        handleTextChange();
                    }
                }
            } else if (control.equals(TextBoxControlKey.LEFT)) {
                if (textIndex > 0) {
                    --textIndex;
                }
            } else if (control.equals(TextBoxControlKey.RIGHT)) {
                if (textIndex < text.length()) {
                    ++textIndex;
                }
            }
        } else if (Character.isLetterOrDigit(typedChar) || VALID_SPECIAL_CHARS.contains(Character.toString(typedChar))) {
            if (text.length() < maxTextLength) {
                text = text.substring(0, textIndex) + typedChar + text.substring(textIndex);
                ++textIndex;
                if (isTextValid(text)) {
                    handleTextChange();
                }
            }
        }

        //Update flashText;
        if (text.isEmpty()) {
            flashText = "_";
        } else if (textIndex == text.length()) {
            flashText = text + "_";
        } else {
            flashText = text.substring(0, textIndex) + "_" + text.substring(textIndex + 1);
        }
    }

    /**
     * Returns true if this text is valid for this text box.  This won't prevent invalid text from being entered,
     * but will prevent {@link #handleTextChange()} from being called if the text is invalid.
     */
    public boolean isTextValid(String newText) {
        return true;
    }

    /**
     * Gets the current text of this component.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text for this component.  Trimming it to meet the maxTextLength
     * parameter if required.  Also updates flashText to ensure proper rendering.
     * This also resets the pointer to the end of the passed-in string to prevent
     * an old reference.
     */
    public void setText(String newText) {
        if (newText.length() > maxTextLength) {
            text = newText.substring(0, maxTextLength);
        } else {
            text = newText;
        }
        textIndex = text.length();

        //Set flash text for cursor.  Should flash at current position.
        if (text.isEmpty()) {
            flashText = "_";
        } else if (textIndex == 0) {
            flashText = "_" + text.substring(1);
        } else if (textIndex == text.length()) {
            flashText = text + "_";
        } else {
            flashText = text.substring(0, textIndex) + text.substring(textIndex + 1);
        }
    }

    /**
     * Called when text is changed in this text box.  This happens only after a keypress
     * changes the text, and {@link #isTextValid(String)} returns true.  This ensures that
     * only valid text changes are processed.  By default this method does nothing.
     */
    public void handleTextChange() {
    }

    @Override
    public void renderText(boolean renderTextLit) {
        //If this box is less than 20px high, center the text.
        //Otherwise, render it at the top aligned.
        if (enabled) {
            if (focused && AGUIBase.inClockPeriod(20, 10)) {
                RenderText.drawText(flashText, null, textPosition, fontColor, TextAlignment.LEFT_ALIGNED, 1.0F, false, width, renderTextLit || ignoreGUILightingState);
            } else {
                RenderText.drawText(text, null, textPosition, fontColor, TextAlignment.LEFT_ALIGNED, 1.0F, false, width, renderTextLit || ignoreGUILightingState);
            }
        } else {
            RenderText.drawText(text, null, textPosition, ColorRGB.GRAY, TextAlignment.LEFT_ALIGNED, 1.0F, false, width, renderTextLit || ignoreGUILightingState);
        }
    }

    /**
     * Enums for control keys of this text box.  Send these in when a specific action needs to occur other
     * than adding text.
     */
    public enum TextBoxControlKey {
        BACKSPACE,
        DELETE,
        LEFT,
        RIGHT
    }
}
