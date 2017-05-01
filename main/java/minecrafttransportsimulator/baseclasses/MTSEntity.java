package minecrafttransportsimulator.baseclasses;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MTSEntity extends Entity{
	
	public MTSEntity(World world){
		super(world);
		this.preventEntitySpawning = true;
	}
	
	@Override
	 public boolean processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand){
		return this.performRightClickAction(this, player);
	}
	/**
	 * Handler for all right-clicking actions performed.
	 * @param entityClicked the entity that was clicked
	 * @param player the player that clicked this entity
	 * 
	 * @return whether or not an action occurred.
	 */
	public abstract boolean performRightClickAction(MTSEntity clicked, EntityPlayer player);
	
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
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
    
    //Do not render entities this way.  Use custom render system instead.
    //This way requires lots of code changes due to the new render systems.
    //None of which are any good at doing efficient rendering anyways.
    //Also allows for parsed models and stuff.
    @Override
    public boolean shouldRenderInPass(int pass){
    	return false;
    }
	
	public void requestDataFromServer(){
		MTS.MFSNet.sendToServer(new EntityClientRequestDataPacket(this.getEntityId()));
	}
	
	public void sendDataToClient(){
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		MTS.MFSNet.sendToAll(new ServerDataPacket(this.getEntityId(), tagCompound));
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
