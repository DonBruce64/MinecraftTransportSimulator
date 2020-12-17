package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is abstract and contains the base code for rendering things common to
 * all vehicles, such as lights and engines.  Other things may be added as needed.
 * 
 * @author don_bruce
 */
public abstract class AGUIPanel extends AGUIBase{
	public static final int PANEL_WIDTH = 400;
	public static final int PANEL_HEIGHT = 140;
	protected static final int GAP_BETWEEN_SELECTORS = 12;
	protected static final int SELECTOR_SIZE = 20;
	protected static final int SELECTOR_TEXTURE_SIZE = 20;
	
	protected final EntityVehicleF_Physics vehicle;
	protected final boolean haveReverseThrustOption;
	protected int xOffset;


	public AGUIPanel(EntityVehicleF_Physics vehicle){
		this.vehicle = vehicle;
		//If we have propellers with reverse thrust capabilities, or are a blimp, or have jet engines, render the reverse thrust selector.
		if(vehicle.definition.general.isBlimp){
			haveReverseThrustOption = true;
		}else{
			boolean foundReversingPart = false;
			for(APart part : vehicle.parts){
				if(part instanceof PartPropeller){
					if(part.definition.propeller.isDynamicPitch){
						foundReversingPart = true;
						break;
					}
				}else if(part instanceof PartEngine && part.definition.engine.jetPowerFactor > 0){
					foundReversingPart = true;
					break;
				}
			}
			haveReverseThrustOption = foundReversingPart;
		}
	}
	
	@Override
	public final void setupComponents(int guiLeft, int guiTop){
		//Tracking variable for how far to the left we are rendering things.
		//This allows for things to be on different columns depending on vehicle configuration.
		//We make this method final and create an abstract method to use instead of this one for
		//setting up any extra components.
		xOffset  = (int) 1.25D*GAP_BETWEEN_SELECTORS;
		
		//Add light selectors.  These are on the left-most side of the panel.
		setupLightComponents(guiLeft, guiTop);
		xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
		
		//Add engine selectors.  These are to the right of the light switches.
		setupEngineComponents(guiLeft, guiTop);
		xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
		
		//Add general selectors.  These are panel-specific, and to the right of the engine selectors.
		setupGeneralComponents(guiLeft, guiTop);
		xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
		
		//Add custom selectors.  These are vehicle-specific, and their placement is panel-specific.
		//These are rendered to the right of the general selectors.
		setupCustomComponents(guiLeft, guiTop);
		
		//Add instruments.  These go wherever they are specified in the JSON.
		for(Integer instrumentNumber : vehicle.instruments.keySet()){
			//Only add instruments that have an optionalPartNumber as those are on the panel.
			if(vehicle.definition.motorized.instruments.get(instrumentNumber).optionalPartNumber != 0){
				addInstrument(new GUIComponentInstrument(guiLeft, guiTop, instrumentNumber, vehicle));
			}
		}
	}
	
	protected abstract void setupLightComponents(int guiLeft, int guiTop);
	
	protected abstract void setupEngineComponents(int guiLeft, int guiTop);
	
	protected abstract void setupGeneralComponents(int guiLeft, int guiTop);
	
	protected abstract void setupCustomComponents(int guiLeft, int guiTop);
	
	@Override
	public GUILightingMode getGUILightMode(){
		return vehicle.areInteriorLightsOn() ? GUILightingMode.LIT : GUILightingMode.DARK;
	}
	
	@Override
	public AEntityBase getGUILightSource(){
		return vehicle;
	}
	
	@Override
	public int getWidth(){
		return PANEL_WIDTH;
	}
	
	@Override
	public int getHeight(){
		return PANEL_HEIGHT;
	}
	
	@Override
	public boolean renderFlushBottom(){
		return true;
	}
	
	@Override
	public String getTexture(){
		return vehicle.definition.rendering.panelTexture != null ? vehicle.definition.rendering.panelTexture : "mts:textures/guis/panel.png";
	}
}
