package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public abstract class APartEngineGeared extends APartEngine{
	public byte currentGear;

	public APartEngineGeared(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.currentGear = dataTag.getByte("gearNumber");
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Do automatic transmission functions if needed.
		if(state.running && definition.engine.isAutomatic){
			if(currentGear > 0){
				if(RPM > getSafeRPMFromMax(this.definition.engine.maxRPM)*0.5F*(1.0F + vehicle.throttle/100F)){
					shiftUp(false);
				}else if(RPM < getSafeRPMFromMax(this.definition.engine.maxRPM)*0.25*(1.0F + vehicle.throttle/100F) && currentGear > 1){
					shiftDown(false);
				}
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = super.getPartNBTTag();
		dataTag.setByte("gearNumber", this.currentGear);
		return dataTag;
	}
	
	protected float getRatioForCurrentGear(){
		return currentGear == -1 ? definition.engine.gearRatios[0] : currentGear > 0 ? definition.engine.gearRatios[currentGear + 1] : 0;
	}
	
	public float getGearshiftRotation(){
		return definition.engine.isAutomatic ? Math.min(1, currentGear)*15F : currentGear*5;
	}
	
	public float getGearshiftPosition_Vertical(){
		if(currentGear == -1){
			return definition.engine.gearRatios.length%2 == 0 ? 15 : -15; 
		}else if(currentGear == 0){
			return 0;
		}else{
			return currentGear%2 == 0 ? -15 : 15;
		}
	}
	
	public float getGearshiftPosition_Horizontal(){
		int columns = (definition.engine.gearRatios.length)/2;
		int firstColumnAngle = columns/2*-5;
		float columnAngleDelta = columns != 1 ? -firstColumnAngle*2/(columns - 1) : 0; 
		if(currentGear == -1){
			return -firstColumnAngle;
		}else if(currentGear == 0){
			return 0;
		}else{
			//Divide the currentGear-1 by two to get our column (0 for column 1, 1 for 2).
			//Then add multiply that by columnAngleDelta to get our delta for this column.
			//Return that value, plus the initial angle.
			return firstColumnAngle + (int)((currentGear - 1)/2)*columnAngleDelta;
		}
	}
	
	public void shiftUp(boolean packet){
		if(currentGear == -1){
			currentGear = 0;
		}else if(currentGear == 0){
			if(vehicle.velocity > -0.1){
				currentGear = 1;
			}else if(vehicle.world.isRemote){
				MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1, vehicle);
			}
		}else if(currentGear < definition.engine.gearRatios.length - 2){
			if(definition.engine.isAutomatic && packet){
				currentGear = 1;
			}else{
				++currentGear;
			}
		}
	}
	
	public void shiftDown(boolean packet){
		if(currentGear > 0){
			if(definition.engine.isAutomatic && packet){
				currentGear = 0;
			}else{
				--currentGear;
			}
		}else if(currentGear == 0){
			if(vehicle.velocity < 0.1){
				currentGear = -1;
			}else if(vehicle.world.isRemote){
				MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1, vehicle);
			}
		}
	}
}
