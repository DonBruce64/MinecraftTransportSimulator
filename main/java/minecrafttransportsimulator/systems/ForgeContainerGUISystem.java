package minecrafttransportsimulator.systems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class ForgeContainerGUISystem implements IGuiHandler{
	//TODO this will need to be present if we need to use the GUI system.
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		/*if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityCrate){
				return new ContainerChest(player.inventory, ((PartCrate) entity).crateInventory, player);
			}
		}*/
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		/*
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityCrate){
				return new GuiChest(player.inventory, (EntityCrate) entity);
			}
		}*/
		return null;
	}
}
