package minecraftflightsimulator.entities.core;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.packets.general.ClientRequestDataPacket;
import minecraftflightsimulator.packets.general.ServerDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
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
	
	@Override
    public boolean interactFirst(EntityPlayer player){
		return this.performRightClickAction(player);
	}
	/**
	 * Handler for all right-clicking actions performed.
	 * @param entityClicked the entity that was clicked
	 * @param player the player that clicked this entity
	 * 
	 * @return whether or not an action occurred.
	 */
	public abstract boolean performRightClickAction(EntityPlayer player);
	
	@Override
    public boolean attackEntityFrom(DamageSource source, float damage){
		return this.performAttackAction(source, damage);
	}
	/**
	 * Handler for all left-clicking actions performed.
	 * @param entityClicked the entity that was clicked
	 * @param player the player that clicked this entity
	 * 
	 * @return whether or not an action occurred.
	 */
	public abstract boolean performAttackAction(DamageSource source, float damage);
	
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
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double posX, double posY, double posZ, float yaw, float pitch, int p_70056_9_){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.renderDistanceWeight = 100;
    	this.ignoreFrustumCheck = true;
    }
	
	public void requestDataFromServer(){
		MFS.MFSNet.sendToServer(new ClientRequestDataPacket(this.getEntityId()));
	}
	
	public void sendDataToClient(){
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		MFS.MFSNet.sendToAll(new ServerDataPacket(this.getEntityId(), tagCompound));
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
