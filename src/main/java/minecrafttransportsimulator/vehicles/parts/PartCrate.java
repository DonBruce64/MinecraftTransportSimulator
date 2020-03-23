package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class PartCrate extends APart{
	public final InventoryBasic crateInventory;
	
	public PartCrate(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.crateInventory = new InventoryBasic("", false, Math.min(definition.crate.rows, 6)*9);
		NBTTagList stackTagList = dataTag.getTagList("Items", 10);
        for (byte i = 0; i < stackTagList.tagCount(); ++i){
            NBTTagCompound stackTag = stackTagList.getCompoundTagAt(i);
            byte slot = (byte) (stackTag.getByte("Slot") & 255);
            crateInventory.setInventorySlotContents(slot, new ItemStack(stackTag));
        }
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!vehicle.locked){
			player.displayGUIChest(this.crateInventory);
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		NBTTagList stackTagList = new NBTTagList();
		for(byte i = 0; i < crateInventory.getSizeInventory(); ++i){
			ItemStack stack = crateInventory.getStackInSlot(i);
			if(!stack.isEmpty()){
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", i);
                stack.writeToNBT(stackTag);
                stackTagList.appendTag(stackTag);
			}
		}
		dataTag.setTag("Items", stackTagList);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
}
