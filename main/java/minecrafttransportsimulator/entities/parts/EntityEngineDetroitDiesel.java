package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineDetroitDiesel extends EntityEngineCar{

	public EntityEngineDetroitDiesel(World world){
		super(world);
	}

	public EntityEngineDetroitDiesel(World world, EntityMultipartParent car, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityMultipartF_Car) car, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineDetroitDiesel;
	}
		
	@Override
	public float getRatioForGear(byte gearNumber){
		switch(gearNumber){
			case(-1): return -6.33F;
			case(1): return 11.83F;
			case(2): return 7.90F;
			case(3): return 5.79F;
			case(4): return 4.32F;
			case(5): return 3.20F;
			case(6): return 2.47F;
			case(7): return 1.81F;
			case(8): return 1.35F;
			case(9): return 1.00F;
			default: return 0.0F;
		}
	}
}
