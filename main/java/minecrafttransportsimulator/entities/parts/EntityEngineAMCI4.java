package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineAMCI4 extends EntityEngineCar{

	public EntityEngineAMCI4(World world){
		super(world);
	}

	public EntityEngineAMCI4(World world, EntityMultipartParent car, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityMultipartF_Car) car, parentUUID, offsetX, offsetY, offsetZ);
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineAMCI4;
	}
		
	@Override
	public float getRatioForGear(byte gearNumber){
		if(this.isAutomatic){
			switch(gearNumber){
				case(-1): return -2.0F;
				case(1): return 3.5F;
				case(2): return 2.5F;
				case(3): return 1.5F;
				case(4): return 0.90F;
				default: return 0.0F;
			}
		}else{
			switch(gearNumber){
				case(-1): return -2.0F;
				case(1): return 3.92F;
				case(2): return 2.33F;
				case(3): return 1.44F;
				case(4): return 1.00F;
				case(5): return 0.85F;
				default: return 0.0F;
			}
		}
	}
}
