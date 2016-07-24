package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityPontoonDummy extends EntityPontoon{
	public EntityPontoonDummy(World world){
		super(world);
	}
	
	public EntityPontoonDummy(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ);
	}
}
