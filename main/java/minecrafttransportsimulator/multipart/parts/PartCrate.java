package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.packets.general.PacketChat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class PartCrate extends APart{
	public final InventoryBasic crateInventory;
	
	public PartCrate(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
		this.crateInventory = new InventoryBasic("", false, 27);
		NBTTagList stackTagList = dataTag.getTagList("Items", 10);
        for (byte i = 0; i < stackTagList.tagCount(); ++i){
            NBTTagCompound stackTag = stackTagList.getCompoundTagAt(i);
            byte slot = (byte) (stackTag.getByte("Slot") & 255);
            crateInventory.setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(stackTag));
        }
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!player.worldObj.isRemote){
			if(!multipart.locked){
				player.displayGUIChest(this.crateInventory);
			}else{
				MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
			}
		}
		return true;
    }
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		NBTTagList stackTagList = new NBTTagList();
		for(byte i = 0; i < crateInventory.getSizeInventory(); ++i){
			ItemStack stack = crateInventory.getStackInSlot(i);
			if(stack != null){
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", (byte)i);
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
