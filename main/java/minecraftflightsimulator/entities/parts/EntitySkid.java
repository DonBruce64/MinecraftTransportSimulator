package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityLandingGear;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.world.World;

public class EntitySkid extends EntityLandingGear{
	public EntitySkid(World world){
		super(world);
		this.setSize(0.3F, 0.3F);
	}
	
	public EntitySkid(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ);
	}
}
