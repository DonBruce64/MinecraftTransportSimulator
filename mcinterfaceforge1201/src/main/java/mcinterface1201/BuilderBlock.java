package mcinterface1201;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
public class BuilderBlock extends Block implements EntityBlock {
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
        super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(block.hardness, block.blastResistance).noOcclusion());
        this.block = block;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if(!(block instanceof ABlockBaseTileEntity))
            return null;

        //Need to return a wrapper class here, not the actual TE.
        if (ITileEntityFluidTankProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityFluidTank(pos, state);
        } else if (ITileEntityInventoryProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityInventoryContainer(pos, state);
        } else if (ITileEntityEnergyCharger.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return new BuilderTileEntityEnergyCharger(pos, state);
        } else {
            return new BuilderTileEntity(pos, state);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        //Forward this click to the block.  For left-clicks we'll need to use item attack calls.
        if (block instanceof ABlockBaseTileEntity) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BuilderTileEntity) {
                    if (((BuilderTileEntity) be).tileEntity != null) {
                        return ((BuilderTileEntity) be).tileEntity.interact(WrapperPlayer.getWrapperFor(player)) ? InteractionResult.CONSUME : InteractionResult.FAIL;
                    }
                }
            } else {
                return InteractionResult.CONSUME;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        //Forward the change of state of a neighbor to the tile if we have one.
        if (block instanceof ABlockBaseTileEntity) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BuilderTileEntity) {
                    if (((BuilderTileEntity) be).tileEntity != null) {
                        ((BuilderTileEntity) be).tileEntity.onNeighborChanged(new Point3D(neighborPos.getX(), neighborPos.getY(), neighborPos.getZ()));
                    }
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        //Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
        //This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
        //By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
        //data on the stack based on the TE state.

        //Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
        //here will result in air being grabbed, and no WAILA support.
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BuilderTileEntity) {
                ATileEntityBase<?> tile = ((BuilderTileEntity) be).tileEntity;
                if (tile != null) {
                    IWrapperItemStack stack = tile.getStack();
                    if (stack != null) {
                        return ((WrapperItemStack) stack).stack;
                    }
                }
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        //If this is a TE, drop TE drops.  Otherwise, drop normal drops.
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof BuilderTileEntity) {
            if (((BuilderTileEntity) be).tileEntity != null) {
                List<ItemStack> convertedDrops = new ArrayList<>();
                convertedDrops.add(((WrapperItemStack) ((BuilderTileEntity) be).tileEntity.getStack()).stack);
                return convertedDrops;
            }
        }
        return super.getDrops(state, params);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        //Forward the breaking call to the block to allow for breaking logic.
        block.onBroken(WrapperWorld.getWrapperFor(level), new Point3D(pos.getX(), pos.getY(), pos.getZ()));
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        //Gets the bounding boxes. We forward this call to the tile entity to handle if we have one.
        //Otherwise, get the bounds from the main block, or just the standard bounds.
        //We add-on 0.5D to offset the box to the correct location, as our blocks are centered.
        //Bounding boxes are not offset, whereas collision are, which is what the boolean parameter is for.
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BuilderTileEntity) {
                ATileEntityBase<?> tile = ((BuilderTileEntity) be).tileEntity;
                if (tile != null) {
                    return Shapes.create(WrapperWorld.convertWithOffset(tile.boundingBox, -pos.getX(), -pos.getY(), -pos.getZ()));
                }
            }
        } else if (block instanceof BlockCollision) {
            return Shapes.create(WrapperWorld.convert(((BlockCollision) block).blockBounds));
        }
        //Return empty here, since we don't every want to be considered a full block as it does bad lighting.
        //When we get our TE data, then we can use that for the actual collision.
        return Shapes.empty();
    }

    // TODO use forge event to set render type
    /*@Override
    @SuppressWarnings("deprecation")
    public BlockRenderType getRenderShape(BlockState state) {
        //Don't render this block.  We manually render via the TE.
        return BlockRenderType.INVISIBLE;
    }*/

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (block instanceof ABlockBaseTileEntity) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof BuilderTileEntity) {
                if (((BuilderTileEntity) tile).tileEntity != null) {
                    return (int) (((BuilderTileEntity) tile).tileEntity.getLightProvided() * 15);
                }
            }
        }
        return super.getLightEmission(state, level, pos);
    }
}
