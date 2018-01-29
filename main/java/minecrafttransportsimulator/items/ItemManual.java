package minecrafttransportsimulator.items;

import minecrafttransportsimulator.MTS;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemManual extends Item{
	public ItemManual(){
		super();
		setFull3D();
		this.setMaxStackSize(1);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand){
		if(world.isRemote){
			MTS.proxy.openGUI(stack, player);
		}
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
    }
}
