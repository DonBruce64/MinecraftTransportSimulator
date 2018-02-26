package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineAircraftWasp extends EntityEngineAircraft{
	public EntityEngineAircraftWasp(World world){
		super(world);
	}
	
	public EntityEngineAircraftWasp(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) parent, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	
	@Override
	public float getWidth(){
		return 1.2F;
	}

	@Override
	public float getHeight(){
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
		return "enginewaspr1340_cranking";
	}

	@Override
	protected String getStartingSoundName(){
		return "enginewaspr1340_starting";
	}

	@Override
	protected String getRunningSoundName(){
		return "enginewaspr1340_running";
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineWaspR1340;
	}
}
