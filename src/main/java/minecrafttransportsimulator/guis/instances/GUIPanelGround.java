package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mcinterface.InterfaceCore;
import mcinterface.InterfaceNetwork;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleLightToggle;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
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
	protected int setupLightComponents(int guiLeft, int guiTop, int xOffset){
		//Create a tri-state selector for the running lights and headlights.
		//For the tri-state we need to make sure we don't try to turn on running lights if we don't have any.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.RUNNINGLIGHT) || RenderVehicle.doesVehicleHaveLight(vehicle, LightType.HEADLIGHT)){
			lightSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.headlights"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LIGHT_TEXTURE_WIDTH_OFFSET, LIGHT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						if(selectorState == 2){
							InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.HEADLIGHT));
						}else if(selectorState == 1){
							InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.RUNNINGLIGHT));
						}
					}else{
						if(selectorState == 0){
							if(RenderVehicle.doesVehicleHaveLight(vehicle, LightType.RUNNINGLIGHT)){
								InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.RUNNINGLIGHT));
							}else{
								InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.HEADLIGHT));
							}
						}else if(selectorState == 1){
							InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.HEADLIGHT));
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
						InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.LEFTTURNLIGHT));
					}else{
						InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.RIGHTTURNLIGHT));
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
					InterfaceNetwork.sendToServer(new PacketVehicleLightToggle(vehicle, LightType.EMERGENCYLIGHT));
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
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.SIREN, !leftSide));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(sirenSelector);
		}
		return xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
	}
	
	@Override
	protected int setupEngineComponents(int guiLeft, int guiTop, int xOffset){
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
						InterfaceNetwork.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_ON));
					}else if(selectorState == 1 && !leftSide){
						InterfaceNetwork.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_ON));
					}else if(selectorState == 1 && leftSide){
						InterfaceNetwork.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_OFF));
					}else if(selectorState == 2 && leftSide){
						InterfaceNetwork.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				}
				
				@Override
				public void onReleased(){
					if(selectorState == 2){
						InterfaceNetwork.sendToServer(new PacketVehiclePartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				}
			};
			engineSelectors.put(engineNumber, engineSelector);
			addSelector(engineSelector);
		}
		
		//If we have reverse thrust, add a selector for it.
		if(haveReverseThrustOption){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
		}
		
		
		if(haveReverseThrustOption && vehicle.definition.motorized.hasCruiseControl){
			//If we have both reverse AND cruise control, render them side-by-side. otherwise just render one in the middle
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
			
			cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CRUISECONTROL, !vehicle.cruiseControl));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(cruiseControlSelector);
		}else{
			//If we have reverse thrust, add a selector for it.
			if(haveReverseThrustOption){
				reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
					}
					
					@Override
					public void onReleased(){}
				};
				addSelector(reverseSelector);
			}
			
			//If we have cruise control, add a selector for it.
			if(vehicle.definition.motorized.hasCruiseControl){
				cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CRUISECONTROL, !vehicle.cruiseControl));
					}
					
					@Override
					public void onReleased(){}
				};
				addSelector(cruiseControlSelector);
			}
		}
		
		//If we have gear, add a selector for it.
		if(vehicle.definition.motorized.gearSequenceDuration != 0){
			gearSelector = new GUIComponentSelector(guiLeft + xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE*2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.gear"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.GEAR, !vehicle.gearUpCommand));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(gearSelector);
		}
		
		//Create the 8 trailer selectors.  Note that not all may be rendered.
		for(int i=0; i<8; ++i){
			//Go to next column if we are on our 4th row.
			//Note that this happens on the 0 (first) row, so we don't need to add this value prior to this loop.
			if(i%4 == 0){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS*1.25;
			}
			
			GUIComponentSelector trailerSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.trailer") + "#" + i, vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					int trailerNumber = trailerSelectors.indexOf(this);
					EntityVehicleF_Physics currentVehicle = vehicle;
					for(int i=0; i<trailerNumber; ++ i){
						currentVehicle = currentVehicle.towedVehicle;
					}
					InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(currentVehicle, PacketVehicleControlDigital.Controls.TRAILER, true));
				}
				
				@Override
				public void onReleased(){}
			};
			trailerSelectors.add(trailerSelector);
			addSelector(trailerSelector);
		}
		
		//Create any custom slots, if we have any.
		if(vehicle.definition.rendering.customVariables != null && vehicle.definition.rendering.customVariables.size() > 0){
			xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
			for(int i=0; i<vehicle.definition.rendering.customVariables.size(); ++i){
				GUIComponentSelector customSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, vehicle.definition.rendering.customVariables.get(i), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CUSTOM_TEXTURE_WIDTH_OFFSET, CUSTOM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						byte selectorNumber = (byte) customSelectors.indexOf(this);
						switch(selectorNumber){
							case(0) : InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CUSTOM_0, !vehicle.customsOn.contains(selectorNumber))); break;
							case(1) : InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CUSTOM_1, !vehicle.customsOn.contains(selectorNumber))); break;
							case(2) : InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CUSTOM_2, !vehicle.customsOn.contains(selectorNumber))); break;
							case(3) : InterfaceNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.CUSTOM_3, !vehicle.customsOn.contains(selectorNumber))); break;
						}
					}
					
					@Override
					public void onReleased(){}
				};
				customSelectors.add(customSelector);
				addSelector(customSelector);
			}
		}
		
		return xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
	}
	
	@Override
	public void setStates(){
		//Set the state of the light selector.
		if(lightSelector != null){
			lightSelector.selectorState = vehicle.lightsOn.contains(LightType.HEADLIGHT) ? 2 : (vehicle.lightsOn.contains(LightType.RUNNINGLIGHT) ? 1 : 0);
		}
		
		//Set the state of the turn signal selector.
		if(turnSignalSelector != null){
			boolean halfSecondClock = inClockPeriod(20, 10);
			if(vehicle.lightsOn.contains(LightType.LEFTTURNLIGHT) && halfSecondClock){
				if(vehicle.lightsOn.contains(LightType.RIGHTTURNLIGHT)){
					turnSignalSelector.selectorState = 3;
				}else{
					turnSignalSelector.selectorState = 1;
				}
			}else if(vehicle.lightsOn.contains(LightType.RIGHTTURNLIGHT) && halfSecondClock){
				turnSignalSelector.selectorState = 2;
			}else{
				turnSignalSelector.selectorState = 0;
			}
		}
		 
		
		//If we have emergency lights, set the state of the emergency light selector.
		if(emergencySelector != null){
			emergencySelector.selectorState = vehicle.lightsOn.contains(LightType.EMERGENCYLIGHT) ? 1 : 0;
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
			if(currentVehicle != null && currentVehicle.definition.motorized.hitchPos != null){
				trailerSelectors.get(i).visible = true;
				trailerSelectors.get(i).selectorState = currentVehicle.towedVehicle != null ? 0 : 1;
				currentVehicle = currentVehicle.towedVehicle;
			}else{
				trailerSelectors.get(i).visible = false;
			}
		}
		
		//Iterate through custom selectors and set their states.
		for(byte i=0; i<customSelectors.size(); ++i){
			customSelectors.get(i).selectorState = vehicle.customsOn.contains(i) ? 1 : 0;
		}
	}
}
