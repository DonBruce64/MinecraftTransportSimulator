package minecrafttransportsimulator.items;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackCollisionBox;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
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
		this.setCreativeTab(MTSCreativeTabs.tabMTS);
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
		return PackParserSystem.getDefinitionForPack(name).itemDisplayName;
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				String entityName = ((ItemMultipartMoving) heldStack.getItem()).name;
				try{
					EntityMultipartMoving newEntity = PackParserSystem.getMultipartType(entityName).multipartClass.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class).newInstance(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, entityName);
					float minHeight = 0;
					for(PackCollisionBox collisionBox : newEntity.pack.collision){
						minHeight = Math.min(collisionBox.pos[1] - collisionBox.height/2F, minHeight);
					}
					newEntity.posY += -minHeight;
					
					for(MTSAxisAlignedBB coreBox : newEntity.getCurrentCollisionBoxes()){
						if(world.collidesWithAnyBlock(coreBox)){
							newEntity.setDead();
							return EnumActionResult.FAIL;
						}
					}
					
					world.spawnEntityInWorld(newEntity);
					if(!player.capabilities.isCreativeMode){
						player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
					}
				}catch(Exception e){
					MTS.MTSLog.error("ERROR SPAWING MULTIPART ENTITY!");
					e.printStackTrace();
				}
			}
		}
		return EnumActionResult.FAIL;
	}
}
