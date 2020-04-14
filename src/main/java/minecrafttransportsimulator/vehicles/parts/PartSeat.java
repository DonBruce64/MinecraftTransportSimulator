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
import net.minecraft.init.Items;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public final class PartSeat extends APart{
	
	public PartSeat(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		//let's get the Item player is holding, in case it is a lead.
		ItemStack heldStack = player.getHeldItemMainhand();
		//See if we can enter this vehicle.
		//Alternately, we can switch seats if we're already in the vehicle.
		if(!vehicle.locked || vehicle.equals(player.getRidingEntity())){
			Entity seatRider = vehicle.getRiderForSeat(this);
			if(seatRider != null){
				if(heldStack != null && heldStack.getItem() == Items.LEAD && ((EntityLiving)seatRider).canBeLeashedTo(player)) {
					((EntityLiving)seatRider).setLeashHolder(player, true);
	                heldStack.shrink(1);
	                return true;
				}
				if (!(seatRider instanceof EntityPlayer)) {
					seatRider.dismountRidingEntity();
					return true;
				}
				if(!player.equals(seatRider)){
					MTS.MTSNet.sendTo(new PacketChat("interact.failure.seattaken"), (EntityPlayerMP) player);
				}
				return true;
			}
			//If we got here we must have the seat number and it must be free.
			//Let's try to get the leashed animal ride this.
			World worldIn = vehicle.world;
			double i = player.posX;
			double j = player.posY;
			double k = player.posZ;
			for (EntityLiving entityliving : worldIn.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(i - 7.0D, j - 7.0D, k - 7.0D, i + 7.0D, j + 7.0D, k + 7.0D))){
				if (entityliving.getLeashed() && entityliving.getLeashHolder() == player){
					entityliving.clearLeashed(true, !player.capabilities.isCreativeMode);
					vehicle.setRiderInSeat(entityliving, this);
					return true;
				}
			}
			//Let the player start riding at this point.
			vehicle.setRiderInSeat(player, this);
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
