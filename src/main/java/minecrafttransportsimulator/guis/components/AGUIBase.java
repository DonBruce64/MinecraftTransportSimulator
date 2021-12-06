package minecrafttransportsimulator.guis.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;

/**Base GUI class.  This type is used in conjunction with {@link InterfaceGUI} to allow us to use
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
	protected static final int STANDARD_COLOR_WIDTH = 20;
	protected static final int STANDARD_COLOR_HEIGHT = 20;
	protected static final int STANDARD_COLOR_WIDTH_OFFSET = 216;
	protected static final int STANDARD_RED_HEIGHT_OFFSET = 196;
	protected static final int STANDARD_YELLOW_HEIGHT_OFFSET = 216;
	protected static final int STANDARD_BLACK_HEIGHT_OFFSET = 236;

	
	public final List<AGUIComponent> generalComponents = new ArrayList<AGUIComponent>();
	public final List<GUIComponentItem> items = new ArrayList<GUIComponentItem>();
	public final List<GUIComponentInstrument> instruments = new ArrayList<GUIComponentInstrument>();
	
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
	 *  Called right after the GUI is closed.  Normally does nothing, but can be
	 *  used for closure events. 
	 */
	public void onClosed(){}
	
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
	 *  Returns the source of where to calculate the light for this GUI.  This is required
	 *  if {@link #getGUILightMode()} is any value other than {@link GUILightingMode#NONE}.
	 */
	public AEntityB_Existing getGUILightSource(){
		return null;
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
	 *  If true, the GUI texture will be rendered translucent.  This affects everything from this texture,
	 *  including buttons and switches!  Does not affect text-rendering as that's its own system.
	 */
	public boolean renderTranslucent(){
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
	 *  Adds an {@link AGUIComponent} to this GUIs component set.  These are rendered
	 *  automatically given their current state.  Said state should be set in {@link #setStates()}.
	 */
	public void addComponent(AGUIComponent component){
		if(component instanceof GUIComponentInstrument){
			instruments.add((GUIComponentInstrument) component);
		}else if(component instanceof GUIComponentItem){
			items.add((GUIComponentItem) component);
		}else{
			generalComponents.add(component);
		}
	}
	
	/**
	 *  Convenience method to clear out all component lists.
	 */
	public void clearComponents(){
		generalComponents.clear();
		items.clear();
		instruments.clear();
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
	 *  List of enums that define if the GUI is lit or not.
	 */
	public enum GUILightingMode{
		NONE,
		DARK,
		LIT;
	}
}
