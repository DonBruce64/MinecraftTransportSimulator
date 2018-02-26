package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineCarAMC extends EntityEngineCar{

	public EntityEngineCarAMC(World world){
		super(world);
	}

	public EntityEngineCarAMC(World world, EntityMultipartParent car, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityCar) car, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	public float getWidth(){
		return 0.5F;
	}

	@Override
	public float getHeight(){
		return 0.5F;
	}

	@Override
	protected byte getStarterPower(){
		return 50;
	}

	@Override
	protected byte getStarterIncrement(){
		return 3;
	}

	@Override
	protected String getCrankingSoundName(){
		return "small_automotive_engine_cranking";
	}

	@Override
	protected String getStartingSoundName(){
		return "small_automotive_engine_starting";
	}

	@Override
	protected String getRunningSoundName(){
		return "small_automotive_engine_running";
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineAMCI4_A;
	}
		
	@Override
	public float getRatioForGear(byte gearNumber){
		if(this.isAutomatic){
			switch(gearNumber){
				case(-1): return -2.0F;
				case(1): return 3.5F;
				case(2): return 2.5F;
				case(3): return 1.25F;
				case(4): return 0.75F;
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
