package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineAircraftBristol extends EntityEngineAircraft{
	public EntityEngineAircraftBristol(World world){
		super(world);
	}
	
	public EntityEngineAircraftBristol(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
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
	protected Item getEngineItem(){
		return MTSRegistry.engineBristolMercury;
	}
}
