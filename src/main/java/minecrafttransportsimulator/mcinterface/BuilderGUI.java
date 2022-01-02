package minecrafttransportsimulator.mcinterface;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.GUILightingMode;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;

/**Builder for MC GUI classes.  Created when {@link InterfaceGUI#openGUI(AGUIBase)}
 * is called to open a GUI.  GUI instance is not saved after closed.
 *
 * @author don_bruce
 */
public class BuilderGUI extends GuiScreen{
	private int guiLeft;
	private int guiTop;
	private GUIComponentButton lastButtonClicked;
	
	/**Current gui we are built around.**/
	public final AGUIBase gui;
	
	public BuilderGUI(AGUIBase gui){
		this.gui = gui;
	}
	
	/**
	 *  This is called by the main MC system when this GUI is first initialized, or when it
	 *  is re-sized.  We use this call, and send out the re-factored width and height
	 *  point of the top-left of the GUI texture via {@link #setupComponents(int, int)} for
	 *  GUIs to create their objects with.
	 */
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (width - gui.getWidth())/2;
		if(gui.renderFlushBottom()){
			guiTop = height - gui.getHeight();
		}else{
			guiTop = (height - gui.getHeight())/2;
		}
		
		//Setup components now.
		gui.setupComponents(guiLeft, guiTop);
	}
	
	/**
	 *  This is called by the main MC system when this GUI is set to render.  We do all
	 *  the rendering here using the component states to ensure a better order that
	 *  doesn't risk clashing textures or other issues like normal rendering does.
	 *  Because of this, this method should never need to be overridden by classes
	 *  extending this class.  Instead, they should populate the appropriate component
	 *  lists and set the appropriate states.
	 */
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
		//First set the states for things in this GUI.
		gui.setStates();
		
		//Set Y-axis to inverted to have correct orientation.
		GL11.glScalef(1.0F, -1.0F, 1.0F);
		
		//If we are light-sensitive, enable lighting.
		if(!gui.getGUILightMode().equals(GUILightingMode.NONE)){
			InterfaceRender.setInternalLightingState(true);
			InterfaceRender.setLightingToPosition(gui.getGUILightSource().position);
		}
		
		//Bind the standard texture and render the background.
		//If we are translucent, enable blending.
		if(gui.renderTranslucent()){
			InterfaceRender.setBlend(true);
		}
		
		//Render textured components.  These choose if they render or not depending on visibility.
		for(AGUIComponent component : gui.components){
			if(component.visible && !(component instanceof GUIComponentInstrument)){
				component.render(gui, mouseX, mouseY, false, partialTicks);
			}
		}
		
		//If we are light-sensitive, and this GUI is said to be lit up, disable the lightmap.
		//This allows all text to be lit up if required.  We also render the lit texture now.
		//This requires a re-render of all the components to ensure the lit texture portions of said components render.
		if(gui.getGUILightMode().equals(GUILightingMode.LIT)){
			InterfaceRender.setLightingState(false);
			InterfaceRender.bindTexture(gui.getTexture().replace(".png", "_lit.png"));
			for(AGUIComponent component : gui.components){
				if(component.visible && !(component instanceof GUIComponentInstrument) && !(component instanceof GUIComponentItem)){
					component.render(gui, mouseX, mouseY, true, partialTicks);
				}
			}
		}
		
		//Disable translucency if we had it enabled.
		if(gui.renderTranslucent()){
			InterfaceRender.setBlend(false);
		}
		
		//Now that all main rendering is done, render text.
		//This includes labels, button text, and text boxes.
		//We only need to do this once, even if we are lit, as we just change the text lighting.
		boolean isTextLit = gui.getGUILightMode().equals(GUILightingMode.LIT);
		for(AGUIComponent component : gui.components){
			if(component.visible && component.text != null && !component.text.isEmpty()){
				component.renderText(isTextLit);
			}
		}
		
		//Enable lighting for instrument rendering,
		//then render the instruments.  These use their own texture,
		//so the text texture will be overridden at this point.
		InterfaceRender.setLightingState(true);
		for(AGUIComponent component : gui.components){
			if(component instanceof GUIComponentInstrument){
				component.render(gui, mouseX, mouseY, false, partialTicks);
			}
		}
		
		//Now render blended parts of the instrument.
		InterfaceRender.setBlend(true);
		for(AGUIComponent component : gui.components){
			if(component instanceof GUIComponentInstrument){
				component.render(gui, mouseX, mouseY, true, partialTicks);
			}
		}
		InterfaceRender.setBlend(false);
		
		//Render any tooltips.
		for(AGUIComponent component : gui.components){
			if(component.visible && component.isMouseInBounds(mouseX, mouseY)){
				component.renderTooltip(gui, mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height);
			}
		}
		
		//Set Y-axis back to regular orientation.
		GL11.glScalef(1.0F, -1.0F, 1.0F);
	}
	
	/**
	 *  This is called by the main MC system for click events.  We override it here to check
	 *  to see if we have clicked any of the registered components.  If so, we fire the appropriate 
	 *  event for those components.  If we click something, we don't check any other components as 
	 *  that could result in us being in a transition state when doing checks.
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
        for(AGUIComponent component : gui.components){
        	if(component instanceof GUIComponentButton){
        		GUIComponentButton button = (GUIComponentButton) component;
	        	if(button.canClick(mouseX, mouseY)){
	    			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	    			button.onClicked(mouseX <= button.constructedX + button.width/2);
	    			lastButtonClicked = button;
	    			return;
	        	}
        	}
        }
        for(AGUIComponent component: gui.components){
        	if(component instanceof GUIComponentTextBox){
        		((GUIComponentTextBox) component).updateFocus(mouseX, mouseY);
        	}
        }
    }
	
	/**
	 *  This is called by the main MC system for click events.  We override it here to tell 
	 *  the last selector we clicked, if any, that the mouse has been released.  This allows
	 *  the selector to resume to it's "resting" state.  This is dependent on the selector code;
	 *  some selectors may not do anything with this action.
	 */
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int actionType){
	    if(lastButtonClicked != null){
	    	lastButtonClicked.onReleased();
	    	lastButtonClicked = null;
	    }
	}
	
	/**
	 *  This is called by the main MC system for keyboard events.  We Override it here to check
	 *  to forward the inputs to focused textBoxes for further processing.
	 */
	@Override
	protected void keyTyped(char key, int keyCode) throws IOException{
		super.keyTyped(key, keyCode);
		 for(AGUIComponent component: gui.components){
	        if(component instanceof GUIComponentTextBox){
	        	GUIComponentTextBox textBox = (GUIComponentTextBox) component;
				if(textBox.focused){
					//If we did a paste from the clipboard, we need to replace everything in the box.
					//Otherwise, just send the char for further processing.
					if(isKeyComboCtrlV(keyCode)){
						textBox.setText(getClipboardString());
					}else{
						switch(keyCode){
							case Keyboard.KEY_BACK: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.BACKSPACE); continue;
							case Keyboard.KEY_DELETE: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.DELETE); continue;
							case Keyboard.KEY_LEFT: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.LEFT); continue;
							case Keyboard.KEY_RIGHT: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.RIGHT); continue;
							default: textBox.handleKeyTyped(key, keyCode, null); continue;
						}
					}
				}
	        }
        }
	}
	
	@Override
	public void onGuiClosed(){
		gui.onClosed();
    }
	
	@Override
	public boolean doesGuiPauseGame(){
		return gui.pauseOnOpen();
	}
}
