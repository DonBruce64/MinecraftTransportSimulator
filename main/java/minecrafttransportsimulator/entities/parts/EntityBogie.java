package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityRollingStock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public abstract class EntityBogie extends EntityMultipartChild{
	public float angularPosition;
	public float angularVelocity;
	protected float wheelDiameter;
	private EntityMultipartMoving moving;
	
	public EntityBogie(World world){
		super(world);
	}
	
	public EntityBogie(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, (EntityRollingStock) parent, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
	}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		return false;
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public void onUpdate(){
		moving = (EntityMultipartMoving) this.parent;
		if(worldObj.isRemote){
			if(this.isOnGround()){
				angularVelocity = (float) (moving.velocity/wheelDiameter);
			}else{
				if(moving.brakeOn || moving.parkingBrakeOn){
					angularVelocity = 0;
				}else if(angularVelocity>0){
					angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
				}
			}
			angularPosition += angularVelocity;
		}
		
	}
	
	public static class EntityBogieLocomotive extends EntityBogie{
		public EntityBogieLocomotive(World world){
			super(world);
			//TODO get the right number here.
			this.wheelDiameter=1.0F;
		}
		
		public EntityBogieLocomotive(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
		}

		@Override
		public ItemStack getItemStack(){
			return new ItemStack(MTSRegistry.bogie);
		}
	}
}
