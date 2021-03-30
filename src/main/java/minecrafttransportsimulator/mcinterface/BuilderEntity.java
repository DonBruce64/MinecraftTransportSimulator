package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshake;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.rendering.components.InterfaceEventsPlayerRendering;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**Builder for a basic MC Entity class.  This builder allows us to create a new entity
 * class that we can control that doesn't have the wonky systems the MC entities have, such
 * as no roll axis, a single hitbox, and tons of immutable objects that get thrown away every update.
 * Constructor simply takes in a world instance per default MC standards, but doesn't create the actual
 * {@link AEntityB_Existing} until later.  This is because we can't build our entity at the same time MC creates 
 * this instance as we might not yet have NBT data.  Instead, we simply hold on to the class and construct 
 * it whenever we get called to do so.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntity extends Entity{
	/**Maps Entity class names to instances of the IItemEntityProvider class that creates them.**/
	public static final Map<String, IItemEntityProvider<?>> entityMap = new HashMap<String, IItemEntityProvider<?>>();
	
	/**Current entity we are built around.  This MAY be null if we haven't loaded NBT from the server yet.**/
	public AEntityB_Existing entity;
	/**This flag is true if we need to get server data for syncing.  Set on construction tick on clients.**/
	private boolean requestDataFromServer;
	/**Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
	 * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
	 * to do their funky logic.  I'm looking at YOU The One Probe!**/
	private NBTTagCompound lastLoadedNBT;
	/**Last saved explosion position (used for damage calcs).**/
	private static Point3d lastExplosionPosition;
	/**Position where we have spawned a fake light.  Used for shader compatibility.**/
	private BlockPos fakeLightPosition;
	/**Collective for interaction boxes.  These are used by this entity to allow players to interact with it.**/
	private WrapperAABBCollective interactionBoxes;
	/**Collective for collision boxes.  These are used by this entity to make things collide with it.**/
	private WrapperAABBCollective collisionBoxes;
	/**Render data to help us render on the proper tick and time.**/
	public final RenderTickData renderData;
	/**Players requesting data for this builder.  This is populated by packets sent to the server.  Each tick players in this list are
	 * sent data about this builder, and the list cleared.  Done this way to prevent the server from trying to handle the packet before
	 * it has created the entity, as the entity is created on the update call, but the packet might get here due to construction.**/
	public final List<WrapperPlayer> playersRequestingData = new ArrayList<WrapperPlayer>();
	
	public BuilderEntity(World world){
		super(world);
		if(world.isRemote){
			requestDataFromServer = true;
		}
		
		//Create render data if we are on the client.
		this.renderData = world.isRemote ? new RenderTickData(world) : null;
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
    	
    	//If our entity isn't null, update it and our position.
    	if(entity != null){
    		//Check if we are still valid, or need to be set dead.
    		if(!entity.isValid){
    			setDead();
    		}else{
	    		//Forward the update call.
	    		entity.update();
	    		
	    		//Set the new position and rotation.
	    		setPosition(entity.position.x, entity.position.y, entity.position.z);
	    		rotationYaw = (float) -entity.angles.y;
	    		rotationPitch = (float) entity.angles.x;
	    		
	    		//If we are outside valid bounds on the server, set us as dead and exit.
	    		if(!world.isRemote && posY < 0 && world.isOutsideBuildHeight(getPosition())){
	    			setDead();
	    			return;
	    		}
	    		
	    		if(entity instanceof AEntityD_Interactable){
	    			AEntityD_Interactable<?> interactable = ((AEntityD_Interactable<?>) entity);
	    			
	    			//Update AABBs.
	        		//We need to update a wrapper class here as normal entities only allow a single collision box.
	        		//We also need to know if we need to increase the max world collision bounds to detect this entity.
	        		//Only do this after the first tick of the entity, as we might have some states that need updating
	        		//on that first tick that would cause bad maths.
	        		//We also do this only every second, as it prevents excess checks.
	        		interactionBoxes = new WrapperAABBCollective(this, interactable instanceof AEntityE_Multipart ? ((AEntityE_Multipart<?>) interactable).allInteractionBoxes : interactable.interactionBoxes);
	        		collisionBoxes = new WrapperAABBCollective(this, interactable instanceof AEntityE_Multipart ? ((AEntityE_Multipart<?>) interactable).allCollisionBoxes : interactable.collisionBoxes);
	        		if(interactable.ticksExisted > 1 && interactable.ticksExisted%20 == 0){
	    	    		double furthestWidthRadius = 0;
	    	    		double furthestHeightRadius = 0;
	    	    		for(BoundingBox box : interactionBoxes.boxes){
	    	    			furthestWidthRadius = (float) Math.max(furthestWidthRadius, Math.abs(box.globalCenter.x - interactable.position.x + box.widthRadius));
	    	    			furthestHeightRadius = (float) Math.max(furthestHeightRadius, Math.abs(box.globalCenter.y - interactable.position.y + box.heightRadius));
	    	    			furthestWidthRadius = (float) Math.max(furthestWidthRadius, Math.abs(box.globalCenter.z - interactable.position.z + box.depthRadius));
	    	    		}
	    	    		setSize((float) furthestWidthRadius*2F, (float) furthestHeightRadius*2F);
	    	    		
	    	    		//Make sure the collision bounds for MC are big enough to collide with this entity.
	    				if(World.MAX_ENTITY_RADIUS < furthestWidthRadius || World.MAX_ENTITY_RADIUS < furthestHeightRadius){
	    					World.MAX_ENTITY_RADIUS = Math.max(furthestWidthRadius, furthestHeightRadius);
	    				}
	        		}
	        		
	        		//Check that riders are still present prior to updating them.
	        		//This handles dismounting of riders from entities in a non-event-driven way.
	        		//We do this because other mods and Sponge like to screw up the events...
	        		if(!world.isRemote){
	    	    		Iterator<WrapperEntity> riderIterator = interactable.locationRiderMap.inverse().keySet().iterator();
	    	    		while(riderIterator.hasNext()){
	    	    			WrapperEntity rider = riderIterator.next();
	    	    			if(!this.equals(rider.entity.getRidingEntity())){
	    	    				interactable.removeRider(rider, riderIterator);
	    	    			}
	    	    		}
	        		}
	    		}
	    		
	    		
	    		//Update fake block lighting.  This helps with shaders as they sometimes refuse to light things up.
	    		if(world.isRemote){
	    			if(entity.getLightProvided() > 0){
						BlockPos newPos = getPosition();
						//Check to see if we need to place a light.
						if(!newPos.equals(fakeLightPosition)){
							//If our prior position is not null, remove that block.
							if(fakeLightPosition != null){
								world.setBlockToAir(fakeLightPosition);
								world.checkLight(fakeLightPosition);
								fakeLightPosition = null;
							}
							//Set block in world and update pos.  Only do this if the block is air.
							if(world.isAirBlock(newPos)){
								world.setBlockState(newPos, BuilderBlockFakeLight.instance.getDefaultState());
								world.checkLight(newPos);
								fakeLightPosition = newPos;
							}
						}
	    			}else if(fakeLightPosition != null){
	    				//Lights are off, turn off fake light.
	    				world.setBlockToAir(fakeLightPosition);
	    				world.checkLight(fakeLightPosition);
	    				fakeLightPosition = null;
	    			}
	    		}
	    		
	    		//Send any packets to clients that requested them.
	    		if(!playersRequestingData.isEmpty()){
		    		for(WrapperPlayer player : playersRequestingData){
		    			WrapperNBT data = new WrapperNBT();
		    			writeToNBT(data.tag);
		    			player.sendPacket(new PacketEntityCSHandshake(getEntityId(), data));
		    		}
		    		playersRequestingData.clear();
	    		}
    		}
    	}else if(world.isRemote){
    		//No entity.  Wait for NBT to be loaded to create it.
    		//As we are on a client we need to send a packet to the server to request NBT data.
    		///Although we could call this in the constructor, Minecraft changes the
    		//entity IDs after spawning and that fouls things up.
    		if(requestDataFromServer){
    			InterfacePacket.sendToServer(new PacketEntityCSHandshake(this.getEntityId(), null));
    			requestDataFromServer = false;
    		}
    	}else{
    		//Builder with no entity on the server.  Try to get it from NBT. If we can't, it's invalid.
    		if(lastLoadedNBT != null){
    			WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
    			try{
    				entity = entityMap.get(lastLoadedNBT.getString("entityid")).createEntity(worldWrapper, new WrapperNBT(lastLoadedNBT));
    			}catch(Exception e){
    				InterfaceCore.logError("Failed to load entity on builder from saved NBT.  Did a pack change?");
    				InterfaceCore.logError(e.getMessage());
    				setDead();
    			}
    			lastLoadedNBT = null;
    		}else{
    			InterfaceCore.logError("Tried to tick a builder without first loading NBT on the server.  This is NOT allowed!  Removing builder.");
    			setDead();
    		}
    	}
    }
    
	@Override
	public void setDead(){
		super.setDead();
		//Get rid of the fake light (if we have one) before we kill ourselves.
		if(fakeLightPosition != null){
			world.setBlockToAir(fakeLightPosition);
		}
		
		//Stop chunkloading of this entity.
		InterfaceChunkloader.removeEntityTicket(this);
		
		//Notify internal entity of it being invalid.
		if(entity != null){
			entity.remove();
		}
	}
	
	@Override
	public void onRemovedFromWorld(){
		super.onRemovedFromWorld();
		//Catch unloaded entities from when the chunk goes away.
		if(entity != null && entity.isValid){
			setDead();
		}
	}
    
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount){
		if(!world.isRemote && entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = ((AEntityD_Interactable<?>) entity);
			Entity attacker = source.getImmediateSource();
			Entity trueSource = source.getTrueSource();
			WrapperPlayer playerSource = trueSource instanceof EntityPlayer ? WrapperPlayer.getWrapperFor((EntityPlayer) trueSource) : null;
			if(lastExplosionPosition != null && source.isExplosion()){
				//We encountered an explosion.  These may or may not have have entities linked to them.  Depends on if
				//it's a player firing a gun that had a bullet, or a random TNT lighting in the world.
				//Explosions, unlike other damage sources, can hit multiple collision boxes on an entity at once.
				BoundingBox explosiveBounds = new BoundingBox(lastExplosionPosition, amount, amount, amount);
				for(BoundingBox box : interactionBoxes.boxes){
					if(box.intersects(explosiveBounds)){
						interactable.attack(new Damage(source.damageType, amount, box, null, playerSource).setExplosive());
					}
				}
				lastExplosionPosition = null;
			}else if(attacker != null){
				Damage damage = null;
				//Check the damage at the current position of the attacker.
				Point3d attackerPosition = new Point3d(attacker.posX, attacker.posY, attacker.posZ);
				for(BoundingBox box : interactionBoxes.boxes){
					if(box.isPointInside(attackerPosition)){
						damage = new Damage(source.damageType, amount, box, null, playerSource);
						break;
					}
				}
				
				if(damage == null){
					//Check the theoretical position of the entity should it have moved.
					//Some projectiles may call their attacking code before updating their positions.
					//We do raytracing here to catch this movement.
					RayTraceResult hitRaytrace = interactionBoxes.calculateIntercept(attacker.getPositionVector(), attacker.getPositionVector().add(attacker.motionX, attacker.motionY, attacker.motionZ));
					if(hitRaytrace != null){
						damage = new Damage(source.damageType, amount, interactionBoxes.lastBoxRayTraced, null, playerSource);
					}
				}
				
				//If we have damage on a point, attack it now.
				if(damage != null){
					interactable.attack(damage);
				} 
			}
		}
		return true;
    }
    
    @Override
    public void updatePassenger(Entity passenger){
    	//Forward passenger updates to the entity, if it exists.
    	if(entity instanceof AEntityD_Interactable){
    		AEntityD_Interactable<?> interactable = ((AEntityD_Interactable<?>) entity);
    		Iterator<WrapperEntity> iterator = interactable.locationRiderMap.inverse().keySet().iterator();
    		while(iterator.hasNext()){
    			WrapperEntity rider = iterator.next();
    			if(rider.entity.equals(passenger)){
    				interactable.updateRider(rider, iterator);
    				return;
    			}
    		}
    		//Couldn't find rider in entity list.  Add them as a passenger.
    		interactable.addRider(WrapperEntity.getWrapperFor(passenger), null);
    	}
    }
    
    @Override
    public boolean shouldRiderSit(){
    	return entity != null ? InterfaceEventsPlayerRendering.renderCurrentRiderSitting : super.shouldRiderSit();
    }
    
    @Override
	public AxisAlignedBB getEntityBoundingBox(){
		//Override this to make interaction checks work with the multiple collision points.
		//We return the collision and interaction boxes here as we need a bounding box large enough to encompass both.
		return interactionBoxes != null ? interactionBoxes : super.getEntityBoundingBox();
	}
	
	@Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(){
		//Override this to make collision checks work with the multiple collision points.
		//We only return collision boxes here as we don't want the player to collide with interaction boxes.
		return collisionBoxes != null ? collisionBoxes : super.getCollisionBoundingBox();
    }
	
	@Override
	public ItemStack getPickedResult(RayTraceResult target){
		if(entity instanceof AEntityE_Multipart){
			for(APart part : ((AEntityE_Multipart<?>) entity).parts){
				for(BoundingBox box : part.interactionBoxes){
					if(box.isPointInside(new Point3d(target.hitVec.x, target.hitVec.y, target.hitVec.z))){
						AItemPack<?> partItem = part.getItem();
						if(partItem != null){
							ItemStack stack = part.getItem().getNewStack();
							WrapperNBT partData = new WrapperNBT();
							part.save(partData);
							stack.setTagCompound(partData.tag);
							return stack;
						}
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}
    
    @Override
	public boolean canBeCollidedWith(){
		//This gets overridden to allow players to interact with this entity.
		return true;
	}
	
	@Override
	public boolean canRiderInteract(){
		//Return true here to allow player to interact with this entity while riding.
        return true;
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
	public void readFromNBT(NBTTagCompound tag){
    	super.readFromNBT(tag);
		if(entity == null && tag.hasKey("entityid")){
			//If we are on a server, save the NBT for loading in the next update call.
			//If we are on a client, we'll get this via packet.
			if(!world.isRemote){
				lastLoadedNBT = tag;
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(entity != null){
			//Entity is valid, save it and return the modified tag.
			//Also save the class ID so we know what to construct when MC loads this Entity back up.
			entity.save(new WrapperNBT(tag));
			tag.setString("entityid", entity.getClass().getSimpleName());
		}else if(!world.isRemote){
			if(lastLoadedNBT != null){
				tag.merge(lastLoadedNBT);
			}else{
				InterfaceCore.logError("Tried to save a builder without an entity on it and no NBT.  This shouldn't happen as it means we are trying to save an invalid builder that hasn't had NBT loaded yet.  Smells like coremod hackery...");
				setDead();
			}
		}
		return tag;
	}
	
	/**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void on(WorldEvent.Unload event){
    	AEntityA_Base.removaAllEntities(WrapperWorld.getWrapperFor(event.getWorld()));
    }
	
	/**
	 * We need to use explosion events here as we don't know where explosions occur in the world.
	 * This results in them being position-less, so we can't get the collision box they hit for damage.
	 * Whenever we have an explosion detonated in the world, save it's position.  We can then use it
	 * in {@link #attackEntityFrom(DamageSource, float)} to tell the system which part to attack.
	 */
	@SubscribeEvent
	public static void on(ExplosionEvent.Detonate event){
		if(!event.getWorld().isRemote){
			lastExplosionPosition = new Point3d(event.getExplosion().getPosition().x, event.getExplosion().getPosition().y, event.getExplosion().getPosition().z);
		}
	}
	
    /**
     * If a player swings and misses a large entity they may still have hit it.
     * MC doesn't look for attacks based on AABB, rather it uses RayTracing.
     * This works on the client where we can see the entity bounding boxes,
     * but on the server the internal distance check nulls this out.
     * If we click an entity, cancel the "interaction" and instead send a packet 
     * to the server to make dang sure we register the interaction!
     * Note that unlike the attack code, we don't want to cancel all interactions.
     * This can lead to blocking of interactions from offhands.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.EntityInteract event){
    	if(event.getTarget() instanceof BuilderEntity && ((BuilderEntity) event.getTarget()).entity instanceof EntityVehicleF_Physics){
    		BuilderEntity builder = (BuilderEntity) event.getTarget();
    		if(event.getEntityPlayer().world.isRemote && event.getEntityPlayer().equals(Minecraft.getMinecraft().player) && event.getHand().equals(EnumHand.MAIN_HAND) && builder.interactionBoxes != null){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
	    		if(boxClicked != null){
		    		InterfacePacket.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity, boxClicked, true));
	    		}else{
	    			InterfaceCore.logError("A entity was clicked (interacted) without doing RayTracing first, or AABBs in vehicle are corrupt!");
	    		}
	    		event.setCanceled(true);
				event.setCancellationResult(EnumActionResult.SUCCESS);
    		}
    	}
    }
	
    /**
     * If a player swings and misses a large entity they may still have hit it.
     * MC doesn't look for attacks based on AABB, rather it uses RayTracing.
     * This works on the client where we can see the entity bounding boxes,
     * but on the server the internal distance check nulls this out.
     * If we click an entity, cancel the "attack" and instead send a packet 
     * to the server to make dang sure we register the attack!
     */
    @SubscribeEvent
    public static void on(AttackEntityEvent event){
    	if(event.getTarget() instanceof BuilderEntity && ((BuilderEntity) event.getTarget()).entity instanceof EntityVehicleF_Physics){
    		BuilderEntity builder = (BuilderEntity) event.getTarget();
    		if(event.getEntityPlayer().world.isRemote && event.getEntityPlayer().equals(Minecraft.getMinecraft().player)){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
    			if(boxClicked != null){
    				InterfacePacket.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity, boxClicked, false));
        		}else{
        			InterfaceCore.logError("A entity was clicked (attacked) without doing RayTracing first, or AABBs in vehicle are corrupt!");
        		}
    			event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
    		}
	    	event.setCanceled(true);
    	}
    }
	
	//Junk methods, forced to pull in.
    @Override protected void entityInit(){}
    @Override protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
    @Override protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
	
	/**
	 * Registers all builder instances that build our own entities into the game.
	 */
	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event){
		//Iterate over all pack items and find those that spawn entities.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			if(packItem instanceof IItemEntityProvider<?>){
				entityMap.put(((IItemEntityProvider<?>) packItem).getEntityClass().getSimpleName(), (IItemEntityProvider<?>) packItem);
			}
		}
		
		//Now register our own classes.
		int entityNumber = 0;
		event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntity.class).id(new ResourceLocation(MasterLoader.MODID, "mts_entity"), entityNumber++).name("mts_entity").tracker(32*16, 5, false).build());
	}
}
