package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;

public final class PartCrate extends APart{
	public final InventoryBasic crateInventory;
	
	public PartCrate(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
		this.crateInventory = new InventoryBasic("entity." + MTS.MODID + ".crate.name", false, 27);
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
			player.displayGUIChest(this.crateInventory);
			//TODO see if we need this or not.
			//player.openGui(MTS.instance, this.multipart.getEntityId(), player.worldObj, (int) this.offset.xCoord, (int) this.offset.yCoord, (int) this.offset.zCoord);
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
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}
}
