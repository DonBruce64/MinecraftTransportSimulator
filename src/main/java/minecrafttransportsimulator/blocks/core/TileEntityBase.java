package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityBase extends TileEntity{

	@Override
    public void validate(){
		super.validate();
        if(world.isRemote){
        	MTS.MTSNet.sendToServer(new PacketTileEntityClientServerHandshake(this, null));
        }
    }
	
	@Override
	public void handleUpdateTag(NBTTagCompound tag){
		//Do nothing here now instead of calling readFromNBT.
		//MC sends incomplete data here in later versions that doesn't contain any of the tags besides the core xyz position.
		//Loading from that tag will wipe all custom data, and that's bad.
	}
}
