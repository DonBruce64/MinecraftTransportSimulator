package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCore;
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
	public final String name;
	
	public ItemMultipartMoving(String name){
		super();
		this.name = name;
		this.setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
		return PackParserSystem.getDefinitionForPack(name).itemDisplayName;
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			//We want to spawn above this block.
			pos = pos.up();
			String entityName = ((ItemMultipartMoving) stack.getItem()).name;
			try{
				EntityMultipartMoving newEntity = PackParserSystem.getMultipartType(entityName).multipartClass.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class).newInstance(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, entityName);
				float minHeight = 0;
				for(Float[] coreCoords : newEntity.getCollisionBoxes()){
					minHeight = -coreCoords[1] > minHeight ? -coreCoords[1] : minHeight;
				}
				newEntity.posY += minHeight;
				if(canSpawn(world, newEntity, pos)){
					newEntity.numberChildren = (byte) newEntity.getCollisionBoxes().size();
					if(!player.capabilities.isCreativeMode){
						--stack.stackSize;
					}
					return EnumActionResult.PASS;
				}
			}catch(Exception e){
				MTS.MTSLog.error("ERROR SPAWING MULTIPART ENTITY!");
				e.printStackTrace();
			}
		}
		return EnumActionResult.FAIL;
	}

	private static boolean canSpawn(World world, EntityMultipartMoving mover, BlockPos pos){
		List<Float[]> coreLocations = mover.getCollisionBoxes();
		List<EntityCore> spawnedCores = new ArrayList<EntityCore>();
		for(Float[] location : coreLocations){
			EntityCore newCore = new EntityCore(world, mover, mover.UUID, location[0], location[1], location[2], location[3], location[4]);
			world.spawnEntityInWorld(newCore);
			spawnedCores.add(newCore);
			if(newCore.isChildOffsetBoxCollidingWithBlocks(newCore.getEntityBoundingBox())){
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
