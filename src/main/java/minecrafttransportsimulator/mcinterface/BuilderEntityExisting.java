package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**Builder for the main entity classes for MTS.  This builder allows us to create a new entity
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
public class BuilderEntityExisting extends ABuilderEntityBase{
	/**Maps Entity class names to instances of the IItemEntityProvider class that creates them.**/
	protected static final Map<String, IItemEntityProvider<?>> entityMap = new HashMap<String, IItemEntityProvider<?>>();
	
	/**Current entity we are built around.  This MAY be null if we haven't loaded NBT from the server yet.**/
	protected AEntityB_Existing entity;
	/**Last saved explosion position (used for damage calcs).**/
	private static Point3d lastExplosionPosition;
	/**Collective for interaction boxes.  These are used by this entity to allow players to interact with it.**/
	private WrapperAABBCollective interactionBoxes;
	/**Collective for collision boxes.  These are used by this entity to make things collide with it.**/
	private WrapperAABBCollective collisionBoxes;
	
	public BuilderEntityExisting(World world){
		super(world);
	}
	
    @Override
    public void onEntityUpdate(){
    	super.onEntityUpdate();
    	
    	//If our entity isn't null, update it and our position.
    	if(entity != null){
    		//Check if we are still valid, or need to be set dead.
    		if(!entity.isValid){
    			setDead();
    		}else{
    			//Start master profiling section.
    			entity.world.beginProfiling("MTSEntity_" + getEntityId(), true);
    			entity.world.beginProfiling("Main_Execution", true);
    			
	    		//Forward the update call.
	    		entity.update();
	    		
	    		//Set the new position and rotation.
	    		entity.world.beginProfiling("MovementOverhead", false);
	    		setPosition(entity.position.x, entity.position.y, entity.position.z);
	    		rotationYaw = (float) -entity.angles.y;
	    		rotationPitch = (float) entity.angles.x;
	    		
	    		//If we are outside valid bounds on the server, set us as dead and exit.
	    		if(!world.isRemote && posY < 0 && world.isOutsideBuildHeight(getPosition())){
	    			setDead();
	    			entity.world.endProfiling();
	    			entity.world.endProfiling();
	    			return;
	    		}
	    		
	    		if(entity instanceof AEntityE_Interactable){
	    			AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);
	    			
	    			//Update AABBs.
	        		//We need to know if we need to increase the max world collision bounds to detect this entity.
	        		//Only do this after the first tick of the entity, as we might have some states that need updating
	        		//on that first tick that would cause bad maths.
	        		//We also do this only every second, as it prevents excess checks.
	    			entity.world.beginProfiling("CollisionOverhead", false);
	    			interactionBoxes = new WrapperAABBCollective(interactable.encompassingBox, interactable.getInteractionBoxes());
	        		collisionBoxes = new WrapperAABBCollective(interactable.encompassingBox, interactable.getCollisionBoxes());
	        		if(interactable.ticksExisted > 1 && interactable.ticksExisted%20 == 0){
        				setSize((float) Math.max(interactable.encompassingBox.widthRadius*2F, interactable.encompassingBox.depthRadius*2F), (float) interactable.encompassingBox.heightRadius*2F);
        				//Make sure the collision bounds for MC are big enough to collide with this entity.
	    				if(World.MAX_ENTITY_RADIUS < interactable.encompassingBox.widthRadius || World.MAX_ENTITY_RADIUS < interactable.encompassingBox.heightRadius || World.MAX_ENTITY_RADIUS < interactable.encompassingBox.depthRadius){
	    					World.MAX_ENTITY_RADIUS = Math.max(Math.max(interactable.encompassingBox.widthRadius, interactable.encompassingBox.depthRadius), interactable.encompassingBox.heightRadius);
	    				}
	        		}
	        		
	        		//Check that riders are still present prior to updating them.
	        		//This handles dismounting of riders from entities in a non-event-driven way.
	        		//We do this because other mods and Sponge like to screw up the events...
	        		entity.world.beginProfiling("RiderOverhead", false);
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
	    		entity.world.endProfiling();
    			entity.world.endProfiling();
    		}
    	}else{
    		//If we have NBT, and haven't loaded it, do so now.
    		if(!loadedFromSavedNBT && loadFromSavedNBT){
				WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
				try{
					entity = entityMap.get(lastLoadedNBT.getString("entityid")).createEntity(worldWrapper, null, new WrapperNBT(lastLoadedNBT));
					entity.world.addEntity(entity);
					loadedFromSavedNBT = true;
					lastLoadedNBT = null;
				}catch(Exception e){
					InterfaceCore.logError("Failed to load entity on builder from saved NBT.  Did a pack change?");
					InterfaceCore.logError(e.getMessage());
					setDead();
				}
			}
    	}
    }
    
	@Override
	public void setDead(){
		super.setDead();
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
		if(!isDead){
			setDead();
		}
	}
    
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount){
		if(!world.isRemote && entity instanceof AEntityE_Interactable){
			AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);
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
    	if(entity instanceof AEntityE_Interactable){
    		AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);
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
    	return entity != null ? InterfaceEventsEntityRendering.renderCurrentRiderSitting : super.shouldRiderSit();
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
		if(entity instanceof AEntityF_Multipart){
			for(APart part : ((AEntityF_Multipart<?>) entity).parts){
				for(BoundingBox box : part.interactionBoxes){
					if(box.isPointInside(new Point3d(target.hitVec.x, target.hitVec.y, target.hitVec.z))){
						AItemPack<?> partItem = part.getItem();
						if(partItem != null){
							return part.getItem().getNewStack(part.save(new WrapperNBT())).stack;
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
		return collisionBoxes != null && !collisionBoxes.boxes.isEmpty();
	}
	
	@Override
	public boolean canRiderInteract(){
		//Return true here to allow player to interact with this entity while riding.
        return true;
    }
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(entity != null){
			//Entity is valid, save it and return the modified tag.
			//Also save the class ID so we know what to construct when MC loads this Entity back up.
			entity.save(new WrapperNBT(tag));
			tag.setString("entityid", entity.getClass().getSimpleName());
		}
		return tag;
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
    	if(event.getTarget() instanceof BuilderEntityExisting && ((BuilderEntityExisting) event.getTarget()).entity instanceof EntityVehicleF_Physics){
    		BuilderEntityExisting builder = (BuilderEntityExisting) event.getTarget();
    		if(event.getEntityPlayer().world.isRemote && event.getEntityPlayer().equals(Minecraft.getMinecraft().player) && event.getHand().equals(EnumHand.MAIN_HAND) && builder.interactionBoxes != null){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
	    		if(boxClicked != null){
		    		InterfacePacket.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity, WrapperPlayer.getWrapperFor(event.getEntityPlayer()), boxClicked, true));
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
    	if(event.getTarget() instanceof BuilderEntityExisting && ((BuilderEntityExisting) event.getTarget()).entity instanceof EntityVehicleF_Physics){
    		BuilderEntityExisting builder = (BuilderEntityExisting) event.getTarget();
    		if(event.getEntityPlayer().world.isRemote && event.getEntityPlayer().equals(Minecraft.getMinecraft().player)){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
    			if(boxClicked != null){
    				InterfacePacket.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity,  WrapperPlayer.getWrapperFor(event.getEntityPlayer()), boxClicked, false));
        		}else{
        			InterfaceCore.logError("A entity was clicked (attacked) without doing RayTracing first, or AABBs in vehicle are corrupt!");
        		}
    			event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
    		}
	    	event.setCanceled(true);
    	}
    }
	
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
		event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityExisting.class).id(new ResourceLocation(MasterLoader.MODID, "mts_entity"), 0).name("mts_entity").tracker(32*16, 5, false).build());
	}
}
