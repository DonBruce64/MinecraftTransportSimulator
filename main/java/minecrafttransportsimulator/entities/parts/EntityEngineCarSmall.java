package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public abstract class EntityEngineCarSmall extends EntityEngineCar{

	public EntityEngineCarSmall(World world){
		super(world);
	}

	public EntityEngineCarSmall(World world, EntityCar car, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, car, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	protected float getWidth(){
		return 0.5F;
	}

	@Override
	protected float getHeight(){
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
		return MTSRegistry.engineCarSmall;
	}
	
	public abstract boolean isAutomatic();
	
	public abstract byte getNumberGears();
	
	public abstract float getRatioForGear(byte gearNumber);
	
	public static class Automatic extends EntityEngineCarSmall{
		public Automatic(World world){
			super(world);
		}

		public Automatic(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityCar) parent, parentUUID, offsetX, offsetY, offsetZ);
		}

		@Override
		public boolean isAutomatic(){
			return true;
		}

		@Override
		public byte getNumberGears(){
			return 4;
		}

		@Override
		public float getRatioForGear(byte gearNumber){
			switch(gearNumber){
				case(-1): return -2.0F;
				case(1): return 3.5F;
				case(2): return 2.5F;
				case(3): return 1.25F;
				case(4): return 0.75F;
				default: return 0.0F;
			}
		}
	}
}
