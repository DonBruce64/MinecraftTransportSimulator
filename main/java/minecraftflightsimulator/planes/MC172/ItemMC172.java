package minecraftflightsimulator.planes.MC172;

import java.util.List;

import minecraftflightsimulator.items.ItemPlane;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemMC172 extends ItemPlane{
	
	public ItemMC172(){
		this.hasSubtypes=true;
		this.setUnlocalizedName("MC172");
	}
	
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int p_77648_7_, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityMC172 plane = new EntityMC172(world, x, y+2.25F, z, player.rotationYaw, item.getItemDamage());
			if(!canSpawnPlane(world, plane)){
				return false;
			}else{
				--item.stackSize;
				return true;
			}
		}else{
			return false;
		}
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<6; ++i){
			itemList.add(new ItemStack(item, 1, i));
		}
    }
}
