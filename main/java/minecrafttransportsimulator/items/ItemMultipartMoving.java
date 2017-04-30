package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemMultipartMoving extends Item{
	private Class<? extends EntityMultipartMoving> moving;
	private int numberTypes;
	private IIcon[] icons;
	
	public ItemMultipartMoving(Class<? extends EntityMultipartMoving> moving, int numberSubtypes){
		super();
		this.moving = moving;
		this.numberTypes = numberSubtypes;
		this.icons = new IIcon[numberSubtypes];
		this.setUnlocalizedName(moving.getSimpleName().substring(6).toLowerCase());
	}
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int p_77648_7_, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityMultipartMoving newEntity;
			try{
				//TODO translate stack name to actual name.
				newEntity = moving.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class).newInstance(world, x, y + 1, z, player.rotationYaw, (byte) stack.getItemDamage());
				float minHeight = 0;
				for(Float[] coreCoords : newEntity.getCollisionBoxes()){
					minHeight = -coreCoords[1] > minHeight ? -coreCoords[1] : minHeight;
				}
				newEntity.posY += minHeight;
				newEntity.ownerName = player.getDisplayName();
				if(canSpawn(world, newEntity)){
					newEntity.numberChildren = (byte) newEntity.getCollisionBoxes().size();
					if(!player.capabilities.isCreativeMode){
						--stack.stackSize;
					}
					return true;//INS190
					/*INS190
					return EnumActionResult.PASS;
					INS190*/
				}
			}catch(Exception e){
				System.err.println("ERROR SPAWING ENTITY!");
				e.printStackTrace();
			}
		}
		return false;//INS190
		/*INS190
		return EnumActionResult.FAIL;
		INS190*/
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
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<numberTypes; ++i){
    		icons[i] = register.registerIcon(MTS.MODID + ":" + moving.getSimpleName().substring(6).toLowerCase() + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage >= numberTypes ? 0 : damage];
    }
    //DEL180END
}
