package minecrafttransportsimulator.vehicles.parts;

import mcinterface.WrapperNBT;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public abstract class APartGroundEffector extends APart{
	protected final Point3i[] lastBlocksModified;
	protected final Point3i[] affectedBlocks;
	
	public APartGroundEffector(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
		lastBlocksModified = new Point3i[definition.effector.blocksWide];
		affectedBlocks = new Point3i[definition.effector.blocksWide];
	}
	
	@Override
	public void update(){
		super.update();
		int startingIndex = -definition.effector.blocksWide/2;
		for(int i=0; i<definition.effector.blocksWide; ++i){
			int xOffset = startingIndex + i;
			Point3d partAffectorPosition = new Point3d(xOffset, 0, 0).rotateFine(totalRotation).add(worldPos);
			affectedBlocks[i] = new Point3i(partAffectorPosition);
			if(effectIsBelowPart()){
				affectedBlocks[i].add(0, -1, 0);
			}
		}
		
		for(byte i=0; i<affectedBlocks.length; ++i){
			if(!affectedBlocks[i].equals(lastBlocksModified[i])){
				performEffectsAt(affectedBlocks[i]);
				lastBlocksModified[i] = affectedBlocks[i];
			}
		}
	}
	
	@Override
	public WrapperNBT getData(){
		return new WrapperNBT(); 
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	protected abstract void performEffectsAt(Point3i position);
	
	protected abstract boolean effectIsBelowPart();
}
