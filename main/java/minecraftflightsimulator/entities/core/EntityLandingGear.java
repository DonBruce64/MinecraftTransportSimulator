package minecraftflightsimulator.entities.core;

import net.minecraft.world.World;

public abstract class EntityLandingGear extends EntityChild{
	public EntityLandingGear(World world) {
		super(world);
	}
	
	public EntityLandingGear(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
}
