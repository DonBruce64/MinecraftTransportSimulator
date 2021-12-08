package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.PackParserSystem;

/**A GUI that is used to put instruments into vehicles.  This GUI is essentially an overlay
 * to {@link GUIHUD} and {@link AGUIPanel} that uses the textures from those GUIs, but does
 * custom rendering over them rather than the usual rendering routines.  This prevents players
 * from messing with the panel while adding instruments, as well as easier tracking of the
 * spots where blank instruments are located (normally those aren't saved in variables).
 * 
 * @author don_bruce
 */
public class GUIInstruments extends AGUIBase{	
	
	//GUIs components created at opening.
	private final EntityVehicleF_Physics vehicle;
	private final WrapperPlayer player;
	private final GUIHUD hudGUI;
	private final AGUIPanel panelGUI;
	private final TreeMap<String, List<ItemInstrument>> playerInstruments = new TreeMap<String, List<ItemInstrument>>();
	
	//Runtime variables.
	private GUIComponentButton prevPackButton;
	private GUIComponentButton nextPackButton;
	private GUIComponentButton clearButton;
	private String currentPack;
	private GUIComponentLabel packName;
	
	private boolean hudSelected = true;
	private GUIComponentButton hudButton;
	private GUIComponentButton panelButton;
	private GUIComponentLabel infoLabel;
	private AEntityD_Interactable<?> selectedEntity;
	private JSONInstrumentDefinition selectedInstrumentDefinition;
	
	private final List<GUIComponentButton> instrumentSlots = new ArrayList<GUIComponentButton>();
	private final List<GUIComponentItem> instrumentSlotIcons = new ArrayList<GUIComponentItem>();
	private final Map<AEntityD_Interactable<?>, List<InstrumentSlotBlock>> entityInstrumentBlocks = new HashMap<AEntityD_Interactable<?>, List<InstrumentSlotBlock>>();
	
	public GUIInstruments(EntityVehicleF_Physics vehicle){
		this.vehicle = vehicle;
		this.player = InterfaceClient.getClientPlayer();
		this.hudGUI = new GUIHUD(vehicle);
		this.panelGUI = vehicle.definition.motorized.isAircraft ? new GUIPanelAircraft(vehicle) : new GUIPanelGround(vehicle);
		
		//Add all packs that have instruments in them.
		//This depends on if the player has the instruments, or if they are in creative.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			if(packItem instanceof ItemInstrument){
				if(player.isCreative() || player.getInventory().hasItem(packItem)){
					//Add the instrument to the list of instruments the player has.
					if(!playerInstruments.containsKey(packItem.definition.packID)){
						playerInstruments.put(packItem.definition.packID, new ArrayList<ItemInstrument>());
						if(currentPack == null){
							currentPack = packItem.definition.packID;
						}
					}
					playerInstruments.get(packItem.definition.packID).add((ItemInstrument) packItem);
				}
			}
		}
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){	
		//Create the prior and next pack buttons.
		addComponent(prevPackButton = new GUIComponentButton(guiLeft, guiTop - 74, 20, 20, "<", true, ColorRGB.WHITE){
			@Override
			public void onClicked(boolean leftSide){
				currentPack = playerInstruments.lowerKey(currentPack);
			}
		});
		addComponent(nextPackButton = new GUIComponentButton(guiLeft, guiTop - 52, 20, 20, ">", true, ColorRGB.WHITE){
			@Override
			public void onClicked(boolean leftSide){
				currentPack = playerInstruments.higherKey(currentPack);
			}
		});
		
		//Create the player instrument buttons and icons.  This is a static list of 20 slots, though not all may be rendered.
		//That depends if there are that many instruments present for the currentPack.
		instrumentSlots.clear();
		instrumentSlotIcons.clear();
		final int instrumentButtonSize = 22;
		if(currentPack != null){
			for(byte i=0; i<30; ++i){				
				GUIComponentButton instrumentButton = new GUIComponentButton(guiLeft + 23 + instrumentButtonSize*(i/2), guiTop - 75 + instrumentButtonSize*(i%2), instrumentButtonSize, instrumentButtonSize){
					@Override
					public void onClicked(boolean leftSide){
						InterfacePacket.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), playerInstruments.get(currentPack).get(instrumentSlots.indexOf(this))));
						selectedEntity = null;
						selectedInstrumentDefinition = null;
					}
				};
				addComponent(instrumentButton);
				instrumentSlots.add(instrumentButton);
				
				GUIComponentItem instrumentItem = new GUIComponentItem(instrumentButton);
				addComponent(instrumentItem);
				instrumentSlotIcons.add(instrumentItem);
			}
		}
		
		//Create the pack name label.
		addComponent(packName = new GUIComponentLabel(guiLeft + 40, guiTop - 85, ColorRGB.WHITE, ""));

		//Create the clear button.
		addComponent(clearButton = new GUIComponentButton(guiLeft + getWidth() - 2*instrumentButtonSize, guiTop - 75, 2*instrumentButtonSize, 2*instrumentButtonSize, InterfaceCore.translate("gui.instruments.clear"), true, ColorRGB.WHITE){
			@Override
			public void onClicked(boolean leftSide){
				InterfacePacket.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), null));
				selectedEntity = null;
				selectedInstrumentDefinition = null;
			}
		});
		
		//Create the HUD selection button.
		addComponent(hudButton = new GUIComponentButton(guiLeft, guiTop - 20, 100, 20, InterfaceCore.translate("gui.instruments.main"), true, ColorRGB.WHITE){
			@Override
			public void onClicked(boolean leftSide){
				hudSelected = true;
				selectedEntity = null;
				selectedInstrumentDefinition = null;
				clearComponents();
				setupComponents(this.x, this.y + 20);
			}
		});
		
		//Create the panel selection button.
		addComponent(panelButton = new GUIComponentButton(guiLeft + getWidth() - 100, guiTop - 20, 100, 20, InterfaceCore.translate("gui.instruments.control"), true, ColorRGB.WHITE){
			@Override
			public void onClicked(boolean leftSide){
				hudSelected = false;
				selectedEntity = null;
				selectedInstrumentDefinition = null;
				clearComponents();
				setupComponents(this.x - getWidth() + 100, this.y + 20);
			}
		});
		
		//Create the info label.
		addComponent(infoLabel = new GUIComponentLabel(guiLeft + getWidth()/2, guiTop - 20, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));
		
		//Get all entities with instruments and adds them to the list. definitions, and add them to a map-list.
		//These come from the vehicle and all parts.
		List<AEntityD_Interactable<?>> entitiesWithInstruments = new ArrayList<AEntityD_Interactable<?>>();
		if(vehicle.definition.instruments != null){
			entitiesWithInstruments.add(vehicle);
		}
		for(APart part : vehicle.parts){
			if(part.definition.instruments != null){
				entitiesWithInstruments.add(part);
			}
		}
		
		//Create the slots.
		//We need one for every instrument, present or not, as we can click on any instrument.
		//This allows us to render instruments as they are added or removed.
		entityInstrumentBlocks.clear();
		for(AEntityD_Interactable<?> entity : entitiesWithInstruments){
			List<InstrumentSlotBlock> instrumentBlocks = new ArrayList<InstrumentSlotBlock>();
			for(JSONInstrumentDefinition packInstrument : entity.definition.instruments){
				if(hudSelected ^ packInstrument.placeOnPanel){
					instrumentBlocks.add(new InstrumentSlotBlock(guiLeft, guiTop, entity, packInstrument));
				}
			}
			entityInstrumentBlocks.put(entity, instrumentBlocks);
		}
	}

	@Override
	public void setStates(){
		//Set pack prior and pack next buttons depending if we have such packs.
		prevPackButton.enabled = playerInstruments.lowerKey(currentPack) != null;
		nextPackButton.enabled = playerInstruments.higherKey(currentPack) != null;
		
		//Set instrument icon and button states depending on which instruments the player has.
		if(currentPack != null){
			for(int i=0; i<instrumentSlots.size(); ++i){
				if(playerInstruments.get(currentPack).size() > i){
					instrumentSlots.get(i).visible = true;
					instrumentSlots.get(i).enabled = selectedInstrumentDefinition != null;
					instrumentSlotIcons.get(i).stack = playerInstruments.get(currentPack).get(i).getNewStack();
					
				}else{
					instrumentSlots.get(i).visible = false;
					instrumentSlotIcons.get(i).stack = null;
				}
			}
			packName.text = PackParserSystem.getPackConfiguration(currentPack).packName;
		}
		
		//Set entity instrument states.
		for(AEntityD_Interactable<?> entity : entityInstrumentBlocks.keySet()){
			for(InstrumentSlotBlock block : entityInstrumentBlocks.get(entity)){
				block.selectorOverlay.visible = entity.equals(selectedEntity) && block.definition.equals(selectedInstrumentDefinition) && inClockPeriod(40, 20);
				block.instrument.visible = !block.selectorOverlay.visible && entity.instruments.containsKey(block.instrument.instrumentPackIndex);
				if(block.instrument.visible){
					block.instrument.instrument = entity.instruments.get(block.instrument.instrumentPackIndex);
				}
				block.blank.visible = !block.selectorOverlay.visible && !block.instrument.visible;
				
			}
		}
			
		
		//Set buttons depending on which vehicle section is selected.
		hudButton.enabled = !hudSelected;
		panelButton.enabled = hudSelected;
		
		//Set info and clear state based on if we've clicked an instrument.
		infoLabel.text = selectedInstrumentDefinition == null ? "\\/  " + InterfaceCore.translate("gui.instruments.idle") + "  \\/" : "/\\  " + InterfaceCore.translate("gui.instruments.decide") + "  /\\";
		clearButton.enabled = selectedInstrumentDefinition != null && selectedEntity.instruments.containsKey(selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition));
	}
	
	@Override
	public boolean renderDarkBackground(){
		return true;
	}
	
	@Override
	public GUILightingMode getGUILightMode(){
		return hudSelected ? hudGUI.getGUILightMode() : panelGUI.getGUILightMode();
	}
	
	@Override
	public EntityVehicleF_Physics getGUILightSource(){
		return hudSelected ? hudGUI.getGUILightSource() : panelGUI.getGUILightSource();
	}
	
	@Override
	public int getWidth(){
		return hudSelected ? hudGUI.getWidth() : panelGUI.getWidth();
	}
	
	@Override
	public int getHeight(){
		return hudSelected ? hudGUI.getHeight() : panelGUI.getHeight();
	}
	
	@Override
	public boolean renderFlushBottom(){
		return hudSelected ? hudGUI.renderFlushBottom() : panelGUI.renderFlushBottom();
	}
	
	@Override
	public boolean renderTranslucent(){
		return hudSelected ? hudGUI.renderTranslucent() : panelGUI.renderTranslucent();
	}
	
	@Override
	public String getTexture(){
		return hudSelected ? hudGUI.getTexture() : panelGUI.getTexture();
	}
	
	private class InstrumentSlotBlock{
		private final JSONInstrumentDefinition definition;
		private final GUIComponentInstrument instrument;
		@SuppressWarnings("unused")//We use this, the complier is just too dumb to realize it.
		private final GUIComponentButton button;
		private final GUIComponentCutout blank;
		private final GUIComponentCutout selectorOverlay;
		
		private InstrumentSlotBlock(int guiLeft, int guiTop, AEntityD_Interactable<?> entity, JSONInstrumentDefinition definition){
			this.definition = definition;
			int instrumentRadius = (int) (64F*definition.hudScale);
			addComponent(this.instrument = new GUIComponentInstrument(guiLeft, guiTop, entity.definition.instruments.indexOf(definition), entity));
			addComponent(this.button = new GUIComponentButton(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius){
				@Override
				public void onClicked(boolean leftSide){
					selectedEntity = entity;
					selectedInstrumentDefinition = definition;
				}
			});
			addComponent(this.blank = new GUIComponentCutout(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius, 448, 0, 2*instrumentRadius, 2*instrumentRadius));
			addComponent(this.selectorOverlay = new GUIComponentCutout(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius, 448, 64, 2*instrumentRadius, 2*instrumentRadius));
		}
	}
}
