package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public final class PartSeat extends APart{
	public final byte rotation;
	private static final Vec3d[] seatRotations = new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0, 90, 0), new Vec3d(0, 180, 0), new Vec3d(0, 270, 0)}; 
	
	public PartSeat(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
		this.rotation = dataTag.getByte("rotation");
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
						MTS.MTSNet.sendTo(new ChatPacket("interact.failure.seattaken"), (EntityPlayerMP) player);
					}
					return true;
				}
				//If we got here we must have the seat number and it must be free.
				//Let the player start riding at this point.
				player.startRiding(multipart);
			}else{
				MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
			}
		}
		return true;
    }
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setByte("rotation", this.rotation);
		return dataTag;
	}

	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}
	
	@Override
	public Vec3d getRotation(float partialTicks){
		return seatRotations[this.rotation/90];
	}
}
