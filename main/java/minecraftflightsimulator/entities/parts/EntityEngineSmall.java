package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityEngineSmall extends EntityEngineAircraft{

	public EntityEngineSmall(World world){
		super(world);
	}

	public EntityEngineSmall(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 1.0F, 1.0F, propertyCode);
	}
	
	@Override
	protected void entityInit(){
		this.starterPower = 50;
		this.starterIncrement = 4;
		this.engineRunningSoundName = "small_engine_running";
		this.engineCrankingSoundName = "small_engine_cranking";
	}

	@Override
	protected double getPropellerForcePenalty(){
		return Math.pow(1.9, 3 + (propeller.diameter - 70)/5)/25 - 0.2;
	}
}
