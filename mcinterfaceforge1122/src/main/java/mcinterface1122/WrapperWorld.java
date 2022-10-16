package mcinterface1122;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
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
        if (world.isRemote) {
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
        return world.isRemote;
    }

    @Override
    public long getTime() {
        return world.getWorldTime() % 24000;
    }

    @Override
    public String getName() {
        return world.provider.getDimensionType().getName();
    }

    @Override
    public long getMaxHeight() {
        return world.getHeight();
    }

    @Override
    public void beginProfiling(String name, boolean subProfile) {
        if (subProfile) {
            world.profiler.startSection(name);
        } else {
            world.profiler.endStartSection(name);
        }
    }

    @Override
    public void endProfiling() {
        world.profiler.endSection();
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
        return new File(world.getSaveHandler().getWorldDirectory(), "mtsdata.dat");
    }

    @Override
    public WrapperEntity getExternalEntity(UUID entityID) {
        for (Entity entity : world.loadedEntityList) {
            if (entity.getUniqueID().equals(entityID)) {
                return WrapperEntity.getWrapperFor(entity);
            }
        }
        return null;
    }

    @Override
    public List<IWrapperEntity> getEntitiesWithin(BoundingBox box) {
        List<IWrapperEntity> entities = new ArrayList<>();
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, WrapperWorld.convert(box))) {
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
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(mcLooker, mcLooker.getEntityBoundingBox().grow(radius))) {
            if (entity instanceof IMob && !entity.isDead && (!(entity instanceof EntityLivingBase) || ((EntityLivingBase) entity).deathTime == 0)) {
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
        Vec3d raytraceStart = mcLooker.getPositionVector().add(0, (entityLooking.getEyeHeight() + entityLooking.getSeatOffset()), 0);
        Point3D lookerLos = entityLooking.getLineOfSight(searchDistance);
        Vec3d raytraceEnd = new Vec3d(lookerLos.x, lookerLos.y, lookerLos.z).add(raytraceStart);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(mcLooker, mcLooker.getEntityBoundingBox().grow(searchDistance))) {
            if (!(entity instanceof ABuilderEntityBase) && entity.canBeCollidedWith() && !entity.equals(mcLooker.getRidingEntity())) {
                float distance = mcLooker.getDistance(entity);
                if (distance < smallestDistance) {
                    AxisAlignedBB testBox = generalArea ? entity.getEntityBoundingBox().grow(2) : entity.getEntityBoundingBox();
                    RayTraceResult rayTrace = testBox.calculateIntercept(raytraceStart, raytraceEnd);
                    if (rayTrace != null) {
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
        builder.setPositionAndRotation(entity.position.x, entity.position.y, entity.position.z, 0, 0);
        builder.entity = entity;
        world.spawnEntity(builder);
        addEntity(entity);
        return builder;
    }

    @Override
    public List<IWrapperEntity> attackEntities(Damage damage, Point3D motion, boolean generateList) {
        AxisAlignedBB mcBox = WrapperWorld.convert(damage.box);
        List<Entity> collidedEntities;

        //Get collided entities.
        if (motion != null) {
            mcBox = mcBox.expand(motion.x, motion.y, motion.z);
        }
        collidedEntities = world.getEntitiesWithinAABB(Entity.class, mcBox);

        //Get variables.  If we aren't moving, we won't need these.
        Point3D startPoint;
        Point3D endPoint;
        Vec3d start = null;
        Vec3d end = null;
        List<IWrapperEntity> hitEntities = new ArrayList<>();

        if (motion != null) {
            startPoint = damage.box.globalCenter;
            endPoint = damage.box.globalCenter.copy().add(motion);
            start = new Vec3d(startPoint.x, startPoint.y, startPoint.z);
            end = new Vec3d(endPoint.x, endPoint.y, endPoint.z);
        }

        //Validate the collided entities to make sure we didn't hit something we shouldn't have.
        //Also get rayTrace hits for advanced checking.
        for (Entity mcEntityCollided : collidedEntities) {
            //Don't check internal entities, we do this in the main classes.
            if (!(mcEntityCollided instanceof ABuilderEntityBase)) {
                //If the damage came from a source, verify that source can hurt the entity.
                if (damage.damgeSource != null) {
                    Entity mcRidingEntity = mcEntityCollided.getRidingEntity();
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
                if (motion == null || mcEntityCollided.getEntityBoundingBox().calculateIntercept(start, end) != null) {
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
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, WrapperWorld.convert(box))) {
            if (!entity.isRiding() && (entity instanceof INpc || entity instanceof EntityCreature) && !(entity instanceof IMob)) {
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
        return world.getBlockState(pos).getBlockHardness(world, pos);
    }

    @Override
    public float getBlockSlipperiness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().getSlipperiness(state, world, pos, null);
    }

    @Override
    public BlockMaterial getBlockMaterial(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        Material material = world.getBlockState(pos).getMaterial();
        if (material.equals(Material.GROUND) || material.equals(Material.GRASS)) {
            return world.isRainingAt(pos.up()) ? BlockMaterial.DIRT_WET : BlockMaterial.DIRT;
        } else if (material.equals(Material.SAND)) {
            return world.isRainingAt(pos.up()) ? BlockMaterial.SAND_WET : BlockMaterial.SAND;
        } else if (material.equals(Material.SNOW) || material.equals(Material.CRAFTED_SNOW)) {
            return BlockMaterial.SNOW;
        } else if (material.equals(Material.ICE) || material.equals(Material.PACKED_ICE)) {
            return BlockMaterial.ICE;
        } else {
            return world.isRainingAt(pos.up()) ? BlockMaterial.NORMAL_WET : BlockMaterial.NORMAL;
        }
    }

    @Override
    public List<IWrapperItemStack> getBlockDrops(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        NonNullList<ItemStack> drops = NonNullList.create();
        state.getBlock().getDrops(drops, world, pos, state, 0);
        List<IWrapperItemStack> convertedList = new ArrayList<>();
        for (ItemStack stack : drops) {
            convertedList.add(new WrapperItemStack(stack.copy()));
        }
        return convertedList;
    }

    @Override
    public BlockHitResult getBlockHit(Point3D position, Point3D delta) {
        Vec3d start = new Vec3d(position.x, position.y, position.z);
        RayTraceResult trace = world.rayTraceBlocks(start, start.add(delta.x, delta.y, delta.z), false, true, false);
        if (trace != null) {
            BlockPos pos = trace.getBlockPos();
            if (pos != null) {
                return new BlockHitResult(new Point3D(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(trace.sideHit.name()));
            }
        }
        return null;
    }

    @Override
    public boolean isBlockSolid(Point3D position, Axis axis) {
        if (axis.blockBased) {
            BlockPos pos = new BlockPos(position.x, position.y, position.z);
            IBlockState state = world.getBlockState(pos);
            Block offsetMCBlock = state.getBlock();
            EnumFacing facing = EnumFacing.valueOf(axis.name());
            return offsetMCBlock != null && !offsetMCBlock.equals(Blocks.BARRIER) && state.isSideSolid(world, pos, facing);
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
        IBlockState state = world.getBlockState(new BlockPos(position.x, position.y - 1, position.z));
        Block block = state.getBlock();
        return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
    }

    @Override
    public boolean isBlockAboveTopSlab(Point3D position) {
        IBlockState state = world.getBlockState(new BlockPos(position.x, position.y + 1, position.z));
        Block block = state.getBlock();
        return block instanceof BlockSlab && !((BlockSlab) block).isDouble() && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP;
    }

    @Override
    public double getHeight(Point3D position) {
        return position.y - world.getHeight(new BlockPos(position.x, 0, position.z)).getY();
    }

    @Override
    public void updateBoundingBoxCollisions(BoundingBox box, Point3D collisionMotion, boolean ignoreIfGreater) {
        AxisAlignedBB mcBox = WrapperWorld.convert(box);
        box.collidingBlockPositions.clear();
        mutableCollidingAABBs.clear();
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (world.isBlockLoaded(pos)) {
                        IBlockState state = world.getBlockState(pos);
                        if (state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null) {
                            int oldCollidingBlockCount = mutableCollidingAABBs.size();
                            state.addCollisionBoxToList(world, pos, mcBox, mutableCollidingAABBs, null, false);
                            if (mutableCollidingAABBs.size() > oldCollidingBlockCount) {
                                box.collidingBlockPositions.add(new Point3D(i, j, k));
                            }
                        }
                        if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                            mutableCollidingAABBs.add(state.getBoundingBox(world, pos).offset(pos));
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
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (!knownAirBlocks.contains(pos)) {
                        if (world.isBlockLoaded(pos)) {
                            IBlockState state = world.getBlockState(pos);
                            if (state.getBlock().canCollideCheck(state, false) && state.getCollisionBoundingBox(world, pos) != null) {
                                int oldCollidingBlockCount = mutableCollidingAABBs.size();
                                state.addCollisionBoxToList(world, pos, mcBox, mutableCollidingAABBs, null, false);
                                if (mutableCollidingAABBs.size() > oldCollidingBlockCount) {
                                    return true;
                                }
                            } else {
                                knownAirBlocks.add(pos);
                            }
                            if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                                if (mcBox.intersects(state.getBoundingBox(world, pos).offset(pos))) {
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
        return world.getRedstonePowerFromNeighbors(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public float getRainStrength(Point3D position) {
        return world.isRainingAt(new BlockPos(position.x, position.y + 1, position.z)) ? world.getRainStrength(1.0F) + world.getThunderStrength(1.0F) : 0.0F;
    }

    @Override
    public float getTemperature(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBiome(pos).getTemperature(pos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONMultiModelProvider> boolean setBlock(ABlockBase block, Point3D position, IWrapperPlayer playerWrapper, Axis axis) {
        if (!world.isRemote) {
            BuilderBlock wrapper = BuilderBlock.blockMap.get(block);
            BlockPos pos = new BlockPos(position.x, position.y, position.z);
            if (playerWrapper != null) {
                EntityPlayer mcPayer = ((WrapperPlayer) playerWrapper).player;
                WrapperItemStack stack = (WrapperItemStack) playerWrapper.getHeldStack();
                AItemBase item = stack.getItem();
                EnumFacing facing = EnumFacing.valueOf(axis.name());
                if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
                    pos = pos.offset(facing);
                    position.add(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
                }

                if (item != null && mcPayer.canPlayerEdit(pos, facing, stack.stack) && world.mayPlace(wrapper, pos, false, facing, null)) {
                    IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, mcPayer, EnumHand.MAIN_HAND);
                    if (world.setBlockState(pos, newState, 11)) {
                        //Block is set.  See if we need to set TE data.
                        if (block instanceof ABlockBaseTileEntity) {
                            BuilderTileEntity<TileEntityType> builderTile = (BuilderTileEntity<TileEntityType>) world.getTileEntity(pos);
                            IWrapperNBT data = stack.getData();
                            if (item instanceof AItemPack) {
                                ((AItemPack<JSONDefinition>) item).populateDefaultData(data);
                            }
                            builderTile.tileEntity = (TileEntityType) ((ABlockBaseTileEntity) block).createTileEntity(this, position, playerWrapper, data);
                            addEntity(builderTile.tileEntity);
                        }
                        //Shrink stack as we placed this block.
                        stack.add(-1);
                        return true;
                    }
                }
            } else {
                IBlockState newState = wrapper.getDefaultState();
                return world.setBlockState(pos, newState, 11);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3D position) {
        TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z));
        return tile instanceof BuilderTileEntity ? ((BuilderTileEntity<TileEntityType>) tile).tileEntity : null;
    }

    @Override
    public void markTileEntityChanged(Point3D position) {
        world.getTileEntity(new BlockPos(position.x, position.y, position.z)).markDirty();
    }

    @Override
    public float getLightBrightness(Point3D position, boolean calculateBlock) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        float sunLight = world.getSunBrightness(0) * (world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted()) / 15F;
        float blockLight = calculateBlock ? world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos) / 15F : 0.0F;
        return Math.max(sunLight, blockLight);
    }

    @Override
    public void updateLightBrightness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //This needs to get fired manually as even if we update the blockstate the light value won't change
        //as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
        world.checkLight(pos);
    }

    @Override
    public void destroyBlock(Point3D position, boolean spawnDrops) {
        world.destroyBlock(new BlockPos(position.x, position.y, position.z), spawnDrops);
    }

    @Override
    public boolean isAir(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block.isAir(state, world, pos);
    }

    @Override
    public boolean isFire(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial().equals(Material.FIRE);
    }

    @Override
    public void setToFire(BlockHitResult hitResult) {
        BlockPos blockpos = new BlockPos(hitResult.position.x, hitResult.position.y, hitResult.position.z).offset(EnumFacing.valueOf(hitResult.side.name()));
        if (world.isAirBlock(blockpos)) {
            world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());
        }
    }

    @Override
    public void extinguish(BlockHitResult hitResult) {
        world.extinguishFire(null, new BlockPos(hitResult.position.x, hitResult.position.y, hitResult.position.z), EnumFacing.valueOf(hitResult.side.name()));
    }

    @Override
    public boolean fertilizeBlock(Point3D position, IWrapperItemStack stack) {
        //Check if the item can fertilize things and we are on the server.
        ItemStack mcStack = ((WrapperItemStack) stack).stack;
        if (mcStack.getItem() == Items.DYE && !world.isRemote) {
            //Check if we are in crops.
            BlockPos cropPos = new BlockPos(position.x, position.y, position.z);
            IBlockState cropState = world.getBlockState(cropPos);
            Block cropBlock = cropState.getBlock();
            if (cropBlock instanceof IGrowable) {
                IGrowable growable = (IGrowable) cropState.getBlock();
                if (growable.canGrow(world, cropPos, cropState, world.isRemote)) {
                    ItemDye.applyBonemeal(mcStack.copy(), world, cropPos);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<IWrapperItemStack> harvestBlock(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        List<IWrapperItemStack> cropDrops = new ArrayList<>();
        if ((state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state)) || state.getBlock() instanceof BlockBush) {
            Block harvestedBlock = state.getBlock();
            NonNullList<ItemStack> drops = NonNullList.create();
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), harvestedBlock.getSoundType(state, world, pos, null).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);

            //Only return drops on servers.  Clients don't do items.
            if (!world.isRemote) {
                harvestedBlock.getDrops(drops, world, pos, state, 0);
                world.setBlockToAir(pos);
                if (harvestedBlock instanceof BlockCrops) {
                    for (ItemStack drop : drops) {
                        cropDrops.add(new WrapperItemStack(drop.copy()));
                    }
                } else {
                    for (ItemStack stack : drops) {
                        if (stack.getCount() > 0) {
                            world.spawnEntity(new EntityItem(world, position.x, position.y, position.z, stack));
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
            IBlockState farmlandState = world.getBlockState(farmlandPos);
            Block farmlandBlock = farmlandState.getBlock();
            if (farmlandBlock.equals(Blocks.FARMLAND)) {
                BlockPos cropPos = farmlandPos.up();
                if (world.isAirBlock(cropPos)) {
                    //Check to make sure the block can sustain the plant we want to plant.
                    IBlockState plantState = plantable.getPlant(world, cropPos);
                    if (farmlandBlock.canSustainPlant(plantState, world, farmlandPos, EnumFacing.UP, plantable)) {
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
    public boolean plowBlock(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState oldState = world.getBlockState(pos);
        IBlockState newState;
        Block block = oldState.getBlock();
        if (block.equals(Blocks.GRASS) || block.equals(Blocks.GRASS_PATH)) {
            newState = Blocks.FARMLAND.getDefaultState();
        } else if (block.equals(Blocks.DIRT)) {
            switch (oldState.getValue(BlockDirt.VARIANT)) {
                case DIRT:
                    newState = Blocks.FARMLAND.getDefaultState();
                    break;
                case COARSE_DIRT:
                    newState = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT);
                    break;
                default:
                    return false;
            }
        } else {
            return false;
        }

        world.setBlockState(pos, newState, 11);
        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        return true;
    }

    @Override
    public void removeSnow(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().equals(Material.SNOW) || state.getMaterial().equals(Material.CRAFTED_SNOW)) {
            world.setBlockToAir(pos);
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    @Override
    public boolean insertStack(Point3D position, Axis axis, IWrapperItemStack stack) {
        EnumFacing facing = EnumFacing.valueOf(axis.name());
        TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z).offset(facing));
        if (tile != null) {
            IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
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
        EnumFacing facing = EnumFacing.valueOf(axis.name());
        TileEntity tile = world.getTileEntity(new BlockPos(position.x, position.y, position.z).offset(facing));
        if (tile != null) {
            IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
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
        world.spawnEntity(new EntityItem(world, point.x, point.y + 1, point.z, ((WrapperItemStack) item.getNewStack(data)).stack));
    }

    @Override
    public void spawnItemStack(IWrapperItemStack stack, Point3D point) {
        world.spawnEntity(new EntityItem(world, point.x, point.y, point.z, ((WrapperItemStack) stack).stack));
    }

    @Override
    public void spawnExplosion(Point3D location, double strength, boolean flames) {
        world.newExplosion(null, location.x, location.y, location.z, (float) strength, flames, ConfigSystem.settings.general.blockBreakage.value);
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
        if (!event.world.isRemote && event.world.equals(world) && event.phase.equals(Phase.END)) {
            for (EntityPlayer player : event.world.playerEntities) {
                UUID playerUUID = player.getUniqueID();
                BuilderEntityExisting gunBuilder = playerServerGunBuilders.get(playerUUID);
                if (gunBuilder != null) {
                    //Gun exists, check if world is the same and it is actually updating.
                    //We check basic states, and then the watchdog bit that gets reset every tick.
                    //This way if we're in the world, but not valid we will know.
                    if (gunBuilder.world != player.world || player.isDead || !gunBuilder.entity.isValid || gunBuilder.idleTickCounter == 20) {
                        //Follower is not linked.  Remove it and re-create in code below.
                        gunBuilder.setDead();
                        playerServerGunBuilders.remove(playerUUID);
                        ticksSincePlayerJoin.remove(playerUUID);
                    } else {
                        ++gunBuilder.idleTickCounter;
                    }
                } else if (!player.isDead) {
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