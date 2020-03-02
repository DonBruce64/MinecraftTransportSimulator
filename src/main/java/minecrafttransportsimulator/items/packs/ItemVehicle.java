package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemVehicle extends AItemPack<JSONVehicle>{
	public final String subName;
	
	public ItemVehicle(JSONVehicle definition, String subName){
		super(definition);
		this.subName = subName;
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				try{
					//First construct the class.
					EntityVehicleE_Powered newVehicle = PackParserSystem.createVehicle(world, pos.getX(), pos.getY(), pos.getZ(), player.rotationYaw, definition, subName);
					
					//Now that the class exists, use the NTB data from this item to add back components.
					//We use a try-catch for parts in case they've changed since this vehicle was last placed.
					//Don't want crashes due to pack updates.
					if(heldStack.hasTagCompound()){
						NBTTagCompound tagCompound = heldStack.getTagCompound();
						//A-level
						NBTTagList partTagList = tagCompound.getTagList("Parts", 10);
						for(byte i=0; i<partTagList.tagCount(); ++i){
							try{
								NBTTagCompound partTag = partTagList.getCompoundTagAt(i);
								VehiclePart packPart = newVehicle.getPackDefForLocation(partTag.getDouble("offsetX"), partTag.getDouble("offsetY"), partTag.getDouble("offsetZ"));
								//If we are using the old naming system for this vehicle, use it to load parts too.
								if(tagCompound.hasKey("vehicleName")){
									String oldPartName = partTag.getString("partName");
									String parsedPackID = oldPartName.substring(0, oldPartName.indexOf(':'));
									String parsedSystemName =  oldPartName.substring(oldPartName.indexOf(':') + 1);
									JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(parsedPackID).get(parsedSystemName).definition;
									newVehicle.addPart(PackParserSystem.createPart(newVehicle, packPart, partDefinition, partTag), true);
								}else{
									JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(partTag.getString("packID")).get(partTag.getString("systemName")).definition;
									newVehicle.addPart(PackParserSystem.createPart(newVehicle, packPart, partDefinition, partTag), true);
								}
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
						for(byte i = 0; i<definition.motorized.instruments.size(); ++i){
							String instrumentPackID;
							String instrumentSystemName;
							//Check to see if we were an old or new vehicle.  If we are old, load using the old naming convention.
							if(tagCompound.hasKey("vehicleName")){
								String instrumentInSlot = tagCompound.getString("instrumentInSlot" + i);
								if(!instrumentInSlot.isEmpty()){
									instrumentPackID = instrumentInSlot.substring(0, instrumentInSlot.indexOf(':'));
									instrumentSystemName =  instrumentInSlot.substring(instrumentInSlot.indexOf(':') + 1);
								}else{
									continue;
								}
							}else{
								instrumentPackID = tagCompound.getString("instrument" + i + "_packID");
								instrumentSystemName = tagCompound.getString("instrument" + i + "_systemName");
							}
							if(!instrumentPackID.isEmpty()){
								ItemInstrument instrument = (ItemInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName);
								//Check to prevent loading of faulty instruments for the wrong vehicle due to updates or stupid people.
								if(instrument != null && instrument.definition.general.validVehicles.contains(this.definition.general.type)){
									newVehicle.instruments.put(i, instrument);
								}
							}
						}
					}else{
						//Since we don't have NBT data, we must be a new vehicle.
						//If we have any default parts or instruments, we should add them now.
						for(VehiclePart packDef : newVehicle.definition.parts){
							while(packDef != null){
								if(packDef.defaultPart != null){
									String partPackID = packDef.defaultPart.substring(0, packDef.defaultPart.indexOf(':'));
									String partSystemName = packDef.defaultPart.substring(packDef.defaultPart.indexOf(':') + 1);
									newVehicle.addPart(PackParserSystem.createPart(newVehicle, packDef, (JSONPart) MTSRegistry.packItemMap.get(partPackID).get(partSystemName).definition, new NBTTagCompound()), true);
								}
								packDef = packDef.additionalPart != null ? packDef.additionalPart : null;
							}
						}
						for(PackInstrument packInstrument : newVehicle.definition.motorized.instruments){
							if(packInstrument.defaultInstrument != null){
								String instrumentPackID = packInstrument.defaultInstrument.substring(0, packInstrument.defaultInstrument.indexOf(':'));
								String instrumentSystemName = packInstrument.defaultInstrument.substring(packInstrument.defaultInstrument.indexOf(':') + 1);
								newVehicle.instruments.put((byte) newVehicle.definition.motorized.instruments.indexOf(packInstrument), (ItemInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName));
							}
						}
						
						//If we have a default fuel, add it now as we SHOULD have an engine to tell
						//us what fuel type we will need to add.
						if(newVehicle.definition.motorized.defaultFuelQty > 0){
							for(APart<? extends EntityVehicleA_Base> part : newVehicle.getVehicleParts()){
								if(part instanceof APartEngine){
									newVehicle.fluidName = part.definition.engine.fuelType;
									newVehicle.fuel = newVehicle.definition.motorized.defaultFuelQty;
									break;
								}
							}
						}
					}
					
					//Get how far above the ground the vehicle needs to be, and move it to that position.
					//First boost Y based on collision boxes.
					double minHeight = 0;
					for(VehicleCollisionBox collisionBox : newVehicle.definition.collision){
						minHeight = Math.min(collisionBox.pos[1] - collisionBox.height/2F, minHeight);
					}
					
					//Next, boost based on parts.
					for(APart<? extends EntityVehicleA_Base> part : newVehicle.getVehicleParts()){
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
