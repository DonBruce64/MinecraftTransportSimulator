package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketVehicleVariableToggle;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;


/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelGround extends AGUIPanel{
	private static final int LIGHT_TEXTURE_WIDTH_OFFSET = 0;
	private static final int LIGHT_TEXTURE_HEIGHT_OFFSET = 196;
	private static final int TURNSIGNAL_TEXTURE_WIDTH_OFFSET = LIGHT_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TURNSIGNAL_TEXTURE_HEIGHT_OFFSET = 176;
	private static final int EMERGENCY_TEXTURE_WIDTH_OFFSET = TURNSIGNAL_TEXTURE_WIDTH_OFFSET + 20;
	private static final int EMERGENCY_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int SIREN_TEXTURE_WIDTH_OFFSET = EMERGENCY_TEXTURE_WIDTH_OFFSET + 20;
	private static final int SIREN_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int ENGINE_TEXTURE_WIDTH_OFFSET = SIREN_TEXTURE_WIDTH_OFFSET + 20;
	private static final int ENGINE_TEXTURE_HEIGHT_OFFSET = 196;
	private static final int TRAILER_TEXTURE_WIDTH_OFFSET = ENGINE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TRAILER_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int REVERSE_TEXTURE_WIDTH_OFFSET = TRAILER_TEXTURE_WIDTH_OFFSET + 20;
	private static final int REVERSE_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int CRUISECONTROL_TEXTURE_WIDTH_OFFSET = REVERSE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int CRUISECONTROL_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int GEAR_TEXTURE_WIDTH_OFFSET = CRUISECONTROL_TEXTURE_WIDTH_OFFSET + 20;
	private static final int GEAR_TEXTURE_HEIGHT_OFFSET = 176;
	private static final int CUSTOM_TEXTURE_WIDTH_OFFSET = GEAR_TEXTURE_WIDTH_OFFSET + 20;
	private static final int CUSTOM_TEXTURE_HEIGHT_OFFSET = 216;
	
	private GUIComponentSelector lightSelector;
	private GUIComponentSelector turnSignalSelector;
	private GUIComponentSelector emergencySelector;
	private GUIComponentSelector sirenSelector;
	private GUIComponentSelector reverseSelector;
	private GUIComponentSelector cruiseControlSelector;
	private GUIComponentSelector gearSelector;
	private final Map<Byte, GUIComponentSelector> engineSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final List<GUIComponentSelector> trailerSelectors = new ArrayList<GUIComponentSelector>();
	private final List<GUIComponentSelector> customSelectors = new ArrayList<GUIComponentSelector>();
	
	public GUIPanelGround(EntityVehicleF_Physics groundVehicle){
		super(groundVehicle);
	}
	
	@Override
	protected void setupLightComponents(int guiLeft, int guiTop){
		//Create a tri-state selector for the running lights and headlights.
		//For the tri-state we need to make sure we don't try to turn on running lights if we don't have any.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.RUNNINGLIGHT) || RenderVehicle.doesVehicleHaveLight(vehicle, LightType.HEADLIGHT)){
			lightSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.headlights"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LIGHT_TEXTURE_WIDTH_OFFSET, LIGHT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						if(selectorState == 2){
							InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.HEADLIGHT.lowercaseName));
						}else if(selectorState == 1){
							InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.RUNNINGLIGHT.lowercaseName));
						}
					}else{
						if(selectorState == 0){
							if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.RUNNINGLIGHT)){
								InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.RUNNINGLIGHT.lowercaseName));
							}else{
								InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.HEADLIGHT.lowercaseName));
							}
						}else if(selectorState == 1){
							InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.HEADLIGHT.lowercaseName));
						}
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(lightSelector);
		}
		
		//Add the turn signal selector if we have turn signals.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.LEFTTURNLIGHT) || RenderVehicle.doesVehicleHaveLight(vehicle, LightType.RIGHTTURNLIGHT)){
			turnSignalSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 1*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.turnsignals"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TURNSIGNAL_TEXTURE_WIDTH_OFFSET, TURNSIGNAL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.LEFTTURNLIGHT.lowercaseName));
					}else{
						InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.RIGHTTURNLIGHT.lowercaseName));
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(turnSignalSelector);
		}
		
		//Add the emergency light selector if we have those.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.EMERGENCYLIGHT)){
			emergencySelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.emergencylights"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, EMERGENCY_TEXTURE_WIDTH_OFFSET, EMERGENCY_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, LightType.EMERGENCYLIGHT.lowercaseName));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(emergencySelector);
		}
		
		//Add the siren selector if we have a siren.
		if(vehicle.definition.motorized.sirenSound != null){
			sirenSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.siren"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, SIREN_TEXTURE_WIDTH_OFFSET, SIREN_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.SIREN, !leftSide));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(sirenSelector);
		}
	}
	
	@Override
	protected void setupEngineComponents(int guiLeft, int guiTop){
		engineSelectors.clear();
		//Create the engine selectors for this vehicle.
		for(Byte engineNumber : vehicle.engines.keySet()){
			//Go to next column if we are on our 5th engine.
			if(engineNumber == 5){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
			}
			GUIComponentSelector engineSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS)*(engineNumber%4), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.engine"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINE_TEXTURE_WIDTH_OFFSET, ENGINE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(selectorState == 0 && !leftSide){
						InterfacePacket.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_ON));
					}else if(selectorState == 1 && !leftSide){
						InterfacePacket.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_ON));
					}else if(selectorState == 1 && leftSide){
						InterfacePacket.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_OFF));
					}else if(selectorState == 2 && leftSide){
						InterfacePacket.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				}
				
				@Override
				public void onReleased(){
					if(selectorState == 2){
						InterfacePacket.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				}
			};
			engineSelectors.put(engineNumber, engineSelector);
			addSelector(engineSelector);
		}
		
		//If we have both reverse AND cruise control, render them side-by-side. Otherwise just render one in the middle
		if(haveReverseThrustOption && vehicle.definition.motorized.hasCruiseControl){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
			
			cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CRUISECONTROL, !vehicle.cruiseControl));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(cruiseControlSelector);
		}else if(haveReverseThrustOption){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
		}else if(vehicle.definition.motorized.hasCruiseControl){
			cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CRUISECONTROL, !vehicle.cruiseControl));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(cruiseControlSelector);
		}
	}
	
	@Override
	protected void setupGeneralComponents(int guiLeft, int guiTop){
		//Create the 8 trailer selectors.  Note that not all may be rendered.
		for(int i=0; i<8; ++i){
			//Go to next column if we are on our 4th trailer selector.
			if(i == 4){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
			}
			
			GUIComponentSelector trailerSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.trailer") + "#" + i, vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					int trailerNumber = trailerSelectors.indexOf(this);
					EntityVehicleF_Physics currentVehicle = vehicle;
					for(int j=0; j<trailerNumber; ++ j){
						currentVehicle = currentVehicle.towedVehicle;
					}
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(currentVehicle, PacketVehicleControlDigital.Controls.TRAILER, true));
				}
				
				@Override
				public void onReleased(){}
			};
			trailerSelectors.add(trailerSelector);
			addSelector(trailerSelector);
		}
		
		//If we have gear, add a selector for it.
		//This is rendered on the 4th row.  It is assumed that this will never be combined with 8 trailers...
		if(vehicle.definition.motorized.gearSequenceDuration != 0){
			gearSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.gear"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.GEAR, !vehicle.gearUpCommand));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(gearSelector);
		}
	}
	
	@Override
	public void setupCustomComponents(int guiLeft, int guiTop){
		//Add custom selectors if we have any.
		//These are the right-most selector and are vehicle-specific.
		List<String> customVariables = new ArrayList<String>();
		if(vehicle.definition.rendering.customVariables != null){
			customVariables.addAll(vehicle.definition.rendering.customVariables);
		}
		for(APart part : vehicle.parts){
			if(part.definition.rendering != null && part.definition.rendering.customVariables != null){
				customVariables.addAll(part.definition.rendering.customVariables);
			}
		}
		for(int i=0; i<customVariables.size(); ++i){
			GUIComponentSelector customSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, customVariables.get(i), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CUSTOM_TEXTURE_WIDTH_OFFSET, CUSTOM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleVariableToggle(vehicle, this.text));
				}
				
				@Override
				public void onReleased(){}
			};
			customSelectors.add(customSelector);
			addSelector(customSelector);
		}
	}
	
	@Override
	public void setStates(){
		//Set the state of the light selector.
		if(lightSelector != null){
			lightSelector.selectorState = vehicle.variablesOn.contains(LightType.HEADLIGHT.lowercaseName) ? 2 : (vehicle.variablesOn.contains(LightType.RUNNINGLIGHT.lowercaseName) ? 1 : 0);
		}
		
		//Set the state of the turn signal selector.
		if(turnSignalSelector != null){
			boolean halfSecondClock = inClockPeriod(20, 10);
			if(vehicle.variablesOn.contains(LightType.LEFTTURNLIGHT.lowercaseName) && halfSecondClock){
				if(vehicle.variablesOn.contains(LightType.RIGHTTURNLIGHT.lowercaseName)){
					turnSignalSelector.selectorState = 3;
				}else{
					turnSignalSelector.selectorState = 1;
				}
			}else if(vehicle.variablesOn.contains(LightType.RIGHTTURNLIGHT.lowercaseName) && halfSecondClock){
				turnSignalSelector.selectorState = 2;
			}else{
				turnSignalSelector.selectorState = 0;
			}
		}
		 
		
		//If we have emergency lights, set the state of the emergency light selector.
		if(emergencySelector != null){
			emergencySelector.selectorState = vehicle.variablesOn.contains(LightType.EMERGENCYLIGHT.lowercaseName) ? 1 : 0;
		}
		
		//If we have a siren, set the state of the siren selector.
		if(sirenSelector != null){
			sirenSelector.selectorState = vehicle.sirenOn ? 1 : 0;
		}
		
		//Set the state of the engine selectors.
		for(Entry<Byte, GUIComponentSelector> engineEntry : engineSelectors.entrySet()){
			if(vehicle.engines.containsKey(engineEntry.getKey())){
				PartEngine.EngineStates engineState = vehicle.engines.get(engineEntry.getKey()).state;
				engineEntry.getValue().selectorState = !engineState.magnetoOn ? 0 : (!engineState.esOn ? 1 : 2);
			}
		}
		
		//If we have reverse thrust, set the selector state.
		if(reverseSelector != null){
			reverseSelector.selectorState = vehicle.reverseThrust ? 1 : 0;
		}
		
		//If we have cruise control, set the selector state.
		if(cruiseControlSelector != null){
			cruiseControlSelector.selectorState = vehicle.cruiseControl ? 1 : 0;
		}
		
		//If we have gear, set the selector state.
		if(gearSelector != null){
			if(vehicle.gearUpCommand){
				gearSelector.selectorState = vehicle.gearMovementTime == vehicle.definition.motorized.gearSequenceDuration ? 2 : 3;
			}else{
				gearSelector.selectorState = vehicle.gearMovementTime == 0 ? 0 : 1;
			}
		}
		
		//Iterate through trailers and set the visibility of the trailer selectors based on their state.
		EntityVehicleF_Physics currentVehicle = vehicle;
		for(int i=0; i<trailerSelectors.size(); ++i){
			if(currentVehicle != null && currentVehicle.hasHitch()){
				trailerSelectors.get(i).visible = true;
				trailerSelectors.get(i).selectorState = currentVehicle.towedVehicle != null ? 0 : 1;
				currentVehicle = currentVehicle.towedVehicle;
			}else{
				trailerSelectors.get(i).visible = false;
			}
		}
		
		//Iterate through custom selectors and set their states.
		for(GUIComponentSelector customSelector : customSelectors){
			customSelector.selectorState = vehicle.variablesOn.contains(customSelector.text) ? 1 : 0;
		}
	}
}
