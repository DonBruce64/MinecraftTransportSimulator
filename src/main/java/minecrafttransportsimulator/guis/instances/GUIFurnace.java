package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.jsondefs.JSONDecor.FurnaceComponentType;
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
	
	private final EntityFurnace furnace;
	
	public GUIFurnace(EntityFurnace furnace, String texture){
		super(texture);
		this.furnace = furnace;
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){
		super.setupComponents(guiLeft, guiTop);
		
		//Create the two or three inventory slots.
		//The third is for fuel, which isn't present if we don't have that type of furnace.
		GUIComponentButton smeltingItemButton = new GUIComponentButton(guiLeft + 52, guiTop + 21){
			@Override
			public void onClicked(){
				InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
			}
		};
		addButton(smeltingItemButton);
		interactableSlotButtons.add(smeltingItemButton);
		
		GUIComponentButton smeltedItemButton = new GUIComponentButton(guiLeft + 110, guiTop + 21){
			@Override
			public void onClicked(){
				InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
			}
		};
		addButton(smeltedItemButton);
		interactableSlotButtons.add(smeltedItemButton);
		
		if(furnace.type.equals(FurnaceComponentType.STANDARD)){
			GUIComponentButton fuelItemButton = new GUIComponentButton(guiLeft + 80, guiTop + 54){
				@Override
				public void onClicked(){
					InterfacePacket.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1));
				}
			};
			addButton(fuelItemButton);
			interactableSlotButtons.add(fuelItemButton);
		}
		
		//Add the section for the backplate that displays the current furnace type.
		int backplaneOffset = 0;
		switch(furnace.type){
			case STANDARD: backplaneOffset = 31; break;
			case FUEL: backplaneOffset = 49; break;
			case ELECTRIC: backplaneOffset = 67; break;
		}
		addCutout(new GUIComponentCutout(61, 53, 54, 18, 176, backplaneOffset));
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
