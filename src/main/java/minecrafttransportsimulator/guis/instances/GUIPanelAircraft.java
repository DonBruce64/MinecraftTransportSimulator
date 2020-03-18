package minecrafttransportsimulator.guis.instances;

import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.LANDINGLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.NAVIGATIONLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.STROBELIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.TAXILIGHT;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.rendering.vehicles.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Blimp;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngineJet;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperNetwork;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelAircraft extends AGUIPanel<EntityVehicleF_Air>{
	private static final int NAVIGATION_TEXTURE_WIDTH_OFFSET = 200;
	private static final int NAVIGATION_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int STROBE_TEXTURE_WIDTH_OFFSET = NAVIGATION_TEXTURE_WIDTH_OFFSET + 20;
	private static final int STROBE_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int TAXI_TEXTURE_WIDTH_OFFSET = STROBE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TAXI_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int LANDING_TEXTURE_WIDTH_OFFSET = TAXI_TEXTURE_WIDTH_OFFSET + 20;
	private static final int LANDING_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int ENGINEMAG_TEXTURE_WIDTH_OFFSET = LANDING_TEXTURE_WIDTH_OFFSET + 20;
	private static final int ENGINEMAG_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int ENGINESTART_TEXTURE_WIDTH_OFFSET = ENGINEMAG_TEXTURE_WIDTH_OFFSET + 20;
	private static final int ENGINESTART_TEXTURE_HEIGHT_OFFSET = 196;
	private static final int REVERSE_TEXTURE_WIDTH_OFFSET = ENGINESTART_TEXTURE_WIDTH_OFFSET + 20;
	private static final int REVERSE_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int TRIM_TEXTURE_WIDTH_OFFSET = REVERSE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TRIM_TEXTURE_HEIGHT_OFFSET = 216;
	
	private final Map<LightType, GUIComponentSelector> lightSelectors = new HashMap<LightType, GUIComponentSelector>();
	private final Map<Byte, GUIComponentSelector> magnetoSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final Map<Byte, GUIComponentSelector> starterSelectors = new HashMap<Byte, GUIComponentSelector>();
	private GUIComponentSelector aileronTrimSelector;
	private GUIComponentSelector elevatorTrimSelector;
	private GUIComponentSelector rudderTrimSelector;
	private GUIComponentSelector reverseSelector;
	
	private GUIComponentSelector selectedTrimSelector;
	private PacketVehicleControlDigital.Controls selectedTrimType = null;
	private boolean selectedTrimDirection;
	private boolean appliedTrimThisRender;
	
	private final boolean haveReverseThrustOption;
	
	public GUIPanelAircraft(EntityVehicleF_Air aircraft){
		super(aircraft);
		//If we have propellers with reverse thrust capabilities, or are a blimp, render the reverse thrust selector.
		if(vehicle instanceof EntityVehicleG_Blimp){
			haveReverseThrustOption = true;
		}else{
			for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
				if(part instanceof PartPropeller){
					if(part.definition.propeller.isDynamicPitch){
						haveReverseThrustOption = true;
						return;
					}
				}else if(part instanceof PartEngineJet){
					haveReverseThrustOption = true;
					return;
				}
			}
			haveReverseThrustOption = false;
		}
	}
	
	@Override
	protected int setupLightComponents(int guiLeft, int guiTop, int xOffset){
		lightSelectors.clear();
		//Create up to four lights depending on how many this vehicle has.
		for(LightType lightType : new LightType[]{NAVIGATIONLIGHT, STROBELIGHT, TAXILIGHT, LANDINGLIGHT}){
			final int LIGHT_TEXTURE_WIDTH_OFFSET;
			final int LIGHT_TEXTURE_HEIGHT_OFFSET;
			switch(lightType){
				case NAVIGATIONLIGHT:  LIGHT_TEXTURE_WIDTH_OFFSET = NAVIGATION_TEXTURE_WIDTH_OFFSET; LIGHT_TEXTURE_HEIGHT_OFFSET = NAVIGATION_TEXTURE_HEIGHT_OFFSET; break;
				case STROBELIGHT:  LIGHT_TEXTURE_WIDTH_OFFSET = STROBE_TEXTURE_WIDTH_OFFSET; LIGHT_TEXTURE_HEIGHT_OFFSET = STROBE_TEXTURE_HEIGHT_OFFSET; break;
				case TAXILIGHT:  LIGHT_TEXTURE_WIDTH_OFFSET = TAXI_TEXTURE_WIDTH_OFFSET; LIGHT_TEXTURE_HEIGHT_OFFSET = TAXI_TEXTURE_HEIGHT_OFFSET; break;
				case LANDINGLIGHT:  LIGHT_TEXTURE_WIDTH_OFFSET = LANDING_TEXTURE_WIDTH_OFFSET; LIGHT_TEXTURE_HEIGHT_OFFSET = LANDING_TEXTURE_HEIGHT_OFFSET; break;
				default: throw new IllegalArgumentException(lightType + " has no texture assigned in the panel!");
			}
			if(RenderVehicle.doesVehicleHaveLight(vehicle, lightType)){
				String lightName = WrapperGUI.translate("gui.panel." + lightType.name().toLowerCase() + "s");
				GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size()*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, lightName, vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LIGHT_TEXTURE_WIDTH_OFFSET, LIGHT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), lightType));
					}
					
					@Override
					public void onReleased(){}
				};
				lightSelectors.put(lightType, lightSwitch);
				addSelector(lightSwitch);
			}
		}
		return xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
	}
	
	@Override
	protected int setupEngineComponents(int guiLeft, int guiTop, int xOffset){
		magnetoSelectors.clear();
		starterSelectors.clear();
		//Create magneto and stater selectors for the engines.
		for(Byte engineNumber : vehicle.engines.keySet()){
			//Go to next column if we are on our 5th engine.
			if(engineNumber == 5){
				xOffset += 2*SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
			}
			
			GUIComponentSelector magnetoSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS)*(engineNumber%4), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.magneto"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINEMAG_TEXTURE_WIDTH_OFFSET, ENGINEMAG_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), vehicle.engines.get(engineNumber).state.magnetoOn ? PacketEngineTypes.MAGNETO_OFF : PacketEngineTypes.MAGNETO_ON));
				}
				
				@Override
				public void onReleased(){}
			};
			magnetoSelectors.put(engineNumber, magnetoSwitch);
			addSelector(magnetoSwitch);
			
			GUIComponentSelector starterSwitch = new GUIComponentSelector(magnetoSwitch.x + SELECTOR_SIZE, magnetoSwitch.y, SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.start"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINESTART_TEXTURE_WIDTH_OFFSET, ENGINESTART_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(vehicle.engines.get(engineNumber).state.magnetoOn){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), vehicle.engines.get(engineNumber).state.esOn ? PacketEngineTypes.ES_OFF : PacketEngineTypes.ES_ON));
					}
				}
				
				@Override
				public void onReleased(){
					MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.ES_OFF));
				}
			};
			starterSelectors.put(engineNumber, starterSwitch);
			addSelector(starterSwitch);

		}
		xOffset += 2*SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
		
		//Add the trim selectors first.
		aileronTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.trim_roll"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(boolean leftSide){
				selectedTrimSelector = this;
				selectedTrimType = PacketVehicleControlDigital.Controls.TRIM_ROLL;
				selectedTrimDirection = !leftSide;
			}
			
			@Override
			public void onReleased(){
				selectedTrimSelector = null;
				selectedTrimType = null;
			}
		};
		addSelector(aileronTrimSelector);
		
		elevatorTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 1*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.trim_pitch"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(boolean leftSide){
				selectedTrimSelector = this;
				selectedTrimType = PacketVehicleControlDigital.Controls.TRIM_PITCH;
				selectedTrimDirection = leftSide;
			}
			
			@Override
			public void onReleased(){
				selectedTrimSelector = null;
				selectedTrimType = null;
			}
		};
		addSelector(elevatorTrimSelector);
		
		rudderTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.trim_yaw"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(boolean leftSide){
				selectedTrimSelector = this;
				selectedTrimType = PacketVehicleControlDigital.Controls.TRIM_YAW;
				selectedTrimDirection = !leftSide;
			}
			
			@Override
			public void onReleased(){
				selectedTrimSelector = null;
				selectedTrimType = null;
			}
		};
		addSelector(rudderTrimSelector);
		
		//If we have reverse thrust, add a selector for it.
		if(haveReverseThrustOption){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
		}
		return xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
	}
	
	@Override
	public void setStates(){
		//Set the states of the light selectors.
		for(Entry<LightType, GUIComponentSelector> lightEntry : lightSelectors.entrySet()){
			lightEntry.getValue().selectorState = vehicle.isLightOn(lightEntry.getKey()) ? 1 : 0;
		}
		
		//Set the states of the magneto selectors.
		for(Entry<Byte, GUIComponentSelector> magnetoEntry : magnetoSelectors.entrySet()){
			magnetoEntry.getValue().selectorState = vehicle.engines.get(magnetoEntry.getKey()).state.magnetoOn ? 1 : 0;
		}
		
		//Set the states of the starter selectors.
		for(Entry<Byte, GUIComponentSelector> starterEntry : starterSelectors.entrySet()){
			starterEntry.getValue().selectorState = vehicle.engines.get(starterEntry.getKey()).state.magnetoOn ? (vehicle.engines.get(starterEntry.getKey()).state.esOn ? 2 : 1) : 0;
		}
				
		//For every tick we have one of the trim selectors pressed, do the corresponding trim action.
		if(selectedTrimSelector != null){
			if(WrapperGUI.inClockPeriod(3, 1)){
				if(!appliedTrimThisRender){
					selectedTrimSelector.selectorState = selectedTrimSelector.selectorState == 0 ? 1 : 0; 
					WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, selectedTrimType, selectedTrimDirection));
					appliedTrimThisRender = true;
				}
			}else{
				appliedTrimThisRender = false;
			}
		}
		
		//If we have reverse thrust, set the selector state.
		if(haveReverseThrustOption){
			reverseSelector.selectorState = vehicle.reverseThrust ? 1 : 0;
		}
	}
}
