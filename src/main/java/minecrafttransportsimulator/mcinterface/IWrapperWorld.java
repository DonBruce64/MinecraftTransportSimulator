package minecrafttransportsimulator.mcinterface;

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
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Wrapper to a world instance.  This contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  Note that
 * clients and servers don't share world interfaces, and there are world interfaces for
 * every loaded world, so multiple interfaces will always be present on a system.
 *
 * @author don_bruce
 */
public interface IWrapperWorld{
	
	/**
	 *  Returns true if this is a client world, false if we're on the server.
	 */
	public boolean isClient();
	
	/**
	 *  Returns the ID of the current dimension.
	 *  0 for overworld.
	 *  1 for the End.
	 *  -1 for the Nether.
	 *  Mods may add other values for their dims, so this list is not inclusive.
	 */
	public int getDimensionID();
	
	/**
	 *  Returns the current world time, in ticks.  Useful when you need to sync
	 *  operations.  For animations, just use the system time.
	 */
	public long getTime();
		
	/**
	 *  Returns the max build height for the world.  Note that entities may move and be saved
	 *  above this height, and moving above this height will result in rendering oddities.
	 */
	public long getMaxHeight();
	
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
	public IWrapperNBT getData();
	
	/**
	 *  Saves the passed-in data as the world's additional saved data.
	 */
	public void setData(IWrapperNBT data);
	
	/**
	 *  Returns the entity that has the passed-in ID.
	 *  If the entity is a player, an instance of {@link IWrapperPlayer}
	 *  is returned instead.
	 */
	public IWrapperEntity getEntity(int id);
	
	/**
	 *  Returns a list of entities within the specified bounds.
	 */
	public List<IWrapperEntity> getEntitiesWithin(BoundingBox box);
	
	/**
	 *  Returns the nearest hostile entity that can be seen by the passed-in entity.
	 */
	public IWrapperEntity getNearestHostile(IWrapperEntity entityLooking, int searchRadius);
	
	/**
	 *  Returns the closest entity whose collision boxes are intercepted by the
	 *  passed-in entity's line of sight.
	 */
	public IWrapperEntity getEntityLookingAt(IWrapperEntity entityLooking, float searchRadius);
	
	/**
	 *  Generates a new wrapper to be used for entity tracking.
	 *  This should be fed into the constructor of {@link AEntityBase}
	 *  at construction time to allow it to interface with the world.
	 */
	public IWrapperEntity generateEntity();
	
	/**
	 *  Spawns the entity into the world.  Only valid for entities that
	 *  have had their wrapper set from {@link #generateEntity()}
	 */
	public void spawnEntity(AEntityBase entity);
	
	/**
	 *  Attacks all entities that are in the passed-in damage range.  If the
	 *  passed-in entity is not null, then any entity riding the passed-in
	 *  entity that are inside the bounding box will not be attacked, nor will
	 *  the passed-in entity be attacked.  Useful for vehicles, where you don't 
	 *  want players firing weapons to hit themselves or the vehicle.
	 *  Note that if this is called on clients, then this method will not attack
	 *  any entities. Instead, it will return a map of all entities that could have
	 *  been attacked with the bounding box attacked if they are of type 
	 *  {@link IBuilderEntity} as the value to the entity key.
	 *  This is because attacking cannot be done on clients, but it may be useful to 
	 *  know what entities could have been attacked should the call have been made on a server.
	 *  Note that the passed-in motion is used to move the Damage BoundingBox a set distance to
	 *  prevent excess collision checking, and may be null if no motion is applied.
	 */
	public Map<IWrapperEntity, BoundingBox> attackEntities(Damage damage, AEntityBase damageSource, Point3d motion);
	
	/**
	 *  Moves all entities that collide with the passed-in bounding boxes by the passed-in offset.
	 *  Offset is determined by the passed-in vector, and the passed-in angle of said vector.
	 *  This allows for angular movement as well as linear.
	 */
	public void moveEntities(List<BoundingBox> boxesToCheck, Point3d intialPosition, Point3d initalRotation, Point3d linearMovement, Point3d angularMovement);
	
	/**
	 *  Loads all entities that are in the passed-in range into the passed-in entity.
	 *  Only non-hostile mobs will be loaded.
	 */
	public void loadEntities(BoundingBox box, AEntityBase vehicle);
	
	/**
	 *  Returns the block wrapper at the passed-in location, or null if the block is air.
	 */
	public IWrapperBlock getWrapperBlock(Point3i point);
	
	/**
	 *  Returns the block at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for blocks of type {@link ABlockBase} others will return null.
	 */
	public ABlockBase getBlock(Point3i point);
	
	/**
	 *  Returns the point where the first block along the path can be hit, or null if there are
	 *  no blocks along the path.
	 */
	public Point3i getBlockHit(Point3d start, Point3d end);
	
    /**
	 *  Returns the rotation (in degrees) of the block at the passed-in location.
	 *  Only valid for blocks of type {@link ABlockBase}.
	 */
    public float getBlockRotation(Point3i point);
	
	/**
	 *  Returns true if the block at the passed-in location is solid.  Solid means
	 *  that said block can be collided with, is a cube, and is generally able to have
	 *  things placed or connected to it.
	 */
	public boolean isBlockSolid(Point3i point);
	
	/**
	 *  Returns true if the block is liquid.
	 */
	public boolean isBlockLiquid(Point3i point);
	
	/**
	 *  Returns true if the block at the passed-in location is a slab, but only the
	 *  bottom portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockBottomSlab(Point3i point);
	
	/**
	 *  Returns true if the block at the passed-in location is a slab, but only the
	 *  top portion of the slab.  May be used to adjust renders to do half-block
	 *  rendering to avoid floating blocks.
	 */
	public boolean isBlockTopSlab(Point3i point);
	
	/**
	 * Updates the blocks and depths of collisions for the passed-in BoundingBox to the box's internal variables.
	 * This is done as it allows for re-use of the variables by the calling object to avoid excess object creation.
	 * Note that if the offset value passed-in for an axis is 0, then no collision checks will be performed on that axis.
	 * This prevents excess calculations when trying to do movement calculations for a single axis.  If ignoreIfGreater
	 * is set, then the system will not set the collisionDepth of corresponding axis if the motion is less than the
	 * collisionMotion axis.  If this value is not set, the function simply looks for a non-zero value to make the
	 * collisionDepth be set for that axis.
	 */
	public void updateBoundingBoxCollisions(BoundingBox box, Point3d collisionMotion, boolean ignoreIfGreater);
	
	/**
	 *  Returns the current redstone power at the passed-in position.
	 */
	public int getRedstonePower(Point3i point);

	/**
	 *  Returns the rain strength at the passed-in position.
	 *  0 is no rain, 1 is rain, and 2 is a thunderstorm.
	 */
	public float getRainStrength(Point3i point);
	
	/**
	 *  Returns the current temperature at the passed-in position.
	 *  Dependent on biome, and likely modified by mods that add new boimes.
	 */
	public float getTemperature(Point3i point);

    /**
	 *  Places the passed-in block at the point specified.
	 *  Returns true if the block was placed, false if not.
	 *  If this block isn't placed by a player, pass in null
	 *  for the player reference.
	 */
	public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONItem<?>> boolean setBlock(ABlockBase block, Point3i location, IWrapperPlayer player, Axis axis);
    
    /**
	 *  Gets the wrapper TE at the specified position.
	 */
	public IWrapperTileEntity getWrapperTileEntity(Point3i position);
	
	/**
	 *  Returns the tile entity at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for TEs of type {@link ATileEntityBase} others will return null.
	 */
	public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3i point);
	
	/**
	 *  Flags the tile entity at the passed-in point for saving.  This means the TE's
	 *  NBT data will be saved to disk when the chunk unloads so it will maintain its state.
	 */
	public void markTileEntityChanged(Point3i point);
	
	/**
	 *  Gets the brightness at this point, as a value between 0.0-1.0. Calculated from the
	 *  sun brightness, and possibly the block brightness if calculateBlock is true.
	 */
	public float getLightBrightness(Point3i point, boolean calculateBlock);
	
	/**
	 *  Updates the brightness of the block at this point.  Only works if the block
	 *  is a dynamic-brightness block that implements {@link IBlockTileEntity}. 
	 */
	public void updateLightBrightness(Point3i point);
	
	/**
	 *  Destroys the block at the position, dropping it as whatever drop it drops as.
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void destroyBlock(Point3i point);
	
	/**
	 *  Returns true if the block at this point is air.
	 */
	public boolean isAir(Point3i point);
	
	/**
	 *  Returns true if the block at this point is fire.
	 *  Note: this will return true on vanilla fire, as well as
	 *  any other blocks made of fire from other mods.
	 */
	public boolean isFire(Point3i point);
	
	/**
	 *  Sets the block at the passed-in position to fire. 
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void setToFire(Point3i point);
	
	/**
	 *  Tries to fertilize the block with the passed-in stack.
	 *  Returns true if the block was fertilized.
	 */
	public boolean fertilizeBlock(Point3i point, IWrapperItemStack stack);
	
	/**
	 *  Tries to harvest the block at the passed-in location.  If the harvest was
	 *  successful, and the block harvested was crops, the result returned is a list
	 *  of the drops from the crops.  If the crops couldn't be harvested, null is returned.
	 *  If the block was harvested, but not crops, then the resulting drops
	 *  are dropped on the ground and an empty list is returned.
	 */
	public List<IWrapperItemStack> harvestBlock(Point3i point);
	
	/**
	 *  Tries to plant the item as a block.  Only works if the land conditions are correct
	 *  and the item is actually seeds that can be planted.
	 */
	public boolean plantBlock(Point3i point, IWrapperItemStack stack);
	
	/**
	 *  Tries to plow the block.  Essentially, this turns grass and dirt into farmland.
	 */
	public boolean plowBlock(Point3i point);
	
	/**
	 *  Spawns the passed-in item as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItem(AItemBase item, IWrapperNBT data, Point3d point);
	
	/**
	 *  Spawns the passed-in stack as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItemStack(IWrapperItemStack stack, Point3d point);
	
	/**
	 *  Spawns an explosion of the specified strength at the passed-in point.
	 *  Explosion in this case is from an entity.
	 */
	public void spawnExplosion(AEntityBase source, Point3d location, double strength, boolean flames);
	
	/**
	 *  Spawns an explosion of the specified strength at the passed-in point.
	 *  Explosion in this case is from the player.
	 */
	public void spawnExplosion(IWrapperPlayer player, Point3d location, double strength, boolean flames);

	/**
	 * Get current world time
	 */
	public long getDayTime();
}