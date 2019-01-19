package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityRotatable extends TileEntity{
	public byte rotation;

	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MTS.MTSNet.sendToServer(new PacketTileEntityClientServerHandshake(this, null));
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
