package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.world.World;

public abstract class EntityRollingStock extends EntityMultipartMoving{

	public EntityRollingStock(World world){
		super(world);
	}
	
	public EntityRollingStock(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!linked){return;}
		
	}
	
	@Override
	protected float getExplosionStrength(){
		return 0;
	}
}
