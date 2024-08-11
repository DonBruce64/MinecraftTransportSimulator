package mcinterface1165;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for a basic MC Block class.  This builder assumes the block will not be a solid
 * block (so no culling) and may have alpha channels in the texture (like glass).
 * It also assumes the block can be rotated, and saves the rotation as a set of
 * FACING properties.  This MAY change in later versions to TE data though...
 *
 * @author don_bruce
 */
public class BuilderBlock extends Block {
    protected static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InterfaceLoader.MODID);

    /**
     * Map of created blocks linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<ABlockBase, BuilderBlock> blockMap = new HashMap<>();

    /**
     * Current block we are built around.
     **/
    protected final ABlockBase block;

    BuilderBlock(ABlockBase block) {
        super(AbstractBlock.Settings.of(Material.STONE, MapColor.STONE_GRAY).strength(block.hardness, block.blastResistance).nonOpaque());
        this.block = block;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        //If our block implements the interface to be a TE, we return true.
        return block instanceof ABlockBaseTileEntity;
    }

    @Nullable
    @Override
    public BlockEntity createTileEntity(BlockState state, BlockView world) {
        //Need to return a wrapper class here, not the actual TE.
        if (ITileEntityFluidTankProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderBlockEntityFluidTank();
        } else if (ITileEntityInventoryProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderBlockEntityInventoryContainer();
        } else if (ITileEntityEnergyCharger.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderBlockEntityEnergyCharger();
        } else {
            return new BuilderBlockEntity();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        //Forward this click to the block.  For left-clicks we'll need to use item attack calls.
        if (block instanceof ABlockBaseTileEntity) {
            if (!world.isClient) {
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile instanceof BuilderBlockEntity) {
                    if (((BuilderBlockEntity) tile).tileEntity != null) {
                        return ((BuilderBlockEntity) tile).tileEntity.interact(WrapperPlayer.getWrapperFor(player)) ? ActionResult.CONSUME : ActionResult.FAIL;
                    }
                }
            } else {
                return ActionResult.CONSUME;
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        //Forward the change of state of a neighbor to the tile if we have one.
        if (block instanceof ABlockBaseTileEntity) {
            if (!world.isClient()) {
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile instanceof BuilderBlockEntity) {
                    if (((BuilderBlockEntity) tile).tileEntity != null) {
                        ((BuilderBlockEntity) tile).tileEntity.onNeighborChanged(new Point3D(neighborPos.getX(), neighborPos.getY(), neighborPos.getZ()));
                    }
                }
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        //Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
        //This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
        //By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
        //data on the stack based on the TE state.

        //Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
        //here will result in air being grabbed, and no WAILA support.
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity mcTile = world.getBlockEntity(pos);
            if (mcTile instanceof BuilderBlockEntity) {
                ATileEntityBase<?> tile = ((BuilderBlockEntity) mcTile).tileEntity;
                if (tile != null) {
                    IWrapperItemStack stack = tile.getStack();
                    if (stack != null) {
                        return ((WrapperItemStack) stack).stack;
                    }
                }
            }
        }
        return super.getPickStack(world, pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        //If this is a TE, drop TE drops.  Otherwise, drop normal drops.
        BlockEntity tile = builder.getNullable(LootContextParameters.BLOCK_ENTITY);
        if (tile instanceof BuilderBlockEntity) {
            if (((BuilderBlockEntity) tile).tileEntity != null) {
                List<ItemStack> convertedDrops = new ArrayList<>();
                convertedDrops.add(((WrapperItemStack) ((BuilderBlockEntity) tile).tileEntity.getStack()).stack);
                return convertedDrops;
            }
        }
        return super.getDroppedStacks(state, builder);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        //Forward the breaking call to the block to allow for breaking logic.
        block.onBroken(WrapperWorld.getWrapperFor(world), new Point3D(pos.getX(), pos.getY(), pos.getZ()));
        super.onStateReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        //Gets the bounding boxes. We forward this call to the tile entity to handle if we have one.
        //Otherwise, get the bounds from the main block, or just the standard bounds.
        //We add-on 0.5D to offset the box to the correct location, as our blocks are centered.
        //Bounding boxes are not offset, whereas collision are, which is what the boolean parameter is for.
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity mcTile = world.getBlockEntity(pos);
            if (mcTile instanceof BuilderBlockEntity) {
                ATileEntityBase<?> tile = ((BuilderBlockEntity) mcTile).tileEntity;
                if (tile != null) {
                    return VoxelShapes.cuboid(WrapperWorld.convertWithOffset(tile.boundingBox, -pos.getX(), -pos.getY(), -pos.getZ()));
                }
            }
        } else if (block instanceof BlockCollision) {
            return VoxelShapes.cuboid(WrapperWorld.convert(((BlockCollision) block).blockBounds));
        }
        //Return empty here, since we don't every want to be considered a full block as it does bad lighting.
        //When we get our TE data, then we can use that for the actual collision.
        return VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockRenderType getRenderType(BlockState state) {
        //Don't render this block.  We manually render via the TE.
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public int getLightValue(BlockState state, BlockView world, BlockPos pos) {
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof BuilderBlockEntity) {
                if (((BuilderBlockEntity) tile).tileEntity != null) {
                    return (int) (((BuilderBlockEntity) tile).tileEntity.getLightProvided() * 15);
                }
            }
        }
        return super.getLightValue(state, world, pos);
    }
}
