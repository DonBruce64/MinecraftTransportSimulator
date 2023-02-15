package mcinterface1165.mixin.common;

import mcinterface1165.WrapperAABBCollective;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {
    @Inject(at = @At(value = "RETURN", target = "Lnet/minecraft/util/math/shapes/VoxelShapeArray;<init>(Lnet/minecraft/util/math/shapes/VoxelShapePart;[D[D[D)V"), method = "create", cancellable = true)
    private static void modShapeArray(AxisAlignedBB box, CallbackInfoReturnable<VoxelShape> cir) {
        if (box instanceof WrapperAABBCollective) {
            System.out.println("MTS entity");
        } else {
            System.out.println("Normal entity");
        }
        //FIXME delete later, keep as reference for now.
    }
}
