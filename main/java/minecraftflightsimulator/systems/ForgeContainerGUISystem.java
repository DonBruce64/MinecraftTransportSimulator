package minecraftflightsimulator.systems;

import cpw.mods.fml.common.network.IGuiHandler;
import minecraftflightsimulator.entities.parts.EntityChest;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.world.World;

public class ForgeContainerGUISystem implements IGuiHandler{

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z){
		if(ID != -1){
			Entity entity = world.getEntityByID(ID);
			if(entity instanceof EntityChest){
				return new ContainerChest(player.inventory, (EntityChest) entity);
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
		}
		return null;
	}
}
