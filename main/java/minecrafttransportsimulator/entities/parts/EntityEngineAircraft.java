package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.helpers.EntityHelper;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public abstract class EntityEngineAircraft extends EntityEngine{
	public EntityPropeller propeller;
	private double engineTargetRPM;
	private double engineRPMDifference;
	private double propellerFeedback;

	public EntityEngineAircraft(World world) {
		super(world);
	}

	public EntityEngineAircraft(World world, EntityPlane plane, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, plane, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	@Override
    public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			ItemStack playerStack = player.inventory.getItemStack();
			if(playerStack != null){
				if(MTSRegistry.propeller.equals(playerStack.getItem()) && propeller == null){
					if(this.parent != null){
						if(playerStack.getTagCompound().getInteger("diameter") > 80 && this instanceof EntityEngineAircraftSmall){
							MTS.MFSNet.sendTo(new ChatPacket(I18n.format("interact.failure.propellertoobig")), (EntityPlayerMP) player);
							return false;
						}
						propeller = new EntityPropeller(worldObj, (EntityPlane) parent, parent.UUID, offsetX, offsetY + (this.height - 1)/2F, offsetZ + 0.9F, playerStack.getItemDamage());
						propeller.setNBTFromStack(playerStack);
						propeller.engineUUID = this.UUID;
						parent.addChild(propeller.UUID, propeller, true);
						if(!player.capabilities.isCreativeMode){
							EntityHelper.removeItemFromHand(player);
						}
						return true;
					}
				}
			}
		}
		return parent != null ? parent.performRightClickAction(clicked, player) : false;
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(state.running){
			engineTargetRPM = vehicle.throttle/100F*(maxRPM - engineStartRPM*1.25 - hours) + engineStartRPM*1.25;
			engineRPMDifference = engineTargetRPM - RPM;
			if(propeller != null){
				propellerFeedback = -(vehicle.velocity - 0.0254*propeller.pitch * RPM/60/20 - this.getPropellerForcePenalty())*15;
				RPM += engineRPMDifference/10 - propellerFeedback;
			}else{
				RPM += engineRPMDifference/10;
			}
		}else{
			if(propeller != null){
				RPM = Math.max(RPM + (vehicle.velocity - 0.0254*propeller.pitch * RPM/60/20)*15 - 10, 0);
			}else{
				RPM = Math.max(RPM - 10, 0);
			}
		}
	}
	
	@Override
	public void setDead(){
		super.setDead();
		if(propeller != null && !worldObj.isRemote){
			worldObj.spawnEntityInWorld(new EntityItem(worldObj, propeller.posX, propeller.posY, propeller.posZ, propeller.getItemStack()));
			if(parent != null){
				parent.removeChild(propeller.UUID, false);
			}else if(propeller.parent != null){
				propeller.parent.removeChild(propeller.UUID, false);
			}
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
		return Math.pow(1.25, 3 + (propeller.diameter - 70)/5)/10;
	}
	
	protected abstract boolean isSmall();
}
