package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public class PartEngineAircraft extends APartEngine{
	public PartPropeller propeller;
	private final EntityMultipartF_Plane plane;

	public PartEngineAircraft(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
		this.plane = (EntityMultipartF_Plane) multipart;
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(state.running){
			double engineTargetRPM = plane.throttle/100F*(pack.engine.maxRPM - engineStartRPM*1.25 - hours) + engineStartRPM*1.25;
			double engineRPMDifference = engineTargetRPM - RPM;
			if(propeller != null){
				double propellerFeedback = -(plane.velocity - 0.0254*propeller.pack.propeller.pitch * RPM/60/20 - this.getPropellerForcePenalty())*15;
				RPM += engineRPMDifference/10 - propellerFeedback;
			}else{
				RPM += engineRPMDifference/10;
			}
		}else{
			if(propeller != null){
				RPM = Math.max(RPM + (plane.velocity - 0.0254*propeller.pack.propeller.pitch * RPM/60/20)*15 - 10, 0);
			}else{
				RPM = Math.max(RPM - 10, 0);
			}
		}
	}
	
	@Override
	public void removePart(){
		if(propeller != null && !multipart.worldObj.isRemote){
			multipart.removePart(propeller, false);
		}
	}
	
	@Override
	protected void explodeEngine(){
		super.explodeEngine();
		if(this.propeller != null && !multipart.worldObj.isRemote){
			propeller.dropAsItem();
		}
	}
	
	public double getPropellerForcePenalty(){
		return (propeller.pack.propeller.diameter - 70)/(50*this.pack.engine.fuelConsumption - 15);
	}
}
