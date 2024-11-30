package mcinterface1182;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
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
        super(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(block.hardness, block.blastResistance).noOcclusion());
        this.block = block;
    }

    BuilderBlock(ABlockBase block, BlockBehaviour.Properties properties) {
        super(properties);
        this.block = block;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        //Forward the breaking call to the block to allow for breaking logic.
        block.onBroken(WrapperWorld.getWrapperFor(world), new Point3D(pos.getX(), pos.getY(), pos.getZ()));
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (block instanceof BlockCollision) {
            return Shapes.create(WrapperWorld.convert(((BlockCollision) block).blockBounds));
        } else {
            return super.getShape(state, world, pos, context);
        }
    }
}
