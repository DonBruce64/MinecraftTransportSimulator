package minecraftflightsimulator.containers;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.entities.parts.EntityChest;
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
			if(entity instanceof EntityVehicle){
				return new ContainerVehicle(player.inventory, (EntityVehicle) entity);
			}else if(entity instanceof EntityChest){
				return new ContainerChest(player.inventory, (EntityChest) entity);
			}
		}else{
			if(world.getBlock(x, y, z) instanceof BlockPropellerBench){
				return new ContainerPropellerBench(player.inventory, (TileEntityPropellerBench) world.getTileEntity(x, y, z));
			}
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityVehicle){
				return ((EntityVehicle) entity).getGUI(player);
			}else if(entity instanceof EntityChest){
				return  new GuiChest(player.inventory, (EntityChest) entity);
			}
		}else{
			if(world != null){
				if(world.getBlock(x, y, z) instanceof BlockPropellerBench){
					return new GUIPropellerBench(player.inventory, (TileEntityPropellerBench) world.getTileEntity(x, y, z));
				}	
			}else if(x == 0 && y == 0 && z == 0){
				return new GUIConfig();
			}else{
				return new GUICredits();
			}
		}
		return null;
	}
}
