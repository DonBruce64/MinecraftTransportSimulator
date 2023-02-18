package mcinterface1165.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import mcinterface1165.WrapperAABBCollective;
import net.minecraft.util.math.shapes.VoxelShapeArray;

@Mixin(VoxelShapeArray.class)
public class VoxelShapeArrayMixin {
    //Just need to make a new variable here, we assign it later.
    public WrapperAABBCollective collection;

    /*
    @Inject(at = @At(value = "HEAD"), method = "collide(Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/vector/Vector3d;")
    private void collide_Inject(Vector3d pVec, CallbackInfoReturnable<Vector3d> ci) {
        Entity entity = (Entity) ((Object) this);
    
        Iterator<VoxelShape> iterator = pPotentialHits.getStream().iterator();
        while (iterator.hasNext()) {
            VoxelShape shape = iterator.next();
            if (shape instanceof WrapperVoxelShape) {
    
            }
        }
        //FIXME delete later, keep as reference for now.
        //InterfaceEventsEntityRendering.adjustCamera(matrixStack, partialTicks);
    }
    */
}
