package minecrafttransportsimulator.items.packs;

import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemVehicle extends AItemPack<JSONVehicle> implements IItemEntityProvider<EntityVehicleF_Physics>{
	public final String subName;
	
	public ItemVehicle(JSONVehicle definition, String subName){
		super(definition);
		this.subName = subName;
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World mcWorld, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		WrapperWorld world = WrapperWorld.getWrapperFor(mcWorld);
		if(!world.isClient() && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				
				//Make sure the definition is set in the NBT we will be giving to our new entity.
				WrapperNBT data = heldStack.hasTagCompound() ? new WrapperNBT(heldStack) : new WrapperNBT();
				data.setString("packID", definition.packID);
				data.setString("systemName", definition.systemName);
				
				//Make sure we don't restore any wold-based entity properties.
				data.setInteger("lookupID", 0);
				data.setPoint3d("position", new Point3d(0D, 0D, 0D));
				data.setPoint3d("motion", new Point3d(0D, 0D, 0D));
				data.setPoint3d("angles", new Point3d(0D, 0D, 0D));
				data.setPoint3d("rotation", new Point3d(0D, 0D, 0D));
				
				
				//First construct the class.
				//This takes into account all saved data in the stack, so the vehicle will re-load its data from it
				//as if it has been saved in the world rather than into an item.  If there's no data,
				//then we just make a blank, new instance.
				EntityVehicleF_Physics newVehicle = createEntity(world, data);
				
				//Set position to the spot that was clicked by the player.
				//Add a -90 rotation offset so the vehicle is facing perpendicular.
				//Makes placement easier and is less likely for players to get stuck.
				newVehicle.position.set((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
				newVehicle.angles.y = -player.rotationYaw + 90;
				
				//If the held stack doesn't have NBT, then we must be spawning a new vehicle.
				//In this case, set default properties, if required.
				if(!heldStack.hasTagCompound()){
					//Set initial electrical power.
					newVehicle.electricPower = 12;
					
					//Add default parts via the vehicle's recusion.
					EntityVehicleF_Physics.addDefaultParts(newVehicle.definition.parts, newVehicle, null);

					//Set default vehicle text.
					if(newVehicle.definition.rendering.textObjects != null){
						for(byte i=0; i<newVehicle.definition.rendering.textObjects.size(); ++i){
							newVehicle.textLines.set(i, newVehicle.definition.rendering.textObjects.get(i).defaultText);
						}
					}
					
					//Add default instruments.
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
						for(APart part : newVehicle.parts){
							if(part instanceof PartEngine){
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
					minHeight = Math.min(collisionBox.pos.y - collisionBox.height/2F, minHeight);
				}
				
				//Next, boost based on parts.
				for(APart part : newVehicle.parts){
					minHeight = Math.min(part.placementOffset.y - part.getHeight()/2F, minHeight);
				}
				
				//Apply the boost, and check collisions.
				//If the core collisions are colliding, set the vehicle as dead and abort.
				//We need to update the boxes first, however, as they haven't been updated yet.
				newVehicle.position.y += -minHeight;
				for(BoundingBox coreBox : newVehicle.collisionBoxes){
					coreBox.updateToEntity(newVehicle);
					if(coreBox.updateCollidingBlocks(newVehicle.world, new Point3d(0D, -minHeight, 0D))){
						//New vehicle shouldn't be spawned.  Bail out.
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
	
	@Override
	public String getModelLocation(){
		return "objmodels/vehicles/" + definition.genericName + ".obj";
	}
	
	@Override
	public String getTextureLocation(){
		return "textures/vehicles/" + definition.systemName + ".png";
	}

	@Override
	public EntityVehicleF_Physics createEntity(WrapperWorld world, WrapperNBT data){
		EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(world, data);
		//Need to wait for vehicle to load-in before we try to add saved parts.
		for(APart part : vehicle.partsFromNBT){
			vehicle.addPart(part, true);
		}
		return vehicle;
	}

	@Override
	public Class<EntityVehicleF_Physics> getEntityClass(){
		return EntityVehicleF_Physics.class;
	}
}
