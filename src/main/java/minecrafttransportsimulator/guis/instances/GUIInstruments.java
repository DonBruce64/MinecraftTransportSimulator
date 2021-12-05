package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.lwjgl.opengl.GL11;

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
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;
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
		addButton(prevPackButton = new GUIComponentButton(guiLeft, guiTop - 74, 20, 20, "<", true, ColorRGB.WHITE, false){
			@Override
			public void onClicked(){
				currentPack = playerInstruments.lowerKey(currentPack);
			}
		});
		addButton(nextPackButton = new GUIComponentButton(guiLeft, guiTop - 52, 20, 20, ">", true, ColorRGB.WHITE, false){
			@Override
			public void onClicked(){
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
				GUIComponentButton instrumentButton = new GUIComponentButton(guiLeft + 23 + instrumentButtonSize*(i/2), guiTop - 75 + instrumentButtonSize*(i%2)){
					@Override
					public void onClicked(){
						InterfacePacket.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), playerInstruments.get(currentPack).get(instrumentSlots.indexOf(this))));
						selectedEntity = null;
						selectedInstrumentDefinition = null;
					}
				};
				addButton(instrumentButton);
				instrumentSlots.add(instrumentButton);
				
				//Item icons are normally rendered as 16x16 textures, so scale them to fit over the buttons.
				GUIComponentItem instrumentItem = new GUIComponentItem(instrumentButton.x, instrumentButton.y, instrumentButtonSize/16F, null);
				addItem(instrumentItem);
				instrumentSlotIcons.add(instrumentItem);
			}
		}
		
		//Create the pack name label.
		addLabel(packName = new GUIComponentLabel(guiLeft + 40, guiTop - 85, ColorRGB.WHITE, ""));

		//Create the clear button.
		addButton(clearButton = new GUIComponentButton(guiLeft + getWidth() - 2*instrumentButtonSize, guiTop - 75, 2*instrumentButtonSize, 2*instrumentButtonSize, InterfaceCore.translate("gui.instruments.clear"), true, ColorRGB.WHITE, false){
			@Override
			public void onClicked(){
				InterfacePacket.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), null));
				selectedEntity = null;
				selectedInstrumentDefinition = null;
			}
		});
		
		//Create the HUD selection button.
		addButton(hudButton = new GUIComponentButton(guiLeft, guiTop - 20, 100, 20, InterfaceCore.translate("gui.instruments.main"), true, ColorRGB.WHITE, false){
			@Override
			public void onClicked(){
				hudSelected = true;
				selectedEntity = null;
				selectedInstrumentDefinition = null;
				clearComponents();
				setupComponents(this.x, this.y + 20);
			}
		});
		
		//Create the panel selection button.
		addButton(panelButton = new GUIComponentButton(guiLeft + getWidth() - 100, guiTop - 20, 100, 20, InterfaceCore.translate("gui.instruments.control"), true, ColorRGB.WHITE, false){
			@Override
			public void onClicked(){
				hudSelected = false;
				selectedEntity = null;
				selectedInstrumentDefinition = null;
				clearComponents();
				setupComponents(this.x - getWidth() + 100, this.y + 20);
			}
		});
		
		//Create the info label.
		addLabel(infoLabel = new GUIComponentLabel(guiLeft + getWidth()/2, guiTop - 20, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F, 180));
		
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
		
		
		//Create the vehicle instruments.
		//We need one for every instrument present on every entity on the vehicle..
		//However, we create one for every possible instrument and render depending if it exists or not.
		//This allows us to render instruments as they are added or removed.
		entityInstrumentBlocks.clear();
		for(AEntityD_Interactable<?> entity : entitiesWithInstruments){
			for(JSONInstrumentDefinition packInstrument : entity.definition.instruments){
				if(hudSelected ^ packInstrument.placeOnPanel){
					InstrumentSlotBlock block = new InstrumentSlotBlock(guiLeft, guiTop, entity, packInstrument);					
						@Override
						public void renderButton(int mouseX, int mouseY){
							//Don't render the button texture.  Instead, render a blank square if the instrument doesn't exist.
							//Otherwise, don't render anything at all as the instrument will be here instead.
							if(!entity.instruments.containsKey(entity.definition.instruments.indexOf(packInstrument))){
								super.renderButton(mouseX, mouseY);
							}
							
							//If the currently-selected vehicle instrument is this instrument, render an overlay.
							//This happens even if there's an instrument rendered as we need to highlight it.
							if(entity.equals(selectedEntity) && packInstrument.equals(selectedInstrumentDefinition)){
								int selectedInstrumentRadius = (int) (64F*packInstrument.hudScale);
								if(inClockPeriod(40, 20)){
									GL11.glPushMatrix();
									GL11.glTranslatef(0, 0, 1.0F);
									InterfaceGUI.renderRectangle(this.x, this.y, 2*selectedInstrumentRadius, 2*selectedInstrumentRadius, ColorRGB.WHITE);
									GL11.glPopMatrix();
								}
							}
					    }
					};
					addButton(instrumentSlotButton);
					entityInstrumentButtons.add(instrumentSlotButton);
				}
			}
			entityInstrumentSlots.put(entity, entityInstrumentButtons);
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
				block.instrument.visible = entity.instruments.containsKey(block.instrument.instrumentPackIndex);
				if(block.instrument.visible){
					block.instrument.instrument = entity.instruments.get(block.instrument.instrumentPackIndex);
				}
				block.blank.visible = !entity.instruments.containsKey(block.instrument.instrumentPackIndex);
				block.selectorOverlay.visible = entity.equals(selectedEntity) && packInstrument.equals(selectedInstrumentDefinition)){
					int selectedInstrumentRadius = (int) (64F*packInstrument.hudScale);
					if(inClockPeriod(40, 20)){
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
	
	private class InstrumentSlotBlock{
		private final GUIComponentInstrument instrument;
		private final GUIComponentButton button;
		private final GUIComponentCutout selectorOverlay;
		private final GUIComponentCutout blank;
		
		private InstrumentSlotBlock(int guiLeft, int guiTop, AEntityD_Interactable<?> entity, JSONInstrumentDefinition packInstrument){
			int instrumentRadius = (int) (64F*packInstrument.hudScale);
			this.instrument = new GUIComponentInstrument(guiLeft, guiTop, entity.definition.instruments.indexOf(packInstrument), entity);
			this.button = new GUIComponentButton(guiLeft + packInstrument.hudX - instrumentRadius, guiTop + packInstrument.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius){
				@Override
				public void onClicked(){
					selectedEntity = entity;
					selectedInstrumentDefinition = packInstrument;
				}
			};
			this.selectorOverlay = new GUIComponentCutout(guiLeft + packInstrument.hudX - instrumentRadius, guiTop + packInstrument.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius, 448, 64, 2*instrumentRadius, 2*instrumentRadius);
			this.blank = new GUIComponentCutout(guiLeft + packInstrument.hudX - instrumentRadius, guiTop + packInstrument.hudY - instrumentRadius, 2*instrumentRadius, 2*instrumentRadius, 448, 0, 2*instrumentRadius, 2*instrumentRadius);
		}
	}
}
