package minecraftflightsimulator.entities.core;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.packets.general.ClientRequestDataPacket;
import minecraftflightsimulator.packets.general.ServerSendDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class EntityBase extends Entity{
	public boolean linked;
	public String UUID = "";
	
	public EntityBase(World world) {
		super(world);
		this.preventEntitySpawning = true;
	}
	
	public boolean hasUUID(){
		if(this.UUID.equals("")){
			if(this.worldObj.isRemote){
				if(this.ticksExisted==1 || this.ticksExisted%10==0){
					this.requestDataFromServer();
				}
				return false;
			}else{
				this.UUID=String.valueOf(this.getUniqueID());
			}
		}
		return true;
	}
	
	@Override
	public float getShadowSize(){return 0.0F;}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double posX, double posY, double posZ, float yaw, float pitch, int p_70056_9_){
    	//Overridden due to stupid tracker behavior.
    	//super.setPositionAndRotation2(posX, posY, posZ, yaw, pitch, p_70056_9_);
    }
	
	public void requestDataFromServer(){
		MFS.MFSNet.sendToServer(new ClientRequestDataPacket(this.getEntityId()));
	}
	
	public void sendDataToClient(){
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		MFS.MFSNet.sendToAll(new ServerSendDataPacket(this.getEntityId(), tagCompound));
	}
	
	//Junk methods, forced to pull in.
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.UUID=tagCompound.getString("UUID");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("UUID", this.UUID);
	}
}
