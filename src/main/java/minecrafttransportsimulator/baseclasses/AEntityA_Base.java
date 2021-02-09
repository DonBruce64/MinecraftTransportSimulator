package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Base entity class.  This class is the base for all in-game entities.  What these
 * entities are is up to the extending class.  They could be players, vehicles, blocks,
 * guns, etc.  The key part here is this base class keeps track of all these entities, and
 * is responsible for assigning them a global ID number when created.  This can be used to
 * obtain their instances via packets as the ID is the same between clients and servers.
 * <br><br>
 * Note that all entities of this type never "load" saved data.  Rather, they are created from it.
 * This means that there is no method call to load properties from data. Instead, data required
 * for loading will be passed-in to the constructor.  This data should be used to create the entity
 * in its loaded state.  For saving, which can happen multiple times in the entity's lifetime,
 * {@link #save(WrapperNBT)} is called.  All data required in the constructor should be saved there.
 * 
 * 
 * @author don_bruce
 */
public abstract class AEntityA_Base{
	/**List of created entities.  Keyed to world instances and then their {@link #lookupID}**/
	private static Map<WrapperWorld, List<AEntityA_Base>> createdEntities = new HashMap<WrapperWorld, List<AEntityA_Base>>();
	/**A general ID for this entity.  This is set when this entity is loaded, and changes between games.  Used for client/server syncing.**/
	public final int lookupID;
	/**A unique ID for this entity.  This is only set when this entity is first spawned, and never changes, even on save/load operations.  Ideal if you need a static reference to the entity.**/
	public final String uniqueUUID;
	/**The world this entity is a part of.**/
	public final WrapperWorld world;
	/**True as long as this entity is part of the world and being ticked.  May be set false internally or externally to remove this entity from the world.**/
	public boolean isValid = true;
	/**Counter for how many ticks this entity has existed in the world.  Realistically, it's the number of update cycles.**/
	public long ticksExisted;
	
	public AEntityA_Base(WrapperWorld world, WrapperNBT data){
		this.uniqueUUID = data.getString("uniqueUUID").isEmpty() ? UUID.randomUUID().toString() : data.getString("uniqueUUID"); 
		this.world = world;
		
		//Get the list of entities we belong to.
		List<AEntityA_Base> worldEntities = createdEntities.get(world);
		if(worldEntities == null){
			worldEntities = new ArrayList<AEntityA_Base>();
			//Add a null entry for the first entity as we don't want to use ID 0.
			worldEntities.add(null);
			createdEntities.put(world, worldEntities);
		}
		
		//Get our lookupID, or make a new one.
		int savedLookupID = data.getInteger("lookupID");
		if(savedLookupID == 0){
			this.lookupID = worldEntities.size();
		}else{
			this.lookupID = savedLookupID;
			
		}
		createdEntities.get(world).add(this);
	}
	
	/**
	 * Call to get the entity with the passed-in ID from the passed-in world.
	 * Returned value may be null if the entity doesn't exist in the world.
	 */
	@SuppressWarnings("unchecked")
	public static <EntityType extends AEntityA_Base> EntityType getEntity(WrapperWorld world, int lookupID){
		List<AEntityA_Base> entities = createdEntities.get(world);
		if(entities != null && entities.size() > lookupID){
			return (EntityType) entities.get(lookupID);
		}
		return null;
	}
	
	/**
	 * Call to get the entity with the passed-in UUID from the passed-in world.
	 * This should be used sparingly, as in general the {@link #getEntity(WrapperWorld, int)}
	 * is faster due to using the {@link #lookupID} rather than a string-based ID, and is lighter
	 * on networking systems, which are the bulk of what does lookups.
	 */
	@SuppressWarnings("unchecked")
	public static <EntityType extends AEntityA_Base> EntityType getEntity(WrapperWorld world, String uniqueUUID){
		List<AEntityA_Base> entities = createdEntities.get(world);
		if(entities != null){
			for(AEntityA_Base entity : entities){
				if(entity.uniqueUUID.equals(uniqueUUID)){
					return (EntityType) entity;
				}
			}
		}
		return null;
	}
	
	 /**
	 * Called to update this entity.  This  may not be called if the entity extending this class
	 * is not slated for updates in some sort of system.
	 */
	public void update(){
		++ticksExisted;
	}
	
	/**
	 * Called to remove this entity from the world.  Removal should perform any and all logic required to ensure
	 * no references are left to this entity in any objects.  This ensures memory can be freed for use elsewhere,
	 * and lingering references do not exist.  After removal, the entity will no longer be returned by {@link #getEntity(WrapperWorld, int)}
	 */
	public void remove(){
		isValid = false;
		createdEntities.get(world).set(lookupID, null);
	}
	
	/**
	 *  Called when the entity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		data.setInteger("lookupID", lookupID);
		data.setString("uniqueUUID", uniqueUUID);
	}
}
