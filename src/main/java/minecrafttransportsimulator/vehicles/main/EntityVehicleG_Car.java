package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.APartEngineGeared;
import minecrafttransportsimulator.vehicles.parts.APartGroundDevice;
import net.minecraft.world.World;


public final class EntityVehicleG_Car extends EntityVehicleF_Ground{	
	private boolean turningLeft;
	private boolean turningRight;
	private byte turningCooldown;
	
	public List<APartGroundDevice> wheels = new ArrayList<APartGroundDevice>();
	public List<APartGroundDevice> groundedWheels = new ArrayList<APartGroundDevice>();
	
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
			//Populate grounded wheels.  Needs to be independent of non-wheeled ground devices.
			groundedWheels.clear();
			for(APartGroundDevice wheel : this.wheels){
				if(wheel.isOnGround()){
					groundedWheels.add(wheel);
				}
			}
			
			//Change turn signal status depending on turning status..
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
			if(velocity != 0){
				if(turningLeft && (steeringAngle > 0 || turningCooldown == 0)){
					turningLeft = false;
					changeLightStatus(LightType.LEFTTURNLIGHT, false);
				}
				if(turningRight && (steeringAngle < 0 || turningCooldown == 0)){
					turningRight = false;
					changeLightStatus(LightType.RIGHTTURNLIGHT, false);
				}
				if(turningCooldown > 0 && steeringAngle == 0){
					--turningCooldown;
				}
			}
			
			//Turn on brake/indicator and backup lights if they are activated.
			changeLightStatus(LightType.BRAKELIGHT, brakeOn);
			changeLightStatus(LightType.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightType.LEFTTURNLIGHT));
			changeLightStatus(LightType.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightType.RIGHTTURNLIGHT));
			boolean backupLightOn = false;
			for(APartEngine<? extends EntityVehicleE_Powered> engine : engines.values()){
				if(engine instanceof APartEngineGeared){
					if(((APartEngineGeared<? extends EntityVehicleE_Powered>) engine).currentGear < 0){
						backupLightOn = true;
					}
				}
			}
			changeLightStatus(LightType.BACKUPLIGHT, backupLightOn);
		}
	}
	
	@Override
	public void addPart(APart<? extends EntityVehicleE_Powered> part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		if(part instanceof APartGroundDevice){
			if(((APartGroundDevice) part).canBeDrivenByEngine()){
				wheels.add((APartGroundDevice) part);
			}
		}
	}
	
	@Override
	public void removePart(APart<? extends EntityVehicleE_Powered> part, boolean playBreakSound){
		super.removePart(part, playBreakSound);
		if(wheels.contains(part)){
			wheels.remove(part);
		}
	}
	
	@Override
	protected float getDragCoefficient(){
		return definition.car.dragCoefficient;
	}
}