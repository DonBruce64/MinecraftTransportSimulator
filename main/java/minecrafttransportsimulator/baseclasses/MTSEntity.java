package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MTSEntity extends Entity{
	
	public MTSEntity(World world){
		super(world);
		this.preventEntitySpawning = true;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
	
	public void requestDataFromServer(){
		MTS.MTSNet.sendToServer(new EntityClientRequestDataPacket(this.getEntityId()));
	}
	
	public void sendDataToClient(){
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		MTS.MTSNet.sendToAll(new ServerDataPacket(this.getEntityId(), tagCompound));
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
