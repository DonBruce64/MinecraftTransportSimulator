package mcinterface1211.mixin.flashback;

import mcinterface1211.FlashbackCompat;
import mcinterface1211.FlashbackCompatUI;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.moulberry.flashback.editor.ui.windows.SelectedEntityPopup", remap = false)
public class FlashbackSelectedEntityPopup {
    @Inject(
            method = "render",
            at = @At(value = "HEAD"),
            remap = false,
            require = 0
    )
    private static void mts$renderHead(Entity entity, @Coerce Object editorState, CallbackInfo ci) {
        FlashbackCompat.markSelected(entity);
    }

    @Inject(
            method = "render",
            at = @At(value = "TAIL"),
            remap = false,
            require = 0
    )
    private static void mts$renderTail(Entity entity, @Coerce Object editorState, CallbackInfo ci) {
        FlashbackCompatUI.renderSelectedVehicleUI(entity);
    }
}
