package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityChildInventory;
import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityPlaneChest extends EntityChildInventory{
	
	public EntityPlaneChest(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntityPlaneChest(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	protected String getDisplayName(){
		return "entity.mfs.Chest.name";
	}
}
