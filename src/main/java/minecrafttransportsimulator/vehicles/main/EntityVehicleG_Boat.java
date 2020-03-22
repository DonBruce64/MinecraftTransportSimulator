package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import net.minecraft.world.World;


public final class EntityVehicleG_Boat extends EntityVehicleF_Ground{	

	public EntityVehicleG_Boat(World world){
		super(world);
	}
	
	public EntityVehicleG_Boat(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected float getDragCoefficient(){
		return 2.0F;
	}
}