package mcinterface1122;

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
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
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
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
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

public class WrapperWorld implements IWrapperWorld{
	private static final Map<World, WrapperWorld> worldWrappers = new HashMap<World, WrapperWorld>();
	private final Map<Entity, WrapperEntity> entityWrappers = new HashMap<Entity, WrapperEntity>();
	private final Map<EntityPlayer, WrapperPlayer> playerWrappers = new HashMap<EntityPlayer, WrapperPlayer>();
	
	final World world;

	private WrapperWorld(World world){
		this.world = world;
		if(world.isRemote){
			NetworkSystem.sendToServer(new PacketWorldSavedDataCSHandshake(getDimensionID(), null));
		}
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
	 *  Null may be passed-in safely to ease function-forwarding.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 *  If the entity is a player, then a player wrapper is returned.
	 */
	public WrapperEntity getWrapperFor(Entity entity){
		if(entity instanceof EntityPlayer){
			return getWrapperFor((EntityPlayer) entity);
		}else if(entity != null){
			if(!entityWrappers.containsKey(entity)){
				entityWrappers.put(entity, new WrapperEntity(entity));
			}
			WrapperEntity wrapper = entityWrappers.get(entity);
			if(!wrapper.isValid() || entity != wrapper.entity){
				wrapper = new WrapperEntity(entity);
				entityWrappers.put(entity, wrapper);
			}
			return wrapper;
		}else{
			return null;
		}
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in player instance.
	 *  Null may be passed-in safely to ease function-forwarding.
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
			if(!wrapper.isValid() || player != wrapper.player){
				wrapper = new WrapperPlayer(player);
				playerWrappers.put(player, wrapper);
			}
			return wrapper;
		}else{
			return null;
		}
	}
	
	@Override
	public boolean isClient(){
		return world.isRemote;
	}
	
	@Override
	public int getDimensionID(){
		return world.provider.getDimension();
	}
	
	@Override
	public long getTick(){
		return world.getTotalWorldTime();
	}
	
	@Override
	public long getTime(){
		return world.getWorldTime();
	}
		
	@Override
	public long getMaxHeight(){
		return world.getHeight();
	}
	
	@Override
	public WrapperNBT getData(){
		if(!world.isRemote){
			if(savedDataAccessor == null){
				savedDataAccessor = (InterfaceWorldSavedData) world.getPerWorldStorage().getOrLoadData(InterfaceWorldSavedData.class, dataID);
				if(savedDataAccessor == null){
					savedDataAccessor = new InterfaceWorldSavedData(dataID);
				}
			}
		}else if(savedDataAccessor == null){
			return null;
		}
		return new WrapperNBT(savedDataAccessor.internalData);
	}
	
	@Override
	public void setData(WrapperNBT data){
		if(!world.isRemote){
			savedDataAccessor.internalData = data.tag;
			savedDataAccessor.markDirty();
			world.getPerWorldStorage().setData(savedDataAccessor.mapName, savedDataAccessor);
		}else{
			NetworkSystem.sendToServer(new PacketWorldSavedDataCSHandshake(getDimensionID(), data));
		}
	}
	
	InterfaceWorldSavedData savedDataAccessor;
	static final String dataID = MasterInterface.MODID + "_WORLD_DATA";
	
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
	
	@Override
	public IWrapperEntity getEntity(int id){
		Entity entity = world.getEntityByID(id);
		return entity instanceof EntityPlayer ? getWrapperFor((EntityPlayer) entity) : getWrapperFor(entity);
	}
	
	@Override
	public List<IWrapperEntity> getEntitiesWithin(BoundingBox box){
		List<IWrapperEntity> entities = new ArrayList<IWrapperEntity>();
		for(Entity entity : world.getEntitiesWithinAABB(Entity.class, convertBox(box))){
			entities.add(getWrapperFor(entity));
		}
		return entities;
	}
	
	@Override
	public WrapperEntity getNearestHostile(IWrapperEntity entityLooking, int searchRadius){
		double smallestDistance = searchRadius*2;
		Entity foundEntity = null;
		Entity mcLooker = ((WrapperEntity) entityLooking).entity;
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
		return foundEntity != null ? this.getWrapperFor(foundEntity) : null;
	}
	
	/*
	 *  Finds the closest entity that the looker's line of sight intersects,
	 *  within the passed-in searchRadius.
	 */
	@Override
	public WrapperEntity getEntityLookingAt(IWrapperEntity entityLooking, float searchRadius){
		double smallestDistance = searchRadius*2;
		Entity foundEntity = null;
		Entity mcLooker = ((WrapperEntity) entityLooking).entity;
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
		return foundEntity != null ? this.getWrapperFor(foundEntity) : null;
	}
	
	@Override
	public IWrapperEntity generateEntity(){
		//Generate a new builder to hold the entity and return the wrapper for it.
    	BuilderEntity builder = new BuilderEntity(world);
    	return getWrapperFor(builder);
    }
	
	@Override
	public void spawnEntity(AEntityBase entity){
		BuilderEntity builder = (BuilderEntity) ((WrapperEntity) entity.wrapper).entity;
		builder.entity = entity;
		builder.setPositionAndRotation(entity.position.x, entity.position.y, entity.position.z, (float) -entity.angles.y, (float) entity.angles.x);
		world.spawnEntity(builder);
    }
	
	@Override
	public Map<IWrapperEntity, BoundingBox> attackEntities(Damage damage, IWrapperEntity damageSource, Point3d motion){
		AxisAlignedBB mcBox = convertBox(damage.box);
		List<Entity> collidedEntities;
		List<Point3d> rayTraceHits = new ArrayList<Point3d>();;
		if(motion != null){
			mcBox = mcBox.expand(motion.x, motion.y, motion.z);
			collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);
			//Iterate over all entities.  If the entity doesn't intersect the damage path, remove it.
			Vec3d start = new Vec3d(damage.box.globalCenter.x, damage.box.globalCenter.y, damage.box.globalCenter.z);
			Vec3d end = start.add(motion.x, motion.y, motion.z);
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
						if(damageSource.equals(testSource.wrapper)){
							iterator.remove();
						}
					}else if(entity.getRidingEntity() instanceof BuilderEntity){
						AEntityBase testSource = ((BuilderEntity) entity.getRidingEntity()).entity;
						if(damageSource.equals(testSource.wrapper)){
							iterator.remove();
						}
					}else{
						if(((WrapperEntity) damageSource).entity.equals(entity)){
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
			Map<IWrapperEntity, BoundingBox> entities = new HashMap<IWrapperEntity, BoundingBox>();
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
	
	@Override
	public void moveEntities(List<BoundingBox> boxesToCheck, Point3d intialPosition, Point3d initalRotation, Point3d linearMovement, Point3d angularMovement){
		List<Entity> movedEntities = new ArrayList<Entity>();
		for(BoundingBox box : boxesToCheck){
			//Check if we collide with any entities.
			//We expand the passed-in box by 0.25 in the Y direction to "grab" any entities that might be above us.
			for(Entity entity : world.getEntitiesWithinAABB(Entity.class, convertBox(box).expand(0, 0.25, 0))){
				//Don't move riding entities or our own builders, or entities we've already moved.
				if(!movedEntities.contains(entity)){
					if(!(entity instanceof BuilderEntity) && entity.getRidingEntity() == null){
						AxisAlignedBB entityBox = entity.getEntityBoundingBox();
						//If the entity is within 0.5 units of the top of the box, we can move them.
						//If not, they are just colliding and not riding the vehicle and we should leave them be.
						double entityBottomDelta = box.globalCenter.y + box.heightRadius - entityBox.minY;
						if(entityBottomDelta >= -0.5 && entityBottomDelta <= 0.5 && (entity.motionY < 0 || entity.motionY < entityBottomDelta)){
							//Get how much the vehicle moved the collision box the entity collided with so we know how much to move the entity.
							//This lets entities "move along" with vehicles when touching a collision box.
							Point3d entityDeltaOffset = new Point3d(entity.posX - intialPosition.x, entity.posY - intialPosition.y, entity.posZ - intialPosition.z);
							Point3d vehicleBoxMovement = entityDeltaOffset.copy().rotateFine(angularMovement).subtract(entityDeltaOffset).add(linearMovement);
							
							//Apply motions to move entity, and add them to the moved entity list.
							entity.move(MoverType.SELF, vehicleBoxMovement.x, vehicleBoxMovement.y + entityBottomDelta, vehicleBoxMovement.z);
							entity.rotationYaw += -angularMovement.y;
							movedEntities.add(entity);
							
							//Set entity as on ground to allow them to jump on the collision box.
							entity.onGround = true;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void loadEntities(BoundingBox box, AEntityBase vehicle){
		for(Entity entity : world.getEntitiesWithinAABBExcludingEntity(((WrapperEntity) vehicle.wrapper).entity, convertBox(box))){
			if((entity instanceof INpc || entity instanceof EntityCreature) && !(entity instanceof IMob)){
				for(Point3d ridableLocation : vehicle.ridableLocations){
					if(!vehicle.locationRiderMap.containsKey(ridableLocation)){
						if(vehicle instanceof EntityVehicleF_Physics){
							if(((EntityVehicleF_Physics) vehicle).getPartAtLocation(ridableLocation).vehicleDefinition.isController){
								continue;
							}
						}
						vehicle.addRider(new WrapperEntity(entity), ridableLocation);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public WrapperBlock getWrapperBlock(Point3i point){
		return isAir(point) ? null : new WrapperBlock(world, new BlockPos(point.x, point.y, point.z));
	}
	
	@Override
	public ABlockBase getBlock(Point3i point){
		Block block = world.getBlockState(new BlockPos(point.x, point.y, point.z)).getBlock();
		return block instanceof BuilderBlock ? ((BuilderBlock) block).block : null;
	}
	
	@Override
	public Point3i getBlockHit(Point3d position, Point3d delta){
		Vec3d start = new Vec3d(position.x, position.y, position.z);
		RayTraceResult trace = world.rayTraceBlocks(start, start.add(delta.x, delta.y, delta.z), false, true, false);
		if(trace != null){
			BlockPos pos = trace.getBlockPos();
			if(pos != null){
				 return new Point3i(pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return null;
	}
	
	@Override
    public float getBlockRotation(Point3i point){
    	return world.getBlockState(new BlockPos(point.x, point.y, point.z)).getValue(BuilderBlock.FACING).getHorizontalAngle();
    }
	
	@Override
	public boolean isBlockSolid(Point3i point){
		IBlockState offsetMCState = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block offsetMCBlock = offsetMCState.getBlock();
        return offsetMCBlock != null ? !offsetMCBlock.equals(Blocks.BARRIER) && offsetMCState.getMaterial().isOpaque() && offsetMCState.isFullCube() && offsetMCState.getMaterial() != Material.GOURD : false;
	}
	
	@Override
	public boolean isBlockLiquid(Point3i point){
		IBlockState offsetMCState = world.getBlockState(new BlockPos(point.x, point.y, point.z));
        return offsetMCState.getMaterial().isLiquid();
	}
	
	@Override
	public boolean isBlockBottomSlab(Point3i point){
		IBlockState state = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
	}
	
	@Override
	public boolean isBlockTopSlab(Point3i point){
		IBlockState state = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block block = state.getBlock();
		return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
	}
	
	@Override
	public void updateBoundingBoxCollisions(BoundingBox box, Point3d collisionMotion, boolean ignoreIfGreater){
		AxisAlignedBB mcBox = convertBox(box);
		box.collidingBlocks.clear();
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
	    						box.collidingBlocks.add(new WrapperBlock(world, pos));
	    					}
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
	
	@Override
	public int getRedstonePower(Point3i point){
		return world.getStrongPower(new BlockPos(point.x, point.y, point.z));
	}

	@Override
	public float getRainStrength(Point3i point){
		return world.isRainingAt(new BlockPos(point.x, point.y, point.z)) ? world.getRainStrength(1.0F) + world.getThunderStrength(1.0F) : 0.0F;
	}
	
	@Override
	public float getTemperature(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		return world.getBiome(pos).getTemperature(pos);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONItem<?>> boolean setBlock(ABlockBase block, Point3i location, IWrapperPlayer playerWrapper, Axis axis){
    	if(!world.isRemote){
    		BuilderBlock wrapper = BuilderBlock.blockMap.get(block);
    		BlockPos pos = new BlockPos(location.x, location.y, location.z);
    		if(playerWrapper != null){
    			WrapperPlayer player = (WrapperPlayer) playerWrapper;
    	    	ItemStack stack = ((WrapperPlayer) playerWrapper).getHeldStack();
    	    	AItemBase item = player.getHeldItem();
    	    	EnumFacing facing = EnumFacing.valueOf(axis.name());
    	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
    	            pos = pos.offset(facing);
    	            location.add(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
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
				            			data.setString("currentSubName", ((AItemSubTyped<?>) item).subName);
				            		}
		            			}
		            		}
		            		data.setDouble("rotation", player.getHeadYaw()%360);
		            		builderTile.tileEntity = ((IBlockTileEntity<TileEntityType>) block).createTileEntity(this, new Point3i(pos.getX(), pos.getY(), pos.getZ()), data);
		            		
		            	}
		            	//Send place event to block class, and also send initial update check.
		            	block.onPlaced(this, location, player);
		                stack.shrink(1);
		                return true;
		            }
	            }
    		}else{
    			IBlockState newState = wrapper.getDefaultState();
    			if(world.setBlockState(pos, newState, 11)){
    				if(block instanceof IBlockTileEntity){
    					BuilderTileEntity<TileEntityType> builderTile = (BuilderTileEntity<TileEntityType>) world.getTileEntity(pos);
    					builderTile.tileEntity = ((IBlockTileEntity<TileEntityType>) block).createTileEntity(this, new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperNBT());
    				}
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
	@Override
	public WrapperTileEntity getWrapperTileEntity(Point3i position){
		TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z));
		return tile != null ? new WrapperTileEntity(tile) : null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3i point){
		TileEntity tile = world.getTileEntity(new BlockPos(point.x, point.y, point.z));
		return tile instanceof BuilderTileEntity ? ((BuilderTileEntity<TileEntityType>) tile).tileEntity : null;
	}
	
	@Override
	public void markTileEntityChanged(Point3i point){
		world.getTileEntity(new BlockPos(point.x, point.y, point.z)).markDirty();
	}
	
	@Override
	public float getLightBrightness(Point3i point, boolean calculateBlock){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		float sunLight = world.getSunBrightness(0)*(world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted())/15F;
		float blockLight = calculateBlock ? world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos)/15F : 0.0F;
		return Math.max(sunLight, blockLight);
	}
	
	@Override
	public void updateLightBrightness(Point3i point){
		ATileEntityBase<?> tile = getTileEntity(point);
		if(tile != null){
			BlockPos pos = new BlockPos(point.x, point.y, point.z);
			//This needs to get fired manually as even if we update the blockstate the light value won't change
			//as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
			world.checkLight(pos);
		}
	}
	
	@Override
	public void destroyBlock(Point3i point){
		world.destroyBlock(new BlockPos(point.x, point.y, point.z), true);
	}
	
	@Override
	public boolean isAir(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		IBlockState state = world.getBlockState(pos); 
		Block block = state.getBlock();
		return block.isAir(state, world, pos);
	}
	
	@Override
	public boolean isFire(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		IBlockState state = world.getBlockState(pos); 
		return state.getMaterial().equals(Material.FIRE);
	}
	
	@Override
	public void setToFire(Point3i point){
		world.setBlockState(new BlockPos(point.x, point.y, point.z), Blocks.FIRE.getDefaultState());
	}
	
	@Override
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
	
	@Override
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
					for(ItemStack drop : drops){
						cropDrops.add(drop);
					}
				}else{
					for(ItemStack stack : drops){
						if(stack.getCount() > 0){
							world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, stack));
						}
					}
				}
			}
			return cropDrops;
		}
		return null;
	}
	
	@Override
	public boolean plantBlock(Point3i point, ItemStack stack){
		//Check for valid seeds.
		Item item = stack.getItem();
		if(item instanceof IPlantable){
			IPlantable plantable = (IPlantable) item;
			
			//Check if we have farmland below and air above.
			BlockPos farmlandPos = new BlockPos(point.x, point.y, point.z);
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
	
	@Override
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
		}else{
			return false;
		}
		
		world.setBlockState(pos, newState, 11);
		world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		return true;
	}
	
	@Override
	public void spawnItem(AItemBase item, WrapperNBT data, Point3d point){
		ItemStack stack = item.getNewStack();
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, stack));
	}
	
	@Override
	public void spawnItemStack(ItemStack stack, Point3d point){
		world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, stack));
	}
	
	@Override
	public void spawnExplosion(AEntityBase source, Point3d location, double strength, boolean flames){
		world.newExplosion(((WrapperEntity) source.wrapper).entity, location.x, location.y, location.z, (float) strength, flames, true);
	}
	
	@Override
	public void spawnExplosion(IWrapperEntity entity, Point3d location, double strength, boolean flames){
		world.newExplosion(((WrapperEntity) entity).entity, location.x, location.y, location.z, (float) strength, flames, true);
	}
	
	/**
	 *  Helper method to convert a BoundingBox to an AxisAlignedBB.
	 */
	private static AxisAlignedBB convertBox(BoundingBox box){
		return new AxisAlignedBB(
			box.globalCenter.x - box.widthRadius,
			box.globalCenter.y - box.heightRadius,
			box.globalCenter.z - box.depthRadius,
			box.globalCenter.x + box.widthRadius,
			box.globalCenter.y + box.heightRadius,
			box.globalCenter.z + box.depthRadius
		);
	}
}