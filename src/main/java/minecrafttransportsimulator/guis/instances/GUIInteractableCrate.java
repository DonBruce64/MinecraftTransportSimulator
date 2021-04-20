package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import net.minecraft.item.ItemStack;

/**A GUI that is used to interface intractable parts.  Displays the player's items on the bottom,
 * and the items in the parts in the top.  Works a bit differently than the MC GUIs, as it
 * doesn't support item dragging or movement.  Just storage to the first available slot.
 * 
 * @author don_bruce
 */
public class GUIInteractableCrate extends AGUIBase{
	private static final int ITEM_BUTTON_SIZE = 18;
	private static final int MAX_ITEMS_PER_SCREEN = 54;
	
	//GUIs components created at opening.
	private GUIComponentButton priorRowButton;
	private GUIComponentButton nextRowButton;
	private final int maxRowIncrements;
	
	private final PartInteractable interactable;
	private final WrapperPlayer player;
	private final WrapperInventory playerInventory;
	private final List<ItemSelectionButton> interactableSlotButtons = new ArrayList<ItemSelectionButton>();
	private final List<GUIComponentItem> interactableSlotIcons = new ArrayList<GUIComponentItem>();
	private final List<ItemSelectionButton> playerSlotButtons = new ArrayList<ItemSelectionButton>();
	private final List<GUIComponentItem> playerSlotIcons = new ArrayList<GUIComponentItem>();
	
	//Runtime variables.
	private int rowOffset;
	
	public GUIInteractableCrate(PartInteractable interactable){
		this.interactable = interactable;
		this.player = InterfaceClient.getClientPlayer();
		this.playerInventory = player.getInventory();
		this.maxRowIncrements = interactable.inventory.size() > MAX_ITEMS_PER_SCREEN ? (interactable.inventory.size() - MAX_ITEMS_PER_SCREEN)/9 + 1 : 0;
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){
		//Create the slider.  This is a button, but doesn't do anything.
		if(maxRowIncrements > 0){
			//Create the prior and next row buttons.
			addButton(priorRowButton = new GUIComponentButton(guiLeft + 174, guiTop + 11, 12, "", 7, true, 12, 7, 220, 0, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(){
					--rowOffset;
				}
			});
			addButton(nextRowButton = new GUIComponentButton(guiLeft + 174, guiTop + 112, 12, "", 7, true, 12, 7, 232, 0, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(){
					++rowOffset;
				}
			});
		
			GUIComponentButton sliderButton;
			addButton(sliderButton = new GUIComponentButton(guiLeft + 174, guiTop + 21, 12, "", 15, true, 12, 15, 244, 0, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(){}
				
				@Override
				public void renderButton(int mouseX, int mouseY){
					//Don't render the normal button way. The slider isn't a button.
					//First render the slider box.
					int sliderBoxWidth = 14;
					int sliderBoxHeight = 90;
					InterfaceGUI.renderSheetTexture(guiLeft + 173, guiTop + 20, sliderBoxWidth, sliderBoxHeight, 242, 45, 242 + sliderBoxWidth, 45 + sliderBoxHeight, getTextureWidth(), getTextureHeight());
					
					//Now render the slider itself.
					//Slider goes down 73 pixels to bottom.
					int yOffset = 73*rowOffset/maxRowIncrements;
					int textureUStart = buttonSectionHeightOffset + 1*buttonSectionHeight;
					
					//Render slider.
					InterfaceGUI.renderSheetTexture(x, y + yOffset, width, height, buttonSectionWidthOffset, textureUStart, buttonSectionWidthOffset + width, textureUStart + buttonSectionHeight, getTextureWidth(), getTextureHeight());
			    }
			});
			sliderButton.enabled = false;
		}
		
		//Create all inventory slots.  This is variable based on the size of the inventory, and can result in multiple pages.
		//However, one page can hold 6 rows, so we make all those slots and adjust as appropriate.
		interactableSlotButtons.clear();
		interactableSlotIcons.clear();
		int slotsToMake = Math.min(interactable.inventory.size(), MAX_ITEMS_PER_SCREEN);
		int inventoryRowOffset = (MAX_ITEMS_PER_SCREEN - slotsToMake)*ITEM_BUTTON_SIZE/9/2;
		for(byte i=0; i<slotsToMake; ++i){				
			ItemSelectionButton itemButton = new ItemSelectionButton(guiLeft + 8 + ITEM_BUTTON_SIZE*(i%9), guiTop + 12 + inventoryRowOffset + ITEM_BUTTON_SIZE*(i/9)){
				@Override
				public void onClicked(){
					InterfacePacket.sendToServer(new PacketPartInteractable(interactable, player, interactableSlotButtons.indexOf(this), -1));
				}
			};
			addButton(itemButton);
			interactableSlotButtons.add(itemButton);
			
			//Item icons are normally rendered as 16x16 textures, so scale them to fit over the buttons.
			GUIComponentItem itemIcon = new GUIComponentItem(itemButton.x, itemButton.y, ITEM_BUTTON_SIZE/16F, null);
			addItem(itemIcon);
			interactableSlotIcons.add(itemIcon);
		}
		
		//Create the player item buttons and icons.  This is a static list of all 36 slots.
		//Rendering will occur if the player has an item in that slot.
		playerSlotButtons.clear();
		playerSlotIcons.clear();
		int yOffset = 197;
		for(byte i=0; i<36; ++i){				
			ItemSelectionButton itemButton = new ItemSelectionButton(guiLeft + 7 + ITEM_BUTTON_SIZE*(i%9), guiTop + yOffset){
				@Override
				public void onClicked(){
					InterfacePacket.sendToServer(new PacketPartInteractable(interactable, player, -1, playerSlotButtons.indexOf(this)));
				}
			};
			addButton(itemButton);
			playerSlotButtons.add(itemButton);
			
			//Item icons are normally rendered as 16x16 textures, so scale them to fit over the buttons.
			GUIComponentItem itemIcon = new GUIComponentItem(itemButton.x, itemButton.y, ITEM_BUTTON_SIZE/16F, null);
			addItem(itemIcon);
			playerSlotIcons.add(itemIcon);
			
			//Move offset up to next row if required.
			if(i == 8){
				yOffset = 175;
			}else if(i == 17 || i == 26){
				yOffset -= ITEM_BUTTON_SIZE;
			}
		}
	}

	@Override
	public void setStates(){
		//Set next and prior row button states, if we have scrolling.
		if(maxRowIncrements > 0){
			priorRowButton.enabled = rowOffset > 0;
			nextRowButton.enabled = rowOffset < maxRowIncrements;
		}
		
		//Set player item icons to player inventory.
		for(int i=0; i<playerSlotButtons.size(); ++i){
			ItemStack stack = playerInventory.getStackInSlot(i);
			playerSlotButtons.get(i).enabled = !stack.isEmpty();
			playerSlotIcons.get(i).stack = stack;
		}
		
		//Set other item icons to other inventory.
		for(int i=0; i<interactableSlotButtons.size(); ++i){
			int index = i + 9*rowOffset;
			if(interactable.inventory.size() > index){
				ItemStack stack = interactable.inventory.get(index);
				interactableSlotButtons.get(i).visible = true;
				interactableSlotButtons.get(i).enabled = !stack.isEmpty();
				interactableSlotIcons.get(i).stack = stack;
			}else{
				interactableSlotButtons.get(i).visible = false;
				interactableSlotIcons.get(i).stack = null;
			}
		}
	}
	
	@Override
	public int getWidth(){
		return 194;
	}
	
	@Override
	public int getHeight(){
		return 221;
	}
	
	@Override
	public String getTexture(){
		return interactable.definition.interactable.inventoryTexture != null ? interactable.definition.interactable.inventoryTexture : "mts:textures/guis/inventory.png";
	}
	
	/**Custom implementation of the button class that doesn't use textures for the button rendering.
	 * This is needed for this GUI for rendering over items to let us select them.
	 *
	 * @author don_bruce
	 */
	private abstract class ItemSelectionButton extends GUIComponentButton{

		public ItemSelectionButton(int x, int y){
			super(x, y, ITEM_BUTTON_SIZE, "", ITEM_BUTTON_SIZE, true, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE, 194, 0, 256, 256);
		}
	}
}
