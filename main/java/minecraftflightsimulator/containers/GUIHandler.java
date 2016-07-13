package minecraftflightsimulator.containers;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.blocks.TileEntityCrafter;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GUIHandler implements IGuiHandler{

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityParent){
				return new ContainerParent(player.inventory, (EntityParent) entity);
			}else if(entity instanceof EntityPlaneChest){
				return new ContainerChest(player.inventory, (EntityPlaneChest) entity);
			}
		}else{
			if(world.getBlock(x, y, z) instanceof BlockPropellerBench){
				return new ContainerPropellerBench(player.inventory, (TileEntityCrafter) world.getTileEntity(x, y, z));
			}
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityParent){
				return ((EntityParent) entity).getGUI(player);
			}else if(entity instanceof EntityPlaneChest){
				return  new GuiChest(player.inventory, (EntityPlaneChest) entity);
			}
		}else{
			if(world != null){
				if(world.getBlock(x, y, z) instanceof BlockPropellerBench){
					return new GUIPropellerBench(player.inventory, (TileEntityCrafter) world.getTileEntity(x, y, z));
				}	
			}else{
				return new GUIConfig();
			}
		}
		return null;
	}
}
