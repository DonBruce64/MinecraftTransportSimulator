package minecrafttransportsimulator.items.core;

import java.lang.reflect.Constructor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.collision.RotatableAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackCollisionBox;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemMultipart extends Item{
	public final String multipartName;
	
	public ItemMultipart(String multipartName){
		super();
		this.multipartName = multipartName;
		this.setUnlocalizedName(multipartName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(multipartName.substring(0, multipartName.indexOf(':'))));
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				String multipartName = ((ItemMultipart) heldStack.getItem()).multipartName;
				try{
					Class<? extends EntityMultipartD_Moving> multipartClass = PackParserSystem.getMultipartClass(multipartName);
					Constructor<? extends EntityMultipartD_Moving> construct = multipartClass.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class);
					EntityMultipartD_Moving newMultipart = construct.newInstance(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, multipartName);
					
					float minHeight = 0;
					for(PackCollisionBox collisionBox : newMultipart.pack.collision){
						minHeight = Math.min(collisionBox.pos[1] - collisionBox.height/2F, minHeight);
					}
					newMultipart.posY += -minHeight;
					
					for(RotatableAxisAlignedBB coreBox : newMultipart.allBoxes){
						if(world.collidesWithAnyBlock(coreBox)){
							newMultipart.setDead();
							return EnumActionResult.FAIL;
						}
					}
					
					//If we are using a picked-up multipart make sure to get no free windows!
					if(heldStack.hasTagCompound()){
						newMultipart.brokenWindows = heldStack.getTagCompound().getByte("brokenWindows");
					}
					world.spawnEntityInWorld(newMultipart);
					if(!player.capabilities.isCreativeMode){
						player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
					}
					return EnumActionResult.SUCCESS;
				}catch(Exception e){
					MTS.MTSLog.error("ERROR SPAWING MULTIPART ENTITY!");
					e.printStackTrace();
				}
			}
		}
		return EnumActionResult.FAIL;
	}
}
