package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityChild;
import minecrafttransportsimulator.entities.core.EntityParent;
import minecrafttransportsimulator.minecrafthelpers.EntityHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntitySeat extends EntityChild{
	public boolean isController;
	private boolean hadRiderLastTick;
	
	public EntitySeat(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntitySeat(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F, propertyCode);
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.seat);
	}
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			Entity rider = EntityHelper.getRider(this);
			if(rider==null){
				EntityHelper.setRider(player, this);
				return true;
			}else if(!rider.equals(player)){
				MTS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.failure.seattaken")), (EntityPlayerMP) player);
			}
		}
		return false;
    }
	
	@Override
	public boolean canRiderInteract(){
		return true;
	}
	
	public void setController(){
		this.isController = true;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.isController=tagCompound.getBoolean("isController");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isController", this.isController);
	}
}
