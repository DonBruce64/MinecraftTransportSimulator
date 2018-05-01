package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.entities.core.EntityMultipart;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.multipart.parts.AMultipartPart;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public final class PartSeat extends AMultipartPart{
	public final byte rotation;
	
	private static final Vec3d[] seatRotations = new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0, 90, 0), new Vec3d(0, 180, 0), new Vec3d(0, 270, 0)}; 
	
	public PartSeat(EntityMultipart multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, NBTTagCompound dataTag, PackPartObject packInfo){
		super(multipart, offset, isController, turnsWithSteer, dataTag, packInfo);
		this.rotation = dataTag.getByte("rotation");
	}
	
	@Override
	public void setPartNBTToTag(NBTTagCompound tagCompound){
		dataTag.setByte("rotation", this.rotation);
	}
	
	@Override
	public Vec3d getRotation(float partialTicks){
		return seatRotations[this.rotation/90];
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		//TOOD get this working so fake seats spawn for riders.
		if(!worldObj.isRemote){
			Entity rider = this.getPassenger();
			if(rider==null){
				//Don't let non-seated players in this vehicle enter if locked.
				if(((EntityMultipartMoving) parent).locked){
					if(player.getRidingEntity() instanceof EntitySeat){
						if(((EntitySeat) player.getRidingEntity()).parent != null){
							if(((EntitySeat) player.getRidingEntity()).parent.equals(this.parent)){
								player.startRiding(this);
								return true;
							}
						}
					}
					MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
				}else{
					player.startRiding(this);
				}
			}else if(!rider.equals(player)){
				MTS.MTSNet.sendTo(new ChatPacket("interact.failure.seattaken"), (EntityPlayerMP) player);
			}
		}
		return true;
    }
	
	@Override
	 public void updatePassenger(Entity passenger){
		super.updatePassenger(passenger);
		if(parent != null){
			Vec3d posVec = RotationSystem.getRotatedPoint(this.offsetX, (float) (this.offsetY - this.getHeight()/2F + passenger.getYOffset() + passenger.height), (float) this.offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
			passenger.setPosition(parent.posX + posVec.xCoord, parent.posY + posVec.yCoord - passenger.height, parent.posZ + posVec.zCoord);
			passenger.motionX = parent.motionX;
			passenger.motionY = parent.motionY;
			passenger.motionZ = parent.motionZ;
			
			if(parent.getEntityId() > this.getEntityId() && !worldObj.isRemote){
				//This is VERY BAD as parts that are spawned before parents will not rotate correctly.
				//Re-spawn to fix.
				EntitySeat newSeat = new EntitySeat(this.worldObj, this.parent, this.parentUUID, this.offsetX, this.offsetY, this.offsetZ, this.propertyCode);
				newSeat.readFromNBT(this.writeToNBT(new NBTTagCompound()));
				//Need to change the UUID MC gives this seat to make it think a new seat is present.
				newSeat.setUniqueId(MathHelper.getRandomUUID());
				parent.removeChild(this.UUID, false);
				parent.addChild(newSeat.UUID, newSeat, false);
				worldObj.spawnEntityInWorld(newSeat);
			}
		}
	}
	
	public Entity getPassenger(){
		return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
	}
	
	//Prevent dismounting from this seat naturally as MC sucks at finding good spots to dismount.
	//Instead, chose a better spot manually to prevent the player from getting stuck in vehicles.
	@SubscribeEvent
	public static void on(EntityMountEvent event){
		if(event.isDismounting()){
			if(event.getEntityBeingMounted() instanceof EntitySeat){
				EntitySeat seat = (EntitySeat) event.getEntityBeingMounted();
				if(seat.parent != null){
					if(seat.getEntityId() == seatLastDismounted){
						seatLastDismounted = 0;
						return;
					}
					Vec3d placePosition = RotationSystem.getRotatedPoint(seat.offsetX + (seat.offsetX > 0 ? 1 : -1), seat.offsetY + 2, seat.offsetZ, seat.parent.rotationPitch, seat.parent.rotationYaw, seat.parent.rotationRoll).addVector(seat.parent.posX, seat.parent.posY, seat.parent.posZ);
					AxisAlignedBB collisionDetectionBox = new AxisAlignedBB(new BlockPos(placePosition)).expand(2, 2, 2);
					if(!seat.worldObj.collidesWithAnyBlock(collisionDetectionBox)){
						event.setCanceled(true);
						seatLastDismounted = seat.getEntityId();
						event.getEntityMounting().dismountRidingEntity();
			            seat.removePassenger(event.getEntityMounting());
						event.getEntityMounting().setPosition(placePosition.xCoord, collisionDetectionBox.minY, placePosition.zCoord);
					}
				}
			}
		}
	 }
}
