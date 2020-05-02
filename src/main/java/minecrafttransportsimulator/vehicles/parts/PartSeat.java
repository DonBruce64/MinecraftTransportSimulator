package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

public final class PartSeat extends APart{
	
	public PartSeat(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		//let's get the Item player is holding, in case it is a lead.
		ItemStack heldStack = player.getHeldItemMainhand();
		//See if we can interact with the seats of this vehicle.
		//This can happen if the vehicle isn't locked, or we're already inside.
		if(!vehicle.locked || vehicle.equals(player.getRidingEntity())){
			Entity seatRider = vehicle.getRiderForSeat(this);
			if(seatRider != null){
				//We already have a rider for this seat.  If it's not us, return failure.
				//If it's an entity that can be leashed, dismount the entity and leash it.
				if(seatRider instanceof EntityPlayer){
					if(!player.equals(seatRider)){
						MTS.MTSNet.sendTo(new PacketChat("interact.failure.seattaken"), (EntityPlayerMP) player);
					}
				}else if(seatRider instanceof EntityLiving){
					if(((EntityLiving) seatRider).canBeLeashedTo(player) && player.getHeldItemMainhand().getItem() instanceof ItemLead){
						((EntityLiving)seatRider).setLeashHolder(player, true);
						if(!player.isCreative()){
							heldStack.shrink(1);
						}
					}else{
						//Can't leash up this animal, so mark the seat as taken.
						MTS.MTSNet.sendTo(new PacketChat("interact.failure.seattaken"), (EntityPlayerMP) player);
					}
				}else{
					//Don't know WHAT this entity is.  But it don't belong here!
					seatRider.dismountRidingEntity();
				}
			}else{
				//If we got here we must have the seat number and it must be free.
				//Either mount this seat, or if we have a leashed animal, set it in that seat.
				for(EntityLiving entityliving : vehicle.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))){
					if(entityliving.getLeashed() && player.equals(entityliving.getLeashHolder())){
						entityliving.clearLeashed(true, !player.capabilities.isCreativeMode);
						vehicle.setRiderInSeat(entityliving, this);
						return true;
					}
				}
				//Didn't find an animal.  Just mount the player.
				vehicle.setRiderInSeat(player, this);
			}
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public void removePart(){
		super.removePart();
		if(vehicle.getRiderForSeat(this) != null){
			vehicle.getRiderForSeat(this).dismountRidingEntity();
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}

	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}
}
