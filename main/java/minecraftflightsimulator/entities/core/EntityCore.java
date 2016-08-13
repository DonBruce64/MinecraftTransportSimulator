package minecraftflightsimulator.entities.core;

import net.minecraft.world.World;

public class EntityCore extends EntityChild{
	
	public EntityCore(World world) {
		super(world);
	}

	public EntityCore(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
	}
}
