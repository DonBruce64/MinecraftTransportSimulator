package mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.IGrowable;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
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
import net.minecraftforge.common.IPlantable;

/**Wrapper for the world class.  This wrapper contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  This class
 * should be used whenever possible to replace the normal world object reference
 * with methods that re-direct to this wrapper.  This wrapper can only be created
 * internally, and is obtained via a static method to ensure world wrappers aren't
 * created constantly.  This method requires an instance of an {@link World} object 
 * passed-in to the constructor, so this means you'll need something to get an 
 * instance of the MC world beforehand.
 * Note that other wrappers may access the world variable directly for things
 * that are specific to their classes (such as blocks getting states).
 *
 * @author don_bruce
 */
public class WrapperWorld{
	private static final Map<World, WrapperWorld> worldWrappers = new HashMap<World, WrapperWorld>();
	private final Map<Entity, WrapperEntity> entityWrappers = new HashMap<Entity, WrapperEntity>();
	private final Map<EntityPlayer, WrapperPlayer> playerWrappers = new HashMap<EntityPlayer, WrapperPlayer>();
	private static final WrapperEntity NULL_ENTITY_WRAPPER = new WrapperEntity(null);
	private static final WrapperPlayer NULL_PLAYER_WRAPPER = new WrapperPlayer(null);
	
	final World world;

	private WrapperWorld(World world){
		this.world = world;
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in world instance.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 */
	public static WrapperWorld getWrapperFor(World world){
		if(world != null){
			if(!worldWrappers.containsKey(world)){
				worldWrappers.put(world, new WrapperWorld(world));
			}
			return worldWrappers.get(world);
		}else{
			return null;
		}
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in entity instance.
	 *  Null may be passed-in to obtain a null wrapper, should this be desired.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 */
	public WrapperEntity getWrapperFor(Entity entity){
		if(entity != null){
			if(!entityWrappers.containsKey(entity)){
				entityWrappers.put(entity, new WrapperEntity(entity));
			}
			WrapperEntity wrapper = entityWrappers.get(entity);
			if(!wrapper.isValid()){
				wrapper = new WrapperEntity(entity);
				entityWrappers.put(entity, wrapper);
			}
			return wrapper;
		}else{
			return NULL_ENTITY_WRAPPER;
		}
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in player instance.
	 *  Null may be passed-in to obtain a null wrapper, should this be desired.
	 *  Note that the wrapped player class MAY be side-specific, so avoid casting
	 *  the wrapped entity directly if you aren't sure what its class is.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 */
	public WrapperPlayer getWrapperFor(EntityPlayer player){
		if(player != null){
			if(!playerWrappers.containsKey(player)){
				playerWrappers.put(player, new WrapperPlayer(player));
			}
			WrapperPlayer wrapper = playerWrappers.get(player);
			if(!wrapper.isValid()){
				wrapper = new WrapperPlayer(player);
				playerWrappers.put(player, wrapper);
			}
			return playerWrappers.get(player);
		}else{
			return NULL_PLAYER_WRAPPER;
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
	 *  Returns the current world time, in ticks.  Useful when you need to sync
	 *  operations.  For animations, just use the system time.
	 */
	public long getTime(){
		return world.getTotalWorldTime();
	}
		
	/**
	 *  Returns the max build height for the world.  Note that entities may move and be saved
	 *  above this height, and moving above this height will result in rendering oddities.
	 */
	public long getMaxHeight(){
		return world.getHeight();
	}
	
	/**
	 *  Returns the entity that has the passed-in ID.
	 *  If the entity is a player, an instance of {@link WrapperPlayer}
	 *  is returned instead.
	 */
	public WrapperEntity getEntity(int id){
		Entity entity = world.getEntityByID(id);
		return entity instanceof EntityPlayer ? getWrapperFor((EntityPlayer) entity) : getWrapperFor(entity);
	}
	
	/**
	 *  Spawns the passed-in entity into the world.
	 *  Position and rotation is set initially to match the entity.
	 */
	public void spawnEntity(AEntityBase entity){
    	BuilderEntity builder = new BuilderEntity(world);
    	builder.setPositionAndRotation(entity.position.x, entity.position.y, entity.position.z, (float) -entity.angles.y, (float) entity.angles.x);
    	builder.entity = entity;
    	BuilderEntity.entitiesToBuilders.put(entity, builder);
    	world.spawnEntity(builder);
    }
	
	/**
	 *  Attacks all entities that are in the passed-in damage range.  If the
	 *  passed-in entity is not null, then any entity riding the passed-in
	 *  entity that is inside the bounding box will not be attacked, nor will
	 *  the passed-in entity be attacked.  Useful for vehicles, where you don't 
	 *  want players firing weapons to hit themselves or the vehicle.
	 *  Note that if this is called on clients, then this method will not attack
	 *  any entities. Instead, it will return a map of all entities that could have
	 *  been attacked with the bounding box attacked if they are of type 
	 *  {@link BuilderEntity} as the value to the entity key.
	 *  This is because attacking cannot be done on clients, but it may be useful to 
	 *  know what entities could have been attacked should the call have been made on a server.
	 *  Note that the passed-in motion is used to move the Damage BoundingBox a set distance to
	 *  prevent excess collision checking, and may be null if no motion is applied.
	 */
	public Map<WrapperEntity, BoundingBox> attackEntities(Damage damage, AEntityBase damageSource, Point3d motion){
		AxisAlignedBB mcBox = new AxisAlignedBB(
				damage.box.globalCenter.x - damage.box.widthRadius,
				damage.box.globalCenter.y - damage.box.heightRadius,
				damage.box.globalCenter.z - damage.box.depthRadius,
				damage.box.globalCenter.x + damage.box.widthRadius,
				damage.box.globalCenter.y + damage.box.heightRadius,
				damage.box.globalCenter.z + damage.box.depthRadius
			);
		
		List<Entity> collidedEntities;
		List<Point3d> rayTraceHits = new ArrayList<Point3d>();;
		if(motion != null){
			mcBox = mcBox.expand(motion.x, motion.y, motion.z);
			collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);
			//Iterate over all entities.  If the entity doesn't intersect the damage path, remove it.
			Vec3d start = new Vec3d(damage.box.globalCenter.x, damage.box.globalCenter.y, damage.box.globalCenter.z);
			Vec3d end = start.addVector(motion.x, motion.y, motion.z);
			Iterator<Entity> iterator = collidedEntities.iterator();
			while(iterator.hasNext()){
				Entity entity = iterator.next();
				RayTraceResult rayTrace = entity.getEntityBoundingBox().calculateIntercept(start, end); 
				if(rayTrace == null){
					iterator.remove();
				}else{
					Point3d hitPoint = new Point3d(rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z);
					rayTraceHits.add(hitPoint);
				}
			}
		}else{
			collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);
			rayTraceHits = null;
		}
		
		//Found collided entities.  Do checks to remove excess entities and attack them if required.
		if(!collidedEntities.isEmpty()){
			if(damageSource != null){
				//Iterate over all entities.  If the entity is the passed-in source, or riding the source, remove it.
				Iterator<Entity> iterator = collidedEntities.iterator();
				while(iterator.hasNext()){
					Entity entity = iterator.next();
					if(entity instanceof BuilderEntity){
						AEntityBase testSource = ((BuilderEntity) entity).entity;
						if(damageSource.equals(testSource)){
							iterator.remove();
						}
					}else if(entity.getRidingEntity() instanceof BuilderEntity){
						AEntityBase testSource = ((BuilderEntity) entity.getRidingEntity()).entity;
						if(damageSource.equals(testSource)){
							iterator.remove();
						}
					}
				}
			}
			
			//If we are on the server, attack the entities.
			if(!isClient()){
				for(Entity entity : collidedEntities){
					WrapperEntity.attack(entity, damage);
				}
			}
		}
		
		//If we are on a client, we won't have attacked any entities, but we need to return what we found.
		if(isClient()){
			Map<WrapperEntity, BoundingBox> entities = new HashMap<WrapperEntity, BoundingBox>();
			for(Entity entity : collidedEntities){
				if(entity instanceof BuilderEntity){
					//Need to check which box we hit for this entity.
					for(BoundingBox box : ((BuilderEntity) entity).entity.interactionBoxes){
						if(motion == null){
							if(box.intersects(damage.box)){
								entities.put(getWrapperFor(entity), box);
								break;
							}
						}else{
							if(box.isPointInside(rayTraceHits.get(collidedEntities.indexOf(entity)))){
								entities.put(getWrapperFor(entity), box);
								break;
							}
						}
					}
				}else{
					entities.put(getWrapperFor(entity), null);
				}
			}
			return entities;
		}else{
			return null;
		}
	}
	
	/**
	 *  Moves all entities that collide with the passed-in bounding box by the passed-in offset.
	 *  Offset is determined by the passed-in vector, and the passed-in angle of said vector.
	 *  This allows for angular movement as well as linear.
	 */
	public void moveEntities(List<BoundingBox> boxesToCheck, Point3d intialPosition, Point3d initalRotation, Point3d linearMovement, Point3d angularMovement){
		for(Entity entity : world.loadedEntityList){
			//Don't move riding entities or our own builders.
			if(!(entity instanceof BuilderEntity) && entity.getRidingEntity() == null){
				for(BoundingBox box : boxesToCheck){
					//Check if we collide with the entity.
					//Add a slight yOffset to every box to "grab" entities standing on collision points.
					AxisAlignedBB entityBox = entity.getEntityBoundingBox();
					if(
						entityBox.maxX > box.globalCenter.x - box.widthRadius - 0.25D && 
						entityBox.minX < box.globalCenter.x + box.widthRadius + 0.25D && 
						entityBox.maxY > box.globalCenter.y - box.heightRadius - 0.5D && 
						entityBox.minY < box.globalCenter.y + box.heightRadius + 0.5D &&
						entityBox.maxZ > box.globalCenter.z - box.depthRadius - 0.25D &&
						entityBox.minZ < box.globalCenter.z + box.depthRadius + 0.25D)
					{
						//Entity has collided with this box.  Adjust movement to allow them to ride on it.
						Point3d entityDeltaOffset = new Point3d(entity.posX - intialPosition.x, entity.posY - intialPosition.y, entity.posZ - intialPosition.z);
						Point3d finalOffset = entityDeltaOffset.copy().rotateFine(angularMovement).subtract(entityDeltaOffset);
						
						//If the entity is within 0.5 units of the top of the box, make them be on top of it.
						//This also keeps the entity from falling into the box due to MC's stupid collision code that doesn't
						//handle moving hitboxes well.
						double entityBottomDelta = box.globalCenter.y + box.heightRadius - entityBox.minY + finalOffset.y;
						if(entityBottomDelta >= -0.5 && entityBottomDelta <= 0.5 && (entity.motionY < 0 || entity.motionY < entityBottomDelta)){
							finalOffset.y = entityBottomDelta;
							if(entity.motionY < 0){
								entity.motionY = 0;
							}
						}
						
						//Set entity position.
						entity.setPosition(entity.posX + finalOffset.x, entity.posY + finalOffset.y, entity.posZ + finalOffset.z);
						break;
					}
				}
			}
		}
	}
	
	/**
	 *  Returns the block wrapper at the passed-in location, or null if the block is air.
	 */
	public WrapperBlock getWrapperBlock(Point3i point){
		return isAir(point) ? null : new WrapperBlock(world, new BlockPos(point.x, point.y, point.z));
	}
	
	/**
	 *  Returns the block at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for blocks of type {@link ABlockBase} others will return null.
	 */
	public ABlockBase getBlock(Point3i point){
		Block block = world.getBlockState(new BlockPos(point.x, point.y, point.z)).getBlock();
		return block instanceof BuilderBlock ? ((BuilderBlock) block).block : null;
	}
	
	/**
	 *  Returns true if the block at the passed-in location is solid.  Solid means
	 *  that said block can be collided with, is a cube, and is generally able to have
	 *  things placed or connected to it.
	 */
	public boolean isBlockSolid(Point3i point){
		IBlockState offsetMCState = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block offsetMCBlock = offsetMCState.getBlock();
        return offsetMCBlock != null ? !offsetMCBlock.equals(Blocks.BARRIER) && offsetMCState.getMaterial().isOpaque() && offsetMCState.isFullCube() && offsetMCState.getMaterial() != Material.GOURD : false;
	}
	
	/**
	 *  Returns true if the block is liquid.
	 */
	public boolean isBlockLiquid(Point3i point){
		IBlockState offsetMCState = world.getBlockState(new BlockPos(point.x, point.y, point.z));
        return offsetMCState.getMaterial().isLiquid();
	}
	
	/**
	 *  Returns true if the block at the passed-in location is a slab, but only the
	 *  bottom portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockBottomSlab(Point3i point){
		IBlockState state = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
	}
	
	/**
	 *  Returns true if the block at the passed-in location is a slab, but only the
	 *  top portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockTopSlab(Point3i point){
		IBlockState state = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
	}
	
	/**
	 * Updates the blocks and depths of collisions for the passed-in BoundingBox to the box's internal variables.
	 * This is done as it allows for re-use of the variables by the calling object to avoid excess object creation.
	 * Note that if the offset value passed-in for an axis is 0, then no collision checks will be performed on that axis.
	 * This prevents excess calculations when trying to do movement calculations for a single axis.  Note that the
	 * actual value of the motion does not matter for this function: only that a non-zero value be present for an axis.
	 */
	public void updateBoundingBoxCollisions(BoundingBox box, Point3d collisionMotion){
		AxisAlignedBB mcBox = new AxisAlignedBB(
			box.globalCenter.x - box.widthRadius,
			box.globalCenter.y - box.heightRadius,
			box.globalCenter.z - box.depthRadius,
			box.globalCenter.x + box.widthRadius,
			box.globalCenter.y + box.heightRadius,
			box.globalCenter.z + box.depthRadius
		);
		
		box.collidingBlocks.clear();
		List<AxisAlignedBB> collidingAABBs = new ArrayList<AxisAlignedBB>(); 
		for(int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i){
    		for(int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j){
    			for(int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				if(world.isBlockLoaded(pos)){
	    				IBlockState state = world.getBlockState(pos);
	    				if(state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null){
	    					state.addCollisionBoxToList(world, pos, mcBox, collidingAABBs, null, false);
	    					box.collidingBlocks.add(new WrapperBlock(world, pos));
	    				}
						if(box.collidesWithLiquids && state.getMaterial().isLiquid()){
							collidingAABBs.add(state.getBoundingBox(world, pos).offset(pos));
							box.collidingBlocks.add(new WrapperBlock(world, pos));
						}
    				}
    			}
    		}
    	}
		
		//If we are in the depth bounds for this collision, set it as the collision depth.
		box.currentCollisionDepth.set(0D, 0D, 0D);
		for(AxisAlignedBB colBox : collidingAABBs){
			if(collisionMotion.x > 0){
				box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, mcBox.maxX - colBox.minX);
			}else if(collisionMotion.x < 0){
				box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, colBox.maxX - mcBox.minX);
			}
			if(collisionMotion.y > 0){
				box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, mcBox.maxY - colBox.minY);
			}else if(collisionMotion.y < 0){
				box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, colBox.maxY - mcBox.minY);
			}
			if(collisionMotion.z > 0){
				box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, colBox.maxZ - mcBox.minZ);
			}else if(collisionMotion.z < 0){
				box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, colBox.maxZ - mcBox.minZ);
			}
		}
	}
	
	/**
	 *  Returns the current redstone power at the passed-in position.
	 */
	public int getRedstonePower(Point3i point){
		return world.getStrongPower(new BlockPos(point.x, point.y, point.z));
	}

	/**
	 *  Returns the rain strength at the passed-in position.
	 *  0 is no rain, 1 is rain, and 2 is a thunderstorm.
	 */
	public float getRainStrength(Point3i point){
		return world.isRainingAt(new BlockPos(point.x, point.y, point.z)) ? world.getRainStrength(1.0F) + world.getThunderStrength(1.0F) : 0.0F;
	}
	
	/**
	 *  Returns the current temperature at the passed-in position.
	 *  Dependent on biome, and likely modified by mods that add new boimes.
	 */
	public float getTemperature(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		return world.getBiome(pos).getTemperature(pos);
	}

    /**
	 *  Places the passed-in block at the point specified.
	 *  Returns true if the block was placed, false if not.
	 */
    @SuppressWarnings("unchecked")
	public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> boolean setBlock(ABlockBase block, Point3i location, WrapperPlayer player, Axis axis){
    	if(!world.isRemote){
	    	BuilderBlock wrapper = BuilderBlock.blockWrapperMap.get(block);
	    	ItemStack stack = player.getHeldStack();
	    	BlockPos pos = new BlockPos(location.x, location.y, location.z);
	    	EnumFacing facing = EnumFacing.valueOf(axis.name());
	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
	            pos = pos.offset(facing);
	            location.add(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
	        }
	    	if(!stack.isEmpty() && player.player.canPlayerEdit(pos, facing, stack) && world.mayPlace(wrapper, pos, false, facing, null)){
	            IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, player.player, EnumHand.MAIN_HAND);
	            if(world.setBlockState(pos, newState, 11)){
	            	//Block is set.  See if we need to set TE data.
	            	if(block instanceof IBlockTileEntity){
	            		BuilderTileEntity<TileEntityType> builderTile = (BuilderTileEntity<TileEntityType>) world.getTileEntity(pos);
	            		WrapperNBT data = null;
	            		if(stack.hasTagCompound()){
	            			data = new WrapperNBT(stack);
	            		}else if(stack.getItem() instanceof AItemPack){
	            			data = new WrapperNBT();
	            			data.setString("packID", ((AItemPack<JSONDefinition>) stack.getItem()).definition.packID);
		            		data.setString("systemName", ((AItemPack<JSONDefinition>) stack.getItem()).definition.systemName);
	            		}
	            		builderTile.tileEntity = ((IBlockTileEntity<TileEntityType>) block).createTileEntity(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), data);
	            	}
	            	//Send place event to block class, and also send initial update cheeck.
	            	block.onPlaced(this, location, player);
	                stack.shrink(1);
	            }
	            return true;
	        }
    	}
    	return false;
    }
    
    /**
	 *  Gets the wrapper TE at the specified position.
	 */
	public WrapperTileEntity getWrapperTileEntity(Point3i position){
		TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z));
		return tile != null ? new WrapperTileEntity(tile) : null;
	}
	
	/**
	 *  Returns the tile entity at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for TEs of type {@link ATileEntityBase} others will return null.
	 */
	public ATileEntityBase<?> getTileEntity(Point3i point){
		TileEntity tile = world.getTileEntity(new BlockPos(point.x, point.y, point.z));
		return tile instanceof BuilderTileEntity ? ((BuilderTileEntity<?>) tile).tileEntity : null;
	}
	
	/**
	 *  Flags the tile entity at the passed-in point for saving.  This means the TE's
	 *  NBT data will be saved to disk when the chunk unloads so it will maintain its state.
	 */
	public void markTileEntityChanged(Point3i point){
		world.getTileEntity(new BlockPos(point.x, point.y, point.z)).markDirty();
	}
	
	/**
	 *  Gets the brightness at this point, as a value between 0.0-1.0. Calculated from the
	 *  sun brightness, and possibly the block brightness if calculateBlock is true.
	 */
	public float getLightBrightness(Point3i point, boolean calculateBlock){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		float sunLight = world.getSunBrightness(0)*(world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted())/15F;
		float blockLight = calculateBlock ? world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos)/15F : 0.0F;
		return Math.max(sunLight, blockLight);
	}
	
	/**
	 *  Updates the brightness of the block at this point.  Only works if the block
	 *  is a dynamic-brightness block that implements {@link ITileEntityProvider}. 
	 */
	public void updateLightBrightness(Point3i point){
		ATileEntityBase<?> tile = getTileEntity(point);
		if(tile != null){
			BlockPos pos = new BlockPos(point.x, point.y, point.z);
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
	public void destroyBlock(Point3i point){
		world.destroyBlock(new BlockPos(point.x, point.y, point.z), true);
	}
	
	/**
	 *  Returns true if the block at this point is air.
	 */
	public boolean isAir(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		IBlockState state = world.getBlockState(pos); 
		Block block = state.getBlock();
		return block.isAir(state, world, pos);
	}
	
	/**
	 *  Returns true if the block at this point is fire.
	 *  Note: this will return true on vanilla fire, as well as
	 *  any other blocks made of fire from other mods.
	 */
	public boolean isFire(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		IBlockState state = world.getBlockState(pos); 
		return state.getMaterial().equals(Material.FIRE);
	}
	
	/**
	 *  Sets the block at the passed-in position to fire. 
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void setToFire(Point3i point){
		world.setBlockState(new BlockPos(point.x, point.y, point.z), Blocks.FIRE.getDefaultState());
	}
	
	/**
	 *  Tries to fertilize the block with the passed-in item.
	 *  Returns true if the block was fertilized.
	 */
	public boolean fertilizeBlock(Point3i point, ItemStack stack){
		//Check if the item can fertilize things and we are on the server.
		if(stack.getItem().equals(Items.DYE) && !world.isRemote){
			//Check if we are in crops.
			BlockPos cropPos = new BlockPos(point.x, point.y, point.z);
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
	 *  Tries to harvest the block at the passed-in location.  If the harvest was
	 *  successful, and the block harvested was crops, the result returned is a list
	 *  of the drops from the crops.  If the crops couldn't be harvested, null is returned.
	 *  If the block was harvested, but not crops, then the resulting drops
	 *  are dropped on the ground and an empty list is returned.
	 */
	public List<ItemStack> harvestBlock(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
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
					cropDrops.addAll(drops);
				}else{
					for(ItemStack stack : drops){
						if(stack.getCount() > 0){
							spawnItemStack(stack, null, new Point3d(point));
						}
					}
				}
			}
			return cropDrops;
		}
		return null;
	}
	
	/**
	 *  Tries to plant the item as a block.  Only works if the land conditions are correct
	 *  and the item is actually seeds that can be planted.
	 */
	public boolean plantBlock(Point3i point, ItemStack stack){
		//Check for valid seeds.
		if(stack.getItem() instanceof IPlantable){
			//Check if we have farmland below and air above.
			BlockPos farmlandPos = new BlockPos(point.x, point.y, point.z);
			IBlockState farmlandState = world.getBlockState(farmlandPos);
			Block farmlandBlock = farmlandState.getBlock();
			if(farmlandBlock.equals(Blocks.FARMLAND)){
				BlockPos cropPos = farmlandPos.up();
				if(world.isAirBlock(cropPos)){
					//Check to make sure the block can sustain the plant we want to plant.
					IPlantable plantable = (IPlantable) stack.getItem();
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
	 *  Tries to plow the block.  Essentially, this turns grass and dirt into farmland.
	 */
	public boolean plowBlock(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
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
		}
		
		if(!oldState.equals(newState)){
			world.setBlockState(pos, newState);
			world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
			return true;
		}
		return false;
	}
	
	/**
	 *  Spawns the passed-in ItemStack as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItemStack(ItemStack stack, WrapperNBT data, Point3d point){
		//TODO this goes away when we get wrapper ItemStacks.
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, stack));
	}
	
	/**
	 *  Spawns an explosion of the specified strength at the passed-in point.
	 *  Explosion in this case is from an entity.
	 */
	public void spawnExplosion(AEntityBase source, Point3d location, double strength, boolean flames){
		world.newExplosion(BuilderEntity.entitiesToBuilders.get(source), location.x, location.y, location.z, (float) strength, flames, true);
	}
	
	/**
	 *  Spawns an explosion of the specified strength at the passed-in point.
	 *  Explosion in this case is from the player.
	 */
	public void spawnExplosion(WrapperPlayer player, Point3d location, double strength, boolean flames){
		world.newExplosion(player.player, location.x, location.y, location.z, (float) strength, flames, true);
	}
}