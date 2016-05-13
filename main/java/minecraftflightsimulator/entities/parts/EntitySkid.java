package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.world.World;

public class EntitySkid extends EntityChild{
	public EntitySkid(World world){
		super(world);
		this.setSize(0.3F, 0.3F);
	}
	
	public EntitySkid(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
}
