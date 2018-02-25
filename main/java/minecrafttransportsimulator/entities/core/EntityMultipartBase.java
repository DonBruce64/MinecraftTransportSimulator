package minecrafttransportsimulator.entities.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityMultipartBase extends Entity{
	public boolean linked;
	public String UUID = "";

	public EntityMultipartBase(World world){
		super(world);
		this.preventEntitySpawning = true;
	}
	
	public boolean hasUUID(){
		if(this.UUID.equals("")){
			if(this.worldObj.isRemote){
				if(this.ticksExisted==1 || this.ticksExisted%10==0){
					MTS.MTSNet.sendToServer(new EntityClientRequestDataPacket(this.getEntityId()));
				}
				return false;
			}else{
				this.UUID=String.valueOf(this.getUniqueID());
			}
		}
		return true;
	}
	
	public void sendDataToClient(){
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		MTS.MTSNet.sendToAll(new ServerDataPacket(this.getEntityId(), tagCompound));
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.UUID=tagCompound.getString("UUID");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("UUID", this.UUID);
		return tagCompound;
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
