package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**Core entities are like children except they cannot be removed from a parent.
 * Used primarily as bounding-box extensions for collision detection and impact calculations.
 * 
 * See about moving this bit of code to EntityMultipartMoving and have it handle collision.
 * We can do this now that only GDs collide and cores are known and don't change.
 * @author don_bruce
 */
@Deprecated
public class EntityCore extends EntityMultipartChild{
	
	public EntityCore(World world) {
		super(world);
	}

	public EntityCore(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int collisionIndex){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, collisionIndex);
		this.setSize(width, height);
	}

	@Override
	protected float getWidth(){
		if(parent != null){
			if(((EntityMultipartMoving) parent).getCollisionBoxes().size() > this.propertyCode){
				return ((EntityMultipartMoving) parent).getCollisionBoxes().get(this.propertyCode)[3];
			}
		}
		return 1.0F;
	}

	@Override
	protected float getHeight(){
		if(parent != null){
			if(((EntityMultipartMoving) parent).getCollisionBoxes().size() > this.propertyCode){
				return ((EntityMultipartMoving) parent).getCollisionBoxes().get(this.propertyCode)[4];
			}
		}
		return 1.0F;
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
}
