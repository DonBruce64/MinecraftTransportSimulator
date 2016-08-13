package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityEngineLarge extends EntityEngineAircraft{

	public EntityEngineLarge(World world){
		super(world);
	}

	public EntityEngineLarge(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 1.2F, 1.2F, propertyCode);
	}
	
	@Override
	protected void entityInit(){
		this.starterPower = 25;
		this.starterIncrement = 22;
		this.engineRunningSoundName = "large_engine_running";
		this.engineCrankingSoundName = "large_engine_cranking";
	}

	@Override
	protected double getPropellerForcePenalty(){
		return Math.pow(1.5, 3 + (propeller.diameter - 70)/5)/20;
	}
}
