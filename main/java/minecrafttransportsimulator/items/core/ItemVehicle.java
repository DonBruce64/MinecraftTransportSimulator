package minecrafttransportsimulator.items.core;

import java.lang.reflect.Constructor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackCollisionBox;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemVehicle extends Item{
	public final String vehicleName;
	
	public ItemVehicle(String vehicleName){
		super();
		this.vehicleName = vehicleName;
		this.setUnlocalizedName(vehicleName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(vehicleName.substring(0, vehicleName.indexOf(':'))));
	}
	
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				String vehicleToSpawnName = ((ItemVehicle) heldStack.getItem()).vehicleName;
				try{
					Class<? extends EntityVehicleE_Powered> vehicleClass = PackParserSystem.getVehicleClass(vehicleToSpawnName);
					Constructor<? extends EntityVehicleE_Powered> construct = vehicleClass.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class);
					EntityVehicleE_Powered newVehicle = construct.newInstance(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, vehicleToSpawnName);
					
					float minHeight = 0;
					for(PackCollisionBox collisionBox : newVehicle.pack.collision){
						minHeight = Math.min(collisionBox.pos[1] - collisionBox.height/2F, minHeight);
					}
					newVehicle.posY += -minHeight;
					
					for(VehicleAxisAlignedBB coreBox : newVehicle.getCurrentCollisionBoxes()){
						if(world.collidesWithAnyBlock(coreBox)){
							newVehicle.setDead();
							return EnumActionResult.FAIL;
						}
					}
					
					//If we are using a picked-up vehicle make sure to get no free windows!
					if(heldStack.hasTagCompound()){
						newVehicle.brokenWindows = heldStack.getTagCompound().getByte("brokenWindows");
					}
					world.spawnEntityInWorld(newVehicle);
					if(!player.capabilities.isCreativeMode){
						player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
					}
					return EnumActionResult.SUCCESS;
				}catch(Exception e){
					MTS.MTSLog.error("ERROR SPAWING VEHICLE ENTITY!");
					e.printStackTrace();
				}
			}
		}
		return EnumActionResult.FAIL;
	}
}
