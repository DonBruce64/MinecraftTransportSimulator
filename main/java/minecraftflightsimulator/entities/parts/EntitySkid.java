package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityLandingGear;
import net.minecraft.world.World;

public class EntitySkid extends EntityLandingGear{
	public EntitySkid(World world){
		super(world);
		this.setSize(0.3F, 0.3F);
	}
	
	public EntitySkid(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 0.3F, 0.3F);
	}
}
