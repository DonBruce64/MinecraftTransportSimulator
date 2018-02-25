package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EntitySeat extends EntityMultipartChild{
	
	public EntitySeat(World world){
		super(world);
	}
	
	public EntitySeat(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
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
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.seat, 1, propertyCode);
	}
	
	@Override
	protected boolean interactPart(EntityPlayer player, EnumHand hand){
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
		return true;
    }
	
	@Override
	 public void updatePassenger(Entity passenger){
		super.updatePassenger(passenger);
		if(parent != null){
			MTSVector posVec = RotationSystem.getRotatedPoint(this.offsetX, (float) (this.offsetY + passenger.getYOffset() + passenger.height), (float) this.offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
			passenger.setPosition(parent.posX + posVec.xCoord, parent.posY + posVec.yCoord - passenger.height, parent.posZ + posVec.zCoord);
			passenger.motionX = parent.motionX;
			passenger.motionY = parent.motionY;
			passenger.motionZ = parent.motionZ;
		}
	}
	
	public Entity getPassenger(){
		return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
	}
}
