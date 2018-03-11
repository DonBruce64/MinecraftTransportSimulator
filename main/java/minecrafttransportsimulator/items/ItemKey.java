package minecrafttransportsimulator.items;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemKey extends Item{
	
	public ItemKey(){
		super();
		setFull3D();
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(I18n.format("info.item.key.line" + String.valueOf(i)));
		}
	}
}
