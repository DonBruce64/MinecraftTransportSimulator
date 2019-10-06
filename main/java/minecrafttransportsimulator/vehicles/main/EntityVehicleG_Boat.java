package minecrafttransportsimulator.vehicles.main;

import net.minecraft.world.World;


public final class EntityVehicleG_Boat extends EntityVehicleF_Ground{	

	public EntityVehicleG_Boat(World world){
		super(world);
	}
	
	public EntityVehicleG_Boat(World world, float posX, float posY, float posZ, float rotation, String vehicleName){
		super(world, posX, posY, posZ, rotation, vehicleName);
	}
	
	@Override
	protected float getDragCoefficient(){
		return 1.0F;
	}
}