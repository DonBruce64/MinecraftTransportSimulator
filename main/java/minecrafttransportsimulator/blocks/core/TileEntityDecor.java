package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityDecor extends TileEntity{
	public String decorName;
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
        this.decorName = tagCompound.getString("decorName");
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setByte("rotation", rotation);
        tagCompound.setString("decorName", this.decorName);
        return tagCompound;
    }
}
