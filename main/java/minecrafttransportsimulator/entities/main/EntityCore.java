package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
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
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!worldObj.isRemote){
			return parent != null ? parent.attackEntityFrom(source, damage) : false;
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
}
