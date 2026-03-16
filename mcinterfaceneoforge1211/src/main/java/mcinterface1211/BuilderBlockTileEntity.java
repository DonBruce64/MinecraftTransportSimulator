package mcinterface1211;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Builder for an advanced  MC Block class that has a Tile Entity associated with it.
 *
 * @author don_bruce
 */
public class BuilderBlockTileEntity extends BuilderBlock implements EntityBlock {

    public static IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    BuilderBlockTileEntity(ABlockBase block) {
        super(block, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(block.hardness, block.blastResistance).noOcclusion().lightLevel(getLightValue()));
        this.registerDefaultState(this.defaultBlockState().setValue(LIGHT, 0));
    }

    public static ToIntFunction<BlockState> getLightValue() {
        return (state) -> {
            return state.getValue(LIGHT);
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(LIGHT, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIGHT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
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

    //All of this crap to just re-direct a tick call...  Stupid....
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> blockEntityType) {
        //Need to return the proper state-ticker here.
        if (ITileEntityFluidTankProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return createTickerHelper(blockEntityType, BuilderTileEntityFluidTank.TE_TYPE2.get(), BuilderBlockTileEntity::tick);
        } else if (ITileEntityInventoryProvider.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return createTickerHelper(blockEntityType, BuilderTileEntityInventoryContainer.TE_TYPE2.get(), BuilderBlockTileEntity::tick);
        } else if (ITileEntityEnergyCharger.class.isAssignableFrom(((ABlockBaseTileEntity) block).getTileEntityClass())) {
            return createTickerHelper(blockEntityType, BuilderTileEntityEnergyCharger.TE_TYPE2.get(), BuilderBlockTileEntity::tick);
        } else {
            return createTickerHelper(blockEntityType, BuilderTileEntity.TE_TYPE.get(), BuilderBlockTileEntity::tick);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> pServerType, BlockEntityType<E> pClientType, BlockEntityTicker<? super E> pTicker) {
        return pClientType == pServerType ? (BlockEntityTicker<A>) pTicker : null;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, BuilderTileEntity blockEntity) {
        blockEntity.tick();
    }


    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        //Forward this click to the block.  For left-clicks we'll need to use item attack calls.
        if (!world.isClientSide()) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof BuilderTileEntity) {
                if (((BuilderTileEntity) tile).tileEntity != null) {
                    return ((BuilderTileEntity) tile).tileEntity.interact(WrapperPlayer.getWrapperFor(player)) ? InteractionResult.CONSUME : InteractionResult.FAIL;
                }
            }
        } else {
            return InteractionResult.CONSUME;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos) {
        //Forward the change of state of a neighbor to the tile if we have one.
        if (!world.isClientSide()) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof BuilderTileEntity) {
                if (((BuilderTileEntity) tile).tileEntity != null) {
                    ((BuilderTileEntity) tile).tileEntity.onNeighborChanged(new Point3D(facingPos.getX(), facingPos.getY(), facingPos.getZ()));
                }
            }
        }
        return super.updateShape(state, facing, facingState, world, pos, facingPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        //Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
        //This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
        //By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
        //data on the stack based on the TE state.

        //Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
        //here will result in air being grabbed, and no WAILA support.
        BlockEntity mcTile = world.getBlockEntity(pos);
        if (mcTile instanceof BuilderTileEntity) {
            ATileEntityBase<?> tile = ((BuilderTileEntity) mcTile).tileEntity;
            if (tile != null) {
                IWrapperItemStack stack = tile.getStack();
                if (stack != null) {
                    return ((WrapperItemStack) stack).stack;
                }
            }
        }
        return super.getCloneItemStack(world, pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        //If this is a TE, drop TE drops.  Otherwise, drop normal drops.
        BlockEntity tile = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tile instanceof BuilderTileEntity) {
            if (((BuilderTileEntity) tile).tileEntity != null) {
                List<ItemStack> convertedDrops = new ArrayList<>();
                convertedDrops.add(((WrapperItemStack) ((BuilderTileEntity) tile).tileEntity.getStack()).stack);
                return convertedDrops;
            }
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        //Forward the breaking call to the block to allow for breaking logic.
        block.onBroken(WrapperWorld.getWrapperFor(world), new Point3D(pos.getX(), pos.getY(), pos.getZ()));
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        //Gets the bounding boxes. We forward this call to the tile entity to handle if we have one.
        //Otherwise, get the bounds from the main block, or just the standard bounds.
        //We add-on 0.5D to offset the box to the correct location, as our blocks are centered.
        //Bounding boxes are not offset, whereas collision are, which is what the boolean parameter is for.
        BlockEntity mcTile = world.getBlockEntity(pos);
        if (mcTile instanceof BuilderTileEntity) {
            ATileEntityBase<?> tile = ((BuilderTileEntity) mcTile).tileEntity;
            if (tile != null) {
                return Shapes.create(WrapperWorld.convertWithOffset(tile.boundingBox, -pos.getX(), -pos.getY(), -pos.getZ()));
            }
        }
        //Return empty here, since we don't every want to be considered a full block as it does bad lighting.
        //When we get our TE data, then we can use that for the actual collision.
        return Shapes.empty();

    }
}
