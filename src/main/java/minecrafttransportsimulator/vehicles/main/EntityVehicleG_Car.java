package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.world.World;


public final class EntityVehicleG_Car extends EntityVehicleF_Ground{	
	private boolean turningLeft;
	private boolean turningRight;
	private byte turningCooldown;
	
	public EntityVehicleG_Car(World world){
		super(world);
	}
	
	public EntityVehicleG_Car(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(definition != null){
			//Change turn signal status depending on turning status.
			//Keep signals on until we have been moving without turning in the
			//pressed direction for 2 seconds, or if we turn in the other direction.
			if(steeringAngle < -200){
				turningLeft = true;
				turningCooldown = 40;
				lightsOn.add(LightType.LEFTTURNLIGHT);
			}else if(steeringAngle > 200){
				turningRight = true;
				turningCooldown = 40;
				lightsOn.add(LightType.RIGHTTURNLIGHT);
			}
			if(turningLeft && (steeringAngle > 0 || turningCooldown == 0)){
				turningLeft = false;
				lightsOn.remove(LightType.LEFTTURNLIGHT);
			}
			if(turningRight && (steeringAngle < 0 || turningCooldown == 0)){
				turningRight = false;
				lightsOn.remove(LightType.RIGHTTURNLIGHT);
			}
			if(velocity != 0 && turningCooldown > 0 && steeringAngle == 0){
				--turningCooldown;
			}
			
			//Turn on brake lights and indicator lights.
			if(brakeOn){
				lightsOn.add(LightType.BRAKELIGHT);
				if(lightsOn.contains(LightType.LEFTTURNLIGHT)){
					lightsOn.remove(LightType.LEFTINDICATORLIGHT);
				}else{
					lightsOn.add(LightType.LEFTINDICATORLIGHT);
				}
				if(lightsOn.contains(LightType.RIGHTTURNLIGHT)){
					lightsOn.remove(LightType.RIGHTINDICATORLIGHT);
				}else{
					lightsOn.add(LightType.RIGHTINDICATORLIGHT);
				}
			}else{
				lightsOn.remove(LightType.BRAKELIGHT);
				lightsOn.remove(LightType.LEFTINDICATORLIGHT);
				lightsOn.remove(LightType.RIGHTINDICATORLIGHT);
			}
			
			//Set backup light state.
			lightsOn.remove(LightType.BACKUPLIGHT);
			for(PartEngine engine : engines.values()){
				if(engine.currentGear < 0){
					lightsOn.add(LightType.BACKUPLIGHT);
					break;
				}
			}
		}
	}
	
	@Override
	protected float getDragCoefficient(){
		//If we don't have any grounded ground devices, assume we are in the air or in water.
		//If both cases, we need to increase drag.
		return groundedGroundDevices.isEmpty() ? definition.car.dragCoefficient*3 : definition.car.dragCoefficient;
	}
}