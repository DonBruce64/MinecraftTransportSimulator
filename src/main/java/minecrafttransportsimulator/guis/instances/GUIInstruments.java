package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketVehicleInstruments;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

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
	private final GUIHUD hudGUI;
	private final AGUIPanel panelGUI;
	private final TreeMap<String, List<IWrapperItemStack>> playerInstruments = new TreeMap<String, List<IWrapperItemStack>>();
	
	//Runtime variables.
	private TexturelessButton prevPackButton;
	private TexturelessButton nextPackButton;
	private TexturelessButton clearButton;
	private String currentPack;
	private GUIComponentLabel packName;
	
	private boolean hudSelected = true;
	private TexturelessButton hudButton;
	private TexturelessButton panelButton;
	private GUIComponentLabel infoLabel;
	private PackInstrument selectedInstrumentOnVehicle;
	
	private final List<TexturelessButton> instrumentSlots = new ArrayList<TexturelessButton>();
	private final List<GUIComponentItem> instrumentSlotIcons = new ArrayList<GUIComponentItem>();
	private final List<TexturelessButton> vehicleInstrumentSlots = new ArrayList<TexturelessButton>();
	private final List<GUIComponentInstrument> vehicleInstruments = new ArrayList<GUIComponentInstrument>();
	
	public GUIInstruments(EntityVehicleF_Physics vehicle, IWrapperPlayer player){
		this.vehicle = vehicle;
		this.hudGUI = new GUIHUD(vehicle);
		this.panelGUI = vehicle.definition.general.isAircraft ? new GUIPanelAircraft(vehicle) : new GUIPanelGround(vehicle);
		
		//Add all packs that have instruments in them.
		//This depends on if the player has the instruments, or if they are in creative.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			if(packItem instanceof ItemInstrument){
				if(player.isCreative() || player.getInventory().hasItem(packItem)){
					//Add the instrument to the list of instruments the player has.
					if(!playerInstruments.containsKey(packItem.definition.packID)){
						playerInstruments.put(packItem.definition.packID, new ArrayList<IWrapperItemStack>());
						if(currentPack == null){
							currentPack = packItem.definition.packID;
						}
					}
					playerInstruments.get(packItem.definition.packID).add(MasterLoader.coreInterface.getStack(packItem));
				}
			}
		}
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){	
		//Create the prior and next pack buttons.
		addButton(prevPackButton = new TexturelessButton(guiLeft, guiTop - 74, 20, "<"){
			@Override
			public void onClicked(){
				currentPack = playerInstruments.lowerKey(currentPack);
			}
		});
		addButton(nextPackButton = new TexturelessButton(guiLeft, guiTop - 52, 20, ">"){
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
				TexturelessButton instrumentButton = new TexturelessButton(guiLeft + 23 + instrumentButtonSize*(i/2), guiTop - 75 + instrumentButtonSize*(i%2), instrumentButtonSize, "", instrumentButtonSize, false){
					@Override
					public void onClicked(){
						MasterLoader.networkInterface.sendToServer(new PacketVehicleInstruments(vehicle, vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle), (ItemInstrument) playerInstruments.get(currentPack).get(instrumentSlots.indexOf(this)).getItem()));
						selectedInstrumentOnVehicle = null;
					}
					
					@Override
					public void renderButton(int mouseX, int mouseY){
						super.renderButton(mouseX, mouseY);
						//Don't render anything.  This is done by the icon object.
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
		addLabel(packName = new GUIComponentLabel(guiLeft + 40, guiTop - 85, Color.WHITE, ""));

		//Create the clear button.
		addButton(clearButton = new TexturelessButton(guiLeft + getWidth() - 2*instrumentButtonSize, guiTop - 75, 2*instrumentButtonSize, MasterLoader.coreInterface.translate("gui.instruments.clear"), 2*instrumentButtonSize, true){
			@Override
			public void onClicked(){
				MasterLoader.networkInterface.sendToServer(new PacketVehicleInstruments(vehicle, vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle), null));
				selectedInstrumentOnVehicle = null;
			}
		});
		
		//Create the HUD selection button.
		addButton(hudButton = new TexturelessButton(guiLeft, guiTop - 20, 100, MasterLoader.coreInterface.translate("gui.instruments.main")){
			@Override
			public void onClicked(){
				hudSelected = true;
				selectedInstrumentOnVehicle = null;
				clearComponents();
				setupComponents(this.x, this.y + 20);
			}
		});
		
		//Create the panel selection button.
		addButton(panelButton = new TexturelessButton(guiLeft + getWidth() - 100, guiTop - 20, 100, MasterLoader.coreInterface.translate("gui.instruments.control")){
			@Override
			public void onClicked(){
				hudSelected = false;
				selectedInstrumentOnVehicle = null;
				clearComponents();
				setupComponents(this.x - getWidth() + 100, this.y + 20);
			}
		});
		
		//Create the info label.
		addLabel(infoLabel = new GUIComponentLabel(guiLeft + getWidth()/2, guiTop - 20, Color.WHITE, "", TextPosition.CENTERED, 150, 1.0F, false));
		
		//Create the slots.
		//We need one for every instrument, present or not, as we can click on any instrument.
		vehicleInstrumentSlots.clear();
		for(PackInstrument packInstrument : vehicle.definition.motorized.instruments){
			int instrumentRadius = (int) (64F*packInstrument.hudScale);
			if(hudSelected ^ packInstrument.optionalPartNumber != 0){
				TexturelessButton instrumentSlotButton = new TexturelessButton(guiLeft + packInstrument.hudX - instrumentRadius, guiTop + packInstrument.hudY - instrumentRadius, 2*instrumentRadius, "", 2*instrumentRadius, false){
					@Override
					public void onClicked(){
						selectedInstrumentOnVehicle = packInstrument;
					}
					
					@Override
					public void renderButton(int mouseX, int mouseY){
						//Don't render the button texture.  Instead, render a blank square if the instrument doesn't exist.
						//Otherwise, don't render anything at all as the instrument will be here instead.
						if(!vehicle.instruments.containsKey(vehicle.definition.motorized.instruments.indexOf(packInstrument))){
							super.renderButton(mouseX, mouseY);
						}
						
						//If the currently-selected vehicle instrument is this instrument, render an overlay.
						//This happens even if there's an instrument rendered as we need to highlight it.
						if(packInstrument.equals(selectedInstrumentOnVehicle)){
							int instrumentRadius = (int) (64F*packInstrument.hudScale);
							if(inClockPeriod(40, 20)){
								GL11.glPushMatrix();
								GL11.glTranslatef(0, 0, 1.0F);
								MasterLoader.guiInterface.renderRectangle(this.x, this.y, 2*instrumentRadius, 2*instrumentRadius, Color.WHITE);
								GL11.glPopMatrix();
							}
						}
				    }
				};
				addButton(instrumentSlotButton);
				vehicleInstrumentSlots.add(instrumentSlotButton);
			}
		}
		
		//Create the vehicle instruments.
		//We need one for every instrument present in the vehicle.
		//However, we create one for every possible instrument and render depending if it exists or not.
		//This allows us to render instruments as they are added or removed.
		vehicleInstruments.clear();
		for(byte i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
			if(hudSelected ^ packInstrument.optionalPartNumber != 0){
				GUIComponentInstrument vehicleInstrument = new GUIComponentInstrument(guiLeft, guiTop, i, vehicle){
					@Override
					public void renderInstrument(){
						//Only render this instrument if it exits in the vehicle.
						if(vehicle.instruments.containsKey(instrumentPackIndex)){
							GL11.glPushMatrix();
							GL11.glTranslated(x, y, 0);
							GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
							RenderInstrument.drawInstrument(vehicle.instruments.get(instrumentPackIndex), packInstrument.optionalPartNumber, vehicle);
							GL11.glPopMatrix();
						}
					}
				};
				addInstrument(vehicleInstrument);
				vehicleInstruments.add(vehicleInstrument);
			}
		}
	}

	@Override
	public void setStates(){
		//Set pack prior and pack next buttons depending if we have such packs.
		prevPackButton.enabled = playerInstruments.lowerKey(currentPack) != null;
		nextPackButton.enabled = playerInstruments.higherKey(currentPack) != null;
		
		//Set instrument icon and button states depending on which instruments the player has.
		if(currentPack != null){
			for(byte i=0; i<instrumentSlots.size(); ++i){
				if(playerInstruments.get(currentPack).size() > i){
					instrumentSlots.get(i).visible = true;
					instrumentSlots.get(i).enabled = selectedInstrumentOnVehicle != null;
					instrumentSlotIcons.get(i).stack = playerInstruments.get(currentPack).get(i);
					
				}else{
					instrumentSlots.get(i).visible = false;
					instrumentSlotIcons.get(i).stack = null;
				}
			}
			packName.text = PackParserSystem.getPackConfiguration(currentPack).packName;
		}
		
		//Set buttons depending on which vehicle section is selected.
		hudButton.enabled = !hudSelected;
		panelButton.enabled = hudSelected;
		
		//Set info and clear state based on if we've clicked an instrument.
		infoLabel.text = selectedInstrumentOnVehicle == null ? "\\/  " + MasterLoader.coreInterface.translate("gui.instruments.idle") + "  \\/" : "/\\  " + MasterLoader.coreInterface.translate("gui.instruments.decide") + "  /\\";
		clearButton.enabled = selectedInstrumentOnVehicle != null && vehicle.instruments.containsKey(vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle));
	}
	
	@Override
	public boolean renderDarkBackground(){
		return true;
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
		return true;
	}
	
	@Override
	public String getTexture(){
		return hudSelected ? hudGUI.getTexture() : panelGUI.getTexture();
	}
	
	/**Custom implementation of the button class that doesn't use textures for the button rendering.
	 * This is needed for this GUI as we bind the panel texture instead, which would make the buttons
	 * render wrongly.
	 *
	 * @author don_bruce
	 */
	private abstract class TexturelessButton extends GUIComponentButton{

		public TexturelessButton(int x, int y, int width, String text){
			super(x, y, width, text);
		}
		
		public TexturelessButton(int x, int y, int width, String text, int height, boolean centeredText){
			super(x, y, width, text, height, centeredText);
		}

		@Override
		public void renderButton(int mouseX, int mouseY){
			//Don't render the texture as it would be bound wrong.
			//Instead, render just a plain background depending on state.
			if(visible){
				if(enabled){
					if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
						MasterLoader.guiInterface.renderRectangle(this.x, this.y, this.width, this.height, Color.LIGHT_GRAY);
					}else{
						MasterLoader.guiInterface.renderRectangle(this.x, this.y, this.width, this.height, Color.GRAY);
					}
				}else{
					MasterLoader.guiInterface.renderRectangle(this.x, this.y, this.width, this.height, Color.BLACK);
				}
			}
		}
	}
}
