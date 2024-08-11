package mcinterface1165;

import mcinterface1165.mixin.common.ConcretePowderBlockMixin;
import minecrafttransportsimulator.baseclasses.*;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.mcinterface.*;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataRequest;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataUpdate;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

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
    private static final Map<UUID, BuilderEntityRenderForwarder> playerFollowers = new HashMap<>();
    private final List<Box> mutableCollidingAABBs = new ArrayList<>();
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
        super();
        this.world = world;
        if (world.isClient) {
            //Send packet to server to request data for this world.
            this.savedData = InterfaceManager.coreInterface.getNewNBTWrapper();
            InterfaceManager.packetInterface.sendToServer(new PacketWorldSavedDataRequest(InterfaceManager.clientInterface.getClientPlayer()));
        } else {
            //Load data from disk.
            try {
                if (getDataFile().exists()) {
                    this.savedData = new WrapperNBT(NbtIo.readCompressed(Files.newInputStream(getDataFile().toPath())));
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
    public AWrapperWorld getWorld() {
        return this;
    }

    @Override
    public boolean isClient() {
        return world.isClient;
    }

    @Override
    public long getTime() {
        return world.getTimeOfDay() % 24000;
    }

    @Override
    public String getName() {
        return world.getRegistryKey().getRegistryName().getPath();
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
            world.getProfiler().swap(name);
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
                NbtIo.writeCompressed(((WrapperNBT) savedData).compound, Files.newOutputStream(getDataFile().toPath()));
                InterfaceManager.packetInterface.sendToAllClients(new PacketWorldSavedDataUpdate(name, value));
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Could not save data to disk!  This will result in data loss if we continue!");
            }
        }
    }

    @Override
    public File getDataFile() {
        return new File(((ServerWorld) world).getPersistentStateManager().directory, "mtsdata.dat");
    }

    @Override
    public WrapperEntity getExternalEntity(UUID entityID) {
        if (world instanceof ServerWorld) {
            return WrapperEntity.getWrapperFor(((ServerWorld) world).getEntity(entityID));
        } else {
            for (Entity entity : ((net.minecraft.client.world.ClientWorld) world).getEntities()) {
                if (entity.getUuid().equals(entityID)) {
                    return WrapperEntity.getWrapperFor(entity);
                }
            }
        }
        return null;
    }

    @Override
    public List<IWrapperEntity> getEntitiesWithin(BoundingBox box) {
        List<IWrapperEntity> entities = new ArrayList<>();
        for (LivingEntity entity : world.getNonSpectatingEntities(LivingEntity.class, WrapperWorld.convert(box))) {
            entities.add(WrapperEntity.getWrapperFor(entity));
        }
        return entities;
    }

    @Override
    public List<IWrapperPlayer> getPlayersWithin(BoundingBox box) {
        List<IWrapperPlayer> players = new ArrayList<>();
        for (PlayerEntity player : world.getNonSpectatingEntities(PlayerEntity.class, WrapperWorld.convert(box))) {
            players.add(WrapperPlayer.getWrapperFor(player));
        }
        return players;
    }

    @Override
    public List<IWrapperEntity> getEntitiesHostile(IWrapperEntity lookingEntity, double radius) {
        List<IWrapperEntity> entities = new ArrayList<>();
        Entity mcLooker = ((WrapperEntity) lookingEntity).entity;
        for (Entity entity : world.getOtherEntities(mcLooker, mcLooker.getBoundingBox().expand(radius))) {
            if (entity instanceof Monster && entity.isAlive() && (!(entity instanceof LivingEntity) || ((LivingEntity) entity).deathTime == 0)) {
                entities.add(WrapperEntity.getWrapperFor(entity));
            }
        }
        return entities;
    }

    @Override
    public void spawnEntity(AEntityB_Existing entity) {
        spawnEntityInternal(entity);
    }

    /**
     * Internal method to spawn entities and return their builders.
     */
    protected BuilderEntityExisting spawnEntityInternal(AEntityB_Existing entity) {
        BuilderEntityExisting builder = new BuilderEntityExisting(BuilderEntityExisting.ENTITY_EXISTING.get(), ((WrapperWorld) entity.world).world);
        builder.loadedFromSavedNBT = true;
        builder.setPos(entity.position.x, entity.position.y, entity.position.z);
        builder.entity = entity;
        world.spawnEntity(builder);
        addEntity(entity);
        return builder;
    }

    @Override
    public List<IWrapperEntity> attackEntities(Damage damage, Point3D motion, boolean generateList) {
        Box mcBox = WrapperWorld.convert(damage.box);
        List<Entity> collidedEntities;

        //Get collided entities.
        if (motion != null) {
            mcBox = mcBox.expand(motion.x, motion.y, motion.z);
        }
        collidedEntities = world.getNonSpectatingEntities(Entity.class, mcBox);

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
                            if (ridingPart.masterEntity.allParts.contains((APart) damage.damgeSource)) {
                                continue;
                            }
                        }
                    }
                }

                //Didn't hit a rider on the damage source. Do normal raytracing or just add if there's no motion.
                if (motion == null || mcEntityCollided.getBoundingBox().raycast(start, end).isPresent()) {
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
        for (LivingEntity entity : world.getNonSpectatingEntities(LivingEntity.class, WrapperWorld.convert(box))) {
            if (entity.getVehicle() == null && !(entity instanceof HostileEntity) && !(entity instanceof PlayerEntity)) {
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
    public void populateItemStackEntities(Map<IWrapperEntity, IWrapperItemStack> map, BoundingBox box) {
        for (ItemEntity mcEntity : world.getNonSpectatingEntities(ItemEntity.class, WrapperWorld.convert(box))) {
            IWrapperEntity entity = WrapperEntity.getWrapperFor(mcEntity);
            if (!map.containsKey(entity)) {
                map.put(entity, new WrapperItemStack(mcEntity.getStack()));
            }
        }
    }

    @Override
    public void removeItemStackEntity(IWrapperEntity entity) {
        ((WrapperEntity) entity).entity.kill();
    }

    @Override
    public boolean chunkLoaded(Point3D position) {
        return world.canSetBlock(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public ABlockBase getBlock(Point3D position) {
        Block block = world.getBlockState(new BlockPos(position.x, position.y, position.z)).getBlock();
        return block instanceof BuilderBlock ? ((BuilderBlock) block).block : null;
    }

    @Override
    public float getBlockHardness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        float hardness = world.getBlockState(pos).getHardness(world, pos);
        if (hardness < 0) {
            hardness = Float.MAX_VALUE;
        }
        return hardness;
    }

    @Override
    public float getBlockSlipperiness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBlockState(pos).getSlipperiness(world, pos, null);
    }

    private static final HashMap<Material, BlockMaterial> materialMap = new HashMap<>();

    @Override
    public BlockMaterial getBlockMaterial(Point3D position) {
        if (materialMap.isEmpty()) {
            materialMap.put(Material.ORGANIC_PRODUCT, BlockMaterial.CLAY);
            materialMap.put(Material.SOIL, BlockMaterial.DIRT);
            materialMap.put(Material.GLASS, BlockMaterial.GLASS);
            materialMap.put(Material.SOLID_ORGANIC, BlockMaterial.GRASS);
            materialMap.put(Material.ICE, BlockMaterial.ICE);
            materialMap.put(Material.DENSE_ICE, BlockMaterial.ICE);
            materialMap.put(Material.LAVA, BlockMaterial.LAVA);
            materialMap.put(Material.LEAVES, BlockMaterial.LEAVES);
            materialMap.put(Material.METAL, BlockMaterial.METAL);
            materialMap.put(Material.AGGREGATE, BlockMaterial.SAND);
            materialMap.put(Material.SNOW_BLOCK, BlockMaterial.SNOW);
            materialMap.put(Material.SNOW_LAYER, BlockMaterial.SNOW);
            materialMap.put(Material.STONE, BlockMaterial.STONE);
            materialMap.put(Material.WATER, BlockMaterial.WATER);
            materialMap.put(Material.WOOD, BlockMaterial.WOOD);
            materialMap.put(Material.WOOL, BlockMaterial.WOOL);
        }
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        if (world.isAir(pos)) {
            return null;
        } else {
            BlockMaterial material = materialMap.get(world.getBlockState(pos).getMaterial());
            if (material == null) {
                return BlockMaterial.NORMAL;
            } else {
                if (material == BlockMaterial.SAND && world.getBlockState(pos).getBlock() == Blocks.GRAVEL) {
                    return BlockMaterial.GRAVEL;
                }
                return material;
            }
        }
    }

    @Override
    public ColorRGB getBlockColor(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        MapColor mcColor = state.getTopMaterialColor(world, pos);
        return new ColorRGB(mcColor.color);
    }

    @Override
    public List<IWrapperItemStack> getBlockDrops(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        List<ItemStack> drops = Block.getDroppedStacks(state, (ServerWorld) world, pos, world.getBlockEntity(pos));
        List<IWrapperItemStack> convertedList = new ArrayList<>();
        for (ItemStack stack : drops) {
            convertedList.add(new WrapperItemStack(stack.copy()));
        }
        return convertedList;
    }

    @Override
    public BlockHitResult getBlockHit(Point3D position, Point3D delta) {
        Vec3d start = new Vec3d(position.x, position.y, position.z);
        net.minecraft.util.hit.BlockHitResult trace = world.raycast(new RaycastContext(start, start.add(delta.x, delta.y, delta.z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, null));
        if (trace.getType() != HitResult.Type.MISS) {
            BlockPos blockPos = trace.getBlockPos();
            if (blockPos != null) {
                Vec3d pos = trace.getPos();
                return new BlockHitResult(new Point3D(blockPos.getX(), blockPos.getY(), blockPos.getZ()), new Point3D(pos.x, pos.y, pos.z), Axis.valueOf(trace.getSide().name()));
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
            return offsetMCBlock != null && !offsetMCBlock.equals(Blocks.BARRIER) && state.isSideSolidFullSquare(world, pos, facing);
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
        return state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    @Override
    public boolean isBlockAboveTopSlab(Point3D position) {
        BlockState state = world.getBlockState(new BlockPos(position.x, position.y + 1, position.z));
        return state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.TOP;
    }

    @Override
    public double getHeight(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //Need to go down till we find a block.
        boolean bottomSlab = false;
        while (pos.getY() > 0) {
            if (!world.isAir(pos)) {
                //Check for a slab, since this affects distance.
                BlockState state = world.getBlockState(pos);
                bottomSlab = state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.BOTTOM;

                //Adjust up since we need to be above the top block.
                pos = pos.up();
                break;
            }
            pos = pos.down();
        }
        return bottomSlab ? position.y - (pos.getY() - 0.5) : position.y - pos.getY();
    }

    @Override
    public void updateBoundingBoxCollisions(BoundingBox box, Point3D collisionMotion, boolean ignoreIfGreater) {
        Box mcBox = WrapperWorld.convert(box);
        VoxelShape mcShape = VoxelShapes.cuboid(mcBox);
        box.collidingBlockPositions.clear();
        mutableCollidingAABBs.clear();
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (!world.isAir(pos)) {
                        BlockState state = world.getBlockState(pos);
                        VoxelShape collisionShape = state.getCollisionShape(world, pos).offset(i, j, k);
                        if (!collisionShape.isEmpty() && VoxelShapes.matchesAnywhere(mcShape, collisionShape, BooleanBiFunction.AND) && state.getMaterial() != Material.LEAVES) {
                            mutableCollidingAABBs.addAll(collisionShape.getBoundingBoxes());
                            box.collidingBlockPositions.add(new Point3D(i, j, k));
                        }
                        if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                            mutableCollidingAABBs.add(VoxelShapes.fullCube().getBoundingBox().offset(pos));
                            box.collidingBlockPositions.add(new Point3D(i, j, k));
                        }
                    }
                }
            }
        }

        //If we are in the depth bounds for this collision, set it as the collision depth.
        box.currentCollisionDepth.set(0D, 0D, 0D);
        double boxCollisionDepth;
        for (Box colBox : mutableCollidingAABBs) {
            if (collisionMotion.x > 0) {
                boxCollisionDepth = mcBox.maxX - colBox.minX;
                if (box.currentCollisionDepth.x < boxCollisionDepth) {
                    box.currentCollisionDepth.x = boxCollisionDepth;
                }
            } else if (collisionMotion.x < 0) {
                boxCollisionDepth = -(colBox.maxX - mcBox.minX);
                if (box.currentCollisionDepth.x > boxCollisionDepth) {
                    box.currentCollisionDepth.x = boxCollisionDepth;
                }
            }
            if (collisionMotion.y > 0) {
                boxCollisionDepth = mcBox.maxY - colBox.minY;
                if (box.currentCollisionDepth.y < boxCollisionDepth) {
                    box.currentCollisionDepth.y = boxCollisionDepth;
                }
            } else if (collisionMotion.y < 0) {
                boxCollisionDepth = -(colBox.maxY - mcBox.minY);
                if (box.currentCollisionDepth.y > boxCollisionDepth) {
                    box.currentCollisionDepth.y = boxCollisionDepth;
                }
            }
            if (collisionMotion.z > 0) {
                boxCollisionDepth = mcBox.maxZ - colBox.minZ;
                if (box.currentCollisionDepth.z < boxCollisionDepth) {
                    box.currentCollisionDepth.z = boxCollisionDepth;
                }
            } else if (collisionMotion.z < 0) {
                boxCollisionDepth = -(colBox.maxZ - mcBox.minZ);
                if (box.currentCollisionDepth.z > boxCollisionDepth) {
                    box.currentCollisionDepth.z = boxCollisionDepth;
                }
            }
        }

        if (ignoreIfGreater) {
            if (collisionMotion.x > 0 && box.currentCollisionDepth.x > collisionMotion.x) {
                box.currentCollisionDepth.x = collisionMotion.x;
            } else if (collisionMotion.x < 0 && box.currentCollisionDepth.x < collisionMotion.x) {
                box.currentCollisionDepth.x = collisionMotion.x;
            }
            if (collisionMotion.y > 0 && box.currentCollisionDepth.y > collisionMotion.y) {
                box.currentCollisionDepth.y = collisionMotion.y;
            } else if (collisionMotion.y < 0 && box.currentCollisionDepth.y < collisionMotion.y) {
                box.currentCollisionDepth.y = collisionMotion.y;
            }
            if (collisionMotion.z > 0 && box.currentCollisionDepth.z > collisionMotion.z) {
                box.currentCollisionDepth.z = collisionMotion.z;
            } else if (collisionMotion.z < 0 && box.currentCollisionDepth.z < collisionMotion.z) {
                box.currentCollisionDepth.z = collisionMotion.z;
            }
        }

        if (box.currentCollisionDepth.isZero()) {
            box.collidingBlockPositions.clear();
        }
    }

    @Override
    public boolean checkForCollisions(BoundingBox box, Point3D offset, boolean clearCache, boolean breakLeaves) {
        if (clearCache) {
            knownAirBlocks.clear();
        }
        mutableCollidingAABBs.clear();
        Box mcBox = WrapperWorld.convertWithOffset(box, offset.x, offset.y, offset.z);
        VoxelShape mcShape = VoxelShapes.cuboid(mcBox);
        for (int i = (int) Math.floor(mcBox.minX); i < Math.ceil(mcBox.maxX); ++i) {
            for (int j = (int) Math.floor(mcBox.minY); j < Math.ceil(mcBox.maxY); ++j) {
                for (int k = (int) Math.floor(mcBox.minZ); k < Math.ceil(mcBox.maxZ); ++k) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (!knownAirBlocks.contains(pos)) {
                        if (world.canSetBlock(pos)) {
                            BlockState state = world.getBlockState(pos);
                            VoxelShape collisionShape = state.getCollisionShape(world, pos).offset(i, j, k);
                            if (state.getMaterial() != Material.LEAVES) {
                                if (collisionShape != null && !collisionShape.isEmpty() && VoxelShapes.matchesAnywhere(mcShape, collisionShape, BooleanBiFunction.AND)) {
                                    return true;
                                } else {
                                    knownAirBlocks.add(pos);
                                }
                                if (box.collidesWithLiquids && state.getMaterial().isLiquid()) {
                                    if (mcBox.intersects(VoxelShapes.fullCube().getBoundingBox().offset(pos))) {
                                        return true;
                                    }
                                }
                            } else if (breakLeaves) {
                                world.breakBlock(pos, false);
                            } else {
                                knownAirBlocks.add(pos);
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
        return world.getReceivedRedstonePower(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public float getRainStrength(Point3D position) {
        return world.hasRain(new BlockPos(position.x, position.y + 1, position.z)) ? world.getRainGradient(1.0F) + world.getThunderGradient(1.0F) : 0.0F;
    }

    @Override
    public float getTemperature(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return world.getBiome(pos).getTemperature(pos);
    }

    @Override
    public <TileEntityType extends ATileEntityBase<JSONDefinition>, JSONDefinition extends AJSONMultiModelProvider> boolean setBlock(ABlockBase block, Point3D position, IWrapperPlayer playerWrapper, Axis axis) {
        if (!world.isClient) {
            BuilderBlock wrapper = BuilderBlock.blockMap.get(block);
            BlockPos pos = new BlockPos(position.x, position.y, position.z);
            if (playerWrapper != null) {
                PlayerEntity mcPlayer = ((WrapperPlayer) playerWrapper).player;
                WrapperItemStack stack = (WrapperItemStack) playerWrapper.getHeldStack();
                AItemBase item = stack.getItem();
                Direction facing = Direction.valueOf(axis.name());
                if (!world.isAir(pos)) {
                    pos = pos.offset(facing);
                    position.add(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
                }

                if (item != null && world.canPlayerModifyAt(mcPlayer, pos) && world.isAir(pos)) {
                    BlockState newState = wrapper.getDefaultState();
                    if (world.setBlockState(pos, newState, 11)) {
                        //Block is set.  See if we need to set TE data.
                        if (block instanceof ABlockBaseTileEntity) {
                            BuilderBlockEntity builderTile = (BuilderBlockEntity) world.getBlockEntity(pos);
                            IWrapperNBT data = stack.getData();
                            if (data != null) {
                                data.deleteAllUUIDTags(); //Do this just in case this is an older item.
                            }
                            builderTile.setTileEntity(((ABlockBaseTileEntity) block).createTileEntity(this, position, playerWrapper, (AItemSubTyped<?>) item, data));
                            addEntity(builderTile.tileEntity);
                        }
                        //Shrink stack as we placed this block.
                        stack.add(-1);
                        return true;
                    }
                }
            } else {
                BlockState newState = wrapper.getDefaultState();
                return world.setBlockState(pos, newState, 11);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TileEntityType extends ATileEntityBase<?>> TileEntityType getTileEntity(Point3D position) {
        BlockEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z));
        return tile instanceof BuilderBlockEntity ? (TileEntityType) ((BuilderBlockEntity) tile).tileEntity : null;
    }

    @Override
    public void markTileEntityChanged(Point3D position) {
        world.getBlockEntity(new BlockPos(position.x, position.y, position.z)).markDirty();
    }

    @Override
    public float getLightBrightness(Point3D position, boolean calculateBlock) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //Sunlight never goes below 11 in this version, so we factor the darkening.
        float darkenFactor = 15 * world.getAmbientDarkness() / 11F;
        float sunLight = (world.getLightLevel(LightType.SKY, pos) - darkenFactor) / 15F;
        float blockLight = calculateBlock ? world.getLightLevel(LightType.BLOCK, pos) / 15F : 0.0F;
        return Math.max(sunLight, blockLight);
    }

    @Override
    public void updateLightBrightness(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //This needs to get fired manually as even if we update the blockstate the light value won't change
        //as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
        world.getLightingProvider().checkBlock(pos);
    }

    @Override
    public void destroyBlock(Point3D position, boolean spawnDrops) {
        world.breakBlock(new BlockPos(position.x, position.y, position.z), spawnDrops);
    }

    @Override
    public boolean isAir(Point3D position) {
        return world.isAir(new BlockPos(position.x, position.y, position.z));
    }

    @Override
    public boolean isFire(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        return state.getMaterial().equals(Material.FIRE);
    }

    @Override
    public void setToFire(Point3D position, Axis side) {
        BlockPos blockpos = new BlockPos(position.x, position.y, position.z).offset(Direction.valueOf(side.name()));
        if (world.isAir(blockpos)) {
            world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());
        }
    }

    @Override
    public void extinguish(Point3D position, Axis side) {
        BlockPos blockpos = new BlockPos(position.x, position.y, position.z).offset(Direction.valueOf(side.name()));
        if (world.getBlockState(blockpos).isIn(BlockTags.FIRE)) {
            world.removeBlock(blockpos, false);
        }
    }

    @Override
    public boolean placeBlock(Point3D position, IWrapperItemStack stack) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        if (world.isAir(pos)) {
            ItemStack mcStack = ((WrapperItemStack) stack).stack;
            return mcStack.useOnBlock(new ItemUsageContext(world, null, Hand.MAIN_HAND, mcStack, new net.minecraft.util.hit.BlockHitResult(new Vec3d(position.x, position.y, position.z), Direction.DOWN, pos, true))) == ActionResult.CONSUME;
        }
        return false;
    }

    @Override
    public boolean fertilizeBlock(Point3D position, IWrapperItemStack stack) {
        //Check if the item can fertilize things and we are on the server.
        ItemStack mcStack = ((WrapperItemStack) stack).stack;
        if (mcStack.getItem() == Items.BONE_MEAL && !world.isClient) {
            //Check if we are in crops.
            BlockPos cropPos = new BlockPos(position.x, position.y, position.z);
            BlockState cropState = world.getBlockState(cropPos);
            Block cropBlock = cropState.getBlock();
            if (cropBlock instanceof Fertilizable) {
                Fertilizable growable = (Fertilizable) cropState.getBlock();
                if (growable.isFertilizable(world, cropPos, cropState, false)) {
                    growable.grow((ServerWorld) world, world.random, cropPos, cropState);
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
        if ((state.getBlock() instanceof CropBlock && ((CropBlock) state.getBlock()).isMature(state)) || state.getBlock() instanceof PlantBlock) {
            Block harvestedBlock = state.getBlock();
            world.playSound(null, pos, harvestedBlock.getSoundType(state, world, pos, null).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);

            //Only return drops on servers.  Clients don't do items.
            if (!world.isClient) {
                List<ItemStack> drops = Block.getDroppedStacks(state, (ServerWorld) world, pos, world.getBlockEntity(pos));
                world.removeBlock(pos, false);
                if (harvestedBlock instanceof CropBlock) {
                    for (ItemStack drop : drops) {
                        cropDrops.add(new WrapperItemStack(drop.copy()));
                    }
                } else {
                    for (ItemStack stack : drops) {
                        if (stack.getCount() > 0) {
                            world.spawnEntity(new ItemEntity(world, position.x, position.y, position.z, stack));
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
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            if (block instanceof IPlantable) {
                IPlantable plantable = (IPlantable) block;

                //Check if we have farmland below and air above.
                BlockPos farmlandPos = new BlockPos(position.x, position.y, position.z);
                BlockState farmlandState = world.getBlockState(farmlandPos);
                Block farmlandBlock = farmlandState.getBlock();
                if (farmlandBlock instanceof FarmlandBlock) {
                    BlockPos cropPos = farmlandPos.up();
                    if (world.isAir(cropPos)) {
                        //Check to make sure the block can sustain the plant we want to plant.
                        BlockState plantState = plantable.getPlant(world, cropPos);
                        if (farmlandBlock.canSustainPlant(farmlandState, world, farmlandPos, Direction.UP, plantable)) {
                            world.setBlockState(cropPos, plantState, 11);
                            world.playSound(null, farmlandPos, plantState.getBlock().getSoundType(plantState, world, farmlandPos, null).getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                            return true;
                        }
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
        if (block == Blocks.GRASS_BLOCK || block == Blocks.GRASS_PATH || block == Blocks.DIRT) {
            newState = Blocks.FARMLAND.getDefaultState();
        } else if (block.equals(Blocks.COARSE_DIRT)) {
            newState = Blocks.DIRT.getDefaultState();
        } else {
            return false;
        }

        world.setBlockState(pos, newState, 11);
        world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public boolean removeSnow(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        if (state.getMaterial().equals(Material.SNOW_BLOCK) || state.getMaterial().equals(Material.SNOW_LAYER)) {
            world.removeBlock(pos, false);
            world.playSound(null, pos, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hydrateBlock(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.LAVA) {
            if (world.getFluidState(pos).isStill()) {
                world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
                return true;
            } else {
                world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
                return true;
            }
        } else if (block instanceof ConcretePowderBlock) {
            world.setBlockState(pos, ((ConcretePowderBlockMixin) block).getHardenedState());
            return true;
        } else if (block == Blocks.FARMLAND) {
            int moisture = state.get(FarmlandBlock.MOISTURE);
            if (moisture < 7) {
                world.setBlockState(pos, state.with(FarmlandBlock.MOISTURE, 7));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean insertStack(Point3D position, Axis axis, IWrapperItemStack stack) {
        Direction facing = Direction.valueOf(axis.name());
        BlockEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z).offset(facing));
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
        BlockEntity tile = world.getBlockEntity(new BlockPos(position.x, position.y, position.z).offset(facing));
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
    public void spawnItemStack(IWrapperItemStack stack, Point3D point, Point3D optionalMotion) {
        ItemEntity item;
        if (optionalMotion != null) {
            item = new ItemEntity(world, point.x, point.y, point.z, ((WrapperItemStack) stack).stack);
            item.setVelocity(new Vec3d(optionalMotion.x, optionalMotion.y, optionalMotion.z));
        } else {
            //Spawn 1 block above in case we're right on a block.
            item = new ItemEntity(world, point.x, point.y + 1, point.z, ((WrapperItemStack) stack).stack);
        }
        world.spawnEntity(item);
    }

    @Override
    public void spawnExplosion(Point3D location, double strength, boolean flames) {
        world.createExplosion(null, location.x, location.y, location.z, (float) strength, flames, Explosion.DestructionType.DESTROY);
    }

    /**
     * Helper method to convert a BoundingBox to an Box.
     */
    public static Box convert(BoundingBox box) {
        return new Box(box.globalCenter.x - box.widthRadius, box.globalCenter.y - box.heightRadius, box.globalCenter.z - box.depthRadius, box.globalCenter.x + box.widthRadius, box.globalCenter.y + box.heightRadius, box.globalCenter.z + box.depthRadius);
    }

    /**
     * Helper method to convert the BoundingBox to an Box.
     * This method allows for an offset to the conversion, to prevent
     * creating two AABBs (the conversion and the offset box).
     */
    public static Box convertWithOffset(BoundingBox box, double x, double y, double z) {
        return new Box(x + box.globalCenter.x - box.widthRadius, y + box.globalCenter.y - box.heightRadius, z + box.globalCenter.z - box.depthRadius, x + box.globalCenter.x + box.widthRadius, y + box.globalCenter.y + box.heightRadius, z + box.globalCenter.z + box.depthRadius);
    }

    /**
     * Spawn "follower" entities for the player if they don't exist already.
     * This only happens 3 seconds after the player joins.
     * This delay is done to ensure all chunks are loaded before spawning any followers.
     * We also track followers, and ensure that if the player doesn't exist, they are removed.
     * This handles players leaving.  We could use events for this, but they're not reliable.
     */
    @SubscribeEvent
    public void onIVWorldTick(TickEvent.WorldTickEvent event) {
        //Need to check if it's our world, because Forge is stupid like that.
        //Note that the client world never calls this method: to do client ticks we need to use the client interface.
        if (!event.world.isClient && event.world.equals(world)) {
            if (event.phase.equals(Phase.START)) {
                beginProfiling("MTS_ServerVehicleUpdates", true);
                tickAll(true);

                for (PlayerEntity mcPlayer : event.world.getPlayers()) {
                    UUID playerUUID = mcPlayer.getUuid();

                    BuilderEntityExisting gunBuilder = playerServerGunBuilders.get(playerUUID);
                    if (gunBuilder != null) {
                        //Gun exists, check if world is the same and it is actually updating.
                        //We check basic states, and then the watchdog bit that gets reset every tick.
                        //This way if we're in the world, but not valid we will know.
                        if (gunBuilder.world != mcPlayer.world || !mcPlayer.isAlive() || !gunBuilder.entity.isValid || gunBuilder.idleTickCounter == 20) {
                            //Follower is not linked.  Remove it and re-create in code below.
                            gunBuilder.remove();
                            playerServerGunBuilders.remove(playerUUID);
                            ticksSincePlayerJoin.remove(playerUUID);
                        } else {
                            ++gunBuilder.idleTickCounter;
                        }
                    }

                    BuilderEntityRenderForwarder followerBuilder = playerFollowers.get(playerUUID);
                    if (followerBuilder != null) {
                        //Follower exists, check if world is the same and it is actually updating.
                        //We check basic states, and then the watchdog bit that gets reset every tick.
                        //This way if we're in the world, but not valid we will know.
                        if (followerBuilder.world != mcPlayer.world || followerBuilder.playerFollowing != mcPlayer || !mcPlayer.isAlive() || !followerBuilder.isAlive() || followerBuilder.idleTickCounter == 20) {
                            //Follower is not linked.  Remove it and re-create in code below.
                            followerBuilder.remove();
                            playerFollowers.remove(playerUUID);
                            ticksSincePlayerJoin.remove(playerUUID);
                            followerBuilder = null;
                        } else {
                            ++followerBuilder.idleTickCounter;
                        }
                    }

                    if (mcPlayer.isAlive() && (gunBuilder == null || followerBuilder == null)) {
                        //Some follower doesn't exist.  Check if player has been present for 3 seconds and spawn it.
                        int totalTicksWaited = 0;
                        if (ticksSincePlayerJoin.containsKey(playerUUID)) {
                            totalTicksWaited = ticksSincePlayerJoin.get(playerUUID);
                        }
                        if (++totalTicksWaited == 60) {
                            IWrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(mcPlayer);

                            //Spawn gun.
                            if (gunBuilder == null) {
                                EntityPlayerGun entity = new EntityPlayerGun(this, playerWrapper, null);
                                playerServerGunBuilders.put(playerUUID, spawnEntityInternal(entity));
                            }

                            //Spawn follower.
                            if (followerBuilder == null) {
                                followerBuilder = new BuilderEntityRenderForwarder(mcPlayer);
                                followerBuilder.loadedFromSavedNBT = true;
                                playerFollowers.put(playerUUID, followerBuilder);
                                world.spawnEntity(followerBuilder);
                            }

                            //If the player is new, add handbooks.
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
                endProfiling();
            } else {
                tickAll(false);
            }
        }
    }

    /**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     * Also remove this wrapper from the created lists, as it's invalid.
     */
    @SubscribeEvent
    public void onIVWorldUnload(WorldEvent.Unload event) {
        //Need to check if it's our world, because Forge is stupid like that.
        if (event.getWorld() == world) {
            for (AEntityA_Base entity : allEntities) {
                entity.remove();
            }
            worldWrappers.remove(world);
        }
    }
}