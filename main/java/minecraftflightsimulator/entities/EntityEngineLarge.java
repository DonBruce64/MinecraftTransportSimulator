package minecraftflightsimulator.entities;

import net.minecraft.world.World;

public class EntityEngineLarge extends EntityEngine{

	public EntityEngineLarge(World world){
		super(world);
		this.setSize(1.2F, 1.2F);
	}

	public EntityEngineLarge(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
	}
	
	@Override
	protected void entityInit(){
		//TODO find other sounds.
		this.starterPower = 50;
		this.starterIncrement = 4;
		this.engineRunningSoundName = "large_engine_running";
		this.engineCrankingSoundName = "small_engine_cranking";
		this.engineStartingSoundName = "small_engine_starting";
	}
}
