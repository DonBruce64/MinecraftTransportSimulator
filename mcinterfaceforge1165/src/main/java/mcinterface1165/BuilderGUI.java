package mcinterface1165;

import org.lwjgl.glfw.GLFW;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.fonts.TextInputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;

/**
 * Builder for MC GUI classes.  Created when {@link InterfaceClient#setActiveGUI(AGUIBase)}}
 * is called to open a GUI.  This builer is purely to handle input forwarding and game pause
 * requests and does no actual rendering as that's left for non-GUI generic rendering code.
 *
 * @author don_bruce
 */
public class BuilderGUI extends Screen {
    private GUIComponentButton lastButtonClicked;
    private int lastKeycodePresed;

    /**
     * Current gui we are built around.
     **/
    public final AGUIBase gui;

    public BuilderGUI(AGUIBase gui) {
        super(new StringTextComponent(""));
        this.gui = gui;
    }

    /**
     * This is called by the main MC system for click events.  We override it here to check
     * to see if we have clicked any of the registered components.  If so, we fire the appropriate
     * event for those components.  If we click something, we don't check any other components as
     * that could result in us being in a transition state when doing checks.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        //FIXME make sure that doubles are same units as ints as before.
        for (AGUIComponent component : gui.components) {
            if (component instanceof GUIComponentButton) {
                GUIComponentButton button = (GUIComponentButton) component;
                if (button.canClick((int) mouseX, (int) mouseY)) {
                    minecraft.getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    button.onClicked(mouseX <= button.constructedX + button.width / 2);
                    lastButtonClicked = button;
                    return true;
                }
            }
        }
        for (AGUIComponent component : gui.components) {
            if (component instanceof GUIComponentTextBox) {
                ((GUIComponentTextBox) component).updateFocus((int) mouseX, (int) mouseY);
            }
        }
        return false;
    }

    /**
     * This is called by the main MC system for click events.  We override it here to tell
     * the last selector we clicked, if any, that the mouse has been released.  This allows
     * the selector to resume to it's "resting" state.  This is dependent on the selector code;
     * some selectors may not do anything with this action.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (lastButtonClicked != null) {
            lastButtonClicked.onReleased();
            lastButtonClicked = null;
        }
        return true;
    }

    /**
     * This is called by the main MC system for keyboard events.  We Override it here to check
     * to forward the inputs to focused textBoxes for further processing.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!super.keyPressed(keyCode, scanCode, modifiers)) {
            lastKeycodePresed = keyCode;
            for (AGUIComponent component : gui.components) {
                if (component instanceof GUIComponentTextBox) {
                    GUIComponentTextBox textBox = (GUIComponentTextBox) component;
                    if (textBox.focused) {
                        //If we did a paste from the clipboard, we need to replace everything in the box.
                        //Otherwise, just send the char for further processing.
                        if (isPaste(keyCode)) {
                            textBox.setText(TextInputUtil.getClipboardContents(minecraft));
                        } else {
                            char key = 0;
                            switch (keyCode) {
                                case GLFW.GLFW_KEY_BACKSPACE:
                                    textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.BACKSPACE);
                                    continue;
                                case GLFW.GLFW_KEY_DELETE:
                                    textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.DELETE);
                                    continue;
                                case GLFW.GLFW_KEY_LEFT:
                                    textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.LEFT);
                                    continue;
                                case GLFW.GLFW_KEY_RIGHT:
                                    textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.RIGHT);
                                    continue;
                                default:
                                    textBox.handleKeyTyped(key, keyCode, null);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This is called by the main MC system for chars being entered via the keyboard.
     * Only text will be here, so don't bother with event checks.
     */
    @Override
    public boolean charTyped(char key, int modifiers) {
        //FIXME does this run after the prior method?  If so, okay, if not, we need to change something.
        if (!super.charTyped(key, modifiers)) {
            for (AGUIComponent component : gui.components) {
                if (component instanceof GUIComponentTextBox) {
                    GUIComponentTextBox textBox = (GUIComponentTextBox) component;
                    if (textBox.focused) {
                        textBox.handleKeyTyped(key, lastKeycodePresed, null);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        //Forward close event as this comes from an ESC key that we don't see.
        //Need to check if GUI is active in case we get multiple events.
        if (AGUIBase.activeGUIs.contains(gui)) {
            gui.close();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return gui.pauseOnOpen();
    }
}
