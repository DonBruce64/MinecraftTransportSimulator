package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineAircraftLarge extends EntityEngineAircraft{
	public EntityEngineAircraftLarge(World world){
		super(world);
	}
	
	public EntityEngineAircraftLarge(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) parent, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	protected float getSize(){
		return 1.2F;
	}

	@Override
	protected byte getStarterPower(){
		return 25;
	}

	@Override
	protected byte getStarterIncrement(){
		return 13;
	}

	@Override
	protected String getCrankingSoundName(){
		return "large_engine_cranking";
	}

	@Override
	protected String getStartingSoundName(){
		return "large_engine_starting";
	}

	@Override
	protected String getRunningSoundName(){
		return "large_engine_running";
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineAircraftLarge;
	}

	@Override
	protected boolean isSmall(){
		return false;
	}
}
