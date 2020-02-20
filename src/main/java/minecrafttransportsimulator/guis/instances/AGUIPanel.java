package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.rendering.RenderInstruments;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is abstract and contains the base code for rendering things common to
 *  all vehicles, such as lights and engines.  Other things may be added as needed.
 * 
 * @author don_bruce
 */
public abstract class AGUIPanel<EntityVehicleX_Type extends EntityVehicleE_Powered> extends AGUIBase{
	protected static final int GAP_BETWEEN_SELECTORS = 12;
	protected static final int SELECTOR_SIZE = 20;
	protected static final int SELECTOR_TEXTURE_SIZE = 20;
	
	protected final EntityVehicleX_Type vehicle;
	protected int xOffset;


	public AGUIPanel(EntityVehicleX_Type vehicle){
		this.vehicle = vehicle;
	}
	
	@Override
	public final void setupComponents(int guiLeft, int guiTop){
		//Tracking variable for how far to the left we are rendering things.
		//This allows for things to be on different columns depending on vehicle configuration.
		//We make this method final and create an abstract method to use instead of this one for
		//setting up any extra components.
		int xOffset  = (int) 1.25D*GAP_BETWEEN_SELECTORS;
		
		//Add light selectors.  These are on the left-most side of the panel.
		xOffset = setupLightComponents(guiLeft, guiTop, xOffset);
		
		//Add engine selectors.  These are to the right of the light switches.
		xOffset = setupEngineComponents(guiLeft, guiTop, xOffset);
		
		//FIXME need to somehow draw instrument here.  Perhaps through another GUI component?
	}
	
	protected abstract int setupLightComponents(int guiLeft, int guiTop, int xOffset);
	
	protected abstract int setupEngineComponents(int guiLeft, int guiTop, int xOffset);
	
	@Override
	public GUILightingMode getGUILightMode(){
		return RenderInstruments.isPanelIlluminated(vehicle) ? GUILightingMode.LIT : GUILightingMode.DARK;
	}
	
	@Override
	public int getWidth(){
		return 400;
	}
	
	@Override
	public int getHeight(){
		return 140;
	}
	
	@Override
	public boolean renderFlushBottom(){
		return true;
	}
	
	@Override
	public String getTexture(){
		//FIXME make this pack-based.
		return "mts:textures/guis/panel.png";
	}
}
