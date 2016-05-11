package minecraftflightsimulator.entities.core;

import net.minecraft.world.World;

public class EntityCore extends EntityChild{
	public EntityCore(World world) {
		super(world);
		this.setSize(1.0F, 1.0F);
	}

	public EntityCore(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	@Override
	public boolean canBeCollidedWith(){
		return false;
	}
}
