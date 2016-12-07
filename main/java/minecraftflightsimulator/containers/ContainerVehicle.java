package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerVehicle extends Container{
	protected EntityVehicle vehicle;
	
	public ContainerVehicle(InventoryPlayer invPlayer, EntityVehicle vehicle){
		this.vehicle = vehicle;
		vehicle.loadInventory(invPlayer.player.getDisplayName());
        for(int j=0; j<3; ++j){
            for(int k=0; k<9; ++k){
                this.addSlotToContainer(new Slot(invPlayer, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + 18*2 + 1));
            }
        }
        for (int j=0; j<9; ++j){
            this.addSlotToContainer(new Slot(invPlayer, j, 8 + j * 18, 161 + 18*2 + 1));
        }
        vehicle.initVehicleContainerSlots(this);
	}
	
	@Override
	public Slot addSlotToContainer(Slot slot){
		return super.addSlotToContainer(slot);
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return vehicle.isUseableByPlayer(player);
	}
	
	@Override
	public void onContainerClosed(EntityPlayer player){
		super.onContainerClosed(player);
		vehicle.saveInventory();
		vehicle.playerInInv = "";
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int sourceSlotIndex){
		ItemStack stack = getSlot(sourceSlotIndex).getStack();
		if(stack != null){
			for(int i=0; i<inventorySlots.size(); ++i){
				Slot slot = (Slot) inventorySlots.get(i);
				if(slot.inventory.equals(vehicle)){
					if(slot.isItemValid(stack) && stack.stackSize != 0){
						if(slot.getHasStack()){
							if(slot.getStack().stackSize < slot.getSlotStackLimit()){
								slot.getStack().stackSize += stack.splitStack(slot.getSlotStackLimit() - slot.getStack().stackSize).stackSize;
								vehicle.setInventorySlotContents(slot.getSlotIndex(), slot.getStack());
							}
						}else{
							slot.putStack(stack.splitStack(Math.min(slot.getSlotStackLimit(), stack.stackSize)));
						}
					}
				}
			}
			if(stack.stackSize == 0){
				((Slot) inventorySlots.get(sourceSlotIndex)).putStack(null);
			}
		}
		return null;		
	}
}
