package minecrafttransportsimulator.entities.parts;

import javax.annotation.Nullable;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EntityEngineAircraftCar extends EntityEngineAircraftSmall{
	public EntityEngineAircraftCar(World world){
		super(world);
	}
	
	public EntityEngineAircraftCar(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.propeller = new EntityPropeller(world, parent, parentUUID, offsetX, offsetY, offsetZ, 2);
		propeller.numberBlades = 0;
		propeller.diameter = 70;
		propeller.pitch = 80;
		propeller.health = 3000;
		propeller.engineUUID = this.UUID;
		parent.addChild(propeller.UUID, propeller, true);
		propeller.height = 0.001F;
		propeller.width = 0.001F;
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand){
		return true;
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(this.propeller == null){
			
		}
		if(state.running){
			double engineTargetRPM = vehicle.throttle/100F*(maxRPM - engineStartRPM*1.25 - hours) + engineStartRPM*1.25;
			double engineRPMDifference = engineTargetRPM - RPM;
			RPM += engineRPMDifference/10;
		}else{
			RPM = Math.max(RPM - 10, 0);
		}
	}
	
	@Override
	protected float getSize(){
		return 0.5F;
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineCarSmall;
	}
}
