package minecraftflightsimulator.planes.PZLP11;

import minecraftflightsimulator.items.ItemPlane;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemPZLP11 extends ItemPlane{
	
	public ItemPZLP11(){
		this.setUnlocalizedName("PZLP11");
	}

	@Override
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int p_77648_7_, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityPZLP11 plane = new EntityPZLP11(world, x, y+2.25F, z, player.rotationYaw, item.getItemDamage());
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
}
