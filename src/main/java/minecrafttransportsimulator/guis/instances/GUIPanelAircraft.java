package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerConnection;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelAircraft extends AGUIPanel{
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
	private static final int AUTOPILOT_TEXTURE_WIDTH_OFFSET = TRIM_TEXTURE_WIDTH_OFFSET + 40;
	private static final int AUTOPILOT_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int GEAR_TEXTURE_WIDTH_OFFSET = AUTOPILOT_TEXTURE_WIDTH_OFFSET + 20;
	private static final int GEAR_TEXTURE_HEIGHT_OFFSET = 176;
	private static final int CUSTOM_TEXTURE_WIDTH_OFFSET = GEAR_TEXTURE_WIDTH_OFFSET + 20;
	private static final int CUSTOM_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int TRAILER_TEXTURE_WIDTH_OFFSET = CUSTOM_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TRAILER_TEXTURE_HEIGHT_OFFSET = 216;
	
	private final Map<String, GUIComponentSelector> lightSelectors = new HashMap<String, GUIComponentSelector>();
	private final Map<Byte, GUIComponentSelector> magnetoSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final Map<Byte, GUIComponentSelector> starterSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final List<GUIComponentSelector> customSelectors = new ArrayList<GUIComponentSelector>();
	private GUIComponentSelector aileronTrimSelector;
	private GUIComponentSelector elevatorTrimSelector;
	private GUIComponentSelector rudderTrimSelector;
	private GUIComponentSelector reverseSelector;
	private GUIComponentSelector autopilotSelector;
	private GUIComponentSelector gearSelector;
	private GUIComponentSelector trailerSelector;
	private GUIComponentTextBox beaconBox;
	
	private GUIComponentSelector selectedTrimSelector;
	private PacketVehicleControlDigital.Controls selectedTrimType = null;
	private boolean selectedTrimDirection;
	private boolean appliedTrimThisRender;
	
	public GUIPanelAircraft(EntityVehicleF_Physics aircraft){
		super(aircraft);
	}
	
	@Override
	protected void setupLightComponents(int guiLeft, int guiTop){
		lightSelectors.clear();
		//Create up to four lights depending on how many this vehicle has.
		if(vehicle.definition.motorized.hasNavLights){
			GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size()*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "NAV", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, NAVIGATION_TEXTURE_WIDTH_OFFSET, NAVIGATION_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "navigation_light"));
				}
				
				@Override
				public void onReleased(){}
			};
			lightSelectors.put("navigation_light", lightSwitch);
			addSelector(lightSwitch);
		}
		if(vehicle.definition.motorized.hasStrobeLights){
			GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size()*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "STROBE", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, STROBE_TEXTURE_WIDTH_OFFSET, STROBE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "strobe_light"));
				}
				
				@Override
				public void onReleased(){}
			};
			lightSelectors.put("strobe_light", lightSwitch);
			addSelector(lightSwitch);
		}
		if(vehicle.definition.motorized.hasTaxiLights){
			GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size()*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "TAXI", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TAXI_TEXTURE_WIDTH_OFFSET, TAXI_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "taxi_light"));
				}
				
				@Override
				public void onReleased(){}
			};
			lightSelectors.put("taxi_light", lightSwitch);
			addSelector(lightSwitch);
		}
		if(vehicle.definition.motorized.hasLandingLights){
			GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size()*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "LAND", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LANDING_TEXTURE_WIDTH_OFFSET, LANDING_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "landing_light"));
				}
				
				@Override
				public void onReleased(){}
			};
			lightSelectors.put("landing_light", lightSwitch);
			addSelector(lightSwitch);
		}
	}
	
	@Override
	protected void setupEngineComponents(int guiLeft, int guiTop){
		magnetoSelectors.clear();
		starterSelectors.clear();
		if(vehicle.definition.motorized.hasSingleEngineControl){
			if(!vehicle.engines.isEmpty()){
				GUIComponentSelector magnetoSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS, SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.magneto"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINEMAG_TEXTURE_WIDTH_OFFSET, ENGINEMAG_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						Signal sentSignal = null;
						for(PartEngine engine : vehicle.engines.values()){
							sentSignal = engine.state.magnetoOn ? Signal.MAGNETO_OFF : Signal.MAGNETO_ON;
							break;
						}
						for(PartEngine engine : vehicle.engines.values()){
							InterfacePacket.sendToServer(new PacketPartEngine(engine, sentSignal));
						}
					}
					
					@Override
					public void onReleased(){}
				};
				magnetoSelectors.put(ENGINE_SINGLE_SELECTOR_INDEX, magnetoSwitch);
				addSelector(magnetoSwitch);
				
				GUIComponentSelector starterSwitch = new GUIComponentSelector(magnetoSwitch.x + SELECTOR_SIZE, magnetoSwitch.y, SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.start"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINESTART_TEXTURE_WIDTH_OFFSET, ENGINESTART_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						Signal sentSignal = null;
						for(PartEngine engine : vehicle.engines.values()){
							if(!engine.state.magnetoOn){
								return;
							}
							sentSignal = engine.state.esOn ? Signal.ES_OFF : Signal.ES_ON;
							break;
						}
						for(PartEngine engine : vehicle.engines.values()){
							InterfacePacket.sendToServer(new PacketPartEngine(engine, sentSignal));
						}
					}
					
					@Override
					public void onReleased(){
						for(PartEngine engine : vehicle.engines.values()){
							InterfacePacket.sendToServer(new PacketPartEngine(engine, Signal.ES_OFF));
						}
					}
				};
				starterSelectors.put(ENGINE_SINGLE_SELECTOR_INDEX, starterSwitch);
				addSelector(starterSwitch);
			}
		}else{
			//Create magneto and stater selectors for the engines.
			for(Byte engineNumber : vehicle.engines.keySet()){
				//Go to next column if we are on our 5th engine.
				if(engineNumber == 4){
					xOffset += 2*SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
				}
				
				GUIComponentSelector magnetoSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS)*(engineNumber%4), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.magneto"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINEMAG_TEXTURE_WIDTH_OFFSET, ENGINEMAG_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), vehicle.engines.get(engineNumber).state.magnetoOn ? Signal.MAGNETO_OFF : Signal.MAGNETO_ON));
					}
					
					@Override
					public void onReleased(){}
				};
				magnetoSelectors.put(engineNumber, magnetoSwitch);
				addSelector(magnetoSwitch);
				
				GUIComponentSelector starterSwitch = new GUIComponentSelector(magnetoSwitch.x + SELECTOR_SIZE, magnetoSwitch.y, SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.start"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINESTART_TEXTURE_WIDTH_OFFSET, ENGINESTART_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						if(vehicle.engines.get(engineNumber).state.magnetoOn){
							InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), vehicle.engines.get(engineNumber).state.esOn ? Signal.ES_OFF : Signal.ES_ON));
						}
					}
					
					@Override
					public void onReleased(){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				};
				starterSelectors.put(engineNumber, starterSwitch);
				addSelector(starterSwitch);
			}
		}
		
		//Need to offset the xOffset by the selector size to account for the two engine controls.
		xOffset += SELECTOR_SIZE;
	}
	
	@Override
	protected void setupGeneralComponents(int guiLeft, int guiTop){
		//Add the trim selectors first.
		aileronTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.trim_roll"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
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
		
		elevatorTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 1*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.trim_pitch"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
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
		
		rudderTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.trim_yaw"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE*2, SELECTOR_TEXTURE_SIZE, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
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
		
		//If we have both reverse thrust AND autopilot, render them side-by-side. Otherwise just render one in the middle
		if(haveReverseThrustOption && vehicle.definition.motorized.hasAutopilot){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, selectorState == 0));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
			
			autopilotSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.autopilot"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, AUTOPILOT_TEXTURE_WIDTH_OFFSET, AUTOPILOT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.AUTOPILOT, !vehicle.autopilot));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(autopilotSelector);
		}else if(haveReverseThrustOption){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, selectorState == 0));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
		}else if(vehicle.definition.motorized.hasAutopilot){
			autopilotSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.autopilot"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, AUTOPILOT_TEXTURE_WIDTH_OFFSET, AUTOPILOT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.AUTOPILOT, !vehicle.autopilot));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(autopilotSelector);
		}
		
		//Need to offset the xOffset by the selector size to account for the double-width trim controls.
		xOffset += SELECTOR_SIZE;
	}
	
	@Override
	public void setupCustomComponents(int guiLeft, int guiTop){
		//Add custom selectors if we have any.
		//These are the right-most selector and are vehicle-specific.
		//We render two rows of side-by-side selectors here.
		Set<String> customVariables = new LinkedHashSet<String>();
		if(vehicle.definition.rendering.customVariables != null){
			customVariables.addAll(vehicle.definition.rendering.customVariables);
		}
		for(APart part : vehicle.parts){
			if(part.definition.rendering != null && part.definition.rendering.customVariables != null){
				customVariables.addAll(part.definition.rendering.customVariables);
			}
		}
		
		int variableNumber = 0;
		for(String customVariable : customVariables){
			GUIComponentSelector customSelector = new GUIComponentSelector(guiLeft + xOffset + (variableNumber%2)*SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + (variableNumber/2)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, customVariable, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CUSTOM_TEXTURE_WIDTH_OFFSET, CUSTOM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, this.text));
				}
				
				@Override
				public void onReleased(){}
			};
			customSelectors.add(customSelector);
			addSelector(customSelector);
			++variableNumber;
		}
		
		if(vehicle.definition.motorized.hasRadioNav){
			//Add beacon text box.  This is stacked below the custom selectors.
			beaconBox = new GUIComponentTextBox(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, vehicle.selectedBeaconName, SELECTOR_SIZE, vehicle.selectedBeacon != null ? Color.GREEN : Color.RED, Color.BLACK, 5){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					//Update the vehicle beacon state.
					InterfacePacket.sendToServer(new PacketVehicleBeaconChange(vehicle, getText()));
				}
			};
			addTextBox(beaconBox);
			
			//Add beacon text box label.
			GUIComponentLabel beaconLabel = new GUIComponentLabel(beaconBox.x + beaconBox.width/2, beaconBox.y + beaconBox.height + 1, vehicle.definition.motorized.panelTextColor != null ? Color.decode(vehicle.definition.motorized.panelTextColor) : Color.WHITE, InterfaceCore.translate("gui.panel.beacon"), null, TextPosition.CENTERED, 0, 0.75F, false);
			beaconLabel.setBox(beaconBox);
			labels.add(beaconLabel);
		}
		
		//If we have both gear and a trailer hookup, render them side-by-side. Otherwise just render one in the middle
		if(vehicle.definition.motorized.gearSequenceDuration != 0 && !trailerSwitchDefs.isEmpty()){
			gearSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.gear"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.GEAR, !vehicle.gearUpCommand));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(gearSelector);
			
			trailerSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, trailerSwitchDefs.get(0).connectionGroup.groupName, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					SwitchEntry switchDef = trailerSwitchDefs.get(0);
					InterfacePacket.sendToServer(new PacketEntityTrailerConnection(switchDef.entityOn, InterfaceClient.getClientPlayer(), switchDef.connectionGroupIndex));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(trailerSelector);
		}else if(vehicle.definition.motorized.gearSequenceDuration != 0){
			gearSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.gear"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.GEAR, !vehicle.gearUpCommand));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(gearSelector);
		}else if(!trailerSwitchDefs.isEmpty()){
			trailerSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, trailerSwitchDefs.get(0).connectionGroup.groupName, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					SwitchEntry switchDef = trailerSwitchDefs.get(0);
					InterfacePacket.sendToServer(new PacketEntityTrailerConnection(switchDef.entityOn, InterfaceClient.getClientPlayer(), switchDef.connectionGroupIndex));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(trailerSelector);
		}
	}
	
	@Override
	public void setStates(){
		//Set the states of the light selectors.
		for(Entry<String, GUIComponentSelector> lightEntry : lightSelectors.entrySet()){
			lightEntry.getValue().selectorState = vehicle.variablesOn.contains(lightEntry.getKey()) ? 1 : 0;
		}
		
		//Set the states of the magneto selectors.
		if(vehicle.definition.motorized.hasSingleEngineControl){
			magnetoSelectors.get((byte)-1).visible = !vehicle.engines.isEmpty();
			for(PartEngine engine : vehicle.engines.values()){
				magnetoSelectors.get((byte)-1).selectorState = engine.state.magnetoOn ? 1 : 0;
				break;
			}
		}else{
			for(Entry<Byte, GUIComponentSelector> magnetoEntry : magnetoSelectors.entrySet()){
				if(vehicle.engines.containsKey(magnetoEntry.getKey())){
					magnetoEntry.getValue().selectorState = vehicle.engines.get(magnetoEntry.getKey()).state.magnetoOn ? 1 : 0;
				}
			}
		}
		
		//Set the states of the starter selectors.
		if(vehicle.definition.motorized.hasSingleEngineControl){
			for(PartEngine engine : vehicle.engines.values()){
				starterSelectors.get(ENGINE_SINGLE_SELECTOR_INDEX).selectorState = engine.state.magnetoOn ? (engine.state.esOn ? 2 : 1) : 0;
				starterSelectors.get(ENGINE_SINGLE_SELECTOR_INDEX).visible = !engine.definition.engine.disableAutomaticStarter;
				break;
			}
		}else{
			for(Entry<Byte, GUIComponentSelector> starterEntry : starterSelectors.entrySet()){
				if(vehicle.engines.containsKey(starterEntry.getKey())){
					PartEngine engine = vehicle.engines.get(starterEntry.getKey());
					starterEntry.getValue().selectorState = engine.state.magnetoOn ? (engine.state.esOn ? 2 : 1) : 0;
					starterEntry.getValue().visible = !engine.definition.engine.disableAutomaticStarter;
				}
			}
		}
				
		//For every tick we have one of the trim selectors pressed, do the corresponding trim action.
		if(selectedTrimSelector != null){
			if(inClockPeriod(3, 1)){
				if(!appliedTrimThisRender){
					selectedTrimSelector.selectorState = selectedTrimSelector.selectorState == 0 ? 1 : 0; 
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, selectedTrimType, selectedTrimDirection));
					appliedTrimThisRender = true;
				}
			}else{
				appliedTrimThisRender = false;
			}
		}
		
		//If we have reverse thrust, set the selector state.
		if(reverseSelector != null){
			if(vehicle.definition.motorized.isBlimp){
				reverseSelector.selectorState = 0;
				for(PartEngine engine : vehicle.engines.values()){
					if(engine.currentGear < 0){
						reverseSelector.selectorState = 1;
						break;
					}
				}
			}else{
				reverseSelector.selectorState = vehicle.reverseThrust ? 1 : 0;
			}
		}
		
		//If we have autopilot, set the selector state.
		if(autopilotSelector != null){
			autopilotSelector.selectorState = vehicle.autopilot ? 1 : 0;
		}
		
		//If we have gear, set the selector state.
		if(gearSelector != null){
			if(vehicle.gearUpCommand){
				gearSelector.selectorState = vehicle.gearMovementTime == vehicle.definition.motorized.gearSequenceDuration ? 2 : 3;
			}else{
				gearSelector.selectorState = vehicle.gearMovementTime == 0 ? 0 : 1;
			}
		}
		
		//If we have a hitch, set the selector state.
		if(trailerSelector != null){
			SwitchEntry switchDef = trailerSwitchDefs.get(0);
			if(switchDef.connectionGroup.hookup){
				trailerSelector.selectorState = switchDef.entityOn.towedByConnection != null ? 0 : 1;
			}else{
				trailerSelector.selectorState = 1;
				for(TrailerConnection connection : switchDef.entityOn.getTowingConnections()){
					if(connection.hitchGroupIndex == switchDef.connectionGroupIndex){
						trailerSelector.selectorState = 0;
						break;
					}
				}
			}
		}
		
		//Set the beaconBox text color depending on if we have an active beacon.
		if(beaconBox != null){
			beaconBox.fontColor = vehicle.selectedBeacon != null ? Color.GREEN : Color.RED;
		}
		
		//Iterate through custom selectors and set their states.
		for(GUIComponentSelector customSelector : customSelectors){
			customSelector.selectorState = vehicle.variablesOn.contains(customSelector.text) ? 1 : 0;
		}
	}
}
