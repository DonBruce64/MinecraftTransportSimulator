package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**Core entities are like children except they cannot be removed from a parent.
 * Used primarily as bounding-box extensions for collision detection and impact calculations.
 * 
 * @author don_bruce
 */
public class EntityCore extends EntityMultipartChild{
	
	public EntityCore(World world) {
		super(world);
	}

	public EntityCore(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
		this.setSize(width, height);
	}
	

	@Override
	protected float getWidth(){
		return this.width;
	}

	@Override
	protected float getHeight(){
		return this.height;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(parent != null){
				return parent.attackEntityFrom(source, damage);
			}else{
				return super.attackEntityFrom(source, damage);
			}
		}
		return false;
    }
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		return false;
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return null;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.width=tagCompound.getFloat("width");
		this.height=tagCompound.getFloat("height");
    	this.setSize(width, height);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setFloat("width", this.width);
		tagCompound.setFloat("height", this.height);
		return tagCompound;
	}
}
