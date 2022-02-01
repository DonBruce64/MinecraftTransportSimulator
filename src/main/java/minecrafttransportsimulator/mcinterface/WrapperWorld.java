package minecrafttransportsimulator.mcinterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataCSHandshake;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**Wrapper to a world instance.  This contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  Note that
 * clients and servers don't share world interfaces, and there are world interfaces for
 * every loaded world, so multiple interfaces will always be present on a system.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class WrapperWorld{
	private static final Map<World, WrapperWorld> worldWrappers = new HashMap<World, WrapperWorld>();
	private final Map<WrapperPlayer, Integer> ticksSincePlayerJoin = new HashMap<WrapperPlayer, Integer>();
	private final Map<WrapperPlayer, BuilderEntityRenderForwarder> activePlayerFollowers = new HashMap<WrapperPlayer, BuilderEntityRenderForwarder>();
	private final List<AxisAlignedBB> mutableCollidingAABBs = new ArrayList<AxisAlignedBB>();
	private final Set<BlockPos> knownAirBlocks = new HashSet<BlockPos>();
	
	//TODO make this protected when we abstract overlays and packets.
	public final World world;
	
	
	public final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<AEntityA_Base>();
	public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<AEntityC_Renderable>();
	private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>>();
	private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<UUID, AEntityA_Base>();
	
	private final WrapperNBT savedData;
	

	private WrapperWorld(World world){
		this.world = world;
		if(world.isRemote){
			//Send packet to server to request data for this world.
			this.savedData = new WrapperNBT();
			InterfacePacket.sendToServer(new PacketWorldSavedDataCSHandshake(InterfaceClient.getClientPlayer(), "", null));
		}else{
			//Load data from disk.
			try{
				if(getDataFile().exists()){
					this.savedData = new WrapperNBT(CompressedStreamTools.readCompressed(new FileInputStream(getDataFile())));
				}else{
					this.savedData = new WrapperNBT();
				}
			}catch(Exception e){
				e.printStackTrace();
				throw new IllegalStateException("Could not load saved data from disk!  This will result in data loss if we continue!");
			}
		}
		MinecraftForge.EVENT_BUS.register(this);
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
	 *  Returns the time of day of the world, in ticks.
	 *  This method will not increment if the world's internal clock isn't currently
	 *  advancing.
	 */
	public long getTime(){
		return world.getWorldTime();
	}
	
	/**
	 *  Returns the name of this world.  All names are assured to be unique, so this may
	 *  be used as a map-key or other identifier.
	 */
	public String getName(){
		return world.getWorldInfo().getWorldName();
	}
		
	/**
	 *  Returns the max build height for the world.  Note that entities may move and be saved
	 *  above this height, and moving above this height will result in rendering oddities.
	 */
	public long getMaxHeight(){
		return world.getHeight();
	}
	
	/**
	 *  Starts profiling with the specified title.
	 *  This should be done as the first thing when doing profiling of any system,
	 *  and the last thing after all profiling is done.  Note that you may begin
	 *  multiple pofiling operations one after the other.  These will stack and
	 *  group in the profiler.  However, for each profiling operation you start
	 *  you MUST end it!
	 */
	public void beginProfiling(String name, boolean subProfile){
		if(subProfile){
			world.profiler.startSection(name);
		}else{
			world.profiler.endStartSection(name);
		}
	}
	
	/**
	 * Ends profiling for the current profile.
	 */
	public void endProfiling(){
		world.profiler.endSection();
	}
	
	/**
	 *  Returns the requested saved data for this world.  As servers save data, while clients don't,
	 *  this method will only ensure valid return values on the server.  On clients, there will
	 *  be some delay in obtaining the data from the server due to packets.  As such, this method
	 *  may return null if the data hasn't arrived from the server.  After this, the object will 
	 *  contain all the server data, and will remain updated with data changes from the server.  
	 *  Do NOT attempt to modify the data object on the client, as it will result in a
	 *  de-synchronized state.  Instead, send a packet to the server to modify its copy, 
	 *  and then wait for the synchronizing packet.  Note that passing-in an empty string here
	 *  will return the entire data block rather than the specific block of data.  This may be
	 *  used to parse through the data, or to break it up into chunks to send to other clients.
	 */
	public WrapperNBT getData(String name){
		if(name.isEmpty()){
			return savedData;
		}else{
			return savedData.getData(name);
		}
	}
	
	/**
	 *  Sends all saved data to the passed-in player.
	 *  This is used 
	 */
	public void setData(String name, WrapperNBT value){
		savedData.setData(name, value);
		if(!isClient()){
			try{
				CompressedStreamTools.writeCompressed(savedData.tag, new FileOutputStream(getDataFile()));
			}catch(Exception e){
				e.printStackTrace();
				throw new IllegalStateException("Could not save data to disk!  This will result in data loss if we continue!");
			}
		}
	}
	
	/**
	 *  Returns the data file where saved data is stored for this world.  This is only valid
	 *  on servers.
	 */
	public File getDataFile(){
		return new File(world.getSaveHandler().getWorldDirectory(), "mtsdata.dat");
	}
	
	/**
	 *  Returns the entity that has the passed-in ID.
	 *  If the entity is a player, an instance of {@link WrapperPlayer}
	 *  is returned instead.
	 */
	public WrapperEntity getExternalEntity(UUID entityID){
		for(Entity entity : world.loadedEntityList){
			if(entity.getUniqueID().equals(entityID)){
				return WrapperEntity.getWrapperFor(entity);
			}
		}
		return null;
	}
	
	/**
	 *  Returns a list of entities within the specified bounds.
	 *  Only for wrapped entities: normal entities should be checked via
	 *  their own methods.
	 */
	public List<WrapperEntity> getEntitiesWithin(BoundingBox box){
		List<WrapperEntity> entities = new ArrayList<WrapperEntity>();
		for(Entity entity : world.getEntitiesWithinAABB(Entity.class, box.convert())){
			if(!(entity instanceof ABuilderEntityBase)){
				entities.add(WrapperEntity.getWrapperFor(entity));
			}
		}
		return entities;
	}
	
	/**
	 *  Returns a list of all hostile entities in the specified radius.
	 */
	public List<WrapperEntity> getEntitiesHostile(WrapperEntity lookingEntity, double radius){
		List<WrapperEntity> entities = new ArrayList<WrapperEntity>();
		for(Entity entity : world.getEntitiesWithinAABBExcludingEntity(lookingEntity.entity, lookingEntity.entity.getEntityBoundingBox().grow(radius))){
			if(entity instanceof IMob && !entity.isDead && (!(entity instanceof EntityLivingBase) || ((EntityLivingBase) entity).deathTime == 0)){
				entities.add(WrapperEntity.getWrapperFor(entity));
			}
		}
		return entities;
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
			if(!entity.equals(mcLooker.getRidingEntity()) && !(entity instanceof BuilderEntityRenderForwarder)){
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
	 *  Spawns the brand-new entity into the world.
	 */
	public void spawnEntity(AEntityB_Existing entity){
		BuilderEntityExisting builder = new BuilderEntityExisting(entity.world.world);
		builder.loadedFromSavedNBT = true;
		builder.setPositionAndRotation(entity.position.x, entity.position.y, entity.position.z, (float) -entity.angles.y, (float) entity.angles.x);
		builder.entity = entity;
		world.spawnEntity(builder);
		addEntity(entity);
    }
	
	/**
	 *  Attacks all entities that are in the passed-in damage range.  
	 *  This only includes external entities, and NOT any entities
	 *  that extend {@link BuilderEntityExisting}  If this is called on 
	 *  clients, then this method will not attack any entities. Instead, 
	 *  it will return a list of all entities that could have been attacked. 
	 *  This is because attacking cannot be done on clients, but it may be useful to 
	 *  know what entities could have been attacked should the call have been made on a server.
	 *  Note that the passed-in motion is used to move the Damage BoundingBox a set distance to
	 *  prevent excess collision checking, and may be null if no motion is applied.
	 */
	public List<WrapperEntity> attackEntities(Damage damage, Point3d motion){
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
		List<WrapperEntity> hitEntities = new ArrayList<WrapperEntity>();
		
		if(motion != null){
			startPoint = damage.box.globalCenter;
			endPoint = damage.box.globalCenter.copy().add(motion);
			start = new Vec3d(startPoint.x, startPoint.y, startPoint.z);
			end = new Vec3d(endPoint.x, endPoint.y, endPoint.z);
		}
		
		//Validate the collided entities to make sure we didn't hit something we shouldn't have.
		//Also get rayTrace hits for advanced checking.
		for(Entity mcEntityCollided : collidedEntities){
			if(!(mcEntityCollided instanceof ABuilderEntityBase)){
				if(damage.damgeSource != null){
					Entity ridingEntity = mcEntityCollided.getRidingEntity();
					if(ridingEntity instanceof BuilderEntityExisting){
						AEntityB_Existing internalEntity = ((BuilderEntityExisting) ridingEntity).entity;
						if(damage.damgeSource.equals(internalEntity)){
							//Don't attack riders of the source of the damage.
							continue;
						}else if(damage.damgeSource instanceof APart){
							APart damagingPart = (APart) damage.damgeSource;
							if(damagingPart.entityOn.equals(internalEntity)){
								//Don't attack riders of the multipart the part applying damage is a part of.
								continue;
							}
						}
					}
				}
				
				//Didn't hit a rider on the damage source. Do normal raytracing.
				if(motion != null){
					if(mcEntityCollided.getEntityBoundingBox().calculateIntercept(start, end) == null){
						//Raytracing doesn't intercept the box, so no hits.
						continue;
					}else{
						hitEntities.add(WrapperEntity.getWrapperFor(mcEntityCollided));
					}
				}
			}
		}
		
		//If we are on the server, attack the entities.
		//If we are on a client, we won't have attacked any entities, but we need to return what we found.
		if(isClient()){
			return hitEntities;
		}else{
			for(Entity entity : collidedEntities){
				WrapperEntity.getWrapperFor(entity).attack(damage);
			}
			return null;
		}
	}
	
	/**
	 *  Loads all entities that are in the passed-in range into the passed-in entity.
	 *  Only non-hostile mobs that are not already riding an entity will be loaded.
	 */
	public void loadEntities(BoundingBox box, AEntityE_Interactable<?> entityToLoad){
		for(Entity entity : world.getEntitiesWithinAABB(Entity.class, box.convert())){
			if(!entity.isRiding() && (entity instanceof INpc || entity instanceof EntityCreature) && !(entity instanceof IMob)){
				for(Point3d ridableLocation : entityToLoad.ridableLocations){
					if(!entityToLoad.locationRiderMap.containsKey(ridableLocation)){
						if(entityToLoad instanceof EntityVehicleF_Physics){
							if(((EntityVehicleF_Physics) entityToLoad).getPartAtLocation(ridableLocation).placementDefinition.isController){
								//continue;
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
		return block instanceof BuilderBlock ? ((BuilderBlock) block).block : null;
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
	 *  0.6 is default slipperiness for blocks. higher values are more slippery.
	 */
	public float getBlockSlipperiness(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		return state.getBlock().getSlipperiness(state, world, pos, null);
	}
	
	/**
	 *  Returns the material of the block.
	 */
	public BlockMaterial getBlockMaterial(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		Material material = world.getBlockState(pos).getMaterial();
		if(material.equals(Material.GROUND) || material.equals(Material.GRASS)){
			return world.isRainingAt(pos.up()) ? BlockMaterial.DIRT_WET : BlockMaterial.DIRT;
		}else if(material.equals(Material.SAND)){
			return world.isRainingAt(pos.up()) ? BlockMaterial.SAND_WET : BlockMaterial.SAND;
		}else if(material.equals(Material.SNOW) || material.equals(Material.CRAFTED_SNOW)){
			return BlockMaterial.SNOW;
		}else if(material.equals(Material.ICE) || material.equals(Material.PACKED_ICE)){
			return BlockMaterial.ICE;
		}else{
			return world.isRainingAt(pos.up()) ? BlockMaterial.NORMAL_WET : BlockMaterial.NORMAL;
		}
	}
	
	/**
	 *  Returns a list of block drops for the block at the passed-in position.
	 *  Does not actually destroy the block and make it drop anything.
	 */
	public List<ItemStack> getBlockDrops(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		NonNullList<ItemStack> drops = NonNullList.create();
		state.getBlock().getDrops(drops, world, pos, state, 0);
		return drops;
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
		mutableCollidingAABBs.clear();
		for(int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i){
    		for(int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j){
    			for(int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				if(world.isBlockLoaded(pos)){
	    				IBlockState state = world.getBlockState(pos);
	    				if(state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null){
	    					int oldCollidingBlockCount = mutableCollidingAABBs.size();
	    					state.addCollisionBoxToList(world, pos, mcBox, mutableCollidingAABBs, null, false);
	    					if(mutableCollidingAABBs.size() > oldCollidingBlockCount){
	    						box.collidingBlockPositions.add(new Point3d(i, j, k));
	    					}
	    				}
						if(box.collidesWithLiquids && state.getMaterial().isLiquid()){
							mutableCollidingAABBs.add(state.getBoundingBox(world, pos).offset(pos));
							box.collidingBlockPositions.add(new Point3d(i, j, k));
						}
    				}
    			}
    		}
    	}
		
		//If we are in the depth bounds for this collision, set it as the collision depth.
		box.currentCollisionDepth.set(0D, 0D, 0D);
		double boxCollisionDepth;
		double minDelta = 0.0001;
		for(AxisAlignedBB colBox : mutableCollidingAABBs){
			if(collisionMotion.x > 0){
				boxCollisionDepth = mcBox.maxX - colBox.minX;
				if(!ignoreIfGreater || collisionMotion.x - boxCollisionDepth > -minDelta){
					box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
				}
			}else if(collisionMotion.x < 0){
				boxCollisionDepth = colBox.maxX - mcBox.minX;
				if(!ignoreIfGreater || collisionMotion.x + boxCollisionDepth < minDelta){
					box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
				}
			}
			if(collisionMotion.y > 0){
				boxCollisionDepth = mcBox.maxY - colBox.minY;
				if(!ignoreIfGreater || collisionMotion.y - boxCollisionDepth > -minDelta){
					box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
				}
			}else if(collisionMotion.y < 0){
				boxCollisionDepth = colBox.maxY - mcBox.minY;
				if(!ignoreIfGreater || collisionMotion.y + boxCollisionDepth < minDelta){
					box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
				}
			}
			if(collisionMotion.z > 0){
				boxCollisionDepth = mcBox.maxZ - colBox.minZ;
				if(!ignoreIfGreater || collisionMotion.z - boxCollisionDepth > -minDelta){
					box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
				}
			}else if(collisionMotion.z < 0){
				boxCollisionDepth = colBox.maxZ - mcBox.minZ;
				if(!ignoreIfGreater || collisionMotion.z + boxCollisionDepth < minDelta){
					box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
				}
			}
		}
		if(box.currentCollisionDepth.isZero()){
			box.collidingBlockPositions.clear();
		}
	}
	
	/**
	 * Checks all passed-in bounding boxes for collisions with other blocks.  Returns true if they collided,
	 * false if they did not.  This is a bulk method designed to handle multiple checks in a row.  As such,
	 * it stores a listing of known air blocks.  If a block has been checked before and is air, it is ignored.
	 * To reset this list, pass in clearCache.  Note that this method, unlike the more granular one for
	 * collision depth, does not support liquid collisions.
	 */
	public boolean checkForCollisions(BoundingBox box, boolean clearCache){
		if(clearCache){
			knownAirBlocks.clear();
		}
		mutableCollidingAABBs.clear();
		AxisAlignedBB mcBox = box.convert();
		for(int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i){
    		for(int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j){
    			for(int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				if(!knownAirBlocks.contains(pos)){
    					if(world.isBlockLoaded(pos)){
    	    				IBlockState state = world.getBlockState(pos);
    	    				if(state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null){
    	    					int oldCollidingBlockCount = mutableCollidingAABBs.size();
    	    					state.addCollisionBoxToList(world, pos, mcBox, mutableCollidingAABBs, null, false);
    	    					if(mutableCollidingAABBs.size() > oldCollidingBlockCount){
    	    						return true;
    	    					}
    	    				}else{
    	    					knownAirBlocks.add(pos);
    	    				}
        				}
    				}
    			}
    		}
    	}
		return false;
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
		            	if(block instanceof ABlockBaseTileEntity){
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
		            		builderTile.tileEntity = (TileEntityType) ((ABlockBaseTileEntity) block).createTileEntity(this, position, player, data);
		            		addEntity(builderTile.tileEntity);
		            	}
		            	//Shrink stack as we placed this block.
		                stack.shrink(1);
		                return true;
		            }
	            }
    		}else{
    			IBlockState newState = wrapper.getDefaultState();
    			if(world.setBlockState(pos, newState, 11)){
    				return true;
    			}
    		}
    	}
    	return false;
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
	 *  is a dynamic-brightness block that extends {@link ABlockBaseTileEntity}. 
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
	 *  Destroys the block at the position, dropping it as whatever drop it drops as if set.
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void destroyBlock(Point3d position, boolean spawnDrops){
		world.destroyBlock(new BlockPos(position.x, position.y, position.z), spawnDrops);
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
	 *  Extinguishes the block at the passed-in position if it's fire.
	 *  If it is not fire, then the block is not modified.
	 *  Note that the position assumes the block hit is the one that is on fire,
	 *  not that the fire itself was hit.  This is because fire blocks do not have collision.
	 */
	public void extinguish(Point3d position){
		world.extinguishFire(null, new BlockPos(position.x, position.y, position.z), EnumFacing.UP);
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
	 *  of the drops from the crops.  If the crops couldn't be harvested, an empty list is returned.
	 *  If the block was harvested, but not crops, then the resulting drops
	 *  are dropped on the ground and an empty list is returned.
	 */
	public List<ItemStack> harvestBlock(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		List<ItemStack> cropDrops = new ArrayList<ItemStack>();
		if((state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state)) || state.getBlock() instanceof BlockBush){
			Block harvestedBlock = state.getBlock();
			NonNullList<ItemStack> drops = NonNullList.create();
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
		}
		return cropDrops;
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
	 *  Tries to remove any snow at the passed-in position.
	 */
	public void removeSnow(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		IBlockState state = world.getBlockState(pos);
		if(state.getMaterial().equals(Material.SNOW) || state.getMaterial().equals(Material.CRAFTED_SNOW)){
			world.setBlockToAir(pos);
			world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		}
	}
	
	/**
	 *  Spawns the passed-in item as an item entity at the passed-in point.
	 *  This should be called only on servers, as spawning items on clients
	 *  leads to phantom items that can't be picked up. 
	 */
	public void spawnItem(AItemBase item, WrapperNBT data, Point3d point){
		//Spawn 1 block above in case we're right on a block.
		world.spawnEntity(new EntityItem(world, point.x, point.y + 1, point.z, item.getNewStack(data)));
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
    * Spawn "follower" entities for the player if they don't exist already.
    * This only happens 3 seconds after the player joins.
    * This delay is done to ensure all chunks are loaded before spawning any followers.
    * We also track followers, and ensure that if the player doesn't exist, they are removed.
    * This handles players leaving.  We could use events for this, but they're not reliable.
    */
   @SubscribeEvent
   public void on(TickEvent.WorldTickEvent event){
	   //Need to check if it's our world, because Forge is stupid like that.
	   if(event.world.equals(world) && event.phase.equals(Phase.END) && !event.world.isRemote){
		   for(EntityPlayer player : event.world.playerEntities){
			   //Need to use wrapper here as the player equality tests don't work if there are two players with the same ID.
			   WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(player);
			   if(activePlayerFollowers.containsKey(playerWrapper)){
				   //Follower exists, check if world is the same and it is actually updating.
				   //We check basic states, and then the watchdog bit that gets reset every tick.
				   //This way if we're in the world, but not valid we will know.
				   BuilderEntityRenderForwarder follower = activePlayerFollowers.get(playerWrapper);
				   if(follower.world != player.world || follower.playerFollowing != player || player.isDead || follower.isDead || follower.idleTickCounter == 20){
					   //Follower is not linked.  Remove it and re-create in code below.
					   follower.setDead();
					   activePlayerFollowers.remove(playerWrapper);
					   ticksSincePlayerJoin.remove(playerWrapper);
				   }else{
					   ++follower.idleTickCounter;
					   continue;
				   }
			   }
			   
			   if(!activePlayerFollowers.containsKey(playerWrapper)){
				   //Follower does not exist, check if player has been present for 3 seconds and spawn it.
				   int totalTicksWaited = 0;
				   if(ticksSincePlayerJoin.containsKey(playerWrapper)){
					   totalTicksWaited = ticksSincePlayerJoin.get(playerWrapper); 
				   }
				   if(++totalTicksWaited == 60){
					   //Spawn fowarder and gun.
					   BuilderEntityRenderForwarder follower = new BuilderEntityRenderForwarder(player);
					   follower.loadedFromSavedNBT = true;
					   event.world.spawnEntity(follower);
					   activePlayerFollowers.put(playerWrapper, follower);
					   
					   EntityPlayerGun entity = new EntityPlayerGun(this, playerWrapper, new WrapperNBT());
					   spawnEntity(entity);
					   
					   //If the player is new, also add handbooks.
					   if(!ConfigSystem.configObject.general.joinedPlayers.value.contains(playerWrapper.getID())){
						   player.addItemStackToInventory(PackParserSystem.getItem("mts", "handbook_car").getNewStack(null));
						   player.addItemStackToInventory(PackParserSystem.getItem("mts", "handbook_plane").getNewStack(null));
						   ConfigSystem.configObject.general.joinedPlayers.value.add(playerWrapper.getID());
						   ConfigSystem.saveToDisk();
					   }
				   }else{
					   ticksSincePlayerJoin.put(playerWrapper, totalTicksWaited);
				   }
			   }
		   }
	   }
   }
	
	/**
    * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
    * Also remove this wrapper from the created lists, as it's invalid.
    */
   @SubscribeEvent
   public void on(WorldEvent.Unload event){
	   	//Need to check if it's our world, because Forge is stupid like that.
	   	if(event.getWorld().equals(world)){
	    	for(AEntityA_Base entity : allEntities){
	    		entity.remove();
	    	}
	    	worldWrappers.remove(this);
	   	}
   }
   

	
	//ENTITY MANAGEMENT CODE----- TO BE OUTSOURCED WHEN CONVERTING.
	
   
   /**
    * Adds the entity to the world.  This will make it get update ticks and be rendered
    * and do collision checks, as applicable.  Note that this should only be called at
    * FULL construction.  As such, it is recommended to NOT put the call in the entity
    * constructor itself unless the class is final, as it is possible that extending
    * constructors won't complete before the entity is accessed from this list.
    */
   public <EntityType extends AEntityA_Base> void addEntity(EntityType entity){
	   allEntities.add(entity);
	   if(entity instanceof AEntityC_Renderable){
		   renderableEntities.add((AEntityC_Renderable) entity);
	   }
	   
	   @SuppressWarnings("unchecked")
	   ConcurrentLinkedQueue<EntityType> classList = (ConcurrentLinkedQueue<EntityType>) entitiesByClass.get(entity.getClass());
	   if(classList == null){
		   classList = new ConcurrentLinkedQueue<EntityType>();
		   entitiesByClass.put(entity.getClass(), classList);
	   }
	   classList.add(entity);
	   if(entity.shouldSync()){
		   trackedEntityMap.put(entity.uniqueUUID, entity);
	   }
   }
   
   /**
    * Gets the entity with the requested UUID.
    */
   @SuppressWarnings("unchecked")
   public <EntityType extends AEntityA_Base> EntityType getEntity(UUID uniqueUUID){
	   return (EntityType) trackedEntityMap.get(uniqueUUID);
   }
   
   /**
    * Gets the list of all entities of the specified class.
    */
   @SuppressWarnings("unchecked")
   public <EntityType extends AEntityA_Base> ConcurrentLinkedQueue<EntityType> getEntitiesOfType(Class<EntityType> entityClass){
	   ConcurrentLinkedQueue<EntityType> classListing = (ConcurrentLinkedQueue<EntityType>) entitiesByClass.get(entityClass);
	   if(classListing == null){
		   classListing = new ConcurrentLinkedQueue<EntityType>();
		   entitiesByClass.put(entityClass, classListing);
	   }
	   return classListing;
   }
   
   /**
    * Removes this entity from the world.  Taking it off the update/functional lists.
    */
   public void removeEntity(AEntityA_Base entity){
	   allEntities.remove(entity);
	   if(entity instanceof AEntityC_Renderable){
		   renderableEntities.remove(entity);
	   }
	   entitiesByClass.get(entity.getClass()).remove(entity);
	   if(entity.shouldSync()){
		   trackedEntityMap.remove(entity.uniqueUUID);
	   }
   }
}