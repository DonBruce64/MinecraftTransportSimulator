package minecrafttransportsimulator.items.packs.parts;

import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class AItemPart extends AItemPack<JSONPart>{
	
	public AItemPart(JSONPart definition){
		super(definition);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
        //Only try to add parts on the server and send packets to the client if we are successful.
    	if(!world.isRemote){
	    	for(Entity entity : world.loadedEntityList){
				if(entity instanceof EntityVehicleC_Colliding){
					EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) entity;
					//If we are riding this vehicle, we cannot add parts to it.
					if(!vehicle.equals(player.getRidingEntity()) && vehicle.definition != null){
						//If this item is a part, find if we are right-clicking a valid part area.
						//If so, add the part and notify all clients.
			    		Vec3d lookVec = player.getLook(1.0F);
        				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
			    		for(float f=1.0F; f<4.0F; f += 0.1F){
			    			for(Entry<Vec3d, VehiclePart> packPartEntry : vehicle.getAllPossiblePackParts().entrySet()){
		    					//If we are a custom part, use the custom hitbox.  Otherwise use the regular one.
		    					VehicleAxisAlignedBB partBox;
								if(packPartEntry.getValue().types.contains("custom") && definition.general.type.equals("custom")){
									Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
									partBox = new VehicleAxisAlignedBB(vehicle.getPositionVector().add(offset), packPartEntry.getKey(), definition.custom.width, definition.custom.height, false, false);		
								}else{
									Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey().addVector(0, 0.25F, 0), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
									partBox = new VehicleAxisAlignedBB(vehicle.getPositionVector().add(offset), packPartEntry.getKey().addVector(0, 0.5F, 0), 0.75F, 1.75F, false, false);
								}
		    					
		    					if(partBox.contains(clickedVec)){
		    						//Check to make sure the spot is free.
		    						if(vehicle.getPartAtLocation(packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z) == null){
		    							//Check to make sure the part is valid.
		    							if(packPartEntry.getValue().types.contains(definition.general.type)){
		    								//Check to make sure the part is in parameter ranges.
		    								if(isPartValidForPackDef(packPartEntry.getValue())){
		        								//Part is valid.  Create it and add it.
		    									ItemStack heldStack = player.getHeldItem(hand);
	    										vehicle.addPart(PackParserSystem.createPart((EntityVehicleE_Powered) vehicle, packPartEntry.getValue(), definition, heldStack.hasTagCompound() ? heldStack.getTagCompound() : new NBTTagCompound()), false);
	    										MTS.MTSNet.sendToAll(new PacketVehicleClientPartAddition(vehicle, packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z, heldStack));
	    										if(!player.capabilities.isCreativeMode){
	    											player.inventory.clearMatchingItems(this, heldStack.getItemDamage(), 1, heldStack.getTagCompound());
	    										}
		    									return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
		        							}
		    							}
		    						}
	        					}
		    				}
        					clickedVec = clickedVec.addVector(lookVec.x*0.1F, lookVec.y*0.1F, lookVec.z*0.1F);
        				}
					}
	    		}
	    	}
    	}
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
	
	public boolean isPartValidForPackDef(VehiclePart partDefinition){
		if(partDefinition.customTypes == null){
			return definition.general.customType == null;
		}else if(definition.general.customType == null){
			return partDefinition.customTypes == null || partDefinition.customTypes.contains("");
		}else{
			return partDefinition.customTypes.contains(definition.general.customType);
		}
	}
}
