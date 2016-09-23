package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.world.World;

public class EntityEngineAircraft extends EntityEngine{
	protected EntityPropeller propeller;

	public EntityEngineAircraft(World world) {
		super(world);
	}

	public EntityEngineAircraft(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode, EngineTypes type){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, propertyCode, type);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
				
		propeller = ((EntityFlyable) parent).getPropellerForEngine(this.UUID);
				
		if(engineOn){
			if(propeller != null){
				engineRPM += (vehicle.throttle/100F*Math.max(maxEngineRPM - hours, maxEngineRPM - 500) - engineRPM)/10 + (vehicle.velocity - 0.0254*propeller.pitch * engineRPM/60/20 - this.getPropellerForcePenalty())*15;
				if(propeller.diameter > 80 && engineRPM < 300 && vehicle.throttle >= 15){
					engineRPM = 300;
				}
			}else{
				engineRPM += (vehicle.throttle/100F*(maxEngineRPM) - engineRPM)/10;
			}
		}else{
			if(propeller != null){
				engineRPM = Math.max(engineRPM + (vehicle.velocity - 0.0254*propeller.pitch * engineRPM/60/20)*15 - 10, 0);
			}else{
				engineRPM = Math.max(engineRPM - 10, 0);
			}
		}
		if(propeller != null){
			propeller.engineRPM = this.engineRPM;
		}
	}
	
	@Override
	protected void explodeEngine(){
		super.explodeEngine();
		if(this.propeller != null){
			this.parent.removeChild(propeller.UUID, false);
		}
	}
	
	private double getPropellerForcePenalty(){
		if(type.equals(EngineTypes.PLANE_SMALL)){
			return Math.pow(1.9, 3 + (propeller.diameter - 70)/5)/25 - 0.2;
		}else if(type.equals(EngineTypes.PLANE_LARGE)){
			return Math.pow(1.5, 3 + (propeller.diameter - 70)/5)/20;
		}else{
			return 0;
		}
	}
}
