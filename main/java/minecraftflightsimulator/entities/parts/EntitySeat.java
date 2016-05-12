package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.helpers.RotationHelper;
import minecraftflightsimulator.packets.general.ChatPacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntitySeat extends EntityChild{
	public boolean driver;
	private boolean hadRiderLastTick;
	
	public EntitySeat(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntitySeat(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode, boolean driver){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.driver=driver;
	}
	
	@Override
    public boolean interactFirst(EntityPlayer player){
		if(!worldObj.isRemote){
			if(riddenByEntity==null){
				player.mountEntity(this);
				return true;
			}else if(!riddenByEntity.equals(player)){
				MFS.MFSNet.sendTo(new ChatPacket("This seat is taken!"), (EntityPlayerMP) player);
			}
		}
		return false;
    }
	
	@Override
	public boolean canRiderInteract(){
		return true;
	}

	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!linked){return;}
		if(this.riddenByEntity != null){
			hadRiderLastTick=true;
			if(worldObj.isRemote){
				MFS.proxy.updateSeatedRider(this, (EntityLivingBase) this.riddenByEntity);
			}
		}else if(hadRiderLastTick){
			hadRiderLastTick=false;
			if(!worldObj.isRemote){
				parent.sendDataToClient();
			}
		}
	}
	
	@Override
	public void updateRiderPosition(){
		if(this.riddenByEntity != null && this.parent != null){
			Vec3 posVec = RotationHelper.getRotatedPoint(offsetX, (float) (offsetY + this.riddenByEntity.getYOffset()), (float) offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
			this.riddenByEntity.setPosition(parent.posX + posVec.xCoord, parent.posY + posVec.yCoord, parent.posZ + posVec.zCoord);
        }
	}
	
	@Override
	public boolean canBeCollidedWith(){
		return true;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.driver=tagCompound.getBoolean("driver");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("driver", this.driver);
	}
}
