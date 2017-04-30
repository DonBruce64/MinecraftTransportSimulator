package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCore;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemMultipartMoving extends Item{
	private Class<? extends EntityMultipartMoving> moving;
	private int numberTypes;
	
	public ItemMultipartMoving(Class<? extends EntityMultipartMoving> moving, int numberSubtypes){
		super();
		this.moving = moving;
		this.numberTypes = numberSubtypes;
		this.setUnlocalizedName(moving.getSimpleName().substring(6).toLowerCase());
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityMultipartMoving newEntity;
			try{
				//TODO translate stack name to actual name.
				//Needs to be done so correct entity is spawned.
				//Set some sort of name on stacks?
				newEntity = moving.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class).newInstance(world, pos.getX(), pos.getY() + 1, pos.getZ(), player.rotationYaw, (byte) stack.getItemDamage());
				float minHeight = 0;
				for(Float[] coreCoords : newEntity.getCollisionBoxes()){
					minHeight = -coreCoords[1] > minHeight ? -coreCoords[1] : minHeight;
				}
				newEntity.posY += minHeight;
				newEntity.ownerName = player.getDisplayNameString();
				if(canSpawn(world, newEntity)){
					newEntity.numberChildren = (byte) newEntity.getCollisionBoxes().size();
					if(!player.capabilities.isCreativeMode){
						--stack.stackSize;
					}
					return EnumActionResult.PASS;
				}
			}catch(Exception e){
				System.err.println("ERROR SPAWING ENTITY!");
				e.printStackTrace();
			}
		}
		return EnumActionResult.FAIL;
	}

	public static boolean canSpawn(World world, EntityMultipartMoving mover){
		List<Float[]> coreLocations = mover.getCollisionBoxes();
		List<EntityCore> spawnedCores = new ArrayList<EntityCore>();
		for(Float[] location : coreLocations){
			EntityCore newCore = new EntityCore(world, mover, mover.UUID, location[0], location[1], location[2], location[3], location[4]);
			world.spawnEntityInWorld(newCore);
			spawnedCores.add(newCore);
			if(!AABBHelper.getCollidingBlockBoxes(world, newCore.getBoundingBox(), newCore.collidesWithLiquids()).isEmpty()){
				for(EntityCore spawnedCore : spawnedCores){
					spawnedCore.setDead();
				}
				mover.setDead();
				return false;
			}
		}
		world.spawnEntityInWorld(mover);
		return true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<numberTypes; ++i){
			itemList.add(new ItemStack(item, 1, i));
		}
    }
}
