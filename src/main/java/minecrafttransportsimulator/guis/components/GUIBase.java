package minecrafttransportsimulator.guis.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.util.ResourceLocation;

/**Base GUI class.  This type is used in the constructor of {@link WrapperGUI} to allow us to use
 * completely custom GUI code that is not associated with MC's standard GUI code.  Allows us to only
 * update the wrapper rather than the whole GUI. In essence, this class holds the data and state of the
 * GUI, while the wrapper chooses how to interpret and render said state.
 *
 * @author don_bruce
 */
public abstract class GUIBase{
	public final List<GUIComponentLabel> labels = new ArrayList<GUIComponentLabel>();
	public final List<GUIComponentButton> buttons = new ArrayList<GUIComponentButton>();
	public final List<GUIComponentTextBox> textBoxes = new ArrayList<GUIComponentTextBox>();
	public final List<GUIComponentItem> items = new ArrayList<GUIComponentItem>();
	
	
	//--------------------START OF NEW CUSTOM METHODS FOR MAKING GUIS--------------------	
	/**
	 *  Called during init to allow for the creation of GUI components.  All components
	 *  should be created in this method, and should be added via the appropriate calls.
	 *  The passed-in guiLeft and guiTop parameters are the top-left of the TEXTURE of
	 *  this GUI, not the screen.  This allows for all objects to be created using offsets
	 *  that won't change, rather than screen pixels that will. 
	 */
	public abstract void setupComponents(int guiLeft, int guiTop);
	
	/**
	 *  Called right before rendering to allow GUIs to set the states of their objects. 
	 */
	public abstract void setStates();
	
	/**
	 *  If this is true, then the dark background gradient will be rendered behind the GUI.
	 */
	public boolean renderDarkBackground(){
		return false;
	}
	
	/**
	 *  If this is true, then the GUI will pause the game when open.
	 */
	public boolean pauseOnOpen(){
		return false;
	}
	
	/**
	 *  Adds an {@link GUIComponentLabel} to this GUIs component set.  These are rendered
	 *  automatically given their current state.  Said state should be set in {@link #setStates()}.
	 */
	public void addLabel(GUIComponentLabel label){
		labels.add(label);
	}
	
	/**
	 *  Adds an {@link GUIComponentButton} to this GUIs component set.  When a mouse click is
	 *  sensed, this GUI will attempt to click all buttons in this set via {@link GUIComponentButton#clicked()}.
	 *  If any of those buttons say they were clicked, their {@link GUIComponentButton#onClicked()} method 
	 *  is fired to allow the button to handle clicking actions.
	 */
	public void addButton(GUIComponentButton button){
		buttons.add(button);
	}
	
	/**
	 *  Adds an {@link GUIComponentTextBox} to this GUIs component set.  When a mouse click is
	 *  sensed, this GUI will attempt to focus all text boxes.  When a key is typed, any focused
	 *  text boxes will get that input set to them via {@link GUIComponentTextBox#handleKeyTyped(char, TextBoxControlKey)}.
	 */
	public void addTextBox(GUIComponentTextBox textBox){
		textBoxes.add(textBox);
	}
	
	/**
	 *  Adds an {@link GUIComponentItem} to this GUIs component set.  These are rendered
	 *  automatically given their current state.  Said state should be set in {@link #setStates()}.
	 */
	public void addItem(GUIComponentItem item){
		items.add(item);
	}
	
	/**
	 *  Convenience method to clear out all component lists.
	 */
	public void clearComponents(){
		labels.clear();
		buttons.clear();
		textBoxes.clear();
		items.clear();
	}
}
