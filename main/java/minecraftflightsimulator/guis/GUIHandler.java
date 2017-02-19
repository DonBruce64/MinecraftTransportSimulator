package minecraftflightsimulator.guis;

import cpw.mods.fml.common.network.IGuiHandler;
import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.parts.EntityChest;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.world.World;

public class GUIHandler implements IGuiHandler{

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityChest){
				return new ContainerChest(player.inventory, (EntityChest) entity);
			}
		}else{
			if(BlockHelper.getBlockFromCoords(world, x, y, z) instanceof BlockPropellerBench){
				return new ContainerPropellerBench(player.inventory, (TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(world, x, y, z));
			}
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityChest){
				return  new GuiChest(player.inventory, (EntityChest) entity);
			}
		}else{
			if(world != null){
				if(BlockHelper.getBlockFromCoords(world, x, y, z) instanceof BlockPropellerBench){
					return new GUIPropellerBench(player.inventory, (TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(world, x, y, z));
				}	
			}
		}
		return null;
	}
}
