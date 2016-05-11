package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.world.World;

public class EntityWheelSmall extends EntityWheel{
	
	public EntityWheelSmall(World world){
		super(world);
		this.setSize(0.5F, 0.5F);
		this.wheelDiameter=0.4375F;
	}
	
	public EntityWheelSmall(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
}
