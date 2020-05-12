package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemBooklet extends AItemPack<JSONBooklet>{
	/*Current page of this booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	
	public ItemBooklet(JSONBooklet definition){
		super(definition);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		if(world.isRemote){
			WrapperGUI.openGUI(new GUIBooklet(this));
		}
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
    }
	
	@Override
	public String getModelLocation(){
		return null;
	}
	
	@Override
	public String getTextureLocation(){
		return null;
	}
}
