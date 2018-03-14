package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineLycomingO360 extends EntityEngineAircraft{
	public EntityEngineLycomingO360(World world){
		super(world);
	}
	
	public EntityEngineLycomingO360(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineLycomingO360;
	}
}
