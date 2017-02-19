package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.world.World;

public class EntityPontoonDummy extends EntityPontoon{
	public EntityPontoonDummy(World world){
		super(world);
	}
	
	public EntityPontoonDummy(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
	}
}
