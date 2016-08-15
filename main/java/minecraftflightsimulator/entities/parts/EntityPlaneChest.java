package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityChildInventory;
import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityPlaneChest extends EntityChildInventory{
	
	public EntityPlaneChest(World world){
		super(world);
	}
	
	public EntityPlaneChest(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
	}

	@Override
	protected String getChildInventoryName(){
		return "entity.mfs.Chest.name";
	}
}
