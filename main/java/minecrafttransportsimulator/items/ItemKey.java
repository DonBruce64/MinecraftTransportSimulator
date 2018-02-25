package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSAchievements;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemKey extends Item{
	
	public ItemKey(){
		super();
		setFull3D();
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	public static void changeOwner(ItemStack stack, EntityMultipartMoving mover, EntityPlayer player){
		if(mover.ownerName.isEmpty()){
			//No owner, take ownership.
			mover.ownerName = player.getUUID(player.getGameProfile()).toString();
			MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.own"), (EntityPlayerMP) player);
		}else{
			//Already owned, check to see if we can disown.
			boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
			if(player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName) || isPlayerOP){
				mover.ownerName = "";
				MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.unown"), (EntityPlayerMP) player);	
			}else{
				MTS.MTSNet.sendTo(new ChatPacket(mover.getEntityWorld().getPlayerEntityByUUID(player.getPersistentID().fromString(mover.ownerName)).getDisplayNameString() + " " + "interact.key.failure.alreadyowned"), (EntityPlayerMP) player);
			}
		}
	}
	
	public static void changeLock(ItemStack stack, EntityMultipartMoving mover, EntityPlayer player){
		String vehicleUUID = stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
		if(vehicleUUID.isEmpty()){
			if(!mover.ownerName.isEmpty()){
				if(!player.getUUID(player.getGameProfile()).toString().equals(mover.ownerName)){
					MTS.MTSNet.sendTo(new ChatPacket("interact.key.failure.notowner"), (EntityPlayerMP) player);
					return;
				}
			}
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("vehicle", mover.UUID);
			stack.setTagCompound(tag);
			
			mover.locked = true;
			MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.lock"), (EntityPlayerMP) player);
			MTSAchievements.triggerKey(player);
		}else if(!vehicleUUID.equals(mover.UUID)){
			MTS.MTSNet.sendTo(new ChatPacket("interact.key.failure.wrongkey"), (EntityPlayerMP) player);
		}else{
			if(mover.locked){
				mover.locked = false;
				MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.unlock"), (EntityPlayerMP) player);
			}else{
				mover.locked = true;
				MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.lock"), (EntityPlayerMP) player);
			}
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(I18n.format("info.item.key.line" + String.valueOf(i)));
		}
	}
}
