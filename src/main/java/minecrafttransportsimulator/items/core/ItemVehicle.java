package minecrafttransportsimulator.items.core;

import java.lang.reflect.Constructor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.VehicleInstrument;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				String vehicleToSpawnName = ((ItemVehicle) heldStack.getItem()).vehicleName;
				try{
					//First construct the class.
					Class<? extends EntityVehicleE_Powered> vehicleClass = PackParserSystem.getVehicleClass(vehicleToSpawnName);
					Constructor<? extends EntityVehicleE_Powered> construct = vehicleClass.getConstructor(World.class, float.class, float.class, float.class, float.class, String.class);
					EntityVehicleE_Powered newVehicle = construct.newInstance(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, vehicleToSpawnName);
					
					//Now that the class exists, use the NTB data from this item to add back components.
					if(heldStack.hasTagCompound()){
						NBTTagCompound tagCompound = heldStack.getTagCompound();
						//A-level
						NBTTagList partTagList = tagCompound.getTagList("Parts", 10);
						for(byte i=0; i<partTagList.tagCount(); ++i){
							try{
								NBTTagCompound partTag = partTagList.getCompoundTagAt(i);
								VehiclePart packPart = newVehicle.getPackDefForLocation(partTag.getDouble("offsetX"), partTag.getDouble("offsetY"), partTag.getDouble("offsetZ"));
								Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partTag.getString("partName"));
								Constructor<? extends APart> partConstruct = partClass.getConstructor(EntityVehicleE_Powered.class, VehiclePart.class, String.class, NBTTagCompound.class);
								APart savedPart = partConstruct.newInstance((EntityVehicleE_Powered) newVehicle, packPart, partTag.getString("partName"), partTag);
								newVehicle.addPart(savedPart, true);
							}catch(Exception e){
								MTS.MTSLog.error("ERROR IN LOADING PART FROM NBT!");
								e.printStackTrace();
							}
						}
						
						//B-level
						newVehicle.locked=tagCompound.getBoolean("locked");
						newVehicle.ownerName=tagCompound.getString("ownerName");
						newVehicle.displayText=tagCompound.getString("displayText");
						
						//C-level
						
						//D-level
						newVehicle.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
						
						//E-level
						newVehicle.fuel=tagCompound.getDouble("fuel");
						newVehicle.fluidName=tagCompound.getString("fluidName");
						newVehicle.electricPower=tagCompound.getDouble("electricPower");
						for(byte i = 0; i<newVehicle.pack.motorized.instruments.size(); ++i){
							if(tagCompound.hasKey("instrumentInSlot" + i)){
								String instrumentInSlot = tagCompound.getString("instrumentInSlot" + i);
								VehicleInstrument instrument = new VehicleInstrument(instrumentInSlot);
								//Check to prevent loading of faulty instruments for the wrong vehicle due to updates or stupid people.
								if(instrument != null && instrument.pack.general.validVehicles.contains(newVehicle.pack.general.type)){
									newVehicle.setInstrumentInSlot(i, instrumentInSlot);
								}
							}
						}
					}else{
						//Since we don't have NBT data, we must be a new vehicle.
						//If we have any default parts, we should add them now.
						for(VehiclePart packDef : newVehicle.pack.parts){
							while(packDef != null){
								if(packDef.defaultPart != null){
									try{
										Class<? extends APart> partClass = PackParserSystem.getPartPartClass(packDef.defaultPart);
										Constructor<? extends APart> partConstruct = partClass.getConstructor(EntityVehicleE_Powered.class, VehiclePart.class, String.class, NBTTagCompound.class);
										APart newPart = partConstruct.newInstance((EntityVehicleE_Powered) newVehicle, packDef, packDef.defaultPart, new NBTTagCompound());
										newVehicle.addPart(newPart, true);
									}catch(Exception e){
										MTS.MTSLog.error("ERROR IN LOADING PART FROM NBT!");
										e.printStackTrace();
									}
								}
								packDef = packDef.additionalPart != null ? packDef.additionalPart : null;
							}
						}
						
						//If we have a default fuel, add it now as we SHOULD have an engine to tell
						//us what fuel type we will need to add.
						if(newVehicle.pack.motorized.defaultFuelQty > 0){
							for(APart part : newVehicle.getVehicleParts()){
								if(part instanceof APartEngine){
									newVehicle.fluidName = part.pack.engine.fuelType;
									newVehicle.fuel = newVehicle.pack.motorized.defaultFuelQty;
									break;
								}
							}
						}
					}
					
					//Get how far above the ground the vehicle needs to be, and move it to that position.
					//First boost Y based on collision boxes.
					double minHeight = 0;
					for(VehicleCollisionBox collisionBox : newVehicle.pack.collision){
						minHeight = Math.min(collisionBox.pos[1] - collisionBox.height/2F, minHeight);
					}
					
					//Next, boost based on parts.
					for(APart part : newVehicle.getVehicleParts()){
						minHeight = Math.min(part.offset.x - part.getHeight()/2F, minHeight);
					}
					
					//Apply the boost, and check collisions.
					//If the core collisions are colliding, set the vehicle as dead and abort.
					newVehicle.posY += -minHeight;
					for(VehicleAxisAlignedBB coreBox : newVehicle.getCurrentCollisionBoxes()){
						if(world.collidesWithAnyBlock(coreBox)){
							newVehicle.setDead();
							return EnumActionResult.FAIL;
						}
					}
					
					//If we didn't collide with anything, let the vehicle remain in the world.
					world.spawnEntity(newVehicle);
					if(!player.capabilities.isCreativeMode){
						player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
					}
				}catch(Exception e){
					MTS.MTSLog.error("ERROR SPAWING VEHICLE ENTITY!");
					e.printStackTrace();
				}
			}
		}
		return EnumActionResult.SUCCESS;
	}
}
