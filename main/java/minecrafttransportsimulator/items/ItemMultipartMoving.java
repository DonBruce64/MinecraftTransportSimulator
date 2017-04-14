package minecrafttransportsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

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
				newEntity = EntityMultipartMoving.class.getConstructor(World.class, float.class, float.class, float.class, float.class, byte.class).newInstance(world, x, y + 1, z, player.rotationYaw, (byte) stack.getItemDamage());
				float minHeight = 0;
				for(float[] coreCoords : newEntity.getCoreLocations()){
					minHeight = -coreCoords[1] > minHeight ? -coreCoords[1] : minHeight;
				}
				newEntity.posY += minHeight;
				newEntity.ownerName = player.getDisplayName();
				if(canSpawnParent(world, newEntity)){
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

	public static boolean canSpawnParent(World world, EntityMultipartParent parent){
		float[][] coreLocations = parent.getCoreLocations();
		EntityCore[] spawnedCores = new EntityCore[coreLocations.length];
		for(int i=0; i<coreLocations.length; ++i){
			EntityCore core = new EntityCore(world, parent, parent.UUID, coreLocations[i][0], coreLocations[i][1], coreLocations[i][2], coreLocations[i][3], coreLocations[i][4]);
			world.spawnEntityInWorld(core);
			spawnedCores[i] = core;
			if(!AABBHelper.getCollidingBlockBoxes(world, core.getBoundingBox(), core.collidesWithLiquids()).isEmpty()){
				for(int j=0; j<=i; ++j){
					spawnedCores[j].setDead();
				}
				parent.setDead();
				return false;
			}
		}
		world.spawnEntityInWorld(parent);
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
