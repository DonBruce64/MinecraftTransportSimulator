package minecraftflightsimulator.items;

import java.util.HashMap;
import java.util.Map;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.blocks.TileEntityTrack;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.general.ChatPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemRailWand extends Item{
	private Map<EntityPlayer, int[]> firstPosition = new HashMap<EntityPlayer, int[]>();
	private Map<EntityPlayer, int[]> secondPosition = new HashMap<EntityPlayer, int[]>();
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			if(player.isSneaking()){
				firstPosition.remove(player);
				secondPosition.remove(player);
				MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.info.clear")), (EntityPlayerMP) player);
			}else if(firstPosition.containsKey(player) && secondPosition.containsKey(player)){
				world.setBlock(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2], MFSRegistry.track);
				world.setBlock(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2], MFSRegistry.track);
				world.markBlockForUpdate(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2]);
				world.markBlockForUpdate(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2]);
				world.setTileEntity(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2], new TileEntityTrack(firstPosition.get(player), secondPosition.get(player), firstPosition.get(player)[3], secondPosition.get(player)[3], true));
				world.setTileEntity(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2], new TileEntityTrack(secondPosition.get(player), firstPosition.get(player), secondPosition.get(player)[3], firstPosition.get(player)[3], false));
				firstPosition.remove(player);
				secondPosition.remove(player);
				MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.info.spawn")), (EntityPlayerMP) player);
			}else if(firstPosition.containsKey(player)){
				if(Math.sqrt(Math.pow(x - firstPosition.get(player)[0], 2) + Math.pow(y - firstPosition.get(player)[1], 2) + Math.pow(z - firstPosition.get(player)[2], 2)) > 125){
					MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.failure.distance")), (EntityPlayerMP) player);
				}else{
					secondPosition.put(player, new int[]{x, y + 1, z, (int) 45*Math.round(player.rotationYaw%360/45)});
					MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.info.set2")), (EntityPlayerMP) player);
				}
			}else{
				firstPosition.put(player, new int[]{x, y + 1, z, (int) 45*Math.round(player.rotationYaw%360/45)});
				MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.info.set1")), (EntityPlayerMP) player);
			}
		}
		return false;
	}
}
