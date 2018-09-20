package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

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
        //We need to check here to make sure we don't get invalid indexes from pre-V11 fuel pumps.
        if(rotation >= EnumFacing.VALUES.length || rotation <= EnumFacing.UP.getIndex()){
        	rotation = (byte) EnumFacing.SOUTH.getIndex();
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setByte("rotation", rotation);
        return tagCompound;
    }
}
