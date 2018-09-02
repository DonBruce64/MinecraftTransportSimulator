package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.packets.general.PacketChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public final class PartSeat extends APart{
	
	public PartSeat(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!multipart.worldObj.isRemote){
			//See if we can enter this multipart.
			//Alternately, we can switch seats if we're already in the multipart.
			if(!multipart.locked || multipart.equals(player.getRidingEntity())){
				Entity seatRider = multipart.getRiderForSeat(this);
				if(seatRider != null){
					if(!player.equals(seatRider)){
						MTS.MTSNet.sendTo(new PacketChat("interact.failure.seattaken"), (EntityPlayerMP) player);
					}
					return true;
				}
				//If we got here we must have the seat number and it must be free.
				//Let the player start riding at this point.
				multipart.setRiderInSeat(player, this);
			}else{
				MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
			}
		}
		return true;
    }
	
	@Override
	public void removePart(){
		if(multipart.getRiderForSeat(this) != null){
			multipart.getRiderForSeat(this).dismountRidingEntity();
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
