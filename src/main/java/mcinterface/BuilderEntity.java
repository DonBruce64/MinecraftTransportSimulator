package mcinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshake;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**Builder for a basic MC Entity class.  This builder allows us to create a new entity
 * class that we can control that doesn't have the wonky systems the MC entities have, such
 * as no roll axis, a single hitbox, and tons of immutable objects that get thrown away every update.
 * Constructor takes a class of {@link AEntityBase}} to construct, but NOT an instance.  This is because
 * we can't create our entity instance at the same time MC creates its instance as we might not yet have NBT
 * data.  Instead, we simply hold on to the class and construct it whenever we get called to do so.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber
public class BuilderEntity extends Entity{
	/**This flag is true if we need to get server data for syncing.  Set on construction tick on clients.**/
	private boolean requestDataFromServer;
	/**Last saved explosion position (used for damage calcs).**/
	private static Point3d lastExplosionPosition;
	/**Position where we have spawned a fake light.  Used for shader compatibility.**/
	private BlockPos fakeLightPosition;
	
	private WrapperAABBCollective interactionBoxes;
	private WrapperAABBCollective collisionBoxes;
	
	/**Current entity we are built around.**/
	AEntityBase entity;
	/**Map of created entities linked to their builder instances.  Used for interface operations.**/
	static final Map<AEntityBase, BuilderEntity> entitiesToBuilders = new HashMap<AEntityBase, BuilderEntity>();
	/**Maps Entity class names to instances of the IItemEntityProvider class that creates them.**/
	static final Map<String, IItemEntityProvider<?>> entityMap = new HashMap<String, IItemEntityProvider<?>>();
	
	public BuilderEntity(World world){
		super(world);
	}
    
    @Override
    public void onEntityUpdate(){
    	//If our entity isn't null, update it and our position.
    	if(entity != null){
    		//First forward the update call.
    		entity.update();
    		
    		//Update AABBs.
    		//We need to update a wrapper class here as normal entities only allow a single collision box.
    		//We also need to know if we need to increase the max world collision bounds to detect this entity.
    		double furthestWidthRadius = 0;
    		double furthestHeightRadius = 0;
    		for(BoundingBox box : entity.interactionBoxes){
    			furthestWidthRadius = (float) Math.max(furthestWidthRadius, box.localCenter.x + box.widthRadius);
    			furthestHeightRadius = (float) Math.max(furthestHeightRadius, box.localCenter.y + box.heightRadius);
    			furthestWidthRadius = (float) Math.max(furthestWidthRadius, box.localCenter.z + box.depthRadius);
    		}
    		setSize((float) furthestWidthRadius*2F, (float) furthestHeightRadius*2F);
    		interactionBoxes = new WrapperAABBCollective(this, entity.interactionBoxes);
    		collisionBoxes = new WrapperAABBCollective(this, entity.collisionBoxes);
    		
    		//Make sure the collision bounds for MC are big enough to collide with this entity.
			if(World.MAX_ENTITY_RADIUS < furthestWidthRadius || World.MAX_ENTITY_RADIUS < furthestHeightRadius){
				World.MAX_ENTITY_RADIUS = Math.max(furthestWidthRadius, furthestHeightRadius);
			}
    		
			//Set the new position and rotation.
    		setPosition(entity.position.x, entity.position.y, entity.position.z);
    		rotationYaw = (float) -entity.angles.y;
    		rotationPitch = (float) entity.angles.x;
    		
    		//Check that riders are still present prior to updating them.
    		//This handles dismounting of riders from entities in a non-event-driven way.
    		//We do this because other mods and Sponge like to screw up the events...
    		Iterator<WrapperEntity> riderIterator = entity.locationRiderMap.inverse().keySet().iterator();
    		while(riderIterator.hasNext()){
    			WrapperEntity rider = riderIterator.next();
    			if(!this.equals(rider.entity.getRidingEntity())){
    				entity.removeRider(rider, riderIterator);
    			}
    		}
    		
    		//Update fake block lighting.  This helps with shaders as they sometimes refuse to light things up.
    		if(world.isRemote){
    			if(entity.isLitUp()){
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
    		
    		//Check if we are still valid, or need to be set dead.
    		if(!entity.isValid || entity.position.y < -5){
    			setDead();
    		}
    	}else{
    		//No entity.  Wait for NBT to be loaded to create it.
    		//If we are on a client, ensure we sent a packet to the server to request it.
    		///Although we could call this in the constructor, Minecraft changes the
    		//entity IDs after spawning and that fouls things up.
    		//To accommodate this, we request a packet whenever the entityID changes.
    		if(requestDataFromServer){
    			InterfaceNetwork.sendToServer(new PacketEntityCSHandshake(this.getEntityId(), null));
    			requestDataFromServer = false;
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
		//Mark entity as invalid and remove from maps.
		if(entity != null){
			entity.isValid = false;
			if(world.isRemote){
				AEntityBase.createdClientEntities.remove(entity.lookupID);
			}else{
				AEntityBase.createdServerEntities.remove(entity.lookupID);
			}
		}
	}
    
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount){
		if(!world.isRemote && entity != null){
			Entity attacker = source.getImmediateSource();
			Entity trueSource = source.getTrueSource();
			WrapperPlayer playerSource = trueSource instanceof EntityPlayer ? WrapperWorld.getWrapperFor(trueSource.world).getWrapperFor((EntityPlayer) trueSource) : null;
			if(lastExplosionPosition != null && source.isExplosion()){
				//We encountered an explosion.  These may or may not have have entities linked to them.  Depends on if
				//it's a player firing a gun that had a bullet, or a random TNT lighting in the world.
				//Explosions, unlike other damage sources, can hit multiple collision boxes on an entity at once.
				BoundingBox explosiveBounds = new BoundingBox(lastExplosionPosition, amount, amount, amount);
				for(BoundingBox box : entity.interactionBoxes){
					if(box.intersects(explosiveBounds)){
						entity.attack(new Damage(source.damageType, amount, box, playerSource).setExplosive());
					}
				}
				lastExplosionPosition = null;
			}else if(attacker != null){
				Damage damage = null;
				//Check the damage at the current position of the attacker.
				Point3d attackerPosition = new Point3d(attacker.posX, attacker.posY, attacker.posZ);
				for(BoundingBox box : entity.interactionBoxes){
					if(box.isPointInside(attackerPosition)){
						damage = new Damage(source.damageType, amount, box, playerSource);
						break;
					}
				}
				
				if(damage == null){
					//Check the theoretical position of the entity should it have moved.
					//Some projectiles may call their attacking code before updating their positions.
					attackerPosition.add(attacker.motionX, attacker.motionY, attacker.motionZ);
					for(BoundingBox box : entity.interactionBoxes){
						if(box.isPointInside(attackerPosition)){
							damage = new Damage(source.damageType, amount, box, playerSource);
							break;
						}
					}
				}
				
				//If we have damage on a point, attack it now.
				if(damage != null){
					entity.attack(damage);
				} 
			}
		}
		return true;
    }
    
    @Override
    public void updatePassenger(Entity passenger){
    	//Forward passenger updates to the entity, if it exists.
    	if(entity != null){
    		Iterator<WrapperEntity> iterator = entity.locationRiderMap.inverse().keySet().iterator();
    		while(iterator.hasNext()){
    			WrapperEntity rider = iterator.next();
    			if(rider.equals(passenger)){
    				entity.updateRider(rider, iterator);
    				return;
    			}
    		}
    		//Couldn't find rider in entity list.  Add them as a passenger.
    		entity.addRider(passenger instanceof EntityPlayer ? entity.world.getWrapperFor((EntityPlayer)passenger) : entity.world.getWrapperFor(passenger), null);
    	}
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
		if(entity instanceof EntityVehicleF_Physics){
			EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
			for(BoundingBox box : vehicle.interactionBoxes){
				if(box.isPointInside(new Point3d(target.hitVec.x, target.hitVec.y, target.hitVec.z))){
					APart part = vehicle.getPartAtLocation(box.localCenter);
					
					//If the part is null, see if we clicked a part's collision box instead.
					if(part == null){
						for(Entry<APart, List<BoundingBox>> partCollisionEntry : vehicle.partCollisionBoxes.entrySet()){
							for(BoundingBox collisionBox : partCollisionEntry.getValue()){
								if(collisionBox.equals(box)){
									part = partCollisionEntry.getKey();
									break;
								}
							}
							if(part != null){
								break;
							}
						}
					}
					
					//If we found a part, return it as an item.
					if(part != null){
						ItemStack stack = new ItemStack(BuilderItem.itemWrapperMap.get(part.getItem()));
						stack.setTagCompound(part.getData().tag);
						return stack;
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}
    
    @Override
    public void setEntityId(int id){
    	super.setEntityId(id);
    	//If we are setting our ID on a client, request NBT data from the server to load the rest of our properties.
    	//We do this on our next update tick, as we may not yet be spawned at this point.
    	requestDataFromServer = world.isRemote;
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
			//Restore the Entity from saved state.
			entity = entityMap.get(tag.getString("entityid")).createEntity(WrapperWorld.getWrapperFor(world), new WrapperNBT(tag));
			entitiesToBuilders.put(entity, this);
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		entity.save(new WrapperNBT(tag));
		//Also save the class ID so we know what to construct when MC loads this Entity back up.
		tag.setString("entityid", entity.getClass().getSimpleName());
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
    	if(event.getTarget() instanceof BuilderEntity && ((BuilderEntity) event.getTarget()).entity instanceof EntityVehicleF_Physics){
    		BuilderEntity builder = (BuilderEntity) event.getTarget();
    		if(event.getEntityPlayer().world.isRemote && event.getHand().equals(EnumHand.MAIN_HAND) && builder.interactionBoxes != null){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
	    		if(boxClicked != null){
		    		InterfaceNetwork.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity, boxClicked.localCenter, true));
	    		}else{
	    			MTS.MTSLog.error("ERROR: A vehicle was clicked (interacted) without doing RayTracing first, or AABBs in vehicle are corrupt!");
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
    		if(event.getEntityPlayer().world.isRemote){
	    		BoundingBox boxClicked = builder.interactionBoxes.lastBoxRayTraced;
    			if(boxClicked != null){
        			InterfaceNetwork.sendToServer(new PacketVehicleInteract((EntityVehicleF_Physics) builder.entity, boxClicked.localCenter, false));
        		}else{
        			MTS.MTSLog.error("ERROR: A vehicle was clicked (attacked) without doing RayTracing first, or AABBs in vehicle are corrupt!");
        		}
    			event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
    		}
	    	event.setCanceled(true);
    	}
    }
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
	
	/**
	 * Registers all builder instances that build our own entities into the game.
	 */
	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event){
		//Iterate over all pack items and find those that spawn entities.
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<?> item : MTSRegistry.packItemMap.get(packID).values()){
				if(item instanceof IItemEntityProvider<?>){
					entityMap.put(((IItemEntityProvider<?>) item).getEntityClass().getSimpleName(), (IItemEntityProvider<?>) item);
				}
			}
		}
		
		//Now register our own classes.
		int entityNumber = 0;
		event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntity.class).id(new ResourceLocation(MTS.MODID, "mts_entity"), entityNumber++).name("mts_entity").tracker(32*16, 5, false).build());
	}
}
