package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.jsondefs.JSONPart.FurnaceComponentType;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import net.minecraft.item.ItemStack;

/**A GUI that is used to interface with furnaces.   Displays the player's items on the bottom,
 * and the furnace type/status in the top.  Works a bit differently than the MC GUIs, as it
 * doesn't support item dragging or movement.  Rather, furnace fuel is clicked to put it into
 * the furnace, and items are clicked to add them to the furnace and remove them when smelted.
 * 
 * @author don_bruce
 */
public class GUIFurnace extends AGUIInventory{
	
	private GUIComponentCutout fuelIcon;
	
	private final EntityFurnace furnace;
	
	public GUIFurnace(EntityFurnace furnace, String texture){
		super(texture != null ? texture : "mts:textures/guis/furnace.png");
		this.furnace = furnace;
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){
		super.setupComponents(guiLeft, guiTop);
		//Create the two or three inventory slots.
		//The third is for fuel, which isn't present if we don't have that type of furnace.
		interactableSlotButtons.clear();
		interactableSlotIcons.clear();
		
		GUIComponentButton smeltingItemButton = new GUIComponentButton(guiLeft + 52, guiTop + 21, false){
			@Override
			public void onClicked(boolean leftSide){
				InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
			}
		};
		addComponent(smeltingItemButton);
		interactableSlotButtons.add(smeltingItemButton);
		
		GUIComponentItem smeltingItemIcon = new GUIComponentItem(smeltingItemButton);
		addComponent(smeltingItemIcon);
		interactableSlotIcons.add(smeltingItemIcon);
		
		
		
		GUIComponentButton smeltedItemButton = new GUIComponentButton(guiLeft + 110, guiTop + 21, false){
			@Override
			public void onClicked(boolean leftSide){
				InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
			}
		};
		addComponent(smeltedItemButton);
		interactableSlotButtons.add(smeltedItemButton);
		
		GUIComponentItem smeltedItemIcon = new GUIComponentItem(smeltedItemButton);
		addComponent(smeltedItemIcon);
		interactableSlotIcons.add(smeltedItemIcon);
		
		if(furnace.definition.furnaceType.equals(FurnaceComponentType.STANDARD)){
			GUIComponentButton fuelItemButton = new GUIComponentButton(guiLeft + 79, guiTop + 53, false){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
				}
			};
			addComponent(fuelItemButton);
			interactableSlotButtons.add(fuelItemButton);
			
			GUIComponentItem fuelItemIcon = new GUIComponentItem(fuelItemButton);
			addComponent(fuelItemIcon);
			interactableSlotIcons.add(fuelItemIcon);
		}
		
		//Add the section for the backplate that displays the current furnace type.
		int backplaneOffset = 0;
		switch(furnace.definition.furnaceType){
			case STANDARD: backplaneOffset = 31; break;
			case FUEL: backplaneOffset = 49; break;
			case ELECTRIC: backplaneOffset = 67; break;
		}
		addComponent(new GUIComponentCutout(guiLeft + 61, guiTop + 53, 54, 18, 176, backplaneOffset));
		
		//Add the flame section that displays how much fuel the furnace has.
		addComponent(this.fuelIcon = new GUIComponentCutout(guiLeft + 81, guiTop + 38, 14, 14, 176, 0){
			@Override
			public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
				if(furnace.definition.furnaceType.equals(FurnaceComponentType.ELECTRIC)){
					super.render(mouseX, mouseY, textureWidth, textureHeight, blendingEnabled, partialTicks);
				}else{
					if(furnace.ticksLeftOfFuel > 0){
						int pixelsRemoved = (int) (textureSectionHeight - textureSectionHeight*((double)furnace.ticksLeftOfFuel/furnace.ticksAddedOfFuel));
						//This could be over due to packet lag.
						if(pixelsRemoved < 0){
							pixelsRemoved = 0;
						}
				    	InterfaceGUI.renderSheetTexture(x + offsetX, y + offsetY + pixelsRemoved, width, height - pixelsRemoved, textureXOffset, textureYOffset + pixelsRemoved, textureXOffset + textureSectionWidth, textureYOffset + textureSectionHeight, textureWidth, textureHeight);
					}
				}
		    }
		});
		
		//Add the arrow section that displays how far along the smelting operation is.
		addComponent(new GUIComponentCutout(guiLeft + 77, guiTop + 20, 24, 17, 176, 14){
			@Override
			public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
				if(furnace.ticksLeftToSmelt > 0){
					int pixelsRemoved = (int) (textureSectionWidth*((double)furnace.ticksLeftToSmelt/furnace.ticksNeededToSmelt));
			    	InterfaceGUI.renderSheetTexture(x + offsetX, y + offsetY, width - pixelsRemoved, height, textureXOffset, textureYOffset, textureXOffset + textureSectionWidth - pixelsRemoved, textureYOffset + textureSectionHeight, textureWidth, textureHeight);
				}
		    }
		});
	}

	@Override
	public void setStates(){
		super.setStates();
		//Set other item icons to other inventory.
		for(int i=0; i<interactableSlotButtons.size(); ++i){
			ItemStack stack = furnace.getStack(i);
			interactableSlotButtons.get(i).enabled = !stack.isEmpty();
			interactableSlotIcons.get(i).stack = stack;
		}
		
		fuelIcon.visible = furnace.ticksLeftOfFuel > 0;
	}
	
	@Override
	protected void handlePlayerItemClick(int slotClicked){
		InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, -1, slotClicked));
	}
	
	@Override
	protected int getPlayerInventoryOffset(){
		return 142;
	}
	
	@Override
	public int getWidth(){
		return 176;
	}
	
	@Override
	public int getHeight(){
		return 166;
	}
}
