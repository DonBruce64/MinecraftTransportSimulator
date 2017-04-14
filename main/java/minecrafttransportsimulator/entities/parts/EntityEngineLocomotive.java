package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityEngineLocomotive extends EntityEngine{
	public EntityEngineLocomotive(World world){
		super(world);
	}

	public EntityEngineLocomotive(World world, EntityMultipartVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
	}

	@Override
	protected float getSize(){
		return 2.0F;
	}

	@Override
	protected byte getStarterPower(){
		return 0;
	}

	@Override
	protected byte getStarterIncrement() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected String getCrankingSoundName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getStartingSoundName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getRunningSoundName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Item getEngineItem() {
		// TODO Auto-generated method stub
		return null;
	}
}
