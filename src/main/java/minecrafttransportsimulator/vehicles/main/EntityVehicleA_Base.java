package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartRemoval;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

/**Base vehicle class.  All vehicle entities should extend this class.
 * It is primarily responsible for the adding and removal of parts,
 * as well as dealing with what happens when this part is killed.
 * It is NOT responsible for custom data sets, sounds, or movement.
 * That should be done in sub-classes to keep methods segregated.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleA_Base extends Entity{
	/**The pack definition for this vehicle.  This is set upon NBT load on the server, but needs a packet
	 * to be present on the client.  Do NOT assume this will be valid simply because
	 * the vehicle has been loaded!
	 */
	public JSONVehicle definition;
	
	/**This list contains all parts this vehicle has.  Do NOT use it in loops or you will get CMEs all over!
	 * Use the getVehicleParts() method instead to return a loop-safe array.*/
	private final List<APart> parts = new ArrayList<APart>();

	/**Cooldown byte to prevent packet spam requests during client-side loading of part packs.**/
	private byte clientPackPacketCooldown = 0;
	
	/**Roll variable, as default MC entities don't have this.**/
	public float rotationRoll;
	public float prevRotationRoll;
	
	//Delta variables, as MC doesn't have these for rotations, only movement.
	public float motionRoll;
	public float motionPitch;
	public float motionYaw;
	
	
	public EntityVehicleA_Base(World world){
		super(world);
	}
	
	public EntityVehicleA_Base(World world, float posX, float posY, float posZ, float playerRotation, JSONVehicle definition){
		this(world);
		this.definition = definition;	
		//Set position to the spot that was clicked by the player.
		//Add a -90 rotation offset so the vehicle is facing perpendicular.
		//Makes placement easier and is less likely for players to get stuck.
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		//We need to get pack data manually if we are on the client-side.
		///Although we could call this in the constructor, Minecraft changes the
		//entity IDs after spawning and that fouls things up.
		if(definition == null){
			if(world.isRemote){
				if(clientPackPacketCooldown == 0){
					clientPackPacketCooldown = 40;
					MTS.MTSNet.sendToServer(new PacketVehicleClientInit((EntityVehicleF_Physics) this));
				}else{
					--clientPackPacketCooldown;
				}
			}
		}
	}
	
    @Override
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass){
        //Need to render in pass 1 to render transparent things in the world like light beams.
    	return true;
    }
    
    /**
	 * Adds the passed-part to this vehicle, but in this case the part is in item form
	 * with associated data rather than a fully-constructed form.  This method will check
	 * if the item-based part can go to this vehicle.  If so, it is constructed and added,
	 * and a packet is sent to all clients to inform them of this change.  Returns true
	 * if all operations completed, false if the part was not able to be added.
	 * Note that the passed-in data MAY be null if the item didn't have any.
	 */
    public boolean addPartFromItem(AItemPart partItem, NBTTagCompound partTag, double xPos, double yPos, double zPos){
		//Get the part to add.
		VehiclePart packPart = getPackDefForLocation(xPos, yPos, zPos);
		//Check to make sure the spot is free.
		if(getPartAtLocation(xPos, yPos, zPos) == null){
			//Check to make sure the part is valid.
			if(packPart.types.contains(partItem.definition.general.type)){
				//Check to make sure the part is in parameter ranges.
				if(partItem.isPartValidForPackDef(packPart)){
					//Part is valid.  Create it and add it.
					addPart(PackParserSystem.createPart((EntityVehicleF_Physics) this, packPart, partItem.definition, partTag != null ? partTag : new NBTTagCompound()), false);
					MTS.MTSNet.sendToAll(new PacketVehicleClientPartAddition((EntityVehicleF_Physics) this, xPos, yPos, zPos, partItem, partTag));
					
					//If the part doesn't have NBT, it must be new and we need to add default parts.
					//Only do this if we actually have subParts for this part.
					if(partTag != null && partItem.definition.subParts != null){
						addDefaultParts(partItem.definition.subParts, this);
					}
					
					//Done adding the part, return true.
					return true;
				}
			}
		}
    	return false;
    }
	
	public void addPart(APart part, boolean ignoreCollision){
		parts.add(part);
		if(!ignoreCollision){
			//Check for collision, and boost if needed.
			if(part.isPartColliding()){
				//Adjust roll first, as otherwise we could end up with a sunk vehicle.
				rotationRoll = 0;
				setPositionAndRotation(posX, posY + part.getHeight(), posZ, rotationYaw, rotationPitch);
			}
			
			//Sometimes we need to do this for parts that are deeper into the ground.
			if(part.wouldPartCollide(new Point3d(0, Math.max(0, -part.placementOffset.y) + part.getHeight(), 0))){
				setPositionAndRotation(posX, posY +  part.getHeight(), posZ, rotationYaw, rotationPitch);
			}
		}
	}
	
	public void removePart(APart part, boolean playBreakSound){
		if(parts.contains(part)){
			parts.remove(part);
			if(part.isValid()){
				part.removePart();
				if(!world.isRemote){
					MTS.MTSNet.sendToAll(new PacketVehicleClientPartRemoval((EntityVehicleF_Physics) this, part.placementOffset.x, part.placementOffset.y, part.placementOffset.z));
				}
			}
			if(!world.isRemote){
				if(playBreakSound){
					this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 2.0F, 1.0F);
				}
			}
		}
	}
	
	/**
	 * Returns a loop-safe array for iterating over parts.
	 * Use this for everything that needs to look at parts.
	 */
	public List<APart> getVehicleParts(){
		return ImmutableList.copyOf(parts);
	}
	
	/**
	 * Gets the part at the specified location.
	 */
	public APart getPartAtLocation(double offsetX, double offsetY, double offsetZ){
		for(APart part : this.parts){
			if((float)part.placementOffset.x == (float)offsetX && (float)part.placementOffset.y == (float)offsetY && (float)part.placementOffset.z == (float)offsetZ){
				return part;
			}
		}
		return null;
	}
	
	/**
	 * Gets all possible pack parts.  This includes additional parts on the vehicle
	 * and extra parts of parts on other parts.  Map returned is the position of the
	 * part positions and the part pack information at those positions.
	 * Note that additional parts will not be added if no part is present
	 * in the primary location.
	 */
	public Map<Point3d, VehiclePart> getAllPossiblePackParts(){
		Map<Point3d, VehiclePart> packParts = new HashMap<Point3d, VehiclePart>();
		//First get all the regular part spots.
		for(VehiclePart packPart : definition.parts){
			Point3d partPos = new Point3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
			packParts.put(partPos, packPart);
			
			//Check to see if we can put a additional parts in this location.
			//If a part is present at a location that can have an additional parts, we allow them to be placed.
			if(packPart.additionalParts != null){
				for(APart part : this.parts){
					if(part.placementOffset.equals(partPos)){
						for(VehiclePart additionalPart : packPart.additionalParts){
							partPos = new Point3d(additionalPart.pos[0], additionalPart.pos[1], additionalPart.pos[2]);
							packParts.put(partPos, additionalPart);
						}
						break;
					}
				}
			}
		}
		
		//Next get any sub parts on parts that are present.
		for(APart part : this.parts){
			if(part.definition.subParts != null){
				VehiclePart parentPack = getPackDefForLocation(part.placementOffset.x, part.placementOffset.y, part.placementOffset.z);
				for(VehiclePart extraPackPart : part.definition.subParts){
					VehiclePart correctedPack = getPackForSubPart(parentPack, extraPackPart);
					packParts.put(new Point3d(correctedPack.pos[0], correctedPack.pos[1], correctedPack.pos[2]), correctedPack);
				}
			}
			
		}
		return packParts;
	}
	
	/**
	 * Gets the pack definition at the specified location.
	 */
	public VehiclePart getPackDefForLocation(double offsetX, double offsetY, double offsetZ){
		//Check to see if this is a main part.
		for(VehiclePart packPart : definition.parts){
			if(isPackAtPosition(packPart, offsetX, offsetY, offsetZ)){
				return packPart;
			}
			
			//Not a main part.  Check if this is an additional part.
			if(packPart.additionalParts != null){
				for(VehiclePart additionalPart : packPart.additionalParts){
					if(isPackAtPosition(additionalPart, offsetX, offsetY, offsetZ)){
						return additionalPart;
					}
				}
			}
		}
		
		//If this is not a main part or an additional part, check the sub-parts.
		for(APart part : this.parts){
			if(part.definition.subParts.size() > 0){
				VehiclePart parentPack = getPackDefForLocation(part.placementOffset.x, part.placementOffset.y, part.placementOffset.z);
				for(VehiclePart extraPackPart : part.definition.subParts){
					VehiclePart correctedPack = getPackForSubPart(parentPack, extraPackPart);
					if(isPackAtPosition(correctedPack, offsetX, offsetY, offsetZ)){
						return correctedPack;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 *Helper method to prevent casting to floats all over for position-specific tests.
	 */
	public static boolean isPackAtPosition(VehiclePart packPart, double offsetX, double offsetY, double offsetZ){
		return (float)packPart.pos[0] == (float)offsetX && (float)packPart.pos[1] == (float)offsetY && (float)packPart.pos[2] == (float)offsetZ;
	}
	
	/**
	 * Returns a PackPart with the correct properties for a SubPart.  This is because
	 * subParts inherit some properties from their parent parts. 
	 */
	public VehiclePart getPackForSubPart(VehiclePart parentPack, VehiclePart subPack){
		VehiclePart correctPack = definition.new VehiclePart();
		correctPack.isSubPart = true;
		
		//Get the offset position for this part.
		//If we will be mirrored, make sure to invert the x-coords of any sub-parts.
		correctPack.pos = new double[3];
		correctPack.pos[0] = parentPack.pos[0] + (parentPack.pos[0] < 0 ^ parentPack.inverseMirroring ? -subPack.pos[0] : subPack.pos[0]);
		correctPack.pos[1] = parentPack.pos[1] + subPack.pos[1];
		correctPack.pos[2] = parentPack.pos[2] + subPack.pos[2];
		
		//Add current and parent rotation to make a total rotation for this part.
		if(parentPack.rot != null || subPack.rot != null){
			correctPack.rot = new double[3];
		}
		if(parentPack.rot != null){
			correctPack.rot[0] += parentPack.rot[0];
			correctPack.rot[1] += parentPack.rot[1];
			correctPack.rot[2] += parentPack.rot[2];
		}
		if(subPack.rot != null){
			correctPack.rot[0] += subPack.rot[0];
			correctPack.rot[1] += subPack.rot[1];
			correctPack.rot[2] += subPack.rot[2];
		}
		
		correctPack.turnsWithSteer = parentPack.turnsWithSteer;
		correctPack.isController = subPack.isController;
		correctPack.inverseMirroring = subPack.inverseMirroring;
		correctPack.types = subPack.types;
		correctPack.customTypes = subPack.customTypes;
		correctPack.minValue = subPack.minValue;
		correctPack.maxValue = subPack.maxValue;
		correctPack.dismountPos = subPack.dismountPos;
		correctPack.exhaustPos = subPack.exhaustPos;
        correctPack.exhaustVelocity = subPack.exhaustVelocity;
        correctPack.intakeOffset = subPack.intakeOffset;
        correctPack.additionalParts = subPack.additionalParts;
        correctPack.treadYPoints = subPack.treadYPoints;
        correctPack.treadZPoints = subPack.treadZPoints;
        correctPack.treadAngles = subPack.treadAngles;
        correctPack.defaultPart = subPack.defaultPart;
		return correctPack;
	}
	
	/**
	 * Helper method to allow for recursion when adding default parts.
	 * This method adds all default parts for the passed-in list of parts.
	 * The part list should consist of a "parts" JSON definition.
	 * This method should only be called when the vehicle or part with the
	 * passed-in definition is first placed, not when it's being loaded from saved data.
	 */
	public static void addDefaultParts(List<VehiclePart> partsToAdd, EntityVehicleA_Base vehicle){
		for(VehiclePart packDef : partsToAdd){
			if(packDef.defaultPart != null){
				try{
					String partPackID = packDef.defaultPart.substring(0, packDef.defaultPart.indexOf(':'));
					String partSystemName = packDef.defaultPart.substring(packDef.defaultPart.indexOf(':') + 1);
					try{
						APart newPart = PackParserSystem.createPart((EntityVehicleF_Physics) vehicle, packDef, (JSONPart) MTSRegistry.packItemMap.get(partPackID).get(partSystemName).definition, new NBTTagCompound());
						vehicle.addPart(newPart, true);
						
						//Check if we have an additional parts.
						//If so, we need to check that for default parts.
						if(packDef.additionalParts != null){
							addDefaultParts(packDef.additionalParts, vehicle);
						}
						
						//Check all sub-parts, if we have any.
						//We need to make sure to convert them to the right type as they're offset.
						if(newPart.definition.subParts != null){
							List<VehiclePart> subPartsToAdd = new ArrayList<VehiclePart>();
							for(VehiclePart subPartPack : newPart.definition.subParts){
								subPartsToAdd.add(vehicle.getPackForSubPart(packDef, subPartPack));
							}
							addDefaultParts(subPartsToAdd, vehicle);
						}
					}catch(NullPointerException e){
						throw new IllegalArgumentException("ERROR: Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + vehicle.definition.genericName + " but that part doesn't exist in the pack item registry.");
					}
				}catch(IndexOutOfBoundsException e){
					throw new IllegalArgumentException("ERROR: Could not parse defaultPart definition: " + packDef.defaultPart + ".  Format should be \"packId:partName\"");
				}
			}
		}
	}
			
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		//Check to see if we were an old or new vehicle.  If we are old, load using the old naming convention.
		if(tagCompound.hasKey("vehicleName")){
			String oldVehicleName = tagCompound.getString("vehicleName");
			String parsedPackID = oldVehicleName.substring(0, oldVehicleName.indexOf(':'));
			String parsedSystemName =  oldVehicleName.substring(oldVehicleName.indexOf(':') + 1);
			this.definition = (JSONVehicle) MTSRegistry.packItemMap.get(parsedPackID).get(parsedSystemName).definition;
		}else{
			this.definition = (JSONVehicle) MTSRegistry.packItemMap.get(tagCompound.getString("packID")).get(tagCompound.getString("systemName")).definition;
		}
		rotationRoll=tagCompound.getFloat("rotationRoll");
		
		if(this.parts.size() == 0){
			NBTTagList partTagList = tagCompound.getTagList("Parts", 10);
			for(byte i=0; i<partTagList.tagCount(); ++i){
				//Use a try-catch for parts in case they've changed since this vehicle was last placed.
				//Don't want crashes due to pack updates.
				try{
					NBTTagCompound partTag = partTagList.getCompoundTagAt(i);
					VehiclePart packPart = getPackDefForLocation(partTag.getDouble("offsetX"), partTag.getDouble("offsetY"), partTag.getDouble("offsetZ"));
					//If we are using the old naming system for this vehicle, use it to load parts too.
					if(tagCompound.hasKey("vehicleName")){
						String oldPartName = partTag.getString("partName");
						String parsedPackID = oldPartName.substring(0, oldPartName.indexOf(':'));
						String parsedSystemName =  oldPartName.substring(oldPartName.indexOf(':') + 1);
						JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(parsedPackID).get(parsedSystemName).definition;
						addPart(PackParserSystem.createPart((EntityVehicleF_Physics) this, packPart, partDefinition, partTag), true);
					}else{
						JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(partTag.getString("packID")).get(partTag.getString("systemName")).definition;
						addPart(PackParserSystem.createPart((EntityVehicleF_Physics) this, packPart, partDefinition, partTag), true);
					}
					
				}catch(Exception e){
					MTS.MTSLog.error("ERROR IN LOADING PART FROM NBT!");
					e.printStackTrace();
				}
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("packID", definition.packID);
		tagCompound.setString("systemName", definition.systemName);
		tagCompound.setFloat("rotationRoll", rotationRoll);
		
		NBTTagList partTagList = new NBTTagList();
		for(APart part : getVehicleParts()){
			//Don't save the part if it's not valid.
			if(part.isValid()){
				NBTTagCompound partTag = part.getPartNBTTag();
				//We need to set some extra data here for the part to allow this vehicle to know where it went.
				//This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
				partTag.setString("packID", part.definition.packID);
				partTag.setString("systemName", part.definition.systemName);
				partTag.setDouble("offsetX", part.placementOffset.x);
				partTag.setDouble("offsetY", part.placementOffset.y);
				partTag.setDouble("offsetZ", part.placementOffset.z);
				partTagList.appendTag(partTag);
			}
		}
		tagCompound.setTag("Parts", partTagList);
		return tagCompound;
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
