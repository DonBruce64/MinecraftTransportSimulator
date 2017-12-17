package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.TileEntityClientRequestDataPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public abstract class MTSTileEntity extends TileEntity{
	public byte rotation;

	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MTS.MTSNet.sendToServer(new TileEntityClientRequestDataPacket(this));
        }
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.rotation = tagCompound.getByte("rotation");
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setByte("rotation", rotation);
        return tagCompound;
    }


}
