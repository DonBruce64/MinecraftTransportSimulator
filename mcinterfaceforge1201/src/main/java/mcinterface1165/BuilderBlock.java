package mcinterface1165;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

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
        super(AbstractBlock.Properties.of(Material.STONE, MaterialColor.STONE).strength(block.hardness, block.blastResistance).noOcclusion());
        this.block = block;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        //If our block implements the interface to be a TE, we return true.
        return block instanceof ABlockBaseTileEntity;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        //Need to return a wrapper class here, not the actual TE.
        if (ITileEntityFluidTankProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityFluidTank();
        } else if (ITileEntityInventoryProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityInventoryContainer();
        } else if (ITileEntityEnergyCharger.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityEnergyCharger();
        } else {
            return new BuilderTileEntity();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        //Forward this click to the block.  For left-clicks we'll need to use item attack calls.
        if (block instanceof ABlockBaseTileEntity) {
            if (!world.isClientSide()) {
                TileEntity tile = world.getBlockEntity(pos);
                if (tile instanceof BuilderTileEntity) {
                    if (((BuilderTileEntity) tile).tileEntity != null) {
                        return ((BuilderTileEntity) tile).tileEntity.interact(WrapperPlayer.getWrapperFor(player)) ? ActionResultType.CONSUME : ActionResultType.FAIL;
                    }
                }
            } else {
                return ActionResultType.CONSUME;
            }
        }
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        //Forward the change of state of a neighbor to the tile if we have one.
        if (block instanceof ABlockBaseTileEntity) {
            if (!world.isClientSide()) {
                TileEntity tile = world.getBlockEntity(pos);
                if (tile instanceof BuilderTileEntity) {
                    if (((BuilderTileEntity) tile).tileEntity != null) {
                        ((BuilderTileEntity) tile).tileEntity.onNeighborChanged(new Point3D(facingPos.getX(), facingPos.getY(), facingPos.getZ()));
                    }
                }
            }
        }
        return super.updateShape(state, facing, facingState, world, pos, facingPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(IBlockReader world, BlockPos pos, BlockState state) {
        //Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
        //This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
        //By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
        //data on the stack based on the TE state.

        //Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
        //here will result in air being grabbed, and no WAILA support.
        if (block instanceof ABlockBaseTileEntity) {
            TileEntity mcTile = world.getBlockEntity(pos);
            if (mcTile instanceof BuilderTileEntity) {
                ATileEntityBase<?> tile = ((BuilderTileEntity) mcTile).tileEntity;
                if (tile != null) {
                    IWrapperItemStack stack = tile.getStack();
                    if (stack != null) {
                        return ((WrapperItemStack) stack).stack;
                    }
                }
            }
        }
        return super.getCloneItemStack(world, pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        //If this is a TE, drop TE drops.  Otherwise, drop normal drops.
        TileEntity tile = builder.getOptionalParameter(LootParameters.BLOCK_ENTITY);
        if (tile instanceof BuilderTileEntity) {
            if (((BuilderTileEntity) tile).tileEntity != null) {
                List<ItemStack> convertedDrops = new ArrayList<>();
                convertedDrops.add(((WrapperItemStack) ((BuilderTileEntity) tile).tileEntity.getStack()).stack);
                return convertedDrops;
            }
        }
        return super.getDrops(state, builder);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void spawnAfterBreak(BlockState state, ServerWorld world, BlockPos pos, ItemStack stack) {
        //Forward the breaking call to the block to allow for breaking logic.
        block.onBroken(WrapperWorld.getWrapperFor(world), new Point3D(pos.getX(), pos.getY(), pos.getZ()));
        super.spawnAfterBreak(state, world, pos, stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        //Gets the bounding boxes. We forward this call to the tile entity to handle if we have one.
        //Otherwise, get the bounds from the main block, or just the standard bounds.
        //We add-on 0.5D to offset the box to the correct location, as our blocks are centered.
        //Bounding boxes are not offset, whereas collision are, which is what the boolean parameter is for.
        if (block instanceof ABlockBaseTileEntity) {
            TileEntity mcTile = world.getBlockEntity(pos);
            if (mcTile instanceof BuilderTileEntity) {
                ATileEntityBase<?> tile = ((BuilderTileEntity) mcTile).tileEntity;
                if (tile != null) {
                    return VoxelShapes.create(WrapperWorld.convertWithOffset(tile.boundingBox, -pos.getX(), -pos.getY(), -pos.getZ()));
                }
            }
        } else if (block instanceof BlockCollision) {
            return VoxelShapes.create(WrapperWorld.convert(((BlockCollision) block).blockBounds));
        }
        //Return empty here, since we don't every want to be considered a full block as it does bad lighting.
        //When we get our TE data, then we can use that for the actual collision.
        return VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockRenderType getRenderShape(BlockState state) {
        //Don't render this block.  We manually render via the TE.
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        if (block instanceof ABlockBaseTileEntity) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof BuilderTileEntity) {
                if (((BuilderTileEntity) tile).tileEntity != null) {
                    return (int) (((BuilderTileEntity) tile).tileEntity.getLightProvided() * 15);
                }
            }
        }
        return super.getLightValue(state, world, pos);
    }
}
