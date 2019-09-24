package minecrafttransportsimulator.items.parts;

import java.lang.reflect.Constructor;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class AItemPart extends Item{
	public final String partName;
	
	public AItemPart(String partName){
		super();
		this.partName = partName;
		this.setUnlocalizedName(partName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(partName.substring(0, partName.indexOf(':'))));
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
        //Only try to add parts on the server and send packets to the client if we are successful.
    	if(!world.isRemote){
	    	for(Entity entity : world.loadedEntityList){
				if(entity instanceof EntityVehicleC_Colliding){
					EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) entity;
					//If we are riding this vehicle, we cannot add parts to it.
					if(!vehicle.equals(player.getRidingEntity()) && vehicle.pack != null){
						//If this item is a part, find if we are right-clicking a valid part area.
						//If so, add the part and notify all clients.
			    		Vec3d lookVec = player.getLook(1.0F);
        				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
        				PackPartObject heldItemPack = PackParserSystem.getPartPack(partName);
			    		for(float f=1.0F; f<4.0F; f += 0.1F){
			    			for(Entry<Vec3d, PackPart> packPartEntry : vehicle.getAllPossiblePackParts().entrySet()){
		    					//If we are a custom part, use the custom hitbox.  Otherwise use the regular one.
		    					VehicleAxisAlignedBB partBox;
								if(packPartEntry.getValue().types.contains("custom") && heldItemPack.general.type.equals("custom")){
									Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
									partBox = new VehicleAxisAlignedBB(vehicle.getPositionVector().add(offset), packPartEntry.getKey(), heldItemPack.custom.width, heldItemPack.custom.height, false, false);		
								}else{
									Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey().addVector(0, 0.25F, 0), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
									partBox = new VehicleAxisAlignedBB(vehicle.getPositionVector().add(offset), packPartEntry.getKey().addVector(0, 0.5F, 0), 0.75F, 1.75F, false, false);
								}
		    					
		    					if(partBox.contains(clickedVec)){
		    						//Check to make sure the spot is free.
		    						if(vehicle.getPartAtLocation(packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z) == null){
		    							//Check to make sure the part is valid.
		    							if(packPartEntry.getValue().types.contains(heldItemPack.general.type)){
		    								//Check to make sure the part is in parameter ranges.
		    								if(isPartValidForPackDef(packPartEntry.getValue())){
		        								//Try to add the part.
		    									try{
		    										ItemStack heldStack = player.getHeldItem(hand);
		    										Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partName);
		    										Constructor<? extends APart> construct = partClass.getConstructor(EntityVehicleE_Powered.class, PackPart.class, String.class, NBTTagCompound.class);
		    										APart newPart = construct.newInstance((EntityVehicleE_Powered) vehicle, packPartEntry.getValue(), partName, heldStack.hasTagCompound() ? heldStack.getTagCompound() : new NBTTagCompound());
		    										vehicle.addPart(newPart, false);
		    										MTS.MTSNet.sendToAll(new PacketVehicleClientPartAddition(vehicle, packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z, heldStack));
		    										if(!player.capabilities.isCreativeMode){
		    											player.inventory.clearMatchingItems(this, heldStack.getItemDamage(), 1, heldStack.getTagCompound());
		    										}
		    									}catch(Exception e){
		    										MTS.MTSLog.error("ERROR SPAWING PART ON SERVER!");
		    										MTS.MTSLog.error(e.getMessage());
		    										e.printStackTrace();
		    									}
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
	
	public boolean isPartValidForPackDef(PackPart packPart){
		PackPartObject itemPack = PackParserSystem.getPartPack(partName);
		if(packPart.customTypes == null){
			return itemPack.general.customType == null;
		}else if(itemPack.general.customType == null){
			return packPart.customTypes == null;
		}else{
			return packPart.customTypes.contains(itemPack.general.customType);
		}
	}
}
