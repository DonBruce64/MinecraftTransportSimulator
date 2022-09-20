package mcinterface1165;

import java.io.IOException;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.java.games.input.Keyboard;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SoundEvents;

/**
 * Builder for MC GUI classes.  Created when {@link InterfaceClient#setActiveGUI(AGUIBase)}}
 * is called to open a GUI.  This builer is purely to handle input forwarding and game pause
 * requests and does no actual rendering as that's left for non-GUI generic rendering code.
 *
 * @author don_bruce
 */
public class BuilderGUI extends Screen {
    private GUIComponentButton lastButtonClicked;

    /**
     * Current gui we are built around.
     **/
    public final AGUIBase gui;

    public BuilderGUI(AGUIBase gui) {
        this.gui = gui;
    }

    /**
     * This is called by the main MC system for click events.  We override it here to check
     * to see if we have clicked any of the registered components.  If so, we fire the appropriate
     * event for those components.  If we click something, we don't check any other components as
     * that could result in us being in a transition state when doing checks.
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (AGUIComponent component : gui.components) {
            if (component instanceof GUIComponentButton) {
                GUIComponentButton button = (GUIComponentButton) component;
                if (button.canClick(mouseX, mouseY)) {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    button.onClicked(mouseX <= button.constructedX + button.width / 2);
                    lastButtonClicked = button;
                    return;
                }
            }
        }
        for (AGUIComponent component : gui.components) {
            if (component instanceof GUIComponentTextBox) {
                ((GUIComponentTextBox) component).updateFocus(mouseX, mouseY);
            }
        }
    }

    /**
     * This is called by the main MC system for click events.  We override it here to tell
     * the last selector we clicked, if any, that the mouse has been released.  This allows
     * the selector to resume to it's "resting" state.  This is dependent on the selector code;
     * some selectors may not do anything with this action.
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int actionType) {
        if (lastButtonClicked != null) {
            lastButtonClicked.onReleased();
            lastButtonClicked = null;
        }
    }

    /**
     * This is called by the main MC system for keyboard events.  We Override it here to check
     * to forward the inputs to focused textBoxes for further processing.
     */
    @Override
    protected void keyTyped(char key, int keyCode) throws IOException {
        super.keyTyped(key, keyCode);
        for (AGUIComponent component : gui.components) {
            if (component instanceof GUIComponentTextBox) {
                GUIComponentTextBox textBox = (GUIComponentTextBox) component;
                if (textBox.focused) {
                    //If we did a paste from the clipboard, we need to replace everything in the box.
                    //Otherwise, just send the char for further processing.
                    if (isKeyComboCtrlV(keyCode)) {
                        textBox.setText(getClipboardString());
                    } else {
                        switch (keyCode) {
                            case Keyboard.KEY_BACK:
                                textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.BACKSPACE);
                                continue;
                            case Keyboard.KEY_DELETE:
                                textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.DELETE);
                                continue;
                            case Keyboard.KEY_LEFT:
                                textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.LEFT);
                                continue;
                            case Keyboard.KEY_RIGHT:
                                textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.RIGHT);
                                continue;
                            default:
                                textBox.handleKeyTyped(key, keyCode, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        //Forward close event as this comes from an ESC key that we don't see.
        //Need to check if GUI is active in case we get multiple events.
        if (AGUIBase.activeGUIs.contains(gui)) {
            gui.close();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return gui.pauseOnOpen();
    }
}
