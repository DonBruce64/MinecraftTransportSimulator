package mcinterface;

import java.util.Iterator;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Builder for a basic MC Entity class.  This builder allows us to create a new entity
 * class that we can control that doesn't have the wonky systems the MC entities have, such
 * as no roll axis, a single hitbox, and tons of immutable objects that get thrown away every update.
 * Constructor takes a class of {@link AEntityBase}} to construct, but NOT an instance.  This is because
 * we can't create our entity instance at the same time MC creates its instance as we might not yet have NBT
 * data.  Instead, we simply hold on to the class and construct it whenever we get called to do so.
 *
 * @author don_bruce
 */
public class BuilderEntity extends Entity{
	AEntityBase entity;
	
	private boolean requestDataFromServer;
	
	public BuilderEntity(World world){
		super(world);
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
    
    @Override
    public void onEntityUpdate(){
    	//If our entity isn't null, update it and our position.
    	if(entity != null){
    		entity.update();
    		posX = entity.position.x;
    		posY = entity.position.y;
    		posZ = entity.position.z;
    		
    		//Check that riders are still present prior to updating them.
    		//This handles dismounting of riders from entities in a non-event-driven way.
    		//We do this because other mods and Sponge like to screw up the events...
    		Iterator<WrapperEntity> riderIterator = entity.ridersToLocations.keySet().iterator();
    		while(riderIterator.hasNext()){
    			WrapperEntity rider = riderIterator.next();
    			if(!this.equals(rider.entity.getRidingEntity())){
    				entity.removeRider(rider, riderIterator);
    			}
    		}
    		entity.updateRiders();
    		//FIXME update hitboxes here.
    	}else{
    		//No entity.  Wait for NBT to be loaded to create it.
    		//If we are on a client, ensure we sent a packet to the server to request it.
    		///Although we could call this in the constructor, Minecraft changes the
    		//entity IDs after spawning and that fouls things up.
    		//To accommodate this, we request a packet whenever the entityID changes.
    		if(requestDataFromServer){
    			MTS.MTSNet.sendToServer(new PacketVehicleClientInit(null);
    			requestDataFromServer = false;
    		}
    	}
    }
    
    @Override
    public void setEntityId(int id){
    	super.setEntityId(id);
    	//If we are setting our ID on a client, request NBT data from the server to load the rest of our properties.
    	//We do this on our next update tick, as we may not yet be spawned at this point.
    	requestDataFromServer = world.isRemote;
    }
    
    @Override
    public boolean startRiding(Entity mcEntity, boolean force){
    	//Forward this call to the entity if this is a force riding.
    	//In this case, we're re-loading riders and need to put them
    	//in their proper locations.
    	if(force){
    		entity.addRider(new WrapperEntity(mcEntity), null);
    	}
    	return super.startRiding(mcEntity, force);
    }
			
    @Override
	public void readFromNBT(NBTTagCompound tag){
    	//FIXME make this be called once on the server once this entity is spawned to kick-off the loading process.
		super.readFromNBT(tag);
		//Build this entity from NBT.  But only do so if the NBT has all the data we need.
		//We can tell this if we have a special bit set that only gets set if we've saved before.
		if(tag.getBoolean("previouslySaved")){
			if(entity != null){
				//TODO see if this occurs frequently.
				MTS.MTSLog.error("ERROR: Loading vehicle after it has already been loaded once.  Things may go badly!");
			}
			entity = new EntityVehicleF_Physics(new WrapperWorld(world));
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		//Write in a special bit to tell us we are loading saved NBT in future calls.
		tag.setBoolean("previouslySaved", true);
		
		//Forward on saving call to entity, if it exists.
		if(entity != null){
			entity.save(new WrapperNBT(tag));
		}
		return tag;
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
