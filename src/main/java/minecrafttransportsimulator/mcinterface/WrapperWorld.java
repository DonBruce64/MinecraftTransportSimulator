package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataCSHandshake;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.INpc;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.IPlantable;

/**Wrapper to a world instance.  This contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  Note that
 * clients and servers don't share world interfaces, and there are world interfaces for
 * every loaded world, so multiple interfaces will always be present on a system.
 *
 * @author don_bruce
 */
public class WrapperWorld{
	private static final Map<World, WrapperWorld> worldWrappers = new HashMap<World, WrapperWorld>();
	
	public final World world;
	public InterfaceWorldSavedData savedDataAccessor;
	public static final String STORED_WORLD_DATA_ID = MasterLoader.MODID + "_WORLD_DATA";

	private WrapperWorld(World world){
		this.world = world;
		if(world.isRemote){
			InterfacePacket.sendToServer(new PacketWorldSavedDataCSHandshake((WrapperNBT)null));
		}
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in world instance.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 */
	public static WrapperWorld getWrapperFor(World world){
		if(world != null){
			WrapperWorld wrapper = worldWrappers.get(world);
			if(wrapper == null || world != wrapper.world){
				wrapper = new WrapperWorld(world);
				worldWrappers.put(world, wrapper);
			}
			return wrapper;
		}else{
			return null;
		}
	}
	
	/**
	 *  Returns true if this is a client world, false if we're on the server.
	 */
	public boolean isClient(){
		return world.isRemote;
	}
	
	/**
	 *  Returns the ID of the current dimension.
	 *  0 for overworld.
	 *  1 for the End.
	 *  -1 for the Nether.
	 *  Mods may add other values for their dims, so this list is not inclusive.
	 */
	public int getDimensionID(){
		return world.provider.getDimension();
	}
	
	/**
	 *  Returns the current world tick value.  Useful when you need to sync
	 *  operations.  For animations, just use the system time.
	 */
	public long getTick(){
		return world.getTotalWorldTime();
	}
	
	/**
	 *  Returns the time of day of the world, in ticks.  Unlike {@link #getTick()},
	 *  this method may not increment if the world's internal clock isn't currently
	 *  advancing.
	 */
	public long getTime(){
		return world.getWorldTime();
	}
		
	/**
	 *  Returns the max build height for the world.  Note that entities may move and be saved
	 *  above this height, and moving above this height will result in rendering oddities.
	 */
	public long getMaxHeight(){
		return world.getHeight();
	}
	
	/**
	 *  Returns the saved world data for this world.  As servers save data, while clients don't,
	 *  this method will only ensure valid return values on the server.  On clients, there will
	 *  be some delay in obtaining the data from the server due to packets.  As such, this method
	 *  may return null if the data hasn't arrived from the server.  After this, the object will 
	 *  contain all the server data, and will remain updated with data changes from the server.  
	 *  Do NOT attempt to modify the data object on the client, as it will result in a
	 *  de-synchronized state.  Instead, send a packet to the server to modify its copy, 
	 *  and then wait for the synchronizing packet.
	 */
	public WrapperNBT getData(){
		if(!world.isRemote){
			if(savedDataAccessor == null){
				savedDataAccessor = (InterfaceWorldSavedData) world.getPerWorldStorage().getOrLoadData(InterfaceWorldSavedData.class, STORED_WORLD_DATA_ID);
				if(savedDataAccessor == null){
					savedDataAccessor = new InterfaceWorldSavedData(STORED_WORLD_DATA_ID);
				}
			}
		}else if(savedDataAccessor == null){
			return null;
		}
		return new WrapperNBT(savedDataAccessor.internalData);
	}
	
	/**
	 *  Saves the passed-in data as the world's additional saved data.
	 *  Do NOT call this on clients.
	 */
	public void setData(WrapperNBT data){
		savedDataAccessor.internalData = data.tag;
		savedDataAccessor.markDirty();
		world.getPerWorldStorage().setData(savedDataAccessor.mapName, savedDataAccessor);
	}
	
	/**
	 *  Returns the entity that has the passed-in ID.
	 *  If the entity is a player, an instance of {@link WrapperPlayer}
	 *  is returned instead.
	 */
	public WrapperEntity getEntity(int id){
		Entity entity = world.getEntityByID(id);
		return WrapperEntity.getWrapperFor(entity);
	}
	
	/**
	 *  Returns a list of entities within the specified bounds.
	 */
	public List<WrapperEntity> getEntitiesWithin(BoundingBox box){
		List<WrapperEntity> entities = new ArrayList<WrapperEntity>();
		for(Entity entity : world.getEntitiesWithinAABB(Entity.class, box.convert())){
			entities.add(WrapperEntity.getWrapperFor(entity));
		}
		return entities;
	}
	
	/**
	 *  Returns the nearest hostile entity that can be seen by the passed-in entity.
	 */
	public WrapperEntity getNearestHostile(WrapperEntity entityLooking, int searchRadius){
		double smallestDistance = searchRadius*2;
		Entity foundEntity = null;
		Entity mcLooker = entityLooking.entity;
		Vec3d mcLookerPos = mcLooker.getPositionVector();
		for(Entity entity : world.getEntitiesWithinAABBExcludingEntity(mcLooker, mcLooker.getEntityBoundingBox().grow(searchRadius))){
			float distance = mcLooker.getDistance(entity);
			if(distance < smallestDistance && entity instanceof IMob && !entity.isDead && (!(entity instanceof EntityLivingBase) || ((EntityLivingBase) entity).deathTime == 0)){
				//This could be a valid entity, but might not be.  Do raytracing to make sure we can see them.
				if(world.rayTraceBlocks(mcLookerPos, entity.getPositionVector().add(0, entity.getEyeHeight(), 0), false, true, false) == null){
					foundEntity = entity;
				}
			}
		}
		return WrapperEntity.getWrapperFor(foundEntity);
	}
	
	/**
	 *  Returns the closest entity whose collision boxes are intercepted by the
	 *  passed-in entity's line of sight.
	 */
	public WrapperEntity getEntityLookingAt(WrapperEntity entityLooking, float searchRadius){
		double smallestDistance = searchRadius*2;
		Entity foundEntity = null;
		Entity mcLooker = entityLooking.entity;
		Vec3d mcLookerPos = mcLooker.getPositionVector();
		Point3d lookerLos = entityLooking.getLineOfSight(searchRadius).add(entityLooking.getPosition());
		Vec3d losVector = new Vec3d(lookerLos.x, lookerLos.y, lookerLos.z);
		for(Entity entity : world.getEntitiesWithinAABBExcludingEntity(mcLooker, mcLooker.getEntityBoundingBox().grow(searchRadius))){
			if(!entity.equals(mcLooker.getRidingEntity())){
				float distance = mcLooker.getDistance(entity);
				if(distance < smallestDistance){
					smallestDistance = distance;
					RayTraceResult rayTrace = entity.getEntityBoundingBox().calculateIntercept(mcLookerPos, losVector);
					if(rayTrace != null){
						foundEntity = entity;
					}
				}
			}
		}
		return WrapperEntity.getWrapperFor(foundEntity);
	}
	
	/**
	 *  Spawns the entity into the world.
	 */
	public void spawnEntity(AEntityB_Existing entity){
		BuilderEntity builder = new BuilderEntity(entity.world.world);
		builder.setPositionAndRotation(entity.position.x, entity.position.y, entity.position.z, (float) -entity.angles.y, (float) entity.angles.x);
		builder.entity = entity;
		world.spawnEntity(builder);
    }
	
	/**
	 *  Attacks all entities that are in the passed-in damage range.  If the
	 *  passed-in entity is not null, then any entity riding the passed-in
	 *  entity that are inside the bounding box will not be attacked, nor will
	 *  the passed-in entity be attacked.  Useful for vehicles, where you don't 
	 *  want players firing weapons to hit themselves or the vehicle.
	 *  Note that if this is called on clients, then this method will not attack
	 *  any entities. Instead, it will return a map of all entities that could have
	 *  been attacked with the bounding boxes attacked if they are of type 
	 *  {@link BuilderEntity} (returned in wrapper form) as the value and the key being the boxes hit.
	 *  This is because attacking cannot be done on clients, but it may be useful to 
	 *  know what entities could have been attacked should the call have been made on a server.
	 *  Note that the passed-in motion is used to move the Damage BoundingBox a set distance to
	 *  prevent excess collision checking, and may be null if no motion is applied.
	 */
	public Map<WrapperEntity, Collection<BoundingBox>> attackEntities(Damage damage, Point3d motion){
		AxisAlignedBB mcBox = damage.box.convert();
		List<Entity> collidedEntities;
		
		//Get collided entities.
		if(motion != null){
			mcBox = mcBox.expand(motion.x, motion.y, motion.z);
			collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);
		}else{
			collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);
		}
		
		//Get variables.  If we aren't moving, we won't need these.
		Point3d startPoint = null;
		Point3d endPoint = null;
		Vec3d start = null;
		Vec3d end = null;
		Map<WrapperEntity, Collection<BoundingBox>> rayTraceHits = null;
		if(motion != null){
			startPoint = damage.box.globalCenter;
			endPoint = damage.box.globalCenter.copy().add(motion);
			start = new Vec3d(startPoint.x, startPoint.y, startPoint.z);
			end = new Vec3d(endPoint.x, endPoint.y, endPoint.z);
			rayTraceHits = new HashMap<WrapperEntity, Collection<BoundingBox>>();
		}
		
		//Validate the collided entities to make sure we didn't hit something we shouldn't have.
		//Also get rayTrace hits for advanced checking.
		Iterator<Entity> iterator = collidedEntities.iterator();
		while(iterator.hasNext()){
			Entity mcEntityCollided = iterator.next();
			if(mcEntityCollided instanceof BuilderEntity){
				AEntityB_Existing entityAttacked = ((BuilderEntity) mcEntityCollided).entity;
				if(damage.damgeSource != null){
					if(damage.damgeSource.equals(entityAttacked)){
						//Don't attack ourselves.
						iterator.remove();
						continue;
					}else if(entityAttacked instanceof AEntityE_Multipart){
						if(((AEntityE_Multipart<?>) entityAttacked).parts.contains(damage.damgeSource)){
							//Don't attack the entity we are a part on.
							iterator.remove();
							continue;
						}
					}
				}
				
				//Get hitboxes hit if we are a moving source of damage.
				if(motion != null){
					TreeMap<Double, BoundingBox> hitBoxes = new TreeMap<Double, BoundingBox>();
					if(entityAttacked instanceof AEntityE_Multipart){
						for(BoundingBox box : ((AEntityE_Multipart<?>) entityAttacked).allInteractionBoxes){
							Point3d delta = box.getIntersectionPoint(startPoint, endPoint); 
							if(delta != null){
								hitBoxes.put(delta.distanceTo(startPoint), box);
							}
						}
					}else if(entityAttacked instanceof AEntityD_Interactable){
						for(BoundingBox box : ((AEntityD_Interactable<?>) entityAttacked).interactionBoxes){
							Point3d delta = box.getIntersectionPoint(startPoint, endPoint); 
							if(delta != null){
								hitBoxes.put(delta.distanceTo(startPoint), box);
							}
						}
					}
					
					//If we hit any box on this entity, add it to the map.
					//If not, remove it as we didn't hit it.
					if(hitBoxes.isEmpty()){
						iterator.remove();
					}else{
						rayTraceHits.put(WrapperEntity.getWrapperFor(mcEntityCollided), hitBoxes.values());
					}
				}
			}else{
				if(damage.damgeSource != null){
					Entity ridingEntity = mcEntityCollided.getRidingEntity();
					if(ridingEntity instanceof BuilderEntity){
						AEntityB_Existing internalEntity = ((BuilderEntity) ridingEntity).entity;
						if(damage.damgeSource.equals(internalEntity)){
							//Don't attack riders of the source of the damage.
							iterator.remove();
							continue;
						}else if(damage.damgeSource instanceof APart){
							APart damagingPart = (APart) damage.damgeSource;
							if(damagingPart.entityOn.equals(internalEntity)){
								//Don't attack riders of the multipart the part applying damage is a part of.
								iterator.remove();
								continue;
							}
						}
					}
				}
				
				//Didn't hit a builder. Do normal raytracing.
				//If we didn't hit anything, remove the entity from the list. 
				if(motion != null){
					if(mcEntityCollided.getEntityBoundingBox().calculateIntercept(start, end) == null){
						iterator.remove();
					}else{
						rayTraceHits.put(WrapperEntity.getWrapperFor(mcEntityCollided), null);
					}
				}
			}
		}
		
		//If we are on the server, attack the entities.
		//If we are on a client, we won't have attacked any entities, but we need to return what we found.
		if(isClient()){
			return rayTraceHits;
		}else{
			for(Entity entity : collidedEntities){
				WrapperEntity.getWrapperFor(entity).attack(damage);
			}
			return null;
		}
	}
	
	/**
	 *  Loads all entities that are in the passed-in range into the passed-in entity.
	 *  Only non-hostile mobs will be loaded.
	 */
	public void loadEntities(BoundingBox box, AEntityD_Interactable<?> entityToLoad){
		for(Entity entity : world.getEntitiesWithinAABB(Entity.class, box.convert())){
			if((entity instanceof INpc || entity instanceof EntityCreature) && !(entity instanceof IMob)){
				for(Point3d ridableLocation : entityToLoad.ridableLocations){
					if(!entityToLoad.locationRiderMap.containsKey(ridableLocation)){
						if(entityToLoad instanceof EntityVehicleF_Physics){
							if(((EntityVehicleF_Physics) entityToLoad).getPartAtLocation(ridableLocation).placementDefinition.isController){
								continue;
							}
						}
						entityToLoad.addRider(new WrapperEntity(entity), ridableLocation);
						break;
					}
				}
			}
		}
	}
	
	/**
	 *  Returns the block at the passed-in position, or null if it doesn't exist in the world.
	 *  Only valid for blocks of type {@link ABlockBase} others will return null.
	 */
	public ABlockBase getBlock(Point3d position){
		Block block = world.getBlockState(new BlockPos(position.x, position.y, position.z)).getBlock();
		return block instanceof BuilderBlock ? ((BuilderBlock) block).mcBlock : null;
	}
	
	/**
	 *  Returns the hardness of the block at the passed-in point.
	 */
	public float getBlockHardness(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		return world.getBlockState(pos).getBlockHardness(world, pos);
	}
	
	/**
	 *  Returns the slipperiness of the block at the passed-in position.
	 *  0.6 is default slipperiness for blocks.
	 */
	public float getBlockSlipperiness(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		return state.getBlock().getSlipperiness(state, world, pos, null);
	}
	
	/**
	 *  Returns the position where the first block along the path can be hit, or null if there are
	 *  no blocks along the path.
	 */
	public Point3d getBlockHit(Point3d position, Point3d delta){
		Vec3d start = new Vec3d(position.x, position.y, position.z);
		RayTraceResult trace = world.rayTraceBlocks(start, start.add(delta.x, delta.y, delta.z), false, true, false);
		if(trace != null){
			BlockPos pos = trace.getBlockPos();
			if(pos != null){
				 return new Point3d(pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return null;
	}
	
	/**
	 *  Returns the rotation (in degrees) of the block at the passed-in position.
	 *  Only valid for blocks of type {@link ABlockBase}.
	 */
    public float getBlockRotation(Point3d position){
    	return world.getBlockState(new BlockPos(position.x, position.y, position.z)).getValue(BuilderBlock.FACING).getHorizontalAngle();
    }
	
    /**
	 *  Returns true if the block at the passed-in position is solid at the passed-in axis.
	 *  Solid means that said block can be collided with, is a cube, and is generally able to have
	 *  things placed or connected to it.
	 */
	public boolean isBlockSolid(Point3d position, Axis axis){
		if(axis.blockBased){
			BlockPos pos = new BlockPos(position.x, position.y, position.z);
			IBlockState state = world.getBlockState(pos);
			Block offsetMCBlock = state.getBlock();
			EnumFacing facing = EnumFacing.valueOf(axis.name());
	        return offsetMCBlock != null ? !offsetMCBlock.equals(Blocks.BARRIER) && state.isSideSolid(world, pos, facing) : false;
		}else{
			return false;
		}
	}
	
	/**
	 *  Returns true if the block is liquid.
	 */
	public boolean isBlockLiquid(Point3d position){
        return world.getBlockState(new BlockPos(position.x, position.y, position.z)).getMaterial().isLiquid();
	}
	
	/**
	 *  Returns true if the block at the passed-in position is a slab, but only the
	 *  bottom portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockBottomSlab(Point3d position){
		IBlockState state = world.getBlockState(new BlockPos(position.x, position.y, position.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
	}
	
	/**
	 *  Returns true if the block at the passed-in position is a slab, but only the
	 *  top portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockTopSlab(Point3d position){
		IBlockState state = world.getBlockState(new BlockPos(position.x, position.y, position.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
	}
	
	/**
	 *  Returns the distance from the passed-in position to the top block in the world, at the position's X/Z coords.
	 */
	public double getHeight(Point3d position){
		return position.y - world.getHeight((int) position.x, (int) position.z);
	}
	
	/**
	 * Updates the blocks and depths of collisions for the passed-in BoundingBox to the box's internal variables.
	 * This is done as it allows for re-use of the variables by the calling object to avoid excess object creation.
	 * Note that if the offset value passed-in for an axis is 0, then no collision checks will be performed on that axis.
	 * This prevents excess calculations when trying to do movement calculations for a single axis.  If ignoreIfGreater
	 * is set, then the system will not set the collisionDepth of corresponding axis if the motion is less than the
	 * collisionMotion axis.  If this value is not set, the function simply looks for a non-zero value to make the
	 * collisionDepth be set for that axis.
	 */
	public void updateBoundingBoxCollisions(BoundingBox box, Point3d collisionMotion, boolean ignoreIfGreater){
		AxisAlignedBB mcBox = box.convert();
		box.collidingBlockPositions.clear();
		List<AxisAlignedBB> collidingAABBs = new ArrayList<AxisAlignedBB>(); 
		for(int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i){
    		for(int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j){
    			for(int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				if(world.isBlockLoaded(pos)){
	    				IBlockState state = world.getBlockState(pos);
	    				if(state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null){
	    					int oldCollidingBlockCount = collidingAABBs.size();
	    					state.addCollisionBoxToList(world, pos, mcBox, collidingAABBs, null, false);
	    					if(collidingAABBs.size() > oldCollidingBlockCount){
	    						box.collidingBlockPositions.add(new Point3d(i, j, k));
	    					}
	    				}
						if(box.collidesWithLiquids && state.getMaterial().isLiquid()){
							collidingAABBs.add(state.getBoundingBox(world, pos).offset(pos));
							box.collidingBlockPositions.add(new Point3d(i, j, k));
						}
    				}
    			}
    		}
    	}
		
		//If we are in the depth bounds for this collision, set it as the collision depth.
		box.currentCollisionDepth.set(0D, 0D, 0D);
		double boxCollisionDepth;
		for(AxisAlignedBB colBox : collidingAABBs){
			if(collisionMotion.x > 0){
				boxCollisionDepth = mcBox.maxX - colBox.minX;
				if(!ignoreIfGreater || collisionMotion.x - boxCollisionDepth > 0){
					box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
				}
			}else if(collisionMotion.x < 0){
				boxCollisionDepth = colBox.maxX - mcBox.minX;
				if(!ignoreIfGreater || collisionMotion.x + boxCollisionDepth < 0){
					box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
				}
			}
			if(collisionMotion.y > 0){
				boxCollisionDepth = mcBox.maxY - colBox.minY;
				if(!ignoreIfGreater || collisionMotion.y - boxCollisionDepth > 0){
					box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
				}
			}else if(collisionMotion.y < 0){
				boxCollisionDepth = colBox.maxY - mcBox.minY;
				if(!ignoreIfGreater || collisionMotion.y + boxCollisionDepth < 0){
					box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
				}
			}
			if(collisionMotion.z > 0){
				boxCollisionDepth = colBox.maxZ - mcBox.minZ;
				if(!ignoreIfGreater || collisionMotion.z - boxCollisionDepth > 0){
					box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
				}
			}else if(collisionMotion.z < 0){
				boxCollisionDepth = colBox.maxZ - mcBox.minZ;
				if(!ignoreIfGreater || collisionMotion.z + boxCollisionDepth < 0){
					box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
				}
			}
		}
	}
	
	/**
	 *  Returns the current redstone power at the passed-in position.
	 */
	public int getRedstonePower(Point3d position){
		return world.getRedstonePowerFromNeighbors(new BlockPos(position.x, position.y, position.z));
	}

	/**
	 *  Returns the rain strength at the passed-in position.
	 *  0 is no rain, 1 is rain, and 2 is a thunderstorm.
	 *  Note that this method offsets the point by 1, as it allows
	 *  for blocks to query rain strength and not get 0 due to no rain
	 *  being possible "in" that block.
	 */
	public float getRainStrength(Point3d position){
		return world.isRainingAt(new BlockPos(position.x, position.y + 1, position.z)) ? world.getRainStrength(1.0F) + world.getThunderStrength(1.0F) : 0.0F;
	}
	
	/**
	 *  Returns the current temperature at the passed-in position.
	 *  Dependent on biome, and likely modified by mods that add new boimes.
	 */
	public float getTemperature(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		return world.getBiome(pos).getTemperature(pos);
	}

	 /**
	 *  Places the passed-in block at the point specified.
	 *  Returns true if the block was placed, false if not.
	 *  If this block isn't placed by a player, pass in null
	 *  for the player reference.
	 */
	@SuppressWarnings("unchecked")
	public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONMultiModelProvider> boolean setBlock(ABlockBase block, Point3d position, WrapperPlayer playerWrapper, Axis axis){
    	if(!world.isRemote){
    		BuilderBlock wrapper = BuilderBlock.blockMap.get(block);
    		BlockPos pos = new BlockPos(position.x, position.y, position.z);
    		if(playerWrapper != null){
    			WrapperPlayer player = playerWrapper;
    	    	ItemStack stack = playerWrapper.getHeldStack();
    	    	AItemBase item = player.getHeldItem();
    	    	EnumFacing facing = EnumFacing.valueOf(axis.name());
    	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
    	            pos = pos.offset(facing);
    	            position.add(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
    	    	}
    	    	
	            if(item != null && player.player.canPlayerEdit(pos, facing, stack) && world.mayPlace(wrapper, pos, false, facing, null)){
	            	IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, player.player, EnumHand.MAIN_HAND);
	            	if(world.setBlockState(pos, newState, 11)){
		            	//Block is set.  See if we need to set TE data.
		            	if(block instanceof IBlockTileEntity){
		            		BuilderTileEntity<TileEntityType> builderTile = (BuilderTileEntity<TileEntityType>) world.getTileEntity(pos);
		            		WrapperNBT data;
		            		if(stack.hasTagCompound()){
		            			data = new WrapperNBT(stack);
		            		}else{
		            			data = new WrapperNBT();
		            			if(item instanceof AItemPack){
			            			data.setString("packID", ((AItemPack<?>) item).definition.packID);
				            		data.setString("systemName", ((AItemPack<?>) item).definition.systemName);
				            		if(item instanceof AItemSubTyped){
				            			data.setString("subName", ((AItemSubTyped<?>) item).subName);
				            		}
		            			}
		            		}
		            		data.setDouble("rotation", Math.round(player.getHeadYaw()/15)*15%360);
		            		builderTile.tileEntity = ((IBlockTileEntity<TileEntityType>) block).createTileEntity(this, position, data);
		            		
		            	}
		            	//Send place event to block class, and also send initial update check.
		            	block.onPlaced(this, position, player);
		                stack.shrink(1);
		                return true;
		            }
	            }
    		}else{
    			IBlockState newState = wrapper.getDefaultState();
    			if(world.setBlockState(pos, newState, 11)){
    				if(block instanceof IBlockTileEntity){
    					BuilderTileEntity<TileEntityType> builderTile = (BuilderTileEntity<TileEntityType>) world.getTileEntity(pos);
    					builderTile.tileEntity = ((IBlockTileEntity<TileEntityType>) block).createTileEntity(this, position, new WrapperNBT());
    				}
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
	 /**
	 *  Gets the wrapper TE at the specified position.
	 */
	public WrapperTileEntity getWrapperTileEntity(Point3d position){
		TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z));
		return tile != null ? new WrapperTileEntity(tile) : null;
	}
	
	/**
	 *  Returns the tile entity at the passed-in position, or null if it doesn't exist in the world.
	 *  Only valid for TEs of type {@link ATileEntityBase} others will return null.
	 */
	@SuppressWarnings("unchecked")
	public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3d position){
		TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z));
		return tile instanceof BuilderTileEntity ? ((BuilderTileEntity<TileEntityType>) tile).tileEntity : null;
	}
	
	/**
	 *  Flags the tile entity at the passed-in position for saving.  This means the TE's
	 *  NBT data will be saved to disk when the chunk unloads so it will maintain its state.
	 */
	public void markTileEntityChanged(Point3d position){
		world.getTileEntity(new BlockPos(position.x, position.y, position.z)).markDirty();
	}
	
	/**
	 *  Gets the brightness at this position, as a value between 0.0-1.0. Calculated from the
	 *  sun brightness, and possibly the block brightness if calculateBlock is true.
	 */
	public float getLightBrightness(Point3d position, boolean calculateBlock){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		float sunLight = world.getSunBrightness(0)*(world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted())/15F;
		float blockLight = calculateBlock ? world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos)/15F : 0.0F;
		return Math.max(sunLight, blockLight);
	}
	
	/**
	 *  Updates the brightness of the block at this position.  Only works if the block
	 *  is a dynamic-brightness block that implements {@link IBlockTileEntity}. 
	 */
	public void updateLightBrightness(Point3d position){
		ATileEntityBase<?> tile = getTileEntity(position);
		if(tile != null){
			BlockPos pos = new BlockPos(position.x, position.y, position.z);
			//This needs to get fired manually as even if we update the blockstate the light value won't change
			//as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
			world.checkLight(pos);
		}
	}
	
	/**
	 *  Destroys the block at the position, dropping it as whatever drop it drops as.
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void destroyBlock(Point3d position){
		world.destroyBlock(new BlockPos(position.x, position.y, position.z), true);
	}
	
	/**
	 *  Returns true if the block at this position is air.
	 */
	public boolean isAir(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos); 
		Block block = state.getBlock();
		return block.isAir(state, world, pos);
	}
	
	/**
	 *  Returns true if the block at this position is fire.
	 *  Note: this will return true on vanilla fire, as well as
	 *  any other blocks made of fire from other mods.
	 */
	public boolean isFire(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos); 
		return state.getMaterial().equals(Material.FIRE);
	}
	
	/**
	 *  Sets the block at the passed-in position to fire. 
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void setToFire(Point3d position){
		world.setBlockState(new BlockPos(position.x, position.y, position.z), Blocks.FIRE.getDefaultState());
	}
	
	/**
	 *  Tries to fertilize the block at the passed-in position with the passed-in stack.
	 *  Returns true if the block was fertilized.
	 */
	public boolean fertilizeBlock(Point3d position, ItemStack stack){
		//Check if the item can fertilize things and we are on the server.
		if(stack.getItem().equals(Items.DYE) && !world.isRemote){
			//Check if we are in crops.
			BlockPos cropPos = new BlockPos(position.x, position.y, position.z);
			IBlockState cropState = world.getBlockState(cropPos);
			Block cropBlock = cropState.getBlock();
			if(cropBlock instanceof IGrowable){
	            IGrowable growable = (IGrowable)cropState.getBlock();
	            if(growable.canGrow(world, cropPos, cropState, world.isRemote)){
	            	ItemDye.applyBonemeal(stack.copy(), world, cropPos);
					return true;
	            }
			}
		}
		return false;
	}
	
	/**
	 *  Tries to harvest the block at the passed-in position.  If the harvest was
	 *  successful, and the block harvested was crops, the result returned is a list
	 *  of the drops from the crops.  If the crops couldn't be harvested, null is returned.
	 *  If the block was harvested, but not crops, then the resulting drops
	 *  are dropped on the ground and an empty list is returned.
	 */
	public List<ItemStack> harvestBlock(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		if((state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state)) || state.getBlock() instanceof BlockBush){
			Block harvestedBlock = state.getBlock();
			NonNullList<ItemStack> drops = NonNullList.create();
			List<ItemStack> cropDrops = new ArrayList<ItemStack>();
			world.playSound(pos.getX(), pos.getY(), pos.getZ(), harvestedBlock.getSoundType(state, world, pos, null).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);
			
			//Only return drops on servers.  Clients don't do items.
			if(!world.isRemote){
				harvestedBlock.getDrops(drops, world, pos, state, 0);
				world.setBlockToAir(pos);
				if(harvestedBlock instanceof BlockCrops){
					for(ItemStack drop : drops){
						cropDrops.add(drop);
					}
				}else{
					for(ItemStack stack : drops){
						if(stack.getCount() > 0){
							world.spawnEntity(new EntityItem(world, position.x, position.y, position.z, stack));
						}
					}
				}
			}
			return cropDrops;
		}
		return null;
	}
	
	/**
	 *  Tries to plant the item as a block at the passed-in position.  Only works if the land conditions are correct
	 *  and the item is actually seeds that can be planted.
	 */
	public boolean plantBlock(Point3d position, ItemStack stack){
		//Check for valid seeds.
		Item item = stack.getItem();
		if(item instanceof IPlantable){
			IPlantable plantable = (IPlantable) item;
			
			//Check if we have farmland below and air above.
			BlockPos farmlandPos = new BlockPos(position.x, position.y, position.z);
			IBlockState farmlandState = world.getBlockState(farmlandPos);
			Block farmlandBlock = farmlandState.getBlock();
			if(farmlandBlock.equals(Blocks.FARMLAND)){
				BlockPos cropPos = farmlandPos.up();
				if(world.isAirBlock(cropPos)){
					//Check to make sure the block can sustain the plant we want to plant.
					IBlockState plantState = plantable.getPlant(world, cropPos);
					if(farmlandBlock.canSustainPlant(plantState, world, farmlandPos, EnumFacing.UP, plantable)){
						world.setBlockState(cropPos, plantState, 11);
						world.playSound(farmlandPos.getX(), farmlandPos.getY(), farmlandPos.getZ(), plantState.getBlock().getSoundType(plantState, world, farmlandPos, null).getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 *  Tries to plow the block at the passed-in position.  Essentially, this turns grass and dirt into farmland.
	 */
	public boolean plowBlock(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState oldState = world.getBlockState(pos);
		IBlockState newState = null;
		Block block = oldState.getBlock();
		if(block.equals(Blocks.GRASS) || block.equals(Blocks.GRASS_PATH)){
			newState = Blocks.FARMLAND.getDefaultState();
		 }else if(block.equals(Blocks.DIRT)){
			 switch(oldState.getValue(BlockDirt.VARIANT)){
			 	case DIRT: newState = Blocks.FARMLAND.getDefaultState(); break;
			 	case COARSE_DIRT: newState = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT); break;
			 	default: return false;
             }
		}else{
			return false;
		}
		
		world.setBlockState(pos, newState, 11);
		world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		return true;
	}
	
	/**
	 *  Spawns the passed-in item as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItem(AItemBase item, WrapperNBT data, Point3d point){
		ItemStack stack = item.getNewStack();
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		//Spawn 1 block above in case we're right on a block.
		world.spawnEntity(new EntityItem(world, point.x, point.y + 1, point.z, stack));
	}
	
	/**
	 *  Spawns the passed-in stack as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItemStack(ItemStack stack, Point3d point){
		world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, stack));
	}
	
	/**
	 *  Spawns an explosion of the specified strength at the passed-in point.
	 */
	public void spawnExplosion(Point3d location, double strength, boolean flames){
		world.newExplosion(null, location.x, location.y, location.z, (float) strength, flames, ConfigSystem.configObject.general.blockBreakage.value);
	}
	
	/**
	 *  Class used to interface with world saved data methods.
	 */
	public static class InterfaceWorldSavedData extends WorldSavedData{
		private NBTTagCompound internalData = new NBTTagCompound(); 
		
		public InterfaceWorldSavedData(String name){
			super(name);
		}

		@Override
		public void readFromNBT(NBTTagCompound tag){
			internalData = tag.getCompoundTag("internalData");
		}

		@Override
		public NBTTagCompound writeToNBT(NBTTagCompound tag){
			tag.setTag("internalData", internalData);
			return tag;
		}
	}
}