package minecrafttransportsimulator.items.packs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
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
							MTS.MTSLog.error("ERROR: Could not load stored part from NBT.  This part will NOT be present on the vehicle.");
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
					addDefaultParts(newVehicle.definition.parts, newVehicle);
					
					for(PackInstrument packInstrument : newVehicle.definition.motorized.instruments){
						if(packInstrument.defaultInstrument != null){
							try{
								String instrumentPackID = packInstrument.defaultInstrument.substring(0, packInstrument.defaultInstrument.indexOf(':'));
								String instrumentSystemName = packInstrument.defaultInstrument.substring(packInstrument.defaultInstrument.indexOf(':') + 1);
								try{
									newVehicle.instruments.put((byte) newVehicle.definition.motorized.instruments.indexOf(packInstrument), (ItemInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName));
								}catch(NullPointerException e){
									throw new IllegalArgumentException("ERROR: Attempted to add defaultInstrument: " + instrumentPackID + ":" + instrumentSystemName + " to: " + newVehicle.definition.genericName + " but that instrument doesn't exist in the pack item registry.");
								}
							}catch(IndexOutOfBoundsException e){
								throw new IllegalArgumentException("ERROR: Could not parse defaultInstrument definition: " + packInstrument.defaultInstrument + ".  Format should be \"packId:instrumentName\"");
							}
						}
					}
					
					//If we have a default fuel, add it now as we SHOULD have an engine to tell
					//us what fuel type we will need to add.
					if(newVehicle.definition.motorized.defaultFuelQty > 0){
						for(APart part : newVehicle.getVehicleParts()){
							if(part instanceof APartEngine){
								//Get the most potent fuel for the vehicle from the fuel configs.
								String mostPotentFluid = "";
								for(String fluidName : ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).keySet()){
									if(mostPotentFluid.isEmpty() || ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).get(mostPotentFluid) < ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).get(fluidName)){
										mostPotentFluid = fluidName;
									}
								}
								newVehicle.fluidName = mostPotentFluid;
								newVehicle.fuel = newVehicle.definition.motorized.defaultFuelQty;
								break;
							}
						}
						if(newVehicle.fluidName.isEmpty()){
							throw new IllegalArgumentException("ERROR: A defaultFuelQty was specified for: " + newVehicle.definition.genericName + ", but no engine was noted as a defaultPart, so we don't know what fuel to put in the vehicle.");
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
				for(APart part : newVehicle.getVehicleParts()){
					minHeight = Math.min(part.offset.x - part.getHeight()/2F, minHeight);
				}
				
				//Apply the boost, and check collisions.
				//If the core collisions are colliding, set the vehicle as dead and abort.
				newVehicle.posY += -minHeight;
				for(VehicleAxisAlignedBB coreBox : newVehicle.collisionBoxes){
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
			}
		}
		return EnumActionResult.SUCCESS;
	}
	
	/**
	 * Static helper method to allow for recursion when adding default parts.
	 */
	private static void addDefaultParts(List<VehiclePart> partsToAdd, EntityVehicleE_Powered newVehicle){
		for(VehiclePart packDef : partsToAdd){
			if(packDef.defaultPart != null){
				try{
					String partPackID = packDef.defaultPart.substring(0, packDef.defaultPart.indexOf(':'));
					String partSystemName = packDef.defaultPart.substring(packDef.defaultPart.indexOf(':') + 1);
					try{
						APart newPart = PackParserSystem.createPart(newVehicle, packDef, (JSONPart) MTSRegistry.packItemMap.get(partPackID).get(partSystemName).definition, new NBTTagCompound());
						newVehicle.addPart(newPart, true);
						
						//Check if we have an additional part.
						//If so, we need to check that for default parts.
						if(packDef.additionalPart != null){
							List<VehiclePart> additionalPart = new ArrayList<VehiclePart>();
							additionalPart.add(packDef.additionalPart);
							addDefaultParts(additionalPart, newVehicle);
						}
						
						//Check all sub-parts, if we have any.
						//We need to make sure to convert them to the right type as they're offset.
						if(newPart.definition.subParts != null){
							List<VehiclePart> subPartsToAdd = new ArrayList<VehiclePart>();
							for(VehiclePart subPartPack : newPart.definition.subParts){
								subPartsToAdd.add(newVehicle.getPackForSubPart(packDef, subPartPack));
							}
							addDefaultParts(subPartsToAdd, newVehicle);
						}
					}catch(NullPointerException e){
						throw new IllegalArgumentException("ERROR: Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + newVehicle.definition.genericName + " but that part doesn't exist in the pack item registry.");
					}
				}catch(IndexOutOfBoundsException e){
					throw new IllegalArgumentException("ERROR: Could not parse defaultPart definition: " + packDef.defaultPart + ".  Format should be \"packId:partName\"");
				}
			}
		}
	}
}
