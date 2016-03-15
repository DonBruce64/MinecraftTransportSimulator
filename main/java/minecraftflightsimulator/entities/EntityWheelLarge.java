package minecraftflightsimulator.entities;

import net.minecraft.world.World;

public class EntityWheelLarge extends EntityWheel{
	
	public EntityWheelLarge(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.wheelDiameter=0.6875F;
	}
	
	public EntityWheelLarge(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 1);
	}
}