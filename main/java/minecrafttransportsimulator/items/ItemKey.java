package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
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
	
	public static String getVehicleUUID(ItemStack stack){
		return stack.hasTagCompound() ? stack.getTagCompound().getString("vehicle") : "";
	}
	
	public static void setVehicle(ItemStack stack, EntityMultipartMoving mover){
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("vehicle", mover.UUID);
		stack.setTagCompound(tag);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_){
		for(byte i=1; i<=5; ++i){
			list.add(I18n.format("info.item.key.line" + String.valueOf(i)));
		}
	}
}
