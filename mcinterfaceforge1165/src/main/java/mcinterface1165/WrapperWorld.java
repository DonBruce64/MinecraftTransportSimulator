package mcinterface1165;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataRequest;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataUpdate;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.INPC;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Wrapper to a world instance.  This contains many common methods that
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  Note that
 * clients and servers don't share world interfaces, and there are world interfaces for
 * every loaded world, so multiple interfaces will always be present on a system.
 *
 * @author don_bruce
 */
public class WrapperWorld extends AWrapperWorld {
    private static final Map<World, WrapperWorld> worldWrappers = new HashMap<>();
    private final Map<UUID, BuilderEntityExisting> playerServerGunBuilders = new HashMap<>();
    private final Map<UUID, Integer> ticksSincePlayerJoin = new HashMap<>();
    private final List<AxisAlignedBB> mutableCollidingAABBs = new ArrayList<>();
    private final Set<BlockPos> knownAirBlocks = new HashSet<>();


    protected final World world;
    private final IWrapperNBT savedData;
    protected final Map<UUID, Entity> entitiesByUUID = new HashMap<>();

    /**
     * Returns a wrapper instance for the passed-in world instance.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     */
    public static WrapperWorld getWrapperFor(World world) {
        if (world != null) {
            WrapperWorld wrapper = worldWrappers.get(world);
            if (wrapper == null || world != wrapper.world) {
                wrapper = new WrapperWorld(world);
                worldWrappers.put(world, wrapper);
            }
            return wrapper;
        } else {
            return null;
        }
    }

    private WrapperWorld(World world) {
        this.world = world;
        if (world.isClientSide) {
            //Send packet to server to request data for this world.
            this.savedData = InterfaceManager.coreInterface.getNewNBTWrapper();
            InterfaceManager.packetInterface.sendToServer(new PacketWorldSavedDataRequest(InterfaceManager.clientInterface.getClientPlayer()));
        } else {
            //Load data from disk.
            try {
                if (getDataFile().exists()) {
                    this.savedData = new WrapperNBT(CompressedStreamTools.readCompressed(Files.newInputStream(getDataFile().toPath())));
                } else {
                    this.savedData = InterfaceManager.coreInterface.getNewNBTWrapper();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Could not load saved data from disk!  This will result in data loss if we continue!");
            }
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public boolean isClient() {
        return world.isClientSide;
    }

    @Override
    public long getTime() {
        return world.getDayTime() % 24000;
    }

    @Override
    public String getName() {
        return world.dimensionType().effectsLocation().getPath();
    }

    @Override
    public long getMaxHeight() {
        return world.getHeight();
    }

    @Override
    public void beginProfiling(String name, boolean subProfile) {
        if (subProfile) {
            world.getProfiler().push(name);
        } else {
            world.getProfiler().popPush(name);
        }
    }

    @Override
    public void endProfiling() {
        world.getProfiler().pop();
    }

    @Override
    public IWrapperNBT getData(String name) {
        if (name.isEmpty()) {
            return savedData;
        } else {
            return savedData.getData(name);
        }
    }

    @Override
    public void setData(String name, IWrapperNBT value) {
        savedData.setData(name, value);
        if (!isClient()) {
            try {
                CompressedStreamTools.writeCompressed(((WrapperNBT) savedData).tag, Files.newOutputStream(getDataFile().toPath()));
                InterfaceManager.packetInterface.sendToAllClients(new PacketWorldSavedDataUpdate(name, value));
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Could not save data to disk!  This will result in data loss if we continue!");
            }
        }
    }

    @Override
    public File getDataFile() {
        //Need to do reflection to get hidden field.  Stupid Mojang restrictions..
        //FD: net/minecraft/world/storage/DimensionSavedDataManager/field_215759_d net/minecraft/world/storage/DimensionSavedDataManager/dataFolder
        File dataFolder;
        for (Field field : DimensionSavedDataManager.class.getDeclaredFields()) {
            if (field.getName().equals("dataFolder") || field.getName().equals("field_215759_d")) {
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }

                    dataFolder = (File) field.get(((ServerWorld) world).getDataStorage());
                    return new File(dataFolder, "mtsdata.dat");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public WrapperEntity getExternalEntity(UUID entityID) {
        return WrapperEntity.getWrapperFor(entitiesByUUID.get(entityID));
    }

    @Override
    public List<IWrapperEntity> getEntitiesWithin(BoundingBox box) {
        List<IWrapperEntity> entities = new ArrayList<>();
        for (Entity entity : world.getEntitiesOfClass(Entity.class, WrapperWorld.convert(box))) {
            if (!(entity instanceof ABuilderEntityBase)) {
                entities.add(WrapperEntity.getWrapperFor(entity));
            }
        }
        return entities;
    }

    @Override
    public List<IWrapperEntity> getEntitiesHostile(IWrapperEntity lookingEntity, double radius) {
        List<IWrapperEntity> entities = new ArrayList<>();
        Entity mcLooker = ((WrapperEntity) lookingEntity).entity;
        for (Entity entity : world.getEntities(mcLooker, mcLooker.getBoundingBox().inflate(radius))) {
            if (entity instanceof IMob && entity.isAlive() && (!(entity instanceof LivingEntity) || ((LivingEntity) entity).deathTime == 0)) {
                entities.add(WrapperEntity.getWrapperFor(entity));
            }
        }
        return entities;
    }

    @Override
    public IWrapperEntity getEntityLookingAt(IWrapperEntity entityLooking, float searchDistance, boolean generalArea) {
        double smallestDistance = searchDistance * 2;
        Entity foundEntity = null;
        Entity mcLooker = ((WrapperEntity) entityLooking).entity;
        Vector3d raytraceStart = mcLooker.position().add(0, (entityLooking.getEyeHeight() + entityLooking.getSeatOffset()), 0);
        Point3D lookerLos = entityLooking.getLineOfSight(searchDistance);
        Vector3d raytraceEnd = new Vector3d(lookerLos.x, lookerLos.y, lookerLos.z).add(raytraceStart);
        for (Entity entity : world.getEntities(mcLooker, mcLooker.getBoundingBox().inflate(searchDistance))) {
            if (!(entity instanceof ABuilderEntityBase) && entity.canBeCollidedWith() && !entity.equals(mcLooker.getVehicle())) {
                float distance = mcLooker.distanceTo(entity);
                if (distance < smallestDistance) {
                    AxisAlignedBB testBox = generalArea ? entity.getBoundingBox().inflate(2) : entity.getBoundingBox();
                    Optional<Vector3d> rayHit = testBox.clip(raytraceStart, raytraceEnd);
                    if (rayHit != null) {
                        smallestDistance = distance;
                        foundEntity = entity;
                    }
                }
            }
        }
        return WrapperEntity.getWrapperFor(foundEntity);
    }

    @Override
    public void spawnEntity(AEntityB_Existing entity) {
        spawnEntityInternal(entity);
    }

    /**
     * Internal method to spawn entities and return their builders.
     */
    protected BuilderEntityExisting spawnEntityInternal(AEntityB_Existing entity) {
        BuilderEntityExisting builder = new BuilderEntityExisting(((WrapperWorld) entity.world).world);
        builder.loadedFromSavedNBT = true;
        builder.setPos(entity.position.x, entity.position.y, entity.position.z);
        builder.entity = entity;
        world.addFreshEntity(builder);
        addEntity(entity);
        return builder;
    }

    @Override
    public List<IWrapperEntity> attackEntities(Damage damage, Point3D motion, boolean generateList) {
        AxisAlignedBB mcBox = WrapperWorld.convert(damage.box);
        List<Entity> collidedEntities;

        //Get collided entities.
        if (motion != null) {
            mcBox = mcBox.inflate(motion.x, motion.y, motion.z);
        }
        collidedEntities = world.getEntitiesOfClass(Entity.class, mcBox);

        //Get variables.  If we aren't moving, we won't need these.
        Point3D startPoint;
        Point3D endPoint;
        Vector3d start = null;
        Vector3d end = null;
        List<IWrapperEntity> hitEntities = new ArrayList<>();

        if (motion != null) {
            startPoint = damage.box.globalCenter;
            endPoint = damage.box.globalCenter.copy().add(motion);
            start = new Vector3d(startPoint.x, startPoint.y, startPoint.z);
            end = new Vector3d(endPoint.x, endPoint.y, endPoint.z);
        }

        //Validate the collided entities to make sure we didn't hit something we shouldn't have.
        //Also get rayTrace hits for advanced checking.
        for (Entity mcEntityCollided : collidedEntities) {
            //Don't check internal entities, we do this in the main classes.
            if (!(mcEntityCollided instanceof ABuilderEntityBase)) {
                //If the damage came from a source, verify that source can hurt the entity.
                if (damage.damgeSource != null) {
                    Entity mcRidingEntity = mcEntityCollided.getVehicle();
                    if (mcRidingEntity instanceof BuilderEntityLinkedSeat) {
                        //Entity hit is riding something of ours.
                        //Verify that it's not the entity that is doing the attacking.
                        AEntityB_Existing internalRidingEntity = ((BuilderEntityLinkedSeat) mcRidingEntity).entity;
                        if (damage.damgeSource == internalRidingEntity) {
                            //Entity can't attack entities riding itself.
                            continue;
                        } else if (internalRidingEntity instanceof APart) {
                            //Attacked entity is riding a part, don't attack if a part on that multipart is the attacker,
                            APart ridingPart = (APart) internalRidingEntity;
                            if (ridingPart.masterEntity.allParts.contains(damage.damgeSource)) {
                                continue;
                            }
                        }
                    }
                }

                //Didn't hit a rider on the damage source. Do normal raytracing or just add if there's no motion.
                if (motion == null || mcEntityCollided.getBoundingBox().clip(start, end) != null) {
                    hitEntities.add(WrapperEntity.getWrapperFor(mcEntityCollided));
                }
            }
        }

        if (generateList) {
            return hitEntities;
        } else {
            for (IWrapperEntity entity : hitEntities) {
                entity.attack(damage);
            }
            return null;
        }
    }

    @Override
    public void loadEntities(BoundingBox box, AEntityE_Interactable<?> entityToLoad) {
        for (Entity entity : world.getEntitiesOfClass(Entity.class, WrapperWorld.convert(box))) {
            if (entity.getVehicle() == null && (entity instanceof INPC || entity instanceof AnimalEntity) && !(entity instanceof IMob)) {
                if (entityToLoad instanceof EntityVehicleF_Physics) {
                    for (APart part : ((EntityVehicleF_Physics) entityToLoad).allParts) {
                        if (part instanceof PartSeat && part.rider == null && !part.placementDefinition.isController) {
                            part.setRider(new WrapperEntity(entity), true);
                            break;
                        }
                    }
                } else {
                    if (entityToLoad.rider == null) {
                        entityToLoad.setRider(new WrapperEntity(entity), true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public ABlockBase getBlock(Point3D position) {
        Block block = world.getBlockState(new BlockPos(position.x, position.y, position.z)).getBlock();
        return block instanceof BuilderBlock ? ((BuilderBlock) block).block : null;
    }

    @Override
    public float getBlockHardness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBlockState(pos).getDestroySpeed(world, pos);
    }

    @Override
    public float getBlockSlipperiness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBlockState(pos).getSlipperiness(world, pos, null);
    }

    @Override
    public BlockMaterial getBlockMaterial(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        Material material = world.getBlockState(pos).getMaterial();
        if (material == Material.DIRT || material == Material.GRASS) {
            return world.isRainingAt(pos.above()) ? BlockMaterial.DIRT_WET : BlockMaterial.DIRT;
        } else if (material == Material.SAND) {
            return world.isRainingAt(pos.above()) ? BlockMaterial.SAND_WET : BlockMaterial.SAND;
        } else if (material == Material.SNOW || material == Material.TOP_SNOW) {
            return BlockMaterial.SNOW;
        } else if (material == Material.ICE || material == Material.ICE_SOLID) {
            return BlockMaterial.ICE;
        } else {
            return world.isRainingAt(pos.above()) ? BlockMaterial.NORMAL_WET : BlockMaterial.NORMAL;
        }
    }

    @Override
    public List<IWrapperItemStack> getBlockDrops(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, (ServerWorld) world, pos, world.getBlockEntity(pos));
        List<IWrapperItemStack> convertedList = new ArrayList<>();
        for (ItemStack stack : drops) {
            convertedList.add(new WrapperItemStack(stack.copy()));
        }
        return convertedList;
    }

    @Override
    public BlockHitResult getBlockHit(Point3D position, Point3D delta) {
        Vector3d start = new Vector3d(position.x, position.y, position.z);
        BlockRayTraceResult trace = world.clip(new RayTraceContext(start, start.add(delta.x, delta.y, delta.z), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, null));
        if (trace != null) {
            BlockPos pos = trace.getBlockPos();
            if (pos != null) {
                return new BlockHitResult(new Point3D(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(trace.getDirection().name()));
            }
        }
        return null;
    }

    @Override
    public boolean isBlockSolid(Point3D position, Axis axis) {
        if (axis.blockBased) {
            BlockPos pos = new BlockPos(position.x, position.y, position.z);
            BlockState state = world.getBlockState(pos);
            Block offsetMCBlock = state.getBlock();
            Direction facing = Direction.valueOf(axis.name());
            return offsetMCBlock != null && !offsetMCBlock.equals(Blocks.BARRIER) && state.isFaceSturdy(world, pos, facing);
        } else {
            return false;
        }
    }

    @Override
    public boolean isBlockLiquid(Point3D position) {
        return world.getBlockState(new BlockPos(position.x, position.y, position.z)).getMaterial().isLiquid();
    }

    @Override
    public boolean isBlockBelowBottomSlab(Point3D position) {
        BlockState state = world.getBlockState(new BlockPos(position.x, position.y - 1, position.z));
        return state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    @Override
    public boolean isBlockAboveTopSlab(Point3D position) {
        BlockState state = world.getBlockState(new BlockPos(position.x, position.y + 1, position.z));
        return state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.TOP;
    }

    @Override
    public double getHeight(Point3D position) {
        return position.y - world.getBlockFloorHeight(new BlockPos(position.x, 0, position.z));
    }

    @Override
    public void updateBoundingBoxCollisions(BoundingBox box, Point3D collisionMotion, boolean ignoreIfGreater) {
        AxisAlignedBB mcBox = WrapperWorld.convert(box);
        VoxelShape mcShape = VoxelShapes.create(mcBox);
        box.collidingBlockPositions.clear();
        mutableCollidingAABBs.clear();
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (!world.isEmptyBlock(pos)) {
                        BlockState state = world.getBlockState(pos);
                        VoxelShape collisionShape = state.getCollisionShape(world, pos);
                        if (collisionShape != null && !collisionShape.isEmpty() && VoxelShapes.joinIsNotEmpty(mcShape, collisionShape, IBooleanFunction.AND)) {
                            mutableCollidingAABBs.addAll(collisionShape.toAabbs());
                            box.collidingBlockPositions.add(new Point3D(i, j, k));
                        }
                        if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                            mutableCollidingAABBs.add(collisionShape.bounds().move(pos));
                            box.collidingBlockPositions.add(new Point3D(i, j, k));
                        }
                    }
                }
            }
        }

        //If we are in the depth bounds for this collision, set it as the collision depth.
        box.currentCollisionDepth.set(0D, 0D, 0D);
        double boxCollisionDepth;
        double minDelta = 0.0;
        for (AxisAlignedBB colBox : mutableCollidingAABBs) {
            if (collisionMotion.x > 0) {
                boxCollisionDepth = mcBox.maxX - colBox.minX;
                if (!ignoreIfGreater || collisionMotion.x - boxCollisionDepth > -minDelta) {
                    box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
                }
            } else if (collisionMotion.x < 0) {
                boxCollisionDepth = colBox.maxX - mcBox.minX;
                if (!ignoreIfGreater || collisionMotion.x + boxCollisionDepth < minDelta) {
                    box.currentCollisionDepth.x = Math.max(box.currentCollisionDepth.x, boxCollisionDepth);
                }
            }
            if (collisionMotion.y > 0) {
                boxCollisionDepth = mcBox.maxY - colBox.minY;
                if (!ignoreIfGreater || collisionMotion.y - boxCollisionDepth > -minDelta) {
                    box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
                }
            } else if (collisionMotion.y < 0) {
                boxCollisionDepth = colBox.maxY - mcBox.minY;
                if (!ignoreIfGreater || collisionMotion.y + boxCollisionDepth < minDelta) {
                    box.currentCollisionDepth.y = Math.max(box.currentCollisionDepth.y, boxCollisionDepth);
                }
            }
            if (collisionMotion.z > 0) {
                boxCollisionDepth = mcBox.maxZ - colBox.minZ;
                if (!ignoreIfGreater || collisionMotion.z - boxCollisionDepth > -minDelta) {
                    box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
                }
            } else if (collisionMotion.z < 0) {
                boxCollisionDepth = colBox.maxZ - mcBox.minZ;
                if (!ignoreIfGreater || collisionMotion.z + boxCollisionDepth < minDelta) {
                    box.currentCollisionDepth.z = Math.max(box.currentCollisionDepth.z, boxCollisionDepth);
                }
            }
        }
        if (box.currentCollisionDepth.isZero()) {
            box.collidingBlockPositions.clear();
        }
    }

    @Override
    public boolean checkForCollisions(BoundingBox box, Point3D offset, boolean clearCache) {
        if (clearCache) {
            knownAirBlocks.clear();
        }
        mutableCollidingAABBs.clear();
        AxisAlignedBB mcBox = WrapperWorld.convertWithOffset(box, offset.x, offset.y, offset.z);
        VoxelShape mcShape = VoxelShapes.create(mcBox);
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (!knownAirBlocks.contains(pos)) {
                        if (world.isLoaded(pos)) {
                            BlockState state = world.getBlockState(pos);
                            VoxelShape collisionShape = state.getCollisionShape(world, pos);
                            if (collisionShape != null && !collisionShape.isEmpty() && VoxelShapes.joinIsNotEmpty(mcShape, collisionShape, IBooleanFunction.AND)) {
                                return true;
                            } else {
                                knownAirBlocks.add(pos);
                            }
                            if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                                if (mcBox.intersects(collisionShape.bounds().move(pos))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int getRedstonePower(Point3D position) {
        return world.getBestNeighborSignal(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public float getRainStrength(Point3D position) {
        return world.isRainingAt(new BlockPos(position.x, position.y + 1, position.z)) ? world.getRainLevel(1.0F) + world.getThunderLevel(1.0F) : 0.0F;
    }

    @Override
    public float getTemperature(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBiome(pos).getTemperature(pos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONMultiModelProvider> boolean setBlock(ABlockBase block, Point3D position, IWrapperPlayer playerWrapper, Axis axis) {
        if (!world.isClientSide) {
            BuilderBlock wrapper = BuilderBlock.blockMap.get(block);
            BlockPos pos = new BlockPos(position.x, position.y, position.z);
            if (playerWrapper != null) {
                PlayerEntity mcPlayer = ((WrapperPlayer) playerWrapper).player;
                WrapperItemStack stack = (WrapperItemStack) playerWrapper.getHeldStack();
                AItemBase item = stack.getItem();
                Direction facing = Direction.valueOf(axis.name());
                if (!world.isEmptyBlock(pos)) {
                    pos = pos.relative(facing);
                    position.add(facing.getStepX(), facing.getStepY(), facing.getStepZ());
                }

                if (item != null && world.mayInteract(mcPlayer, pos) && world.isEmptyBlock(pos)) {
                    BlockState newState = wrapper.defaultBlockState();
                    if (world.setBlock(pos, newState, 11)) {
                        //Block is set.  See if we need to set TE data.
                        if (block instanceof ABlockBaseTileEntity) {
                            BuilderTileEntity builderTile = (BuilderTileEntity) world.getBlockEntity(pos);
                            IWrapperNBT data = stack.getData();
                            if (item instanceof AItemPack) {
                                ((AItemPack<JSONDefinition>) item).populateDefaultData(data);
                            }
                            builderTile.setTileEntity(((ABlockBaseTileEntity) block).createTileEntity(this, position, playerWrapper, data));
                            addEntity(builderTile.tileEntity);
                        }
                        //Shrink stack as we placed this block.
                        stack.add(-1);
                        return true;
                    }
                }
            } else {
                BlockState newState = wrapper.defaultBlockState();
                return world.setBlock(pos, newState, 11);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3D position) {
        TileEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z));
        return tile instanceof BuilderTileEntity ? (TileEntityType) ((BuilderTileEntity) tile).tileEntity : null;
    }

    @Override
    public void markTileEntityChanged(Point3D position) {
        world.getBlockEntity(new BlockPos(position.x, position.y, position.z)).setChanged();
    }

    @Override
    public float getLightBrightness(Point3D position, boolean calculateBlock) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        float sunLight = (world.getBrightness(LightType.SKY, pos) - world.getSkyDarken()) / 15F;
        float blockLight = calculateBlock ? world.getBrightness(LightType.BLOCK, pos) / 15F : 0.0F;
        return Math.max(sunLight, blockLight);
    }

    @Override
    public void updateLightBrightness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //This needs to get fired manually as even if we update the blockstate the light value won't change
        //as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
        world.getLightEngine().checkBlock(pos);
    }

    @Override
    public void destroyBlock(Point3D position, boolean spawnDrops) {
        world.destroyBlock(new BlockPos(position.x, position.y, position.z), spawnDrops);
    }

    @Override
    public boolean isAir(Point3D position) {
        return world.isEmptyBlock(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public boolean isFire(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        return state.getMaterial().equals(Material.FIRE);
    }

    @Override
    public void setToFire(BlockHitResult hitResult) {
        BlockPos blockpos = new BlockPos(hitResult.position.x, hitResult.position.y, hitResult.position.z).relative(Direction.valueOf(hitResult.side.name()));
        if (isAir(hitResult.position)) {
            world.setBlockAndUpdate(blockpos, Blocks.FIRE.defaultBlockState());
        }
    }

    @Override
    public void extinguish(BlockHitResult hitResult) {
        BlockPos blockpos = new BlockPos(hitResult.position.x, hitResult.position.y, hitResult.position.z).relative(Direction.valueOf(hitResult.side.name()));
        if (world.getBlockState(blockpos).is(BlockTags.FIRE)) {
            world.removeBlock(blockpos, false);
        }
    }

    @Override
    public boolean fertilizeBlock(Point3D position, IWrapperItemStack stack) {
        //Check if the item can fertilize things and we are on the server.
        ItemStack mcStack = ((WrapperItemStack) stack).stack;
        if (mcStack.getItem() == Items.BONE_MEAL && !world.isClientSide) {
            //Check if we are in crops.
            BlockPos cropPos = new BlockPos(position.x, position.y, position.z);
            BlockState cropState = world.getBlockState(cropPos);
            Block cropBlock = cropState.getBlock();
            if (cropBlock instanceof IGrowable) {
                IGrowable growable = (IGrowable) cropState.getBlock();
                if (growable.isValidBonemealTarget(world, cropPos, cropState, world.isClientSide)) {
                    growable.performBonemeal((ServerWorld) world, world.random, cropPos, cropState);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<IWrapperItemStack> harvestBlock(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        List<IWrapperItemStack> cropDrops = new ArrayList<>();
        if ((state.getBlock() instanceof CropsBlock && ((CropsBlock) state.getBlock()).isMaxAge(state)) || state.getBlock() instanceof BushBlock) {
            Block harvestedBlock = state.getBlock();
            world.playSound(null, pos, harvestedBlock.getSoundType(state, world, pos, null).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);

            //Only return drops on servers.  Clients don't do items.
            if (!world.isClientSide) {
                List<ItemStack> drops = Block.getDrops(state, (ServerWorld) world, pos, world.getBlockEntity(pos));
                world.removeBlock(pos, false);
                if (harvestedBlock instanceof CropsBlock) {
                    for (ItemStack drop : drops) {
                        cropDrops.add(new WrapperItemStack(drop.copy()));
                    }
                } else {
                    for (ItemStack stack : drops) {
                        if (stack.getCount() > 0) {
                            world.addFreshEntity(new ItemEntity(world, position.x, position.y, position.z, stack));
                        }
                    }
                }
            }
        }
        return cropDrops;
    }

    @Override
    public boolean plantBlock(Point3D position, IWrapperItemStack stack) {
        //Check for valid seeds.
        Item item = ((WrapperItemStack) stack).stack.getItem();
        if (item instanceof IPlantable) {
            IPlantable plantable = (IPlantable) item;

            //Check if we have farmland below and air above.
            BlockPos farmlandPos = new BlockPos(position.x, position.y, position.z);
            BlockState farmlandState = world.getBlockState(farmlandPos);
            Block farmlandBlock = farmlandState.getBlock();
            if (farmlandBlock instanceof FarmlandBlock) {
                BlockPos cropPos = farmlandPos.above();
                if (world.isEmptyBlock(cropPos)) {
                    //Check to make sure the block can sustain the plant we want to plant.
                    BlockState plantState = plantable.getPlant(world, cropPos);
                    if (farmlandBlock.canSustainPlant(plantState, world, farmlandPos, Direction.UP, plantable)) {
                        world.setBlock(cropPos, plantState, 11);
                        world.playSound(null, farmlandPos, plantState.getBlock().getSoundType(plantState, world, farmlandPos, null).getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean plowBlock(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState oldState = world.getBlockState(pos);
        BlockState newState;
        Block block = oldState.getBlock();
        if (block == Blocks.GRASS || block == Blocks.GRASS_PATH || block == Blocks.DIRT) {
            newState = Blocks.FARMLAND.defaultBlockState();
        } else if (block.equals(Blocks.COARSE_DIRT)) {
            newState = Blocks.DIRT.defaultBlockState();
        } else {
            return false;
        }

        world.setBlock(pos, newState, 11);
        world.playSound(null, pos, SoundEvents.HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void removeSnow(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        if (state.getMaterial().equals(Material.SNOW) || state.getMaterial().equals(Material.TOP_SNOW)) {
            world.removeBlock(pos, false);
            world.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean insertStack(Point3D position, Axis axis, IWrapperItemStack stack) {
        Direction facing = Direction.valueOf(axis.name());
        TileEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z).relative(facing));
        if (tile != null) {
            IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null);
            if (itemHandler != null) {
                for (int i = 0; i < itemHandler.getSlots(); ++i) {
                    ItemStack remainingStack = itemHandler.insertItem(i, ((WrapperItemStack) stack).stack, true);
                    if (remainingStack.getCount() < stack.getSize()) {
                        IWrapperItemStack stackToInsert = stack.split(1);
                        itemHandler.insertItem(i, ((WrapperItemStack) stackToInsert).stack, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public WrapperItemStack extractStack(Point3D position, Axis axis) {
        Direction facing = Direction.valueOf(axis.name());
        TileEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z).relative(facing));
        if (tile != null) {
            IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null);
            if (itemHandler != null) {
                for (int i = 0; i < itemHandler.getSlots(); ++i) {
                    ItemStack extractedStack = itemHandler.extractItem(i, 1, false);
                    if (!extractedStack.isEmpty()) {
                        return new WrapperItemStack(extractedStack);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void spawnItem(AItemBase item, IWrapperNBT data, Point3D point) {
        //Spawn 1 block above in case we're right on a block.
        world.addFreshEntity(new ItemEntity(world, point.x, point.y + 1, point.z, ((WrapperItemStack) item.getNewStack(data)).stack));
    }

    @Override
    public void spawnItemStack(IWrapperItemStack stack, Point3D point) {
        world.addFreshEntity(new ItemEntity(world, point.x, point.y, point.z, ((WrapperItemStack) stack).stack));
    }

    @Override
    public void spawnExplosion(Point3D location, double strength, boolean flames) {
        world.explode(null, location.x, location.y, location.z, (float) strength, flames, ConfigSystem.settings.general.blockBreakage.value ? Explosion.Mode.DESTROY : Explosion.Mode.NONE);
    }

    /**
     * Helper method to convert a BoundingBox to an AxisAlignedBB.
     */
    public static AxisAlignedBB convert(BoundingBox box) {
        return new AxisAlignedBB(box.globalCenter.x - box.widthRadius, box.globalCenter.y - box.heightRadius, box.globalCenter.z - box.depthRadius, box.globalCenter.x + box.widthRadius, box.globalCenter.y + box.heightRadius, box.globalCenter.z + box.depthRadius);
    }

    /**
     * Helper method to convert the BoundingBox to an AxisAlignedBB.
     * This method allows for an offset to the conversion, to prevent
     * creating two AABBs (the conversion and the offset box).
     */
    public static AxisAlignedBB convertWithOffset(BoundingBox box, double x, double y, double z) {
        return new AxisAlignedBB(x + box.globalCenter.x - box.widthRadius, y + box.globalCenter.y - box.heightRadius, z + box.globalCenter.z - box.depthRadius, x + box.globalCenter.x + box.widthRadius, y + box.globalCenter.y + box.heightRadius, z + box.globalCenter.z + box.depthRadius);
    }

    /**
     * Checks for joined and left entities to ensure we maintain a map of them for lookups.
     */
    @SubscribeEvent
    public void on(EntityJoinWorldEvent event) {
        //Need to check if it's our world, because Forge is stupid like that.
        if (event.getWorld() == world) {
            entitiesByUUID.put(event.getEntity().getUUID(), event.getEntity());
        }
    }

    @SubscribeEvent
    public void on(EntityLeaveWorldEvent event) {
        //Need to check if it's our world, because Forge is stupid like that.
        if (event.getWorld() == world) {
            entitiesByUUID.remove(event.getEntity().getUUID());
        }
    }

    /**
     * Spawn "follower" entities for the player if they don't exist already.
     * This only happens 3 seconds after the player joins.
     * This delay is done to ensure all chunks are loaded before spawning any followers.
     * We also track followers, and ensure that if the player doesn't exist, they are removed.
     * This handles players leaving.  We could use events for this, but they're not reliable.
     */
    @SubscribeEvent
    public void on(TickEvent.WorldTickEvent event) {
        //Need to check if it's our world, because Forge is stupid like that.
        //Note that the client world never calls this method: to do client ticks we need to use the client interface.
        if (!event.world.isClientSide && event.world.equals(world) && event.phase.equals(Phase.END)) {
            for (PlayerEntity player : event.world.players()) {
                UUID playerUUID = player.getUUID();
                BuilderEntityExisting gunBuilder = playerServerGunBuilders.get(playerUUID);
                if (gunBuilder != null) {
                    //Gun exists, check if world is the same and it is actually updating.
                    //We check basic states, and then the watchdog bit that gets reset every tick.
                    //This way if we're in the world, but not valid we will know.
                    if (gunBuilder.level != player.level || !player.isAlive() || !gunBuilder.entity.isValid || gunBuilder.idleTickCounter == 20) {
                        //Follower is not linked.  Remove it and re-create in code below.
                        gunBuilder.setDead();
                        playerServerGunBuilders.remove(playerUUID);
                        ticksSincePlayerJoin.remove(playerUUID);
                    } else {
                        ++gunBuilder.idleTickCounter;
                    }
                } else if (player.isAlive()) {
                    //Gun does not exist, check if player has been present for 3 seconds and spawn it.
                    int totalTicksWaited = 0;
                    if (ticksSincePlayerJoin.containsKey(playerUUID)) {
                        totalTicksWaited = ticksSincePlayerJoin.get(playerUUID);
                    }
                    if (++totalTicksWaited == 60) {
                        //Spawn gun.
                        IWrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(player);
                        IWrapperNBT newData = InterfaceManager.coreInterface.getNewNBTWrapper();
                        EntityPlayerGun entity = new EntityPlayerGun(this, playerWrapper, newData);
                        playerServerGunBuilders.put(playerUUID, spawnEntityInternal(entity));
                        entity.addPartsPostAddition(playerWrapper, newData);

                        //If the player is new, also add handbooks.
                        if (ConfigSystem.settings.general.giveManualsOnJoin.value && !ConfigSystem.settings.general.joinedPlayers.value.contains(playerUUID)) {
                            playerWrapper.getInventory().addStack(PackParser.getItem("mts", "handbook_car").getNewStack(null));
                            playerWrapper.getInventory().addStack(PackParser.getItem("mts", "handbook_plane").getNewStack(null));
                            ConfigSystem.settings.general.joinedPlayers.value.add(playerUUID);
                            ConfigSystem.saveToDisk();
                        }
                    } else {
                        ticksSincePlayerJoin.put(playerUUID, totalTicksWaited);
                    }
                }
            }

            //Update bullets.
            beginProfiling("MTS_BulletUpdates", true);
            for (EntityBullet bullet : getEntitiesOfType(EntityBullet.class)) {
                bullet.update();
            }
        }
    }

    /**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     * Also remove this wrapper from the created lists, as it's invalid.
     */
    @SubscribeEvent
    public void on(WorldEvent.Unload event) {
        //Need to check if it's our world, because Forge is stupid like that.
        if (event.getWorld() == world) {
            for (AEntityA_Base entity : allEntities) {
                entity.remove();
            }
            worldWrappers.remove(world);
        }
    }
}