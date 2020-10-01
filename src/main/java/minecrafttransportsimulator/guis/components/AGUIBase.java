package minecrafttransportsimulator.guis.components;

import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderGUI;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;

/**Base GUI class.  This type is used in the constructor of {@link BuilderGUI} to allow us to use
 * completely custom GUI code that is not associated with MC's standard GUI code.  Allows us to only
 * update the wrapper rather than the whole GUI. In essence, this class holds the data and state of the
 * GUI, while the wrapper chooses how to interpret and render said state.
 *
 * @author don_bruce
 */
public abstract class AGUIBase{
	private static final int STANDARD_GUI_WIDTH = 256;
	private static final int STANDARD_GUI_HEIGHT = 192;
	private static final String STANDARD_TEXTURE_NAME = "mts:textures/guis/standard.png";
	
	public final List<GUIComponentLabel> labels = new ArrayList<GUIComponentLabel>();
	public final List<GUIComponentButton> buttons = new ArrayList<GUIComponentButton>();
	public final List<GUIComponentSelector> selectors = new ArrayList<GUIComponentSelector>();
	public final List<GUIComponentTextBox> textBoxes = new ArrayList<GUIComponentTextBox>();
	public final List<GUIComponentItem> items = new ArrayList<GUIComponentItem>();
	public final List<GUIComponentInstrument> instruments = new ArrayList<GUIComponentInstrument>();
	public final List<GUIComponentOBJModel> objModels = new ArrayList<GUIComponentOBJModel>();
	
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
	 *  If this is false, then no background texture will be rendered.
	 */
	public boolean renderBackground(){
		return true;
	}
	
	/**
	 *  If this is true, then the dark background gradient will be rendered behind the GUI.
	 */
	public boolean renderDarkBackground(){
		return false;
	}
	
	/**
	 *  Used to determine lighting for this GUI.  If {@link GUILightingMode#NONE} is returned,
	 *  then this GUI renders as normal.  If {@link GUILightingMode#DARK} is returned, then the
	 *  brightness of the GUI is set to the brightness of the player.  If {@link GUILightingMode#LIT}
	 *  is returned, then the GUI is rendered with lit text and items, but dark controls.  It is
	 *  also rendered with the lit texture (the texture name with _lit appended) as well.  This
	 *  can be used to make light-sensitive GUIs for vehicles as well as other things.
	 *  
	 */
	public GUILightingMode getGUILightMode(){
		return GUILightingMode.NONE;
	}
	
	/**
	 *  If this is true, then the GUI will pause the game when open.
	 */
	public boolean pauseOnOpen(){
		return false;
	}
	
	/**
	 *  Returns the width of this GUI.  Used for centering.
	 */
	public int getWidth(){
		return STANDARD_GUI_WIDTH;
	}
	
	/**
	 *  Returns the height of this GUI.  Used for centering.
	 */
	public int getHeight(){
		return STANDARD_GUI_HEIGHT;
	}
	
	/**
	 *  If true, the GUI will be rendered flush with the bottom
	 *  and sides of the screen rather than the center of the screen.
	 *  This may cause textures to not render in their proper locations,
	 *  so watch out!
	 */
	public boolean renderFlushBottom(){
		return false;
	}
	
	/**
	 *  Returns the width of this GUI's texture.  Used for rendering.
	 */
	public final int getTextureWidth(){
		return getWidth() <= 256 ? 256 : (getWidth() <= 512 ? 512 : (getWidth() <= 1024 ? 1024 : 2048));
	}
	
	/**
	 *  Returns the height of this GUI's texture.  Used for rendering.
	 */
	public final int getTextureHeight(){
		return getHeight() <= 256 ? 256 : (getHeight() <= 512 ? 512 : (getHeight() <= 1024 ? 1024 : 2048));
	}
	
	/**
	 *  Returns the texture that goes with this GUI.  This will be used for
	 *  all rendering operations on this cycle, but may be changed out
	 *  on different cycles if needed.
	 */
	public String getTexture(){
		return STANDARD_TEXTURE_NAME;
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
	 *  sensed, this GUI will attempt to click all buttons in this set via {@link GUIComponentButton#canClick(int, int)}.
	 *  If any of those buttons say they were clicked, their {@link GUIComponentButton#onClicked()} method 
	 *  is fired to allow the button to handle clicking actions.
	 */
	public void addButton(GUIComponentButton button){
		buttons.add(button);
	}
	
	/**
	 *  Adds an {@link GUIComponentSelector} to this GUIs component set.  When a mouse click is
	 *  sensed, this GUI will attempt to click all selectors in this set via {@link GUIComponentSelector#canClick(int, int)}.
	 *  If any of those selectors say they were clicked, their {@link GUIComponentButton#onClicked(boolean)} method 
	 *  is fired to allow the button to handle clicking actions.
	 */
	public void addSelector(GUIComponentSelector selector){
		selectors.add(selector);
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
	 *  Adds an {@link GUIComponentInstrument} to this GUIs component set.  These are rendered
	 *  depending on the vehicle's state, and are really just a pass-through to {@link RenderInstrument#drawInstrument(EntityVehicleE_Powered, ItemInstrument, byte)}.
	 */
	public void addInstrument(GUIComponentInstrument instrument){
		instruments.add(instrument);
	}
	
	/**
	 *  Adds an {@link GUIComponentOBJModel} to this GUIs component set.  These are rendered
	 *  based on their internal variables, which may be set after creation to allow for switching
	 *  without re-creating the object.  Note that while they cause a texture re-bind, they don't
	 *  do any other odd state changes so can be rendered any time the main texture is done with.
	 */
	public void addOBJModel(GUIComponentOBJModel objModel){
		objModels.add(objModel);
	}
	
	/**
	 *  Convenience method to clear out all component lists.
	 */
	public void clearComponents(){
		labels.clear();
		buttons.clear();
		selectors.clear();
		textBoxes.clear();
		items.clear();
		instruments.clear();
		objModels.clear();
	}
	
	/**
	 *  Clock method used to make flashing text and icons on screen.  Put here
	 *  for all GUIs to use.  Returns true if the period is active.  Both
	 *  parameters are in ticks, or 1/20 a second.
	 */
	public static boolean inClockPeriod(int totalPeriod, int onPeriod){
		return System.currentTimeMillis()*0.02D%totalPeriod <= onPeriod;
	}
	
	/**
	 *  List of enums that define how text is rendered.
	 */
	public static enum TextPosition{
		CENTERED,
		LEFT_ALIGNED,
		RIGHT_ALIGNED;
	}
	
	/**
	 *  List of enums that define if the GUI is lit or not.
	 */
	public enum GUILightingMode{
		NONE,
		DARK,
		LIT;
	}
}
