package minecrafttransportsimulator.mcinterface;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.EntityManager;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;

/**
 * IWrapper to a world instance.  This contains many common methods that
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  Note that
 * clients and servers don't share world objects, and there are world objects for
 * every loaded world, so multiple objects will always be present on a system.
 * Unlike everything else in the interface system, this is an abstract class.
 * This is required as we need to bolt-on an entity manager to it upon construction.
 *
 * @author don_bruce
 */
public abstract class AWrapperWorld extends EntityManager {

    /**
     * Returns true if this is a client world, false if we're on the server.
     */
    public abstract boolean isClient();

    /**
     * Returns the time of day of the world, in ticks.
     * This method will not increment if the world's internal clock isn't currently
     * advancing.
     */
    public abstract long getTime();

    /**
     * Returns the name of this world (dimension).  All names are assured to be unique, so this may
     * be used as a map-key or other identifier.
     */
    public abstract String getName();

    /**
     * Returns the max build height for the world.  Note that entities may move and be saved
     * above this height, and moving above this height will result in rendering oddities.
     */
    public abstract long getMaxHeight();

    /**
     * Starts profiling with the specified title.
     * This should be done as the first thing when doing profiling of any system,
     * and the last thing after all profiling is done.  Note that you may begin
     * multiple pofiling operations one after the other.  These will stack and
     * group in the profiler.  However, for each profiling operation you start
     * you MUST end it!
     */
    public abstract void beginProfiling(String name, boolean subProfile);

    /**
     * Ends profiling for the current profile.
     */
    public abstract void endProfiling();

    /**
     * Returns the requested saved data for this world.  As servers save data, while clients don't,
     * this method will only ensure valid return values on the server.  On clients, there will
     * be some delay in obtaining the data from the server due to packets.  As such, this method
     * may return null if the data hasn't arrived from the server.  After this, the object will
     * contain all the server data, and will remain updated with data changes from the server.
     * Do NOT attempt to modify the data object on the client, as it will result in a
     * de-synchronized state.  Instead, send a packet to the server to modify its copy,
     * and then wait for the synchronizing packet.  Note that passing-in an empty string here
     * will return the entire data block rather than the specific block of data.  This may be
     * used to parse through the data, or to break it up into chunks to send to other clients.
     */
    public abstract IWrapperNBT getData(String name);

    /**
     * Sends all saved data to the passed-in player.
     * This is used
     */
    public abstract void setData(String name, IWrapperNBT value);

    /**
     * Returns the data file where saved data is stored for this world.  This is only valid
     * on servers.
     */
    public abstract File getDataFile();

    /**
     * Returns the entity that has the passed-in ID.
     * If the entity is a player, an instance of {@link IWrapperPlayer}
     * is returned instead.
     */
    public abstract IWrapperEntity getExternalEntity(UUID entityID);

    /**
     * Returns a list of entities within the specified bounds.
     * Only for wrapped entities: normal entities should be checked via
     * their own methods.
     */
    public abstract List<IWrapperEntity> getEntitiesWithin(BoundingBox box);

    /**
     * Like {@link #getEntitiesWithin(BoundingBox)}, but for players.
     */
    public abstract List<IWrapperPlayer> getPlayersWithin(BoundingBox box);

    /**
     * Returns a list of all hostile entities in the specified radius.
     */
    public abstract List<IWrapperEntity> getEntitiesHostile(IWrapperEntity lookingEntity, double radius);

    /**
     * Spawns the brand-new entity into the world.
     */
    public abstract void spawnEntity(AEntityB_Existing entity);

    /**
     * Attacks all entities that are in the passed-in damage range.
     * This only includes external entities, and NOT any entities
     * that extend {@link AEntityA_Base}  If this is called with
     * generateList as true, then this method will not attack any entities. Instead,
     * it will return a list of all entities that could have been attacked.
     * Otherwise, the method returns null.
     */
    public abstract List<IWrapperEntity> attackEntities(Damage damage, Point3D motion, boolean generateList);

    /**
     * Loads all entities that are in the passed-in range into the passed-in entity.
     * If a vehicle is clicked, it will load the whole vehicle besides controllers.
     * Otherwise, only the specific entity will be loaded.
     * Only non-hostile mobs that are not already riding an entity will be loaded.
     */
    public abstract void loadEntities(BoundingBox box, AEntityE_Interactable<?> entityToLoad);

    /**
     * Adds to the map a list of all item entities within the passed-in bounds.
     */
    public abstract void populateItemStackEntities(Map<IWrapperEntity, IWrapperItemStack> map, BoundingBox b);

    /**
     * Removes the specified item stack entity from the world.
     */
    public abstract void removeItemStackEntity(IWrapperEntity entity);

    /**
     * Returns true if the chunk that contains the position is loaded.
     */
    public abstract boolean chunkLoaded(Point3D position);

    /**
     * Returns the block at the passed-in position, or null if it doesn't exist in the world.
     * Only valid for blocks of type {@link ABlockBase} others will return null.
     */
    public abstract ABlockBase getBlock(Point3D position);

    /**
     * Returns the name of the block.
     */
    public abstract String getBlockName(Point3D position);

    /**
     * Returns the hardness of the block at the passed-in point.
     */
    public abstract float getBlockHardness(Point3D position);

    /**
     * Returns the slipperiness of the block at the passed-in position.
     * 0.6 is default slipperiness for blocks. higher values are more slippery.
     */
    public abstract float getBlockSlipperiness(Point3D position);

    /**
     * Returns the material of the block.
     */
    public abstract BlockMaterial getBlockMaterial(Point3D position);
    
    /**
     * Returns the color of the block as determined by map coloring.
     */
    public abstract ColorRGB getBlockColor(Point3D position);

    /**
     * Returns a list of block drops for the block at the passed-in position.
     * Does not actually destroy the block and make it drop anything.
     */
    public abstract List<IWrapperItemStack> getBlockDrops(Point3D position);

    /**
     * Returns the position where the first block along the path can be hit, or null if there are
     * no blocks along the path.
     */
    public abstract BlockHitResult getBlockHit(Point3D position, Point3D delta);

    /**
     * Returns true if the block at the passed-in position is solid at the passed-in axis.
     * Solid means that said block can be collided with, is a cube, and is generally able to have
     * things placed or connected to it.
     */
    public abstract boolean isBlockSolid(Point3D position, Axis axis);

    /**
     * Returns true if the block is liquid.
     */
    public abstract boolean isBlockLiquid(Point3D position);

    /**
     * Returns true if the block below the passed-in position is a slab, but only the
     * bottom portion of the slab.  May be used to adjust renders to do half-block
     * rendering to avoid floating blocks.
     */
    public abstract boolean isBlockBelowBottomSlab(Point3D position);

    /**
     * Returns true if the block above the passed-in position is a slab, but only the
     * top portion of the slab.  May be used to adjust renders to do half-block
     * rendering to avoid floating blocks.
     */
    public abstract boolean isBlockAboveTopSlab(Point3D position);

    /**
     * Returns the distance from the passed-in position to highest block below this position in the world, at the position's X/Z coords.
     * This may or may not be the highest block in the column depending on block layout.
     */
    public abstract double getHeight(Point3D position);

    /**
     * Updates the blocks and depths of collisions for the passed-in BoundingBox to the box's internal variables.
     * This is done as it allows for re-use of the variables by the calling object to avoid excess object creation.
     * Note that if the offset value passed-in for an axis is 0, then no collision checks will be performed on that axis.
     * This prevents excess calculations when trying to do movement calculations for a single axis.  If ignoreIfGreater
     * is set, then the system will not set the collisionDepth of corresponding axis if the motion is less than the
     * collisionMotion axis.  If this value is not set, the function simply looks for a non-zero value to make the
     * collisionDepth be set for that axis.  Note that leaves are never checked in this code.
     */
    public abstract void updateBoundingBoxCollisions(BoundingBox box, Point3D collisionMotion, boolean ignoreIfGreater);

    /**
     * Checks the passed-in bounding box for collisions with other blocks.  Returns true if they collided,
     * false if they did not.  This is a bulk method designed to handle multiple checks in a row.  As such,
     * it stores a listing of known air blocks.  If a block has been checked before and is air, it is ignored.
     * To reset this list, pass in clearCache.  Note that leaves are ignored, but can be broken if requested.
     */
    public abstract boolean checkForCollisions(BoundingBox box, Point3D offset, boolean clearCache, boolean breakLeaves);

    /**
     * Returns the current redstone power at the passed-in position.
     */
    public abstract int getRedstonePower(Point3D position);

    /**
     * Returns the rain strength at the passed-in position.
     * 0 is no rain, 1 is rain, and 2 is a thunderstorm.
     * Note that this method offsets the point by 1, as it allows
     * for blocks to query rain strength and not get 0 due to no rain
     * being possible "in" that block.
     */
    public abstract float getRainStrength(Point3D position);

    /**
     * Returns the current temperature at the passed-in position.
     * Dependent on biome, and likely modified by mods that add new boimes.
     */
    public abstract float getTemperature(Point3D position);

    /**
     * Places the passed-in block at the point specified.
     * Returns true if the block was placed, false if not.
     * If this block isn't placed by a player, pass in null
     * for the player reference.
     */
    public abstract <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONMultiModelProvider> boolean setBlock(ABlockBase block, Point3D position, IWrapperPlayer playerIWrapper, Axis axis);

    /**
     * Returns the tile entity at the passed-in position, or null if it doesn't exist in the world.
     * Only valid for TEs of type {@link ATileEntityBase} others will return null.
     */
    public abstract <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3D position);

    /**
     * Flags the tile entity at the passed-in position for saving.  This means the TE's
     * NBT data will be saved to disk when the chunk unloads so it will maintain its state.
     */
    public abstract void markTileEntityChanged(Point3D position);

    /**
     * Gets the brightness at this position, as a value between 0.0-1.0. Calculated from the
     * sun brightness, and possibly the block brightness if calculateBlock is true.
     */
    public abstract float getLightBrightness(Point3D position, boolean calculateBlock);

    /**
     * Updates the brightness of the block at this position.  Only works if the block
     * is a dynamic-brightness block that extends {@link ABlockBaseTileEntity}.
     */
    public abstract void updateLightBrightness(Point3D position);

    /**
     * Destroys the block at the position, dropping it as whatever drop it drops as if set.
     * This does no sanity checks, so make sure you're
     * actually allowed to do such a thing before calling.
     */
    public abstract void destroyBlock(Point3D position, boolean spawnDrops);

    /**
     * Returns true if the block at this position is air.
     */
    public abstract boolean isAir(Point3D position);

    /**
     * Returns true if the block at this position is fire.
     * Note: this will return true on vanilla fire, as well as
     * any other blocks made of fire from other mods.
     */
    public abstract boolean isFire(Point3D position);

    /**
     * Sets the block at the passed-in position to fire.
     * This does no sanity checks, so make sure you're
     * actually allowed to do such a thing before calling.
     */
    public abstract void setToFire(Point3D position, Axis side);

    /**
     * Extinguishes the block at the passed-in position if it's fire.
     * If it is not fire, then the block is not modified.
     * Note that the position assumes the block hit is the one that is on fire,
     * not that the fire itself was hit.  This is because fire blocks do not have collision.
     */
    public abstract void extinguish(Point3D position, Axis side);

    /**
     * Tries to place the item as a block at the passed-in position.
     * Only allows placing of the block in air.
     * Returns true if the block was placed, false if not.
     */
    public abstract boolean placeBlock(Point3D position, IWrapperItemStack stack);

    /**
     * Tries to fertilize the block at the passed-in position with the passed-in stack.
     * Returns true if the block was fertilized.
     */
    public abstract boolean fertilizeBlock(Point3D position, IWrapperItemStack stack);

    /**
     * Tries to harvest the block at the passed-in position.  If the harvest was
     * successful, and the block harvested was crops, the result returned is a list
     * of the drops from the crops.  If the crops couldn't be harvested, an empty list is returned.
     * If the block was harvested, but not crops, then the resulting drops
     * are dropped on the ground and an empty list is returned.
     */
    public abstract List<IWrapperItemStack> harvestBlock(Point3D position);

    /**
     * Tries to plant the item as a block at the passed-in position.  Only works if the land conditions are correct
     * and the item is actually seeds that can be planted.
     */
    public abstract boolean plantBlock(Point3D position, IWrapperItemStack stack);

    /**
     * Tries to plow the block at the passed-in position.  Essentially, this turns grass and dirt into farmland.
     */
    public abstract boolean plowBlock(Point3D position);

    /**
     * Tries to remove any snow at the passed-in position.
     * Returns true if snow was removed.
     */
    public abstract boolean removeSnow(Point3D position);

    /**
     * Tries to hydrate the block at the passed-in position.
     * Returns true if it was hydrated.
     */
    public abstract boolean hydrateBlock(Point3D position);

    /**
     * Attempts to insert a stack-item into the block that is at the specified
     * position-offset.  The position is of the block wanting to insert the item,
     * not the block to insert the item into.  Returns true if the stack was inserted.
     * Note that only one entry from the stack will be inserted for each call, even
     * if the stack has multiple item in it.
     */
    public abstract boolean insertStack(Point3D position, Axis axis, IWrapperItemStack stack);

    /**
     * Attempts to pull a single stack-item out of the block that is at the specified
     * position-offset.  The position is of the block wanting to extract the item,
     * not the block to extract the item from.  Returns the stack extracted, or null if
     * no stack was able to be found or no block was present to extract from.
     */
    public abstract IWrapperItemStack extractStack(Point3D position, Axis axis);

    /**
     * Spawns the passed-in stack as an item entity at the passed-in point.
     * This should be called only on servers, as spawning items on clients
     * leads to phantom items that can't be picked up.
     * Normally, items are spawned as if they're on top of blocks in a default MC behavior.
     * You can specify a motion to override this and spawn them right at the point with the requested motion.
     */
    public abstract void spawnItemStack(IWrapperItemStack stack, Point3D point, Point3D optionalMotion);

    /**
     * Spawns an explosion of the specified strength at the passed-in point.
     */
    public abstract void spawnExplosion(Point3D location, double strength, boolean flames);
}