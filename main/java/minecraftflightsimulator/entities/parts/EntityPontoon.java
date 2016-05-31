package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityLandingGear;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class EntityPontoon extends EntityLandingGear{
	public EntityPontoon(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntityPontoon(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(worldObj.getBlock(MathHelper.floor_double(posX), MathHelper.floor_double(posY + 0.75), MathHelper.floor_double(posZ)).getMaterial().isLiquid()){
			parent.motionY += 0.1;
		}
	}
	
	@Override
	protected boolean isBlockAtLocation(double x, double y, double z){
		return worldObj.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y + 0.35), MathHelper.floor_double(z)).getMaterial().isLiquid() ? true : super.isBlockAtLocation(x, y, z);
	}
}
