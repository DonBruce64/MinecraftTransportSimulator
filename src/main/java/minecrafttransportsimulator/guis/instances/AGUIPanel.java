package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is abstract and contains the base code for rendering things common to
 * all vehicles, such as lights and engines.  Other things may be added as needed.
 * 
 * @author don_bruce
 */
public abstract class AGUIPanel extends AGUIBase{
	protected static final int PANEL_WIDTH = 400;
	protected static final int PANEL_HEIGHT = 140;
	protected static final int GAP_BETWEEN_SELECTORS = 12;
	protected static final int SELECTOR_SIZE = 20;
	protected static final int SELECTOR_TEXTURE_SIZE = 20;
	
	public final EntityVehicleF_Physics vehicle;
	protected final boolean haveReverseThrustOption;
	protected final List<SwitchEntry> trailerSwitchDefs = new ArrayList<SwitchEntry>();
	protected int xOffset;


	public AGUIPanel(EntityVehicleF_Physics vehicle){
		this.vehicle = vehicle;
		//If we have propellers with reverse thrust capabilities, or are a blimp, or have jet engines, render the reverse thrust selector.
		if(vehicle.definition.motorized.isBlimp){
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
		setupTowingButtons(vehicle);
	}
	
	private void setupTowingButtons(AEntityD_Interactable<?> entity){
		//Add trailer switch defs to allow switches to be displayed.
		//These depend on our connections, and our part's connections.
		//This method allows for recursion for connected trailers.
		if(entity.definition.connectionGroups != null){
			for(JSONConnectionGroup connectionGroup : entity.definition.connectionGroups){
				if(connectionGroup.canIntiateConnections){
					trailerSwitchDefs.add(new SwitchEntry(entity, connectionGroup));
				}
			}
			
			//Also check things we are towing, if we are set to do so.
			for(TrailerConnection connection : entity.towingConnections){
				if(connection.hookupConnectionGroup.canIntiateSubConnections){
					setupTowingButtons(connection.hookupBaseEntity);
				}
			}
		}
		
		//Check parts, if we have any.
		if(entity instanceof AEntityE_Multipart){
			for(APart part : ((AEntityE_Multipart<?>) entity).parts){
				if(part.definition.connectionGroups != null){
					for(JSONConnectionGroup connectionGroup : part.definition.connectionGroups){
						if(connectionGroup.canIntiateConnections){
							trailerSwitchDefs.add(new SwitchEntry(part, connectionGroup));
						}
					}
				}
			}
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
		return vehicle.renderTextLit() ? GUILightingMode.LIT : GUILightingMode.DARK;
	}
	
	@Override
	public EntityVehicleF_Physics getGUILightSource(){
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
		return vehicle.definition.motorized.panelTexture != null ? vehicle.definition.motorized.panelTexture : "mts:textures/guis/panel.png";
	}
	
	protected static class SwitchEntry{
		protected final AEntityD_Interactable<?> entityOn;
		protected final JSONConnectionGroup connectionGroup;
		protected final int connectionGroupIndex;
		
		private SwitchEntry(AEntityD_Interactable<?> entityOn, JSONConnectionGroup connectionGroup){
			this.entityOn = entityOn;
			this.connectionGroup = connectionGroup;
			this.connectionGroupIndex = entityOn.definition.connectionGroups.indexOf(connectionGroup);
		}
	}
}
