package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.APartEngineGeared;
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
				changeLightStatus(LightType.LEFTTURNLIGHT, true);
			}else if(steeringAngle > 200){
				turningRight = true;
				turningCooldown = 40;
				changeLightStatus(LightType.RIGHTTURNLIGHT, true);
			}
			if(turningLeft && (steeringAngle > 0 || turningCooldown == 0)){
				turningLeft = false;
				changeLightStatus(LightType.LEFTTURNLIGHT, false);
			}
			if(turningRight && (steeringAngle < 0 || turningCooldown == 0)){
				turningRight = false;
				changeLightStatus(LightType.RIGHTTURNLIGHT, false);
			}
			if(velocity != 0 && turningCooldown > 0 && steeringAngle == 0){
				--turningCooldown;
			}
			
			//Turn on brake/indicator and backup lights if they are activated.
			changeLightStatus(LightType.BRAKELIGHT, brakeOn);
			changeLightStatus(LightType.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightType.LEFTTURNLIGHT));
			changeLightStatus(LightType.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightType.RIGHTTURNLIGHT));
			boolean backupLightOn = false;
			for(APartEngine engine : engines.values()){
				if(engine instanceof APartEngineGeared){
					if(((APartEngineGeared) engine).currentGear < 0){
						backupLightOn = true;
					}
				}
			}
			changeLightStatus(LightType.BACKUPLIGHT, backupLightOn);
		}
	}
	
	@Override
	protected float getDragCoefficient(){
		//If we don't have any grounded ground devices, assume we are in the air or in water.
		//If both cases, we need to increase drag.
		return groundedGroundDevices.isEmpty() ? definition.car.dragCoefficient*3 : definition.car.dragCoefficient;
	}
}