package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.helpers.EntityHelper;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemMultipartMoving extends Item{
	private final String name;
	
	public ItemMultipartMoving(String name){
		super();
		this.name = name;
		this.setUnlocalizedName("item:" + name);
		//TODO have this link to a central system for correct creative tabs.
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
		return ((ItemMultipartMoving) stack.getItem()).name;
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityMultipartMoving newEntity;
			String entityName = ((ItemMultipartMoving) stack.getItem()).name;
			try{
				//TODO Central system should be down here too.
				if(PackParserSystem.getStringProperty(entityName, "type").equals("plane")){
					newEntity = EntityPlane.class.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class).newInstance(world, pos.getX(), pos.getY() + 1, pos.getZ(), player.rotationYaw, entityName);
				}else{
					throw new TypeNotPresentException("type", null);
				}
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
			if(EntityHelper.isBoxCollidingWithBlocks(world, newCore.getEntityBoundingBox(), newCore.collidesWithLiquids())){
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
}
