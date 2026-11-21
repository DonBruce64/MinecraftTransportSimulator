package mcinterface1211.mixin.flashback;

import mcinterface1211.FlashbackCompat;
import mcinterface1211.FlashbackCompatUI;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.moulberry.flashback.editor.ui.ReplayUI", remap = false)
public class FlashbackReplayUIMixin {
    @Inject(
            method = "drawOverlayInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/flashback/editor/ui/windows/WindowType;renderAll()V",
                    shift = At.Shift.AFTER
            ),
            remap = false,
            require = 0
    )
    private static void mts$renderAll(CallbackInfo ci) {
        FlashbackCompatUI.renderVehicleList();
    }

    @Inject(method = "getLookTarget", at = @At("HEAD"), remap = false)
    private static void mts$getLookTargetHead(CallbackInfoReturnable<HitResult> cir) {
        FlashbackCompat.pickingActive = true;
    }

    @Inject(method = "getLookTarget", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void mts$getLookTargetReturn(CallbackInfoReturnable<HitResult> cir) {
        FlashbackCompat.pickingActive = false;
        HitResult hit = FlashbackCompat.pickVehicle(cir.getReturnValue());
        if (hit != null) {
            cir.setReturnValue(hit);
        }
    }
}
