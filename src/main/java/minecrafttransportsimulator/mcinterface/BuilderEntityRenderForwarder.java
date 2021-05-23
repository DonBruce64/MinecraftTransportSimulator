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
import net.minecraftforge.client.MinecraftForgeClient;
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
	
	private long[] lastTickRendered = new long[]{0L, 0L, 0L};
	private float[] lastPartialTickRendered = new float[]{0F, 0F, 0F};
	private boolean doneRenderingShaders;
	private static boolean shadersDetected;
	
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
		setSize(0.25F, 0.25F);
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
    		//Need to move the fake entity forwards to account for the partial ticks interpolation MC does.
    		//If we don't do this, and we move faster than 1 block per tick, we'll get flickering.
    		double playerVelocity = Math.sqrt(playerFollowing.motionX*playerFollowing.motionX + playerFollowing.motionY*playerFollowing.motionY + playerFollowing.motionZ*playerFollowing.motionZ);
    		Vec3d playerEyesVec = playerFollowing.getLookVec().scale(Math.max(1, playerVelocity/2));
    		setPosition(playerFollowing.posX + playerEyesVec.x, playerFollowing.posY + playerFollowing.eyeHeight + playerEyesVec.y, playerFollowing.posZ + playerEyesVec.z);
    	}else if(!world.isRemote){
    		//Forwarder with no player, or no living player.  Player either left, or is dead.  Remove.
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
    public boolean shouldRenderInPass(int pass){
        //Need to render in pass 1 to render transparent things in the world like light beams.
    	return true;
    }
    
    /**
	 *  Helper method to tell if we need to render this entity on the current pass.
	 *  Always renders on passes 0 and 1, and sometimes on pass -1 if we didn't
	 *  render on pass 0 or 1.
	 */
    public boolean shouldRenderEntity(float partialTicks){
		//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
		int renderPass = MinecraftForgeClient.getRenderPass();
		if(renderPass == -1){
			renderPass = 2;
		}
		
		//We always render on pass 0 and 1, but we only render on pass 2 if we haven't rendered on pass 0 or 1.
		//If we are rendering on pass 0 or 1 a second time (before pass 2), it means shaders are present.
		//Set bit to detect these buggers and keep vehicles from rendering funny or disappearing.
		if(renderPass != 2 && lastTickRendered[renderPass] > lastTickRendered[2] && lastTickRendered[2] > 0){
			shadersDetected = true;
		}
		
		//If we are rendering the object, update times.
		//This may not be the case if shaders are present and we haven't rendered the shader component.
		//Shaders do a pre-render to get their shadow, so the first render pass is actually invalid.
		if(!shadersDetected || doneRenderingShaders){
			//Rendering the actual model now.
			lastTickRendered[renderPass] = world.getTotalWorldTime();
			lastPartialTickRendered[renderPass] = partialTicks;
		}else if(shadersDetected && !doneRenderingShaders){
			//Rendering shader components.  If we're on pass 1, then shaders should be done rendering this cycle.
			if(renderPass == 1){
				doneRenderingShaders = true;
			}
		}
		
		if(renderPass == 2){
			//If we already rendered in pass 0, don't render now.
			//Note that shaders may do operations in pass 0 for lighting, but won't render the actual model.
			//In this case, the lastTickPass won't have been updated, so we do render here.
			//We also need to reset the shader render state variable to ensure we are ready for the next cycle.
			if(shadersDetected){
				doneRenderingShaders = false;
			}
			return lastTickRendered[0] != lastTickRendered[2] || lastPartialTickRendered[0] != lastPartialTickRendered[2];
		}else{
			return true;
		}
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
