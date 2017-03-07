package minecraftflightsimulator.items;

import java.util.HashMap;
import java.util.Map;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.blocks.TileEntityRail;
import net.minecraft.entity.player.EntityPlayer;
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
				System.out.println("CLEAR");
			}else if(firstPosition.containsKey(player) && secondPosition.containsKey(player)){
				world.setBlock(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2], MFSRegistry.rail);
				world.setBlock(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2], MFSRegistry.rail);
				world.markBlockForUpdate(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2]);
				world.markBlockForUpdate(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2]);
				world.setTileEntity(firstPosition.get(player)[0], firstPosition.get(player)[1], firstPosition.get(player)[2], new TileEntityRail(firstPosition.get(player), secondPosition.get(player), firstPosition.get(player)[3], secondPosition.get(player)[3], true));
				world.setTileEntity(secondPosition.get(player)[0], secondPosition.get(player)[1], secondPosition.get(player)[2], new TileEntityRail(secondPosition.get(player), firstPosition.get(player), secondPosition.get(player)[3], firstPosition.get(player)[3], false));
				firstPosition.remove(player);
				secondPosition.remove(player);
				System.out.println("SPAWN");
				
			}else if(firstPosition.containsKey(player)){
				secondPosition.put(player, new int[]{x, y + 1, z, (int) 45*Math.round(player.rotationYaw%360/45)});
				System.out.println("SECOND");
			}else{
				firstPosition.put(player, new int[]{x, y + 1, z, (int) 45*Math.round(player.rotationYaw%360/45)});
				System.out.println("FIRST");
			}
		}
		return false;
	}
}
