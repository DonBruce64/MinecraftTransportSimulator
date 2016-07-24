package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityWheelLarge extends EntityWheel{
	
	public EntityWheelLarge(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.wheelDiameter=0.6875F;
	}
	
	public EntityWheelLarge(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ);
	}
}