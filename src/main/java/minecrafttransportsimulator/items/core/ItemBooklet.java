package minecrafttransportsimulator.items.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemBooklet extends Item{
	public final String bookletName;
	/*Current page of this manual.  Don't want it to be specific to a single manual, so this works.*/
	public int pageNumber;
	
	public ItemBooklet(String manualName){
		super();
		this.bookletName = manualName;
		this.setUnlocalizedName(manualName.replace(":", "."));
		this.setCreativeTab(manualName.startsWith("mts") ? MTSRegistry.coreTab : MTSRegistry.packTabs.get(manualName.substring(0, manualName.indexOf(':'))));
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		if(world.isRemote){
			MTS.proxy.openGUI(this, null);
		}
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
    }
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return PackParserSystem.getBooklet(bookletName).general.name;
	}
}
