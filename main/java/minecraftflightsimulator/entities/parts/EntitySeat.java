package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.packets.general.ChatPacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntitySeat extends EntityChild{
	public boolean controller;
	private boolean hadRiderLastTick;
	
	public EntitySeat(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntitySeat(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode, boolean controller){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.controller=controller;
	}
	
	@Override
	public boolean performRightClickAction(EntityPlayer player){
		if(!worldObj.isRemote){
			if(this.getRider()==null){
				this.setRider(player);
				return true;
			}else if(!this.getRider().equals(player)){
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
		
		if(this.getRider() != null){
			hadRiderLastTick=true;
			if(worldObj.isRemote){
				MFS.proxy.updateSeatedRider(this, (EntityLivingBase) this.getRider());
			}
		}else if(hadRiderLastTick){
			hadRiderLastTick=false;
			if(worldObj.isRemote){
				requestDataFromServer();
			}
		}
	}
	
	@Override
	public void loadDataFromNBT(NBTTagCompound tagCompound){
		super.loadDataFromNBT(tagCompound);
		this.controller=tagCompound.getBoolean("controller");
	}
    
	@Override
	public void saveDataToNBT(NBTTagCompound tagCompound){
		super.saveDataToNBT(tagCompound);
		tagCompound.setBoolean("controller", this.controller);
	}
}
