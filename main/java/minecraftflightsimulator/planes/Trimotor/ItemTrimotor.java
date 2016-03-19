package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.items.ItemPlane;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemTrimotor extends ItemPlane{
	
	public ItemTrimotor(){
		this.setUnlocalizedName("Trimotor");
	}

	@Override
	public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int p_77648_7_, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityTrimotor plane = new EntityTrimotor(world, x, y+2+2.25F, z, player.rotationYaw, item.getItemDamage());
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
