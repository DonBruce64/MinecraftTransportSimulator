package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInstruments;
import minecrafttransportsimulator.rendering.RenderInstrument;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

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
	private final EntityVehicleE_Powered vehicle;
	private final EntityPlayer player;
	private final GUIHUD hudGUI;
	private final AGUIPanel<? extends EntityVehicleE_Powered> panelGUI;
	private final TreeMap<String, List<ItemInstrument>> playerInstruments = new TreeMap<String, List<ItemInstrument>>();
	
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
	
	public GUIInstruments(EntityVehicleE_Powered vehicle, EntityPlayer player){
		this.vehicle = vehicle;
		this.player = player;
		this.hudGUI = new GUIHUD(vehicle);
		this.panelGUI = vehicle instanceof EntityVehicleF_Ground ? new GUIPanelGround((EntityVehicleF_Ground) vehicle) : new GUIPanelAircraft((EntityVehicleF_Air) vehicle);
		
		//Add all packs that have instruments in them.
		//This depends on if the player has the instruments, or if they are in creative.
		boolean isPlayerCreative = player.capabilities.isCreativeMode;
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packID).values()){
				if(packItem instanceof ItemInstrument){
					if(isPlayerCreative || player.inventory.hasItemStack(new ItemStack(packItem))){
						//Player has this instrument, but can it go on this vehicle?
						if(((ItemInstrument) packItem).definition.general.validVehicles.contains(vehicle.definition.general.type)){
							//Add the instrument to the list of instruments the player has.
							if(!playerInstruments.containsKey(packID)){
								playerInstruments.put(packID, new ArrayList<ItemInstrument>());
								if(currentPack == null){
									currentPack = packID;
								}
							}
							playerInstruments.get(packID).add((ItemInstrument) packItem);
						}
					}
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
						MTS.MTSNet.sendToServer(new PacketVehicleInstruments(vehicle, player, (byte) vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle), playerInstruments.get(currentPack).get(instrumentSlots.indexOf(this))));
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
				GUIComponentItem instrumentItem = new GUIComponentItem(instrumentButton.x, instrumentButton.y, instrumentButtonSize/16F, null, 1, -1);
				addItem(instrumentItem);
				instrumentSlotIcons.add(instrumentItem);
			}
		}
		
		//Create the pack name label.
		addLabel(packName = new GUIComponentLabel(guiLeft + 40, guiTop - 85, Color.WHITE, "", 1.0F, false, false, 0));

		//Create the clear button.
		addButton(clearButton = new TexturelessButton(guiLeft + getWidth() - 2*instrumentButtonSize, guiTop - 75, 2*instrumentButtonSize, WrapperGUI.translate("gui.instruments.clear"), 2*instrumentButtonSize, true){
			@Override
			public void onClicked(){
				MTS.MTSNet.sendToServer(new PacketVehicleInstruments(vehicle, player, (byte) vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle), null));
				selectedInstrumentOnVehicle = null;
			}
		});
		
		//Create the HUD selection button.
		addButton(hudButton = new TexturelessButton(guiLeft, guiTop - 20, 100, WrapperGUI.translate("gui.instruments.main")){
			@Override
			public void onClicked(){
				hudSelected = true;
				selectedInstrumentOnVehicle = null;
				clearComponents();
				setupComponents(this.x, this.y + 20);
			}
		});
		
		//Create the panel selection button.
		addButton(panelButton = new TexturelessButton(guiLeft + getWidth() - 100, guiTop - 20, 100, WrapperGUI.translate("gui.instruments.control")){
			@Override
			public void onClicked(){
				hudSelected = false;
				selectedInstrumentOnVehicle = null;
				clearComponents();
				setupComponents(this.x - getWidth() + 100, this.y + 20);
			}
		});
		
		//Create the info label.
		addLabel(infoLabel = new GUIComponentLabel(guiLeft + getWidth()/2, guiTop - 20, Color.WHITE, "", 1.0F, true, false, 150));
		
		//Create the slots.
		//We need one for every instrument, present or not, as we can click on any instrument.
		vehicleInstrumentSlots.clear();
		for(PackInstrument packInstrument : vehicle.definition.motorized.instruments){
			int instrumentRadius = (int) (64F*packInstrument.hudScale);
			if(hudSelected ^ packInstrument.optionalEngineNumber != 0){
				TexturelessButton instrumentSlotButton = new TexturelessButton(guiLeft + packInstrument.hudX - instrumentRadius, guiTop + packInstrument.hudY - instrumentRadius, 2*instrumentRadius, "", 2*instrumentRadius, false){
					@Override
					public void onClicked(){
						selectedInstrumentOnVehicle = packInstrument;
					}
					
					@Override
					public void renderButton(int mouseX, int mouseY){
						//Don't render the button texture.  Instead, render a blank square if the instrument doesn't exist.
						//Otherwise, don't render anything at all as the instrument will be here instead.
						if(!vehicle.instruments.containsKey((byte) vehicle.definition.motorized.instruments.indexOf(packInstrument))){
							super.renderButton(mouseX, mouseY);
						}
						
						//If the currently-selected vehicle instrument is this instrument, render an overlay.
						//This happens even if there's an instrument rendered as we need to highlight it.
						if(packInstrument.equals(selectedInstrumentOnVehicle)){
							int instrumentRadius = (int) (64F*packInstrument.hudScale);
							if(WrapperGUI.inClockPeriod(40, 20)){
								GL11.glPushMatrix();
								GL11.glTranslatef(0, 0, 1.0F);
								WrapperGUI.renderRectangle(this.x, this.y, 2*instrumentRadius, 2*instrumentRadius, Color.WHITE);
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
			if(hudSelected ^ packInstrument.optionalEngineNumber != 0){
				GUIComponentInstrument vehicleInstrument = new GUIComponentInstrument(guiLeft, guiTop, i, vehicle){
					@Override
					public void renderInstrument(){
						//Only render this instrument if it exits in the vehicle.
						if(vehicle.instruments.containsKey(this.instrumentPackIndex)){
							GL11.glPushMatrix();
							GL11.glTranslated(x, y, 0);
							GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
							RenderInstrument.drawInstrument(vehicle.instruments.get(this.instrumentPackIndex), packInstrument.optionalEngineNumber, vehicle);
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
					instrumentSlotIcons.get(i).itemName = playerInstruments.get(currentPack).get(i).definition.packID + ":" + playerInstruments.get(currentPack).get(i).definition.systemName;
					
				}else{
					instrumentSlots.get(i).visible = false;
					instrumentSlotIcons.get(i).itemName = null;
				}
			}
			packName.text = Loader.instance().getIndexedModList().get(currentPack).getName();
		}
		
		//Set buttons depending on which vehicle section is selected.
		hudButton.enabled = !hudSelected;
		panelButton.enabled = hudSelected;
		
		//Set info and clear state based on if we've clicked an instrument.
		infoLabel.text = selectedInstrumentOnVehicle == null ? "\\/  " + WrapperGUI.translate("gui.instruments.idle") + "  \\/" : "/\\  " + WrapperGUI.translate("gui.instruments.decide") + "  /\\";
		clearButton.enabled = selectedInstrumentOnVehicle != null && vehicle.instruments.containsKey((byte) vehicle.definition.motorized.instruments.indexOf(selectedInstrumentOnVehicle));
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
						WrapperGUI.renderRectangle(this.x, this.y, this.width, this.height, Color.LIGHT_GRAY);
					}else{
						WrapperGUI.renderRectangle(this.x, this.y, this.width, this.height, Color.GRAY);
					}
				}else{
					WrapperGUI.renderRectangle(this.x, this.y, this.width, this.height, Color.BLACK);
				}
			}
		}
	}
}
