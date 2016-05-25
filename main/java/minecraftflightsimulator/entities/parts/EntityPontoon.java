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
	protected boolean isBlockAtLocation(double x, double y, double z){
		return worldObj.isAirBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
	}
}
