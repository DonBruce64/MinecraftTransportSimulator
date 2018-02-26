package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineAircraftLycoming extends EntityEngineAircraft{
	public EntityEngineAircraftLycoming(World world){
		super(world);
	}
	
	public EntityEngineAircraftLycoming(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}

	@Override
	protected byte getStarterPower(){
		return 50;
	}

	@Override
	protected byte getStarterIncrement(){
		return 4;
	}

	@Override
	protected String getCrankingSoundName(){
		return "enginelycoming0360_cranking";
	}

	@Override
	protected String getStartingSoundName(){
		return "enginelycoming0360_starting";
	}

	@Override
	protected String getRunningSoundName(){
		return "enginelycoming0360_running";
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineLycoming0360;
	}
}
