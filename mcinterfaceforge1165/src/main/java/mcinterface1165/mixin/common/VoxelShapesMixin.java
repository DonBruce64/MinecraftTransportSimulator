package mcinterface1165.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1165.WrapperAABBCollective;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {
    @Inject(at = @At(value = "NEW"), method = "create(Lnet/minecraft/util/math/AxisAlignedBB;)Lnet/minecraft/util/math/shapes/VoxelShape;", cancellable = true)
    private void create_Inject(AxisAlignedBB box, CallbackInfoReturnable<VoxelShape> ci) {
        if (box instanceof WrapperAABBCollective) {
            //Create a new VoxelShapesArrayMixin, set the Collective to the box param, and return that value with the callback.
            //FIXME how to only invoice this on the return call for the array in VoxelShapes?
            //FIXME make the BLOCK paramter accessable and ref that here.
            //VoxelShapeArray array = new VoxelShapeArray(BLOCK.shape, new double[]{box.minX, box.maxX}, new double[]{box.minY, box.maxY}, new double[]{box.minZ, box.maxZ});
            //((VoxelShapeArrayMixin) array).collection = (WrapperAABBCollective) box;
            //ci.setReturnValue(array); 
        } else {
            //Do nothing: let the code return the standard VoxelShapes array.
        }
        //FIXME delete later, keep as reference for now.
    }
}
