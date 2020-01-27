package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
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
				changeLightStatus(LightTypes.LEFTTURNLIGHT, true);
			}else if(steeringAngle > 200){
				turningRight = true;
				turningCooldown = 40;
				changeLightStatus(LightTypes.RIGHTTURNLIGHT, true);
			}
			if(velocity != 0){
				if(turningLeft && (steeringAngle > 0 || turningCooldown == 0)){
					turningLeft = false;
					changeLightStatus(LightTypes.LEFTTURNLIGHT, false);
				}
				if(turningRight && (steeringAngle < 0 || turningCooldown == 0)){
					turningRight = false;
					changeLightStatus(LightTypes.RIGHTTURNLIGHT, false);
				}
				if(turningCooldown > 0 && steeringAngle == 0){
					--turningCooldown;
				}
			}
			
			//Turn on brake/indicator and backup lights if they are activated.
			changeLightStatus(LightTypes.BRAKELIGHT, brakeOn);
			changeLightStatus(LightTypes.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.LEFTTURNLIGHT));
			changeLightStatus(LightTypes.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.RIGHTTURNLIGHT));
			changeLightStatus(LightTypes.BACKUPLIGHT, getEngineByNumber((byte) 0) != null && ((PartEngineCar) getEngineByNumber((byte) 0)).getGearshiftRotation() < 0);
		}
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		if(part instanceof APartGroundDevice){
			if(((APartGroundDevice) part).canBeDrivenByEngine()){
				wheels.add((APartGroundDevice) part);
			}
		}
	}
	
	@Override
	public void removePart(APart part, boolean playBreakSound){
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