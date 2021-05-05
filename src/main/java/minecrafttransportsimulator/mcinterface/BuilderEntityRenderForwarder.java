package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.MasterLoader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**Builder for an entity to forward rendering calls to all internal renderer.  This is due to prevent
 * MC from culling entities when it should be rendering them instead.  MC does this when you can't see
 * the chunk the entity is in, or if they are above the world, but we don't want that.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntityRenderForwarder extends ABuilderEntityBase{
	
	public EntityPlayer playerFollowing;
	public static final Map<UUID, BuilderEntityRenderForwarder> activeFollowers = new HashMap<UUID, BuilderEntityRenderForwarder>();
	
	public BuilderEntityRenderForwarder(World world){
		super(world);
		setSize(0.05F, 0.05F);
		if(!world.isRemote){
			//Need to set this so we don't try to re-load the player data.
			//We'll then get marked dead on the next update.
			loadedFromNBT = true;
		}
	}
	
	public BuilderEntityRenderForwarder(EntityPlayer playerFollowing){
		super(playerFollowing.world);
		setSize(0.05F, 0.05F);
		this.playerFollowing = playerFollowing;
		this.setPosition(playerFollowing.posX, playerFollowing.posY, playerFollowing.posZ);
		activeFollowers.put(playerFollowing.getUniqueID(), this);
		//Need to set this as we don't spawn this builder normally.
		loadedFromNBT = true;
	}
	
    @Override
    public void onEntityUpdate(){
    	super.onEntityUpdate();
    	if(playerFollowing != null && !playerFollowing.isDead && playerFollowing.world == this.world){
    		Vec3d playerEyesVec = playerFollowing.getLookVec();
    		setPosition(playerFollowing.posX + playerEyesVec.x, playerFollowing.posY + playerFollowing.eyeHeight + playerEyesVec.y, playerFollowing.posZ + playerEyesVec.z);
    	}else if(!world.isRemote){
    		setDead();
    	}
    }
    
	@Override
	public void setDead(){
		super.setDead();
		if(!world.isRemote && playerFollowing != null){
			activeFollowers.remove(playerFollowing.getUniqueID());
		}
	}
	
	@Override
	public void onRemovedFromWorld(){
		super.onRemovedFromWorld();
		//Catch unloaded entities from when the chunk goes away.
		setDead();
	}
    
    @Override
    public boolean shouldRenderInPass(int pass){
        //Need to render in pass 1 to render transparent things in the world like light beams.
    	return true;
    }

	@Override
	public void handleLoadedNBT(NBTTagCompound tag){
		playerFollowing = world.getPlayerEntityByUUID(tag.getUniqueId("playerFollowing"));
		if(!world.isRemote){
			if(playerFollowing == null){
				setDead();
			}else{
				activeFollowers.put(playerFollowing.getUniqueID(), this);
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(playerFollowing != null){
			//Player valid, save it and return the modified tag.
			tag.setUniqueId("playerFollowing", playerFollowing.getUniqueID());
		}
		return tag;
	}
	
	/**
	 * Registers our own class for use.
	 */
	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event){
		event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityRenderForwarder.class).id(new ResourceLocation(MasterLoader.MODID, "mts_entity_renderer"), 1).name("mts_entity_renderer").tracker(32*16, 5, false).build());
	}
}
