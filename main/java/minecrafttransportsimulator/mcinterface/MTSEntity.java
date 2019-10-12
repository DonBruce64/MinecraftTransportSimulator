package minecrafttransportsimulator.mcinterface;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Class that is used by MTS for all Entity operations.  This provides a standard set of
 * methods for interacting with entities, so all entities in MTS should extend this
 * class to ensure they allow use of those methods.  We prefer extending the Entity
 * class over using a wrapper like we do for EntityPlayer as we frequently need to
 * make references to vehicles and having a wrapper for that would be a large
 * performance drain.
 * This segmentation also allows us to make a set of generic methods that we can reference
 * anywhere, all without having to worry about name changes during updates.
 * 
 * @author don_bruce
 */
public abstract class MTSEntity extends Entity{
	
	public MTSEntity(World worldIn){
		super(worldIn);
	}
	
	
	
	//---------------START OF OVERRIDEN METHODS---------------//	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
	
	
	
	//---------------START OF FORWARDED METHODS---------------//
	@Override 
	public void onEntityUpdate(){
		super.onEntityUpdate();
		handleUpdate();
	}
	/**Called when an update is scheduled.*/
	protected abstract void handleUpdate();
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		return handleAttack(source, damage);
	}
	/**Called when the entity is being attacked..*/
	protected abstract boolean handleAttack(DamageSource source, float damage);
	
	
	@Override
	public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		handleLoad(tag);
	}
	/**Called when the entity is being told to load itself from NBT.*/
	protected abstract void handleLoad(NBTTagCompound tag);
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		return handleSave(super.writeToNBT(tag));
	}
	/**Called when the entity is being told to save itself to NBT.*/
	protected abstract NBTTagCompound handleSave(NBTTagCompound tag);
}
