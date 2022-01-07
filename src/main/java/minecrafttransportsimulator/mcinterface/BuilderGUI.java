package minecrafttransportsimulator.mcinterface;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
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
		Keyboard.enableRepeatEvents(true);
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
		//Set Y-axis to inverted to have correct orientation.
		GL11.glScalef(1.0F, -1.0F, 1.0F);
		
		//Enable lighting.
		RenderHelper.enableStandardItemLighting();
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		InterfaceRender.setLightingState(true);
		
		//Render main pass, then blended pass.
		gui.render(width, height, mouseX, mouseY, false, partialTicks);
		InterfaceRender.setBlend(true);
		gui.render(width, height, mouseX, mouseY, true, partialTicks);
		
		//Set states back to normal.
		InterfaceRender.setBlend(false);
		InterfaceRender.setLightingState(false);
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		RenderHelper.disableStandardItemLighting();
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
