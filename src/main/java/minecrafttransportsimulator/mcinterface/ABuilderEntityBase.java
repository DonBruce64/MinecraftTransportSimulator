package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshakeClient;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshakeServer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Builder for a basic MC Entity class.  This builder provides basic entity logic that's common
 * to all entities we may want to spawn.
 *
 * @author don_bruce
 */
public abstract class ABuilderEntityBase extends Entity{
	
	/**This flag is true if we need to get server data for syncing.  Set on construction tick on clients.**/
	private boolean needDataFromServer;
	/**This flag is true once we load from NBT.  Used to prevent duplicate-loading.  If this builder is spawned
	 * manually, this should be set.**/
	public boolean loadedFromNBT;
	/**Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
	 * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
	 * to do their funky logic.  I'm looking at YOU The One Probe!**/
	private NBTTagCompound lastLoadedNBT;
	/**Players requesting data for this builder.  This is populated by packets sent to the server.  Each tick players in this list are
	 * sent data about this builder, and the list cleared.  Done this way to prevent the server from trying to handle the packet before
	 * it has created the entity, as the entity is created on the update call, but the packet might get here due to construction.**/
	public final List<WrapperPlayer> playersRequestingData = new ArrayList<WrapperPlayer>();
	
	public ABuilderEntityBase(World world){
		super(world);
		if(world.isRemote){
			needDataFromServer = true;
		}
	}
    
	@Override
	public void onUpdate(){
		//Don't call the super, because some mods muck with our logic here.
    	//Said mods are Sponge plugins, but I'm sure there are others.
		//super.onUpdate();
		
		onEntityUpdate();
	}
	
    @Override
    public void onEntityUpdate(){
    	//Don't call the super, because some mods muck with our logic here.
    	//Said mods are Sponge plugins, but I'm sure there are others.
    	//super.onEntityUpdate();
    	
    	if(world.isRemote){
    		//No data.  Wait for NBT to be loaded.
    		//As we are on a client we need to send a packet to the server to request NBT data.
    		///Although we could call this in the constructor, Minecraft changes the
    		//entity IDs after spawning and that fouls things up.
    		if(needDataFromServer){
    			InterfacePacket.sendToServer(new PacketEntityCSHandshakeClient(InterfaceClient.getClientPlayer(), this));
    			needDataFromServer = false;
    		}
    	}else{
    		//Builder ticked on the server.  If we don't have NBT, it's invalid.
    		if(!loadedFromNBT){
	    		if(lastLoadedNBT == null){
	    			InterfaceCore.logError("Tried to tick a builder without first loading NBT on the server.  This is NOT allowed!  Removing builder.");
	    			setDead();
	    			return;
	    		}else{
	    			handleLoadedNBT(lastLoadedNBT);
	    			loadedFromNBT = true;
	    		}
    		}
    		
    		//Send any packets to clients that requested them.
    		if(!playersRequestingData.isEmpty()){
	    		for(WrapperPlayer player : playersRequestingData){
	    			WrapperNBT data = new WrapperNBT();
	    			writeToNBT(data.tag);
	    			player.sendPacket(new PacketEntityCSHandshakeServer(this, data));
	    		}
	    		playersRequestingData.clear();
    		}
    	}
    }
	
    @Override
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    }
    
    @Override
    public boolean shouldRenderInPass(int pass){
        //Don't normally render entities.
    	return false;
    }
    
    public abstract void handleLoadedNBT(NBTTagCompound tag);
			
    @Override
	public void readFromNBT(NBTTagCompound tag){
    	super.readFromNBT(tag);
    	//If we are on a server, save the NBT for loading in the next update call.
		//If we are on a client, we'll get this via packet.
		if(!world.isRemote){
			lastLoadedNBT = tag;
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(!world.isRemote){
			if(lastLoadedNBT != null){
				tag.merge(lastLoadedNBT);
			}
		}
		return tag;
	}
	
	//Junk methods, forced to pull in.
    @Override protected void entityInit(){}
    @Override protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
    @Override protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
