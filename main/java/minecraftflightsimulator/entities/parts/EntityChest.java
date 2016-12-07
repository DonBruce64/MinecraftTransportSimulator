package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityChildInventory;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.world.World;

public class EntityChest extends EntityChildInventory{
	
	public EntityChest(World world){
		super(world);
	}
	
	public EntityChest(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
	}

	@Override
	protected String getChildInventoryName(){
		return "entity.mfs.Chest.name";
	}
}
