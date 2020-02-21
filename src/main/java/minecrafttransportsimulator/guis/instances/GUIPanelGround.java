package minecrafttransportsimulator.guis.instances;

import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.EMERGENCYLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.HEADLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.LEFTTURNLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.RIGHTTURNLIGHT;
import static minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType.RUNNINGLIGHT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.SirenPacket;
import minecrafttransportsimulator.packets.control.TrailerPacket;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.wrappers.WrapperGUI;


/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelGround extends AGUIPanel<EntityVehicleF_Ground>{
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
	
	private GUIComponentSelector lightSelector;
	private GUIComponentSelector turnSignalSelector;
	private GUIComponentSelector emergencySelector;
	private GUIComponentSelector sirenSelector;
	private final Map<Byte, GUIComponentSelector> engineSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final List<GUIComponentSelector> trailerSelectors = new ArrayList<GUIComponentSelector>();
	
	public GUIPanelGround(EntityVehicleF_Ground groundVehicle){
		super(groundVehicle);
	}
	
	@Override
	protected int setupLightComponents(int guiLeft, int guiTop, int xOffset){
		//Create a tri-state selector for the running lights and headlights.
		//For the tri-state we need to make sure we don't try to turn on running lights if we don't have any.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, RUNNINGLIGHT) || RenderVehicle.doesVehicleHaveLight(vehicle, HEADLIGHT)){
			lightSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.headlights"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LIGHT_TEXTURE_WIDTH_OFFSET, LIGHT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						if(selectorState == 2){
							MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), HEADLIGHT));
						}else if(selectorState == 1){
							MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), RUNNINGLIGHT));
						}
					}else{
						if(selectorState == 0){
							if(RenderVehicle.doesVehicleHaveLight(vehicle, RUNNINGLIGHT)){
								MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), RUNNINGLIGHT));
							}else{
								MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), HEADLIGHT));
							}
						}else if(selectorState == 1){
							MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), HEADLIGHT));
						}
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(lightSelector);
		}
		
		//Add the turn signal selector if we have turn signals.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, LEFTTURNLIGHT) || RenderVehicle.doesVehicleHaveLight(vehicle, RIGHTTURNLIGHT)){
			turnSignalSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 1*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.turnsignals"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TURNSIGNAL_TEXTURE_WIDTH_OFFSET, TURNSIGNAL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), LEFTTURNLIGHT));
					}else{
						MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), RIGHTTURNLIGHT));
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(turnSignalSelector);
		}
		
		//Add the emergency light selector if we have those.
		if(RenderVehicle.doesVehicleHaveLight(vehicle, EMERGENCYLIGHT)){
			emergencySelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.emergencylights"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, EMERGENCY_TEXTURE_WIDTH_OFFSET, EMERGENCY_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), EMERGENCYLIGHT));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(emergencySelector);
		}
		
		//Add the siren selector if we have a siren.
		if(vehicle.definition.motorized.sirenSound != null){
			sirenSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.siren"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, SIREN_TEXTURE_WIDTH_OFFSET, SIREN_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					MTS.MTSNet.sendToServer(new SirenPacket(vehicle.getEntityId()));
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
			GUIComponentSelector engineSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS)*(engineNumber%4), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.engine"), vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINE_TEXTURE_WIDTH_OFFSET, ENGINE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(selectorState == 0 && !leftSide){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.MAGNETO_ON));
					}else if(selectorState == 1 && !leftSide){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.ES_ON));
					}else if(selectorState == 1 && leftSide){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.MAGNETO_OFF));
					}else if(selectorState == 2 && leftSide){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.ES_OFF));
					}
				}
				
				@Override
				public void onReleased(){
					if(selectorState == 2){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(vehicle.engines.get(engineNumber), PacketEngineTypes.ES_OFF));
					}
				}
			};
			engineSelectors.put(engineNumber, engineSelector);
			addSelector(engineSelector);
		}
		
		//Create the 8 trailer selectors.  Note that not all may be rendered.
		for(int i=0; i<8; ++i){
			//Go to next column if we are on our 4th row.
			//Note that this happens on the 0 (first) row, so we don't need to add this value prior to this loop.
			if(i%4 == 0){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS*1.25;
			}
			
			GUIComponentSelector trailerSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, WrapperGUI.translate("gui.panel.trailer") + "#" + i, vehicle.definition.rendering.panelTextColor, vehicle.definition.rendering.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					int trailerNumber = trailerSelectors.indexOf(this);
					EntityVehicleF_Ground currentVehicle = vehicle;
					for(int i=0; i<trailerNumber; ++ i){
						currentVehicle = currentVehicle.towedVehicle;
					}
					MTS.MTSNet.sendToServer(new TrailerPacket(currentVehicle.getEntityId()));
				}
				
				@Override
				public void onReleased(){}
			};
			trailerSelectors.add(trailerSelector);
			addSelector(trailerSelector);
		}
		return xOffset + GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;
	}
	
	@Override
	public void setStates(){
		//Set the state of the light selector.
		lightSelector.selectorState = vehicle.isLightOn(HEADLIGHT) ? 2 : (vehicle.isLightOn(RUNNINGLIGHT) ? 1 : 0);
		
		//Set the state of the turn signal selector.
		boolean halfSecondClock = WrapperGUI.inClockPeriod(20, 10);
		if(vehicle.isLightOn(LEFTTURNLIGHT) && halfSecondClock){
			if(vehicle.isLightOn(RIGHTTURNLIGHT)){
				turnSignalSelector.selectorState = 3;
			}else{
				turnSignalSelector.selectorState = 1;
			}
		}else if(vehicle.isLightOn(RIGHTTURNLIGHT) && halfSecondClock){
			turnSignalSelector.selectorState = 2;
		}else{
			turnSignalSelector.selectorState = 0;
		}
		 
		
		//If we have emergency lights, set the state of the emergency light selector.
		if(emergencySelector != null){
			emergencySelector.selectorState = vehicle.isLightOn(EMERGENCYLIGHT) ? 1 : 0;
		}
		
		//If we have a siren, set the state of the siren selector.
		if(sirenSelector != null){
			sirenSelector.selectorState = vehicle.sirenOn ? 1 : 0;
		}
		
		//Set the state of the engine selectors.
		for(Entry<Byte, GUIComponentSelector> engineEntry : engineSelectors.entrySet()){
			APartEngine.EngineStates engineState = vehicle.engines.get(engineEntry.getKey()).state;
			engineEntry.getValue().selectorState = !engineState.magnetoOn ? 0 : (!engineState.esOn ? 1 : 2);
		}
		
		//Iterate through trailers and set the visibility of the trailer selectors based on their state.
		EntityVehicleF_Ground currentVehicle = vehicle;
		for(int i=0; i<trailerSelectors.size(); ++i){
			if(currentVehicle != null && currentVehicle.definition.motorized.hitchPos != null){
				trailerSelectors.get(i).visible = true;
				currentVehicle = currentVehicle.towedVehicle;
			}else{
				trailerSelectors.get(i).visible = false;
			}
		}
	}
}
