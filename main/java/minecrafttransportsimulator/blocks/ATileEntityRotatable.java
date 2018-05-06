package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.TileEntityClientServerHandshakePacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public abstract class ATileEntityRotatable extends TileEntity{
	public byte rotation;

	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MTS.MTSNet.sendToServer(new TileEntityClientServerHandshakePacket(this, null));
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
