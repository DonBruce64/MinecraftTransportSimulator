package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.world.World;

public class EntityPontoonDummy extends EntityPontoon{
	public EntityPontoonDummy(World world){
		super(world);
	}
	
	public EntityPontoonDummy(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
}
