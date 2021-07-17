package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import net.minecraft.item.ItemStack;

/**A GUI that is used to interface with inventory containers.   Displays the player's items on the bottom,
 * and the items in the container in the top.  Works a bit differently than the MC GUIs, as it
 * doesn't support item dragging or movement.  Just storage to the first available slot.
 * 
 * @author don_bruce
 */
public class GUIInventoryContainer extends AGUIInventory{
	private static final int MAX_ITEMS_PER_SCREEN = 54;
	
	//GUIs components created at opening.
	private GUIComponentButton priorRowButton;
	private GUIComponentButton nextRowButton;
	private final int maxRowIncrements;
	
	private final EntityInventoryContainer inventory;
	
	//Runtime variables.
	private int rowOffset;
	
	public GUIInventoryContainer(EntityInventoryContainer inventory, String texture){
		super(texture);
		this.inventory = inventory;
		this.maxRowIncrements = inventory.getSize() > MAX_ITEMS_PER_SCREEN ? (inventory.getSize() - MAX_ITEMS_PER_SCREEN)/9 + 1 : 0;
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){
		super.setupComponents(guiLeft, guiTop);
		
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
		int slotsToMake = Math.min(inventory.getSize(), MAX_ITEMS_PER_SCREEN);
		int inventoryRowOffset = (MAX_ITEMS_PER_SCREEN - slotsToMake)*GUIComponentButton.ITEM_BUTTON_SIZE/9/2;
		for(byte i=0; i<slotsToMake; ++i){				
			GUIComponentButton itemButton = new GUIComponentButton(guiLeft + 8 + GUIComponentButton.ITEM_BUTTON_SIZE*(i%9), guiTop + 12 + inventoryRowOffset + GUIComponentButton.ITEM_BUTTON_SIZE*(i/9)){
				@Override
				public void onClicked(){
					InterfacePacket.sendToServer(new PacketPlayerItemTransfer(inventory, player, interactableSlotButtons.indexOf(this), -1));
				}
			};
			addButton(itemButton);
			interactableSlotButtons.add(itemButton);
			
			//Item icons are normally rendered as 16x16 textures, so scale them to fit over the buttons.
			GUIComponentItem itemIcon = new GUIComponentItem(itemButton.x, itemButton.y, GUIComponentButton.ITEM_BUTTON_SIZE/16F, null);
			addItem(itemIcon);
			interactableSlotIcons.add(itemIcon);
		}
	}

	@Override
	public void setStates(){
		super.setStates();
		//Set next and prior row button states, if we have scrolling.
		if(maxRowIncrements > 0){
			priorRowButton.enabled = rowOffset > 0;
			nextRowButton.enabled = rowOffset < maxRowIncrements;
		}
		
		//Set other item icons to other inventory.
		for(int i=0; i<interactableSlotButtons.size(); ++i){
			int index = i + 9*rowOffset;
			if(inventory.getSize() > index){
				ItemStack stack = inventory.getStack(index);
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
	protected void handlePlayerItemClick(int slotClicked){
		InterfacePacket.sendToServer(new PacketPlayerItemTransfer(inventory, player, -1, slotClicked));
	}
}
